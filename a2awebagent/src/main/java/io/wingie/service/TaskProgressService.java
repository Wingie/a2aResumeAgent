package io.wingie.service;

import io.wingie.entity.TaskExecution;
import io.wingie.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TaskProgressService {

    private final TaskExecutionRepository taskRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    public TaskProgressService(TaskExecutionRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    private static final String REDIS_PROGRESS_PREFIX = "task:progress:";

    public void updateProgress(TaskExecution task, int progressPercent, String message) {
        try {
            String taskId = task.getTaskId();
            
            // Update task entity
            task.setProgressPercent(progressPercent);
            task.setProgressMessage(message);
            task.setUpdated(LocalDateTime.now());
            
            // Save to database
            taskRepository.save(task);
            
            // Update Redis for real-time updates (if available)
            if (redisTemplate != null) {
                Map<String, Object> progressData = Map.of(
                    "taskId", taskId,
                    "status", task.getStatus().name(),
                    "message", message,
                    "progressPercent", progressPercent,
                    "screenshots", task.getScreenshots() != null ? task.getScreenshots() : List.of(),
                    "timestamp", LocalDateTime.now().toString()
                );
                
                redisTemplate.opsForValue().set(REDIS_PROGRESS_PREFIX + taskId, progressData, 1, TimeUnit.HOURS);
                redisTemplate.convertAndSend("task:progress", progressData);
            } else {
                log.debug("Redis not available - skipping real-time progress update for task {}", taskId);
            }
            
            log.debug("Updated progress for task {}: {}% - {}", taskId, progressPercent, message);
            
        } catch (Exception e) {
            log.error("Failed to update progress for task {}", task.getTaskId(), e);
        }
    }

    public void addScreenshot(TaskExecution task, String screenshotPath) {
        try {
            task.addScreenshot(screenshotPath);
            taskRepository.save(task);
            
            // Update Redis with new screenshot (if available)
            String taskId = task.getTaskId();
            if (redisTemplate != null) {
                Map<String, Object> progressData = Map.of(
                    "taskId", taskId,
                    "status", task.getStatus().name(),
                    "message", task.getProgressMessage() != null ? task.getProgressMessage() : "",
                    "progressPercent", task.getProgressPercent() != null ? task.getProgressPercent() : 0,
                    "screenshots", task.getScreenshots(),
                    "timestamp", LocalDateTime.now().toString(),
                    "newScreenshot", screenshotPath
                );
                
                redisTemplate.opsForValue().set(REDIS_PROGRESS_PREFIX + taskId, progressData, 1, TimeUnit.HOURS);
                redisTemplate.convertAndSend("task:progress", progressData);
            }
            
            log.debug("Added screenshot to task {}: {}", taskId, screenshotPath);
            
        } catch (Exception e) {
            log.error("Failed to add screenshot for task {}", task.getTaskId(), e);
        }
    }

    public Map<String, Object> getProgress(String taskId) {
        try {
            // Try Redis first (if available)
            if (redisTemplate != null) {
                Object progressData = redisTemplate.opsForValue().get(REDIS_PROGRESS_PREFIX + taskId);
                if (progressData instanceof Map) {
                    return (Map<String, Object>) progressData;
                }
            }
            
            // Fallback to database if Redis not available or no data in Redis
            return taskRepository.findById(taskId)
                .map(task -> Map.of(
                    "taskId", task.getTaskId(),
                    "status", task.getStatus().name(),
                    "message", task.getProgressMessage() != null ? task.getProgressMessage() : "",
                    "progressPercent", task.getProgressPercent() != null ? task.getProgressPercent() : 0,
                    "screenshots", task.getScreenshots() != null ? task.getScreenshots() : List.of(),
                    "timestamp", task.getUpdated().toString()
                ))
                .orElse(Map.of());
                
        } catch (Exception e) {
            log.error("Failed to get progress for task {}", taskId, e);
            return Map.of("error", "Failed to retrieve progress");
        }
    }
}