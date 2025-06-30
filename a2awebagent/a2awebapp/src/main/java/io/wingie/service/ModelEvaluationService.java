package io.wingie.service;

import io.wingie.entity.*;
import io.wingie.repository.ModelEvaluationRepository;
import io.wingie.repository.EvaluationTaskRepository;
import io.wingie.repository.EvaluationScreenshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            List<BenchmarkTask> benchmarkTasks = benchmarkService.getBenchmarkTasks(benchmarkName);
            createEvaluationTasks(evaluationId, benchmarkTasks);
            
            // Update total tasks count
            evaluation.setTotalTasks(benchmarkTasks.size());
            evaluationRepository.save(evaluation);
            
            // Start async execution
            CompletableFuture<Void> future = executeEvaluationAsync(evaluation);
            runningEvaluations.put(evaluationId, future);
            
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
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeEvaluation(evaluation);
            } catch (Exception e) {
                log.error("Async evaluation execution failed for evaluation: {}", evaluationId, e);
                handleEvaluationFailure(evaluation, "Async execution error: " + e.getMessage());
            }
        });
        
        // Remove from tracking when completed
        future.whenComplete((result, throwable) -> {
            runningEvaluations.remove(evaluationId);
            if (throwable != null) {
                log.error("Evaluation {} completed with error", evaluationId, throwable);
            } else {
                log.info("Evaluation {} completed successfully", evaluationId);
            }
        });
        
        return future;
    }

    /**
     * Execute the evaluation synchronously
     */
    private void executeEvaluation(ModelEvaluation evaluation) {
        String evaluationId = evaluation.getEvaluationId();
        
        try {
            // Mark evaluation as started
            updateEvaluationProgress(evaluation, EvaluationStatus.RUNNING, "Evaluation started", 0);
            
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
                    
                    // Execute the task
                    boolean success = executeTask(task, evaluation);
                    
                    completedTasks++;
                    if (success) {
                        successfulTasks++;
                    }
                    
                    // Update progress
                    int progressPercent = (completedTasks * 100) / tasks.size();
                    String progressMessage = String.format("Completed %d/%d tasks (%d successful)", 
                                                         completedTasks, tasks.size(), successfulTasks);
                    updateEvaluationProgress(evaluation, EvaluationStatus.RUNNING, progressMessage, progressPercent);
                    
                } catch (Exception e) {
                    log.error("Task execution failed for task {} in evaluation {}", task.getTaskId(), evaluationId, e);
                    task.markAsFailed("Task execution error: " + e.getMessage());
                    taskRepository.save(task);
                    completedTasks++;
                }
            }
            
            // Mark evaluation as completed
            markEvaluationCompleted(evaluation);
            
        } catch (EvaluationCancelledException e) {
            log.info("Evaluation {} was cancelled: {}", evaluationId, e.getMessage());
            markEvaluationCancelled(evaluation);
        } catch (Exception e) {
            log.error("Evaluation {} failed with error", evaluationId, e);
            handleEvaluationFailure(evaluation, e.getMessage());
        }
    }

    /**
     * Execute a single evaluation task
     */
    private boolean executeTask(EvaluationTask task, ModelEvaluation evaluation) {
        String taskId = task.getTaskId();
        
        try {
            // Mark task as started
            task.markAsStarted();
            taskRepository.save(task);
            
            // Configure the model for this evaluation
            configureModelForEvaluation(evaluation);
            
            // Execute the web browsing task using the existing processor
            TaskExecution webTask = createWebTaskFromEvaluationTask(task, evaluation);
            String result = webBrowsingProcessor.processWebBrowsing(webTask);
            
            // Evaluate the result
            boolean success = evaluateTaskResult(task, result);
            double score = calculateTaskScore(task, result, success);
            
            // Mark task as completed
            task.markAsCompleted(result, success, score);
            taskRepository.save(task);
            
            // Copy screenshots from web task to evaluation task
            if (webTask.getScreenshots() != null && !webTask.getScreenshots().isEmpty()) {
                copyScreenshotsToEvaluationTask(task, webTask.getScreenshots());
            }
            
            log.debug("Task {} completed with success: {}, score: {}", taskId, success, score);
            return success;
            
        } catch (Exception e) {
            log.error("Failed to execute task {}", taskId, e);
            task.markAsFailed("Task execution failed: " + e.getMessage());
            taskRepository.save(task);
            return false;
        }
    }

    /**
     * Create evaluation tasks from benchmark tasks
     */
    private void createEvaluationTasks(String evaluationId, List<BenchmarkTask> benchmarkTasks) {
        for (int i = 0; i < benchmarkTasks.size(); i++) {
            BenchmarkTask benchmarkTask = benchmarkTasks.get(i);
            
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
     * Copy screenshots from web task to evaluation task
     */
    private void copyScreenshotsToEvaluationTask(EvaluationTask evaluationTask, List<String> screenshotPaths) {
        for (int i = 0; i < screenshotPaths.size(); i++) {
            String screenshotPath = screenshotPaths.get(i);
            
            EvaluationScreenshot screenshot = EvaluationScreenshot.builder()
                .taskId(evaluationTask.getTaskId())
                .screenshotPath(screenshotPath)
                .stepNumber(i + 1)
                .stepDescription("Step " + (i + 1))
                .timestamp(LocalDateTime.now())
                .build();
            
            screenshotRepository.save(screenshot);
        }
    }

    /**
     * Update evaluation progress
     */
    private void updateEvaluationProgress(ModelEvaluation evaluation, EvaluationStatus status, 
                                        String message, Integer progressPercent) {
        String evaluationId = evaluation.getEvaluationId();
        
        try {
            // Update database
            evaluation.setStatus(status);
            if (status == EvaluationStatus.RUNNING && evaluation.getStartedAt() == null) {
                evaluation.setStartedAt(LocalDateTime.now());
            }
            
            // Update progress from tasks
            evaluation.updateProgress();
            evaluationRepository.save(evaluation);
            
            // Update Redis for real-time updates
            updateRedisEvaluationProgress(evaluationId, status, message, progressPercent, evaluation);
            
            log.debug("Updated progress for evaluation {}: {} - {}% - {}", evaluationId, status, progressPercent, message);
            
        } catch (Exception e) {
            log.error("Failed to update progress for evaluation {}", evaluationId, e);
        }
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
     * Mark evaluation as completed
     */
    private void markEvaluationCompleted(ModelEvaluation evaluation) {
        evaluation.markAsCompleted();
        evaluationRepository.save(evaluation);
        
        // Update Redis
        updateRedisEvaluationProgress(evaluation.getEvaluationId(), EvaluationStatus.COMPLETED, 
                                    "Evaluation completed successfully", 100, evaluation);
        
        log.info("Evaluation {} completed successfully. Score: {}", 
                evaluation.getEvaluationId(), evaluation.getScoreFormatted());
    }

    /**
     * Mark evaluation as cancelled
     */
    private void markEvaluationCancelled(ModelEvaluation evaluation) {
        evaluation.setStatus(EvaluationStatus.CANCELLED);
        evaluation.setCompletedAt(LocalDateTime.now());
        evaluationRepository.save(evaluation);
        
        updateRedisEvaluationProgress(evaluation.getEvaluationId(), EvaluationStatus.CANCELLED, 
                                    "Evaluation was cancelled", null, evaluation);
        
        log.info("Evaluation {} was cancelled", evaluation.getEvaluationId());
    }

    /**
     * Handle evaluation failure
     */
    private void handleEvaluationFailure(ModelEvaluation evaluation, String errorMessage) {
        evaluation.markAsFailed(errorMessage);
        evaluationRepository.save(evaluation);
        
        updateRedisEvaluationProgress(evaluation.getEvaluationId(), EvaluationStatus.FAILED, 
                                    "Evaluation failed: " + errorMessage, null, evaluation);
        
        log.error("Evaluation {} failed: {}", evaluation.getEvaluationId(), errorMessage);
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
        evaluationRepository.findById(evaluationId).ifPresent(evaluation -> {
            if (!evaluation.getStatus().isTerminal()) {
                markEvaluationCancelled(evaluation);
            }
        });
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
    public void cleanupTimedOutEvaluations() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(2);
            List<ModelEvaluation> timedOutEvaluations = evaluationRepository.findTimedOutEvaluations(timeoutThreshold);
            
            for (ModelEvaluation evaluation : timedOutEvaluations) {
                log.warn("Marking stuck evaluation as failed: {}", evaluation.getEvaluationId());
                handleEvaluationFailure(evaluation, "Evaluation exceeded maximum execution time");
                
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

    // Custom exceptions
    public static class EvaluationCancelledException extends RuntimeException {
        public EvaluationCancelledException(String message) {
            super(message);
        }
    }
}