package io.wingie.service;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutorService {

    private final TaskExecutionRepository taskRepository;
    private final WebBrowsingTaskProcessor webBrowsingProcessor;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Track running tasks for cancellation
    private final Map<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();
    
    private static final String REDIS_PROGRESS_PREFIX = "task:progress:";
    private static final String REDIS_STATUS_PREFIX = "task:status:";

    @Async("taskExecutor")
    public CompletableFuture<Void> executeTaskAsync(TaskExecution task) {
        String taskId = task.getTaskId();
        log.info("Starting async execution for task: {} (type: {})", taskId, task.getTaskType());
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                log.error("Async task execution failed for task: {}", taskId, e);
                handleTaskFailure(task, "Async execution error: " + e.getMessage());
            }
        });
        
        runningTasks.put(taskId, future);
        
        // Remove from tracking when completed
        future.whenComplete((result, throwable) -> {
            runningTasks.remove(taskId);
            if (throwable != null) {
                log.error("Task {} completed with error", taskId, throwable);
            } else {
                log.info("Task {} completed successfully", taskId);
            }
        });
        
        return future;
    }

    private void executeTask(TaskExecution task) {
        String taskId = task.getTaskId();
        
        try {
            // Mark task as started
            updateTaskProgress(task, TaskStatus.RUNNING, "Task execution started", 0);
            
            // Check if task was cancelled before starting
            if (isTaskCancelled(taskId)) {
                log.info("Task {} was cancelled before execution", taskId);
                return;
            }
            
            // Route to appropriate processor based on task type
            String results = switch (task.getTaskType().toLowerCase()) {
                case "travel_search" -> webBrowsingProcessor.processTravelSearch(task);
                case "linkedin_search" -> webBrowsingProcessor.processLinkedInSearch(task);
                case "web_browsing" -> webBrowsingProcessor.processWebBrowsing(task);
                default -> throw new IllegalArgumentException("Unknown task type: " + task.getTaskType());
            };
            
            // Check for cancellation one more time before completing
            if (isTaskCancelled(taskId)) {
                log.info("Task {} was cancelled during execution", taskId);
                return;
            }
            
            // Mark task as completed
            markTaskCompleted(task, results);
            
        } catch (TaskCancelledException e) {
            log.info("Task {} was cancelled: {}", taskId, e.getMessage());
            markTaskCancelled(task);
        } catch (TaskTimeoutException e) {
            log.warn("Task {} timed out: {}", taskId, e.getMessage());
            markTaskTimedOut(task, e.getMessage());
        } catch (Exception e) {
            log.error("Task {} failed with error", taskId, e);
            handleTaskFailure(task, e.getMessage());
        }
    }

    private void updateTaskProgress(TaskExecution task, TaskStatus status, String message, Integer progressPercent) {
        String taskId = task.getTaskId();
        
        try {
            // Update database
            task.setStatus(status);
            task.setProgressMessage(message);
            if (progressPercent != null) {
                task.setProgressPercent(progressPercent);
            }
            task.setUpdated(LocalDateTime.now());
            
            if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
                task.setStartedAt(LocalDateTime.now());
            }
            
            taskRepository.save(task);
            
            // Update Redis for real-time updates
            updateRedisProgress(taskId, status, message, progressPercent, task.getScreenshots());
            
            log.debug("Updated progress for task {}: {} - {}% - {}", taskId, status, progressPercent, message);
            
        } catch (Exception e) {
            log.error("Failed to update progress for task {}", taskId, e);
        }
    }

    private void updateRedisProgress(String taskId, TaskStatus status, String message, Integer progressPercent, List<String> screenshots) {
        try {
            Map<String, Object> progressData = Map.of(
                "taskId", taskId,
                "status", status.name(),
                "message", message != null ? message : "",
                "progressPercent", progressPercent != null ? progressPercent : 0,
                "screenshots", screenshots != null ? screenshots : List.of(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            // Store progress in Redis with 1 hour expiry
            redisTemplate.opsForValue().set(REDIS_PROGRESS_PREFIX + taskId, progressData, 1, TimeUnit.HOURS);
            
            // Publish progress update to subscribers
            redisTemplate.convertAndSend("task:progress", progressData);
            
        } catch (Exception e) {
            log.error("Failed to update Redis progress for task {}", taskId, e);
        }
    }

    private void markTaskCompleted(TaskExecution task, String results) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setExtractedResults(results);
        task.setProgressPercent(100);
        task.setProgressMessage("Task completed successfully");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        
        if (task.getStartedAt() != null) {
            long duration = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).getSeconds();
            task.setActualDurationSeconds((int) duration);
        }
        
        taskRepository.save(task);
        
        // Update Redis
        updateRedisProgress(task.getTaskId(), TaskStatus.COMPLETED, "Task completed successfully", 100, task.getScreenshots());
        
        log.info("Task {} completed successfully in {}", task.getTaskId(), task.getDurationFormatted());
    }

    private void markTaskCancelled(TaskExecution task) {
        task.setStatus(TaskStatus.CANCELLED);
        task.setProgressMessage("Task was cancelled");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        
        taskRepository.save(task);
        updateRedisProgress(task.getTaskId(), TaskStatus.CANCELLED, "Task was cancelled", null, task.getScreenshots());
        
        log.info("Task {} was cancelled", task.getTaskId());
    }

    private void markTaskTimedOut(TaskExecution task, String errorMessage) {
        task.setStatus(TaskStatus.TIMEOUT);
        task.setErrorDetails(errorMessage);
        task.setProgressMessage("Task timed out");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        
        taskRepository.save(task);
        updateRedisProgress(task.getTaskId(), TaskStatus.TIMEOUT, "Task timed out", null, task.getScreenshots());
        
        log.warn("Task {} timed out: {}", task.getTaskId(), errorMessage);
    }

    private void handleTaskFailure(TaskExecution task, String errorMessage) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorDetails(errorMessage);
        task.setProgressMessage("Task failed: " + errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        
        if (task.getStartedAt() != null) {
            long duration = java.time.Duration.between(task.getStartedAt(), LocalDateTime.now()).getSeconds();
            task.setActualDurationSeconds((int) duration);
        }
        
        taskRepository.save(task);
        updateRedisProgress(task.getTaskId(), TaskStatus.FAILED, "Task failed: " + errorMessage, null, task.getScreenshots());
        
        log.error("Task {} failed: {}", task.getTaskId(), errorMessage);
    }

    public void cancelTask(String taskId) {
        log.info("Cancelling task: {}", taskId);
        
        // Mark in Redis as cancelled for immediate feedback
        redisTemplate.opsForValue().set(REDIS_STATUS_PREFIX + taskId, "CANCELLED", 1, TimeUnit.HOURS);
        
        // Cancel the running CompletableFuture if exists
        CompletableFuture<Void> runningTask = runningTasks.get(taskId);
        if (runningTask != null) {
            runningTask.cancel(true);
            runningTasks.remove(taskId);
            log.info("Cancelled running future for task: {}", taskId);
        }
        
        // Update database record
        taskRepository.findById(taskId).ifPresent(task -> {
            if (!task.getStatus().isTerminal()) {
                markTaskCancelled(task);
            }
        });
    }

    private boolean isTaskCancelled(String taskId) {
        try {
            String status = (String) redisTemplate.opsForValue().get(REDIS_STATUS_PREFIX + taskId);
            return "CANCELLED".equals(status);
        } catch (Exception e) {
            log.debug("Error checking cancellation status for task {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    // Scheduled cleanup tasks
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupTimedOutTasks() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
            List<TaskExecution> timedOutTasks = taskRepository.findTimedOutTasks(timeoutThreshold);
            
            for (TaskExecution task : timedOutTasks) {
                log.warn("Marking stuck task as timed out: {}", task.getTaskId());
                markTaskTimedOut(task, "Task exceeded maximum execution time");
                
                // Cancel if still in running tasks map
                CompletableFuture<Void> runningTask = runningTasks.get(task.getTaskId());
                if (runningTask != null) {
                    runningTask.cancel(true);
                    runningTasks.remove(task.getTaskId());
                }
            }
            
            if (!timedOutTasks.isEmpty()) {
                log.info("Cleaned up {} timed out tasks", timedOutTasks.size());
            }
            
        } catch (Exception e) {
            log.error("Error during timeout cleanup", e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldTasks() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7); // Keep 7 days
            List<TaskExecution> oldTasks = taskRepository.findTasksForCleanup(cutoffTime);
            
            if (!oldTasks.isEmpty()) {
                log.info("Cleaning up {} old completed tasks", oldTasks.size());
                
                // Remove from Redis
                for (TaskExecution task : oldTasks) {
                    redisTemplate.delete(REDIS_PROGRESS_PREFIX + task.getTaskId());
                    redisTemplate.delete(REDIS_STATUS_PREFIX + task.getTaskId());
                }
                
                // Keep in database for audit purposes but could delete if needed
                // taskRepository.deleteAll(oldTasks);
                
                log.info("Cleaned up Redis data for {} old tasks", oldTasks.size());
            }
            
        } catch (Exception e) {
            log.error("Error during old task cleanup", e);
        }
    }

    // Get current task statistics
    public Map<String, Object> getTaskStats() {
        try {
            return Map.of(
                "runningTasks", runningTasks.size(),
                "queuedTasks", taskRepository.countByStatus(TaskStatus.QUEUED),
                "completedToday", getCompletedTasksToday(),
                "failedToday", getFailedTasksToday(),
                "averageExecutionTime", getAverageExecutionTime()
            );
        } catch (Exception e) {
            log.error("Error getting task stats", e);
            return Map.of("error", e.getMessage());
        }
    }

    private long getCompletedTasksToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return taskRepository.findCompletedTasksBetween(startOfDay, LocalDateTime.now()).size();
    }

    private long getFailedTasksToday() {
        // This is a simplified implementation - could be enhanced with proper date filtering
        return taskRepository.countByStatus(TaskStatus.FAILED);
    }

    private double getAverageExecutionTime() {
        return taskRepository.getAverageDurationForTaskType("travel_search").orElse(0.0);
    }

    // Custom exceptions
    public static class TaskCancelledException extends RuntimeException {
        public TaskCancelledException(String message) {
            super(message);
        }
    }

    public static class TaskTimeoutException extends RuntimeException {
        public TaskTimeoutException(String message) {
            super(message);
        }
    }
}