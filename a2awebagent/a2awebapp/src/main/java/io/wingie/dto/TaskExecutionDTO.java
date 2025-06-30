package io.wingie.dto;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for TaskExecution to avoid Hibernate LazyInitializationException
 * in API responses and SSE endpoints. This DTO safely serializes all fields including
 * the screenshots collection without requiring an active Hibernate session.
 */
@Data
@Builder
public class TaskExecutionDTO {
    
    private String taskId;
    private TaskStatus status;
    private String taskType;
    private String originalQuery;
    
    // Safe copy of screenshots - no lazy loading issues
    @Builder.Default
    private List<String> screenshots = new ArrayList<>();
    
    private String extractedResults;
    private String progressMessage;
    private Integer progressPercent;
    
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String errorDetails;
    private String requesterId;
    
    private Integer estimatedDurationSeconds;
    private Integer actualDurationSeconds;
    private Integer retryCount;
    private Integer maxRetries;
    private Integer timeoutSeconds;
    
    // Computed fields for convenience
    private String durationFormatted;
    private boolean completed;
    private boolean failed;
    private boolean running;
    private boolean queued;
    private boolean canRetry;
    
    /**
     * Create DTO from TaskExecution entity with safe screenshot loading
     */
    public static TaskExecutionDTO fromEntity(TaskExecution entity) {
        if (entity == null) {
            return null;
        }
        
        TaskExecutionDTOBuilder builder = TaskExecutionDTO.builder()
            .taskId(entity.getTaskId())
            .status(entity.getStatus())
            .taskType(entity.getTaskType())
            .originalQuery(entity.getOriginalQuery())
            .extractedResults(entity.getExtractedResults())
            .progressMessage(entity.getProgressMessage())
            .progressPercent(entity.getProgressPercent())
            .created(entity.getCreated())
            .updated(entity.getUpdated())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .errorDetails(entity.getErrorDetails())
            .requesterId(entity.getRequesterId())
            .estimatedDurationSeconds(entity.getEstimatedDurationSeconds())
            .actualDurationSeconds(entity.getActualDurationSeconds())
            .retryCount(entity.getRetryCount())
            .maxRetries(entity.getMaxRetries())
            .timeoutSeconds(entity.getTimeoutSeconds());
        
        // Safely handle screenshots collection
        try {
            if (entity.getScreenshots() != null) {
                builder.screenshots(new ArrayList<>(entity.getScreenshots()));
            }
        } catch (Exception e) {
            // If lazy loading fails, use empty list
            builder.screenshots(new ArrayList<>());
        }
        
        TaskExecutionDTO dto = builder.build();
        
        // Set computed fields
        dto.durationFormatted = entity.getDurationFormatted();
        dto.completed = entity.isCompleted();
        dto.failed = entity.isFailed();
        dto.running = entity.isRunning();
        dto.queued = entity.isQueued();
        dto.canRetry = entity.canRetry();
        
        return dto;
    }
    
    /**
     * Create DTO with eager-loaded screenshots
     */
    public static TaskExecutionDTO fromEntityWithScreenshots(TaskExecution entity, List<String> screenshots) {
        TaskExecutionDTO dto = fromEntity(entity);
        if (dto != null && screenshots != null) {
            dto.screenshots = new ArrayList<>(screenshots);
        }
        return dto;
    }
    
    /**
     * Create minimal DTO for list views (without screenshots for performance)
     */
    public static TaskExecutionDTO fromEntityMinimal(TaskExecution entity) {
        if (entity == null) {
            return null;
        }
        
        TaskExecutionDTO dto = TaskExecutionDTO.builder()
            .taskId(entity.getTaskId())
            .status(entity.getStatus())
            .taskType(entity.getTaskType())
            .originalQuery(truncateQuery(entity.getOriginalQuery()))
            .progressMessage(entity.getProgressMessage())
            .progressPercent(entity.getProgressPercent())
            .created(entity.getCreated())
            .updated(entity.getUpdated())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .errorDetails(truncateError(entity.getErrorDetails()))
            .requesterId(entity.getRequesterId())
            .actualDurationSeconds(entity.getActualDurationSeconds())
            .retryCount(entity.getRetryCount())
            .screenshots(new ArrayList<>()) // Empty for performance
            .build();
        
        // Set computed fields
        dto.durationFormatted = entity.getDurationFormatted();
        dto.completed = entity.isCompleted();
        dto.failed = entity.isFailed();
        dto.running = entity.isRunning();
        dto.queued = entity.isQueued();
        dto.canRetry = entity.canRetry();
        
        return dto;
    }
    
    /**
     * Get screenshot count without loading the full collection
     */
    public int getScreenshotCount() {
        return screenshots != null ? screenshots.size() : 0;
    }
    
    /**
     * Check if task has any screenshots
     */
    public boolean hasScreenshots() {
        return screenshots != null && !screenshots.isEmpty();
    }
    
    private static String truncateQuery(String query) {
        if (query == null) return null;
        return query.length() > 200 ? query.substring(0, 200) + "..." : query;
    }
    
    private static String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 500 ? error.substring(0, 500) + "..." : error;
    }
}