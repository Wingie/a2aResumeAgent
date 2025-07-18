package io.wingie.service;

import io.wingie.entity.*;
import io.wingie.repository.ModelEvaluationRepository;
import io.wingie.repository.EvaluationTaskRepository;
import io.wingie.repository.EvaluationScreenshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ModelEvaluationService {

    private final ModelEvaluationRepository evaluationRepository;
    private final EvaluationTaskRepository taskRepository;
    private final EvaluationScreenshotRepository screenshotRepository;
    private final BenchmarkDefinitionService benchmarkService;
    private final WebBrowsingTaskProcessor webBrowsingProcessor;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // Self-injection to get proxied instance for transaction management
    @Autowired
    @Lazy
    private ModelEvaluationService self;
    
    public ModelEvaluationService(ModelEvaluationRepository evaluationRepository,
                                 EvaluationTaskRepository taskRepository,
                                 EvaluationScreenshotRepository screenshotRepository,
                                 BenchmarkDefinitionService benchmarkService,
                                 WebBrowsingTaskProcessor webBrowsingProcessor) {
        this.evaluationRepository = evaluationRepository;
        this.taskRepository = taskRepository;
        this.screenshotRepository = screenshotRepository;
        this.benchmarkService = benchmarkService;
        this.webBrowsingProcessor = webBrowsingProcessor;
    }
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // Track running evaluations for cancellation and monitoring
    private final Map<String, CompletableFuture<Void>> runningEvaluations = new ConcurrentHashMap<>();
    
    private static final String REDIS_EVALUATION_PROGRESS_PREFIX = "evaluation:progress:";
    private static final String REDIS_EVALUATION_STATUS_PREFIX = "evaluation:status:";

    /**
     * Start a new model evaluation with the specified parameters
     */
    @Transactional("primaryTransactionManager")
    public String startEvaluation(String modelName, String modelProvider, String benchmarkName, 
                                 String initiatedBy, Map<String, Object> configuration) {
        String evaluationId = UUID.randomUUID().toString();
        
        log.info("Starting evaluation {} for model {} on benchmark {}", evaluationId, modelName, benchmarkName);
        
        try {
            // Create the evaluation record
            ModelEvaluation evaluation = ModelEvaluation.builder()
                .evaluationId(evaluationId)
                .modelName(modelName)
                .modelProvider(modelProvider)
                .benchmarkName(benchmarkName)
                .benchmarkVersion(benchmarkService.getBenchmarkVersion(benchmarkName))
                .status(EvaluationStatus.QUEUED)
                .initiatedBy(initiatedBy)
                .configuration(configuration != null ? configuration.toString() : null)
                .environmentInfo(getEnvironmentInfo())
                .createdAt(LocalDateTime.now())  // Explicitly set createdAt to avoid validation error
                .build();
            
            evaluationRepository.save(evaluation);
            
            // Get benchmark tasks and create evaluation tasks
            List<BenchmarkTaskTemplate> benchmarkTasks = benchmarkService.getBenchmarkTasks(benchmarkName);
            createEvaluationTasks(evaluationId, benchmarkTasks);
            
            // Update total tasks count
            evaluation.setTotalTasks(benchmarkTasks.size());
            evaluationRepository.save(evaluation);
            
            // Ensure all changes are flushed before async execution
            entityManager.flush();
            
            // Start async execution after transaction commits using safe synchronization
            safeRegisterTransactionSynchronization(() -> {
                // Load evaluation with tasks to avoid LazyInitializationException in async context
                ModelEvaluation evaluationWithTasks = loadEvaluationWithTasks(evaluationId);
                if (evaluationWithTasks != null) {
                    CompletableFuture<Void> future = executeEvaluationAsync(evaluationWithTasks);
                    runningEvaluations.put(evaluationId, future);
                } else {
                    log.error("Failed to load evaluation {} with tasks for async execution", evaluationId);
                }
            }, "start async evaluation " + evaluationId);
            
            log.info("Evaluation {} queued successfully with {} tasks", evaluationId, benchmarkTasks.size());
            return evaluationId;
            
        } catch (Exception e) {
            log.error("Failed to start evaluation {} for model {}", evaluationId, modelName, e);
            // Mark evaluation as failed
            Optional<ModelEvaluation> evalOpt = evaluationRepository.findById(evaluationId);
            if (evalOpt.isPresent()) {
                ModelEvaluation eval = evalOpt.get();
                eval.markAsFailed("Failed to start evaluation: " + e.getMessage());
                evaluationRepository.save(eval);
            }
            throw new RuntimeException("Failed to start evaluation: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the evaluation asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> executeEvaluationAsync(ModelEvaluation evaluation) {
        String evaluationId = evaluation.getEvaluationId();
        log.info("Starting async execution for evaluation: {}", evaluationId);
        
        // Log transaction context in async thread for debugging
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean isSyncActive = TransactionSynchronizationManager.isSynchronizationActive();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        log.debug("Async thread transaction state for {}: actualTxActive={}, syncActive={}, txName={}", 
                 evaluationId, isActive, isSyncActive, txName);
        
        try {
            // Execute directly in the async thread context (avoid nested async)
            executeEvaluation(evaluation);
            log.info("Evaluation {} completed successfully", evaluationId);
            
            // Remove from tracking when completed successfully
            runningEvaluations.remove(evaluationId);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Async evaluation execution failed for evaluation: {}", evaluationId, e);
            self.handleEvaluationFailureWithTransaction(evaluationId, "Async execution error: " + e.getMessage());
            
            // Remove from tracking when failed
            runningEvaluations.remove(evaluationId);
            
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Execute the evaluation synchronously
     */
    private void executeEvaluation(ModelEvaluation evaluation) {
        String evaluationId = evaluation.getEvaluationId();
        
        try {
            // Mark evaluation as started
            self.updateEvaluationProgressWithTransaction(evaluationId, EvaluationStatus.RUNNING, "Evaluation started", 0);
            
            // Get all tasks for this evaluation
            List<EvaluationTask> tasks = taskRepository.findByEvaluationIdOrderByExecutionOrder(evaluationId);
            
            if (tasks.isEmpty()) {
                throw new IllegalStateException("No tasks found for evaluation: " + evaluationId);
            }
            
            log.info("Executing {} tasks for evaluation {}", tasks.size(), evaluationId);
            
            int completedTasks = 0;
            int successfulTasks = 0;
            
            // Execute tasks in order
            for (EvaluationTask task : tasks) {
                // Check if evaluation was cancelled
                if (isEvaluationCancelled(evaluationId)) {
                    log.info("Evaluation {} was cancelled during execution", evaluationId);
                    return;
                }
                
                try {
                    log.debug("Executing task {} for evaluation {}", task.getTaskName(), evaluationId);
                    
                    // Execute the task with proper transaction handling using self-injection
                    boolean success = self.executeTaskWithTransaction(task.getTaskId(), evaluationId);
                    
                    completedTasks++;
                    if (success) {
                        successfulTasks++;
                    }
                    
                    // Update progress with proper transaction isolation
                    int progressPercent = (completedTasks * 100) / tasks.size();
                    String progressMessage = String.format("Completed %d/%d tasks (%d successful)", 
                                                         completedTasks, tasks.size(), successfulTasks);
                    self.updateEvaluationProgressWithTransaction(evaluationId, EvaluationStatus.RUNNING, progressMessage, progressPercent);
                    
                } catch (Exception e) {
                    log.error("Task execution failed for task {} in evaluation {}", task.getTaskId(), evaluationId, e);
                    self.markTaskAsFailedWithTransaction(task.getTaskId(), "Task execution error: " + e.getMessage());
                    completedTasks++;
                }
            }
            
            // Mark evaluation as completed
            self.markEvaluationCompletedWithTransaction(evaluationId);
            
        } catch (EvaluationCancelledException e) {
            log.info("Evaluation {} was cancelled: {}", evaluationId, e.getMessage());
            self.markEvaluationCancelledWithTransaction(evaluationId);
        } catch (Exception e) {
            log.error("Evaluation {} failed with error", evaluationId, e);
            self.handleEvaluationFailureWithTransaction(evaluationId, e.getMessage());
        }
    }

    /**
     * Execute a single evaluation task with proper transaction boundaries
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW,
                  isolation = Isolation.READ_COMMITTED)
    public boolean executeTaskWithTransaction(String taskId, String evaluationId) {
        final boolean[] result = {false};
        
        // Debug transaction context
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        boolean isSyncActive = TransactionSynchronizationManager.isSynchronizationActive();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        log.debug("executeTaskWithTransaction transaction state: actualTxActive={}, syncActive={}, txName={}", 
                 isActive, isSyncActive, txName);
        
        if (!isActive) {
            log.error("No transaction is active in executeTaskWithTransaction for task {} - this should not happen", taskId);
            throw new IllegalStateException("No transaction active in executeTaskWithTransaction");
        }
        
        retryOnOptimisticLock(() -> {
            // Load task with pessimistic lock to prevent concurrent modifications
            EvaluationTask task = entityManager.find(EvaluationTask.class, taskId, LockModeType.PESSIMISTIC_WRITE);
            if (task == null) {
                throw new IllegalStateException("Task not found: " + taskId);
            }
            
            // Load evaluation for model configuration
            ModelEvaluation evaluation = entityManager.find(ModelEvaluation.class, evaluationId, LockModeType.PESSIMISTIC_READ);
            if (evaluation == null) {
                throw new IllegalStateException("Evaluation not found: " + evaluationId);
            }
            
            // Mark task as started
            task.markAsStarted();
            taskRepository.saveAndFlush(task);
            
            // Configure the model for this evaluation
            configureModelForEvaluation(evaluation);
            
            // Execute the web browsing task using the existing processor
            TaskExecution webTask = createWebTaskFromEvaluationTask(task, evaluation);
            String webResult;
            try {
                webResult = webBrowsingProcessor.processWebBrowsing(webTask);
            } catch (Exception e) {
                throw new RuntimeException("Web browsing task failed: " + e.getMessage(), e);
            }
            
            // Evaluate the result
            boolean success = evaluateTaskResult(task, webResult);
            double score = calculateTaskScore(task, webResult, success);
            
            // Mark task as completed
            task.markAsCompleted(webResult, success, score);
            taskRepository.saveAndFlush(task);
            
            // Copy screenshots in a separate transaction to avoid holding locks
            if (webTask.getScreenshots() != null && !webTask.getScreenshots().isEmpty()) {
                copyScreenshotsToEvaluationTaskInNewTransaction(task.getTaskId(), webTask.getScreenshots());
            }
            
            log.debug("Task {} completed with success: {}, score: {}", taskId, success, score);
            result[0] = success;
            
        }, "execute task " + taskId);
        
        return result[0];
    }

    /**
     * Copy screenshots to evaluation task in a new transaction
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW)
    public void copyScreenshotsToEvaluationTaskInNewTransaction(String taskId, List<String> screenshotPaths) {
        try {
            for (int i = 0; i < screenshotPaths.size(); i++) {
                String screenshotPath = screenshotPaths.get(i);
                
                EvaluationScreenshot screenshot = EvaluationScreenshot.builder()
                    .taskId(taskId)
                    .screenshotPath(screenshotPath)
                    .stepNumber(i + 1)
                    .stepDescription("Step " + (i + 1))
                    .timestamp(LocalDateTime.now())
                    .build();
                
                screenshotRepository.save(screenshot);
            }
            screenshotRepository.flush();
        } catch (Exception e) {
            log.error("Failed to copy screenshots for task {}", taskId, e);
            // Don't fail the task execution due to screenshot copy failure
        }
    }

    /**
     * Mark task as failed with proper transaction
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW)
    public void markTaskAsFailedWithTransaction(String taskId, String errorMessage) {
        try {
            EvaluationTask task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.markAsFailed(errorMessage);
                taskRepository.saveAndFlush(task);
            }
        } catch (Exception e) {
            log.error("Failed to mark task {} as failed", taskId, e);
        }
    }

    /**
     * Create evaluation tasks from benchmark tasks
     */
    private void createEvaluationTasks(String evaluationId, List<BenchmarkTaskTemplate> benchmarkTasks) {
        for (int i = 0; i < benchmarkTasks.size(); i++) {
            BenchmarkTaskTemplate benchmarkTask = benchmarkTasks.get(i);
            
            EvaluationTask evaluationTask = EvaluationTask.builder()
                .taskId(UUID.randomUUID().toString())
                .evaluationId(evaluationId)
                .taskName(benchmarkTask.getName())
                .taskDescription(benchmarkTask.getDescription())
                .prompt(benchmarkTask.getPrompt())
                .status(EvaluationTaskStatus.PENDING)
                .executionOrder(i + 1)
                .expectedResult(benchmarkTask.getExpectedResult())
                .maxScore(benchmarkTask.getMaxScore())
                .evaluationCriteria(benchmarkTask.getEvaluationCriteria())
                .taskCategory(benchmarkTask.getCategory())
                .difficultyLevel(benchmarkTask.getDifficultyLevel())
                .tags(String.join(",", benchmarkTask.getTags()))
                .timeoutSeconds(benchmarkTask.getTimeoutSeconds())
                .build();
            
            taskRepository.save(evaluationTask);
        }
    }

    /**
     * Configure the AI model for this evaluation
     */
    private void configureModelForEvaluation(ModelEvaluation evaluation) {
        // This would configure the tools4ai properties to use the specified model
        // For now, we'll log the configuration
        log.debug("Configuring model {} from provider {} for evaluation {}", 
                 evaluation.getModelName(), evaluation.getModelProvider(), evaluation.getEvaluationId());
        
        // In a real implementation, you would:
        // 1. Update the active model configuration
        // 2. Switch to the appropriate provider
        // 3. Set model-specific parameters
    }

    /**
     * Create a TaskExecution from an EvaluationTask for web browsing
     */
    private TaskExecution createWebTaskFromEvaluationTask(EvaluationTask evaluationTask, ModelEvaluation evaluation) {
        return TaskExecution.builder()
            .taskId(UUID.randomUUID().toString())
            .taskType("web_browsing")
            .originalQuery(evaluationTask.getPrompt())
            .status(TaskStatus.QUEUED)
            .requesterId("evaluation-" + evaluation.getEvaluationId())
            .timeoutSeconds(evaluationTask.getTimeoutSeconds())
            .build();
    }

    /**
     * Evaluate if a task result meets the expected criteria
     */
    private boolean evaluateTaskResult(EvaluationTask task, String actualResult) {
        // Simple implementation - in a real system, this would use AI to evaluate results
        String expectedResult = task.getExpectedResult();
        if (expectedResult == null || expectedResult.trim().isEmpty()) {
            // If no expected result, consider it successful if we got any result
            return actualResult != null && !actualResult.trim().isEmpty();
        }
        
        // For now, do a simple contains check
        return actualResult != null && 
               actualResult.toLowerCase().contains(expectedResult.toLowerCase());
    }

    /**
     * Calculate a score for the task based on the result
     */
    private double calculateTaskScore(EvaluationTask task, String result, boolean success) {
        if (!success) {
            return 0.0;
        }
        
        // Simple scoring - in a real system, this would be more sophisticated
        Double maxScore = task.getMaxScore();
        if (maxScore == null) {
            return success ? 1.0 : 0.0;
        }
        
        // For now, give full score if successful
        return maxScore;
    }

    /**
     * Update evaluation progress with proper transaction isolation
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW,
                  isolation = Isolation.READ_COMMITTED)
    public void updateEvaluationProgressWithTransaction(String evaluationId, EvaluationStatus status, 
                                                       String message, Integer progressPercent) {
        retryOnOptimisticLock(() -> {
            // Load evaluation using repository
            Optional<ModelEvaluation> evalOpt = evaluationRepository.findById(evaluationId);
            if (!evalOpt.isPresent()) {
                log.error("Evaluation not found for progress update: {}", evaluationId);
                return;
            }
            ModelEvaluation evaluation = evalOpt.get();
            
            // Update status
            evaluation.setStatus(status);
            if (status == EvaluationStatus.RUNNING && evaluation.getStartedAt() == null) {
                evaluation.setStartedAt(LocalDateTime.now());
            }
            
            // Update progress from tasks - this queries the database for fresh data
            List<EvaluationTask> tasks = taskRepository.findByEvaluationId(evaluationId);
            evaluation.setTasks(tasks);
            evaluation.updateProgress();
            
            // Save and flush to ensure changes are persisted
            evaluationRepository.saveAndFlush(evaluation);
            
            // Update Redis after database commit using safe synchronization
            safeRegisterTransactionSynchronization(() -> {
                updateRedisEvaluationProgress(evaluationId, status, message, progressPercent, evaluation);
            }, "update Redis progress for evaluation " + evaluationId);
            
            log.debug("Updated progress for evaluation {}: {} - {}% - {}", evaluationId, status, progressPercent, message);
        }, "update evaluation progress for " + evaluationId);
    }

    /**
     * Update Redis with evaluation progress
     */
    private void updateRedisEvaluationProgress(String evaluationId, EvaluationStatus status, String message, 
                                             Integer progressPercent, ModelEvaluation evaluation) {
        if (redisTemplate == null) {
            log.debug("Redis not available - skipping real-time progress update for evaluation {}", evaluationId);
            return;
        }
        
        try {
            Map<String, Object> progressData = Map.of(
                "evaluationId", evaluationId,
                "modelName", evaluation.getModelName(),
                "benchmarkName", evaluation.getBenchmarkName(),
                "status", status.name(),
                "message", message != null ? message : "",
                "progressPercent", progressPercent != null ? progressPercent : evaluation.getProgressPercent(),
                "completedTasks", evaluation.getCompletedTasks(),
                "totalTasks", evaluation.getTotalTasks(),
                "successfulTasks", evaluation.getSuccessfulTasks(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            // Store progress in Redis with 2 hour expiry
            redisTemplate.opsForValue().set(REDIS_EVALUATION_PROGRESS_PREFIX + evaluationId, progressData, 2, TimeUnit.HOURS);
            
            // Publish progress update to subscribers
            redisTemplate.convertAndSend("evaluation:progress", progressData);
            
        } catch (Exception e) {
            log.error("Failed to update Redis progress for evaluation {}", evaluationId, e);
        }
    }

    /**
     * Mark evaluation as completed with transaction
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW)
    public void markEvaluationCompletedWithTransaction(String evaluationId) {
        try {
            // Load evaluation with tasks to avoid LazyInitializationException
            ModelEvaluation evaluation = loadEvaluationWithTasks(evaluationId);
            if (evaluation == null) {
                log.error("Evaluation not found for completion: {}", evaluationId);
                return;
            }
            
            evaluation.markAsCompleted();
            evaluationRepository.saveAndFlush(evaluation);
            
            // Update Redis after commit using safe synchronization
            safeRegisterTransactionSynchronization(() -> {
                updateRedisEvaluationProgress(evaluationId, EvaluationStatus.COMPLETED, 
                                            "Evaluation completed successfully", 100, evaluation);
            }, "update Redis completion for evaluation " + evaluationId);
            
            log.info("Evaluation {} completed successfully. Score: {}", 
                    evaluationId, evaluation.getScoreFormatted());
        } catch (Exception e) {
            log.error("Failed to mark evaluation {} as completed", evaluationId, e);
            throw e;
        }
    }

    /**
     * Mark evaluation as cancelled with transaction
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW)
    public void markEvaluationCancelledWithTransaction(String evaluationId) {
        try {
            // Load evaluation with tasks to avoid LazyInitializationException
            ModelEvaluation evaluation = loadEvaluationWithTasks(evaluationId);
            if (evaluation == null) {
                log.error("Evaluation not found for cancellation: {}", evaluationId);
                return;
            }
            
            evaluation.setStatus(EvaluationStatus.CANCELLED);
            evaluation.setCompletedAt(LocalDateTime.now());
            evaluationRepository.saveAndFlush(evaluation);
            
            safeRegisterTransactionSynchronization(() -> {
                updateRedisEvaluationProgress(evaluationId, EvaluationStatus.CANCELLED, 
                                            "Evaluation was cancelled", null, evaluation);
            }, "update Redis cancellation for evaluation " + evaluationId);
            
            log.info("Evaluation {} was cancelled", evaluationId);
        } catch (Exception e) {
            log.error("Failed to mark evaluation {} as cancelled", evaluationId, e);
            throw e;
        }
    }

    /**
     * Handle evaluation failure with transaction
     */
    @Transactional(value = "primaryTransactionManager", 
                  propagation = Propagation.REQUIRES_NEW)
    public void handleEvaluationFailureWithTransaction(String evaluationId, String errorMessage) {
        try {
            // Load evaluation with tasks to avoid LazyInitializationException
            ModelEvaluation evaluation = loadEvaluationWithTasks(evaluationId);
            if (evaluation == null) {
                log.error("Evaluation not found for failure handling: {}", evaluationId);
                return;
            }
            
            evaluation.markAsFailed(errorMessage);
            evaluationRepository.saveAndFlush(evaluation);
            
            safeRegisterTransactionSynchronization(() -> {
                updateRedisEvaluationProgress(evaluationId, EvaluationStatus.FAILED, 
                                            "Evaluation failed: " + errorMessage, null, evaluation);
            }, "update Redis failure for evaluation " + evaluationId);
            
            log.error("Evaluation {} failed: {}", evaluationId, errorMessage);
        } catch (Exception e) {
            log.error("Failed to handle evaluation {} failure", evaluationId, e);
            throw e;
        }
    }

    /**
     * Cancel a running evaluation
     */
    public void cancelEvaluation(String evaluationId) {
        log.info("Cancelling evaluation: {}", evaluationId);
        
        // Mark in Redis as cancelled for immediate feedback
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(REDIS_EVALUATION_STATUS_PREFIX + evaluationId, "CANCELLED", 2, TimeUnit.HOURS);
        }
        
        // Cancel the running CompletableFuture if exists
        CompletableFuture<Void> runningEvaluation = runningEvaluations.get(evaluationId);
        if (runningEvaluation != null) {
            runningEvaluation.cancel(true);
            runningEvaluations.remove(evaluationId);
            log.info("Cancelled running future for evaluation: {}", evaluationId);
        }
        
        // Update database record
        self.markEvaluationCancelledWithTransaction(evaluationId);
    }

    /**
     * Check if evaluation was cancelled
     */
    private boolean isEvaluationCancelled(String evaluationId) {
        try {
            if (redisTemplate != null) {
                String status = (String) redisTemplate.opsForValue().get(REDIS_EVALUATION_STATUS_PREFIX + evaluationId);
                return "CANCELLED".equals(status);
            }
            return false;
        } catch (Exception e) {
            log.debug("Error checking cancellation status for evaluation {}: {}", evaluationId, e.getMessage());
            return false;
        }
    }

    /**
     * Retry mechanism for optimistic locking failures
     */
    private void retryOnOptimisticLock(Runnable operation, String operationDescription) {
        int maxAttempts = 3;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                operation.run();
                return; // Success, exit retry loop
                
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    log.error("Failed to {} after {} attempts due to optimistic locking", operationDescription, maxAttempts, e);
                    throw e;
                }
                
                log.warn("Optimistic locking failure on attempt {} for {}, retrying...", attempt, operationDescription);
                
                // Exponential backoff: wait 100ms, 200ms, 400ms...
                try {
                    Thread.sleep(100L * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
                
            } catch (Exception e) {
                // For non-optimistic locking exceptions, fail immediately
                log.error("Failed to {} on attempt {}", operationDescription, attempt + 1, e);
                throw e;
            }
        }
    }

    /**
     * Get environment information
     */
    private String getEnvironmentInfo() {
        return String.format("Java: %s, OS: %s, Time: %s", 
                           System.getProperty("java.version"),
                           System.getProperty("os.name"),
                           LocalDateTime.now());
    }

    /**
     * Get evaluation statistics
     */
    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Map<String, Object> getEvaluationStats() {
        try {
            return Map.of(
                "runningEvaluations", runningEvaluations.size(),
                "queuedEvaluations", evaluationRepository.countByStatus(EvaluationStatus.QUEUED),
                "completedToday", getCompletedEvaluationsToday(),
                "failedToday", evaluationRepository.countByStatus(EvaluationStatus.FAILED),
                "totalEvaluations", evaluationRepository.count(),
                "averageScore", getAverageScore(),
                "lastUpdated", LocalDateTime.now().toString()
            );
        } catch (Exception e) {
            log.error("Error getting evaluation stats", e);
            return Map.of("error", e.getMessage(), "lastUpdated", LocalDateTime.now().toString());
        }
    }

    private long getCompletedEvaluationsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return evaluationRepository.findCompletedEvaluationsBetween(startOfDay, LocalDateTime.now()).size();
    }

    private double getAverageScore() {
        // Calculate average score across all completed evaluations
        return evaluationRepository.findCompletedEvaluationsOrderedByScore()
            .stream()
            .filter(e -> e.getOverallScore() != null)
            .mapToDouble(ModelEvaluation::getOverallScore)
            .average()
            .orElse(0.0);
    }

    // Scheduled cleanup tasks
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    @Transactional(value = "primaryTransactionManager")
    public void cleanupTimedOutEvaluations() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(2);
            List<ModelEvaluation> timedOutEvaluations = evaluationRepository.findTimedOutEvaluations(timeoutThreshold);
            
            for (ModelEvaluation evaluation : timedOutEvaluations) {
                log.warn("Marking stuck evaluation as failed: {}", evaluation.getEvaluationId());
                self.handleEvaluationFailureWithTransaction(evaluation.getEvaluationId(), "Evaluation exceeded maximum execution time");
                
                // Cancel if still in running evaluations map
                CompletableFuture<Void> runningEvaluation = runningEvaluations.get(evaluation.getEvaluationId());
                if (runningEvaluation != null) {
                    runningEvaluation.cancel(true);
                    runningEvaluations.remove(evaluation.getEvaluationId());
                }
            }
            
            if (!timedOutEvaluations.isEmpty()) {
                log.info("Cleaned up {} timed out evaluations", timedOutEvaluations.size());
            }
            
        } catch (Exception e) {
            log.error("Error during evaluation timeout cleanup", e);
        }
    }

    // Process queued evaluations
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void processQueuedEvaluations() {
        try {
            List<ModelEvaluation> queuedEvaluations = 
                evaluationRepository.findByStatus(EvaluationStatus.QUEUED);
            
            for (ModelEvaluation evaluation : queuedEvaluations) {
                if (!runningEvaluations.containsKey(evaluation.getEvaluationId())) {
                    log.info("Starting queued evaluation: {}", evaluation.getEvaluationId());
                    
                    try {
                        // Load evaluation with tasks to avoid LazyInitializationException in async context
                        ModelEvaluation evaluationWithTasks = loadEvaluationWithTasks(evaluation.getEvaluationId());
                        if (evaluationWithTasks != null) {
                            // Start the evaluation asynchronously
                            CompletableFuture<Void> future = executeEvaluationAsync(evaluationWithTasks);
                            runningEvaluations.put(evaluation.getEvaluationId(), future);
                        } else {
                            log.error("Failed to load evaluation {} with tasks for async execution", evaluation.getEvaluationId());
                            self.handleEvaluationFailureWithTransaction(evaluation.getEvaluationId(), 
                                                                 "Failed to load evaluation with tasks for async execution");
                        }
                    } catch (Exception e) {
                        log.error("Failed to start evaluation {}: {}", evaluation.getEvaluationId(), e.getMessage());
                        // Handle failure in separate transaction to avoid affecting batch processing
                        self.handleEvaluationFailureWithTransaction(evaluation.getEvaluationId(), 
                                                             "Failed to start evaluation: " + e.getMessage());
                    }
                }
            }
            
            if (!queuedEvaluations.isEmpty()) {
                log.debug("Processed {} queued evaluations", queuedEvaluations.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing queued evaluations", e);
        }
    }

    /**
     * Load evaluation with tasks eagerly to avoid LazyInitializationException in async context.
     * This is needed because the evaluation entity becomes detached when passed to async execution.
     */
    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public ModelEvaluation loadEvaluationWithTasks(String evaluationId) {
        try {
            // Load evaluation with tasks using a query that fetches tasks eagerly
            ModelEvaluation evaluation = entityManager.createQuery(
                "SELECT e FROM ModelEvaluation e LEFT JOIN FETCH e.tasks WHERE e.evaluationId = :id", 
                ModelEvaluation.class)
                .setParameter("id", evaluationId)
                .getSingleResult();
            
            log.debug("Loaded evaluation {} with {} tasks for async execution", 
                     evaluationId, evaluation.getTasks().size());
            return evaluation;
        } catch (Exception e) {
            log.error("Failed to load evaluation {} with tasks: {}", evaluationId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Safely register transaction synchronization with fallback to immediate execution.
     * This prevents "Transaction synchronization is not active" errors in async contexts.
     */
    private void safeRegisterTransactionSynchronization(Runnable afterCommitAction, String description) {
        try {
            // Log current transaction state for debugging
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            boolean isSyncActive = TransactionSynchronizationManager.isSynchronizationActive();
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            
            log.debug("Transaction state check for '{}': actualTxActive={}, syncActive={}, txName={}", 
                     description, isActive, isSyncActive, txName);
            
            if (isSyncActive) {
                log.debug("Transaction synchronization is active, registering for {}", description);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.debug("Executing after commit action for {}", description);
                        try {
                            afterCommitAction.run();
                        } catch (Exception e) {
                            log.error("Error executing after commit action for {}: {}", description, e.getMessage(), e);
                        }
                    }
                    
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            log.debug("Transaction committed successfully for {}", description);
                        } else if (status == STATUS_ROLLED_BACK) {
                            log.warn("Transaction rolled back for {}", description);
                        } else {
                            log.warn("Transaction completed with unknown status {} for {}", status, description);
                        }
                    }
                });
            } else {
                log.debug("Transaction synchronization is not active, executing immediately for {}", description);
                // Execute immediately if not in transaction context
                try {
                    afterCommitAction.run();
                } catch (Exception e) {
                    log.error("Error executing immediate action for {}: {}", description, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in transaction synchronization setup for {}: {}", description, e.getMessage(), e);
            // Last resort: try to execute immediately
            try {
                afterCommitAction.run();
            } catch (Exception fallbackError) {
                log.error("Fallback execution also failed for {}: {}", description, fallbackError.getMessage(), fallbackError);
            }
        }
    }

    // Custom exceptions
    public static class EvaluationCancelledException extends RuntimeException {
        public EvaluationCancelledException(String message) {
            super(message);
        }
    }
}