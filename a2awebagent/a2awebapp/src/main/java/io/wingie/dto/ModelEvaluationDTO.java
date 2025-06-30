package io.wingie.dto;

import io.wingie.entity.EvaluationStatus;
import io.wingie.entity.ModelEvaluation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for ModelEvaluation to avoid Hibernate LazyInitializationException
 * in API responses and SSE endpoints. This DTO safely serializes all fields including
 * computed metrics without requiring an active Hibernate session.
 */
@Data
@Builder
public class ModelEvaluationDTO {
    
    private String evaluationId;
    private String modelName;
    private String modelProvider;
    private String benchmarkName;
    private String benchmarkVersion;
    private EvaluationStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer successfulTasks;
    private Integer failedTasks;
    private Double progressPercent;
    
    private Double overallScore;
    private Double successRate;
    private Long totalExecutionTimeSeconds;
    
    private String configuration;
    private String environmentInfo;
    private String errorMessage;
    private String initiatedBy;
    
    // Computed fields for convenience
    private String durationFormatted;
    private String scoreFormatted;
    private String progressFormatted;
    private boolean completed;
    private boolean failed;
    private boolean running;
    private boolean queued;
    
    /**
     * Create DTO from ModelEvaluation entity without loading tasks collection
     */
    public static ModelEvaluationDTO fromEntity(ModelEvaluation entity) {
        if (entity == null) {
            return null;
        }
        
        ModelEvaluationDTO dto = ModelEvaluationDTO.builder()
            .evaluationId(entity.getEvaluationId())
            .modelName(entity.getModelName())
            .modelProvider(entity.getModelProvider())
            .benchmarkName(entity.getBenchmarkName())
            .benchmarkVersion(entity.getBenchmarkVersion())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .totalTasks(entity.getTotalTasks())
            .completedTasks(entity.getCompletedTasks())
            .successfulTasks(entity.getSuccessfulTasks())
            .failedTasks(entity.getFailedTasks())
            .progressPercent(entity.getProgressPercent())
            .overallScore(entity.getOverallScore())
            .successRate(entity.getSuccessRate())
            .totalExecutionTimeSeconds(entity.getTotalExecutionTimeSeconds())
            .configuration(truncateConfiguration(entity.getConfiguration()))
            .environmentInfo(entity.getEnvironmentInfo())
            .errorMessage(truncateError(entity.getErrorMessage()))
            .initiatedBy(entity.getInitiatedBy())
            .build();
        
        // Set computed fields safely
        try {
            dto.durationFormatted = entity.getDurationFormatted();
            dto.scoreFormatted = entity.getScoreFormatted();
            dto.progressFormatted = dto.getProgressPercentFormatted();
            dto.completed = entity.isCompleted();
            dto.failed = entity.isFailed();
            dto.running = entity.isRunning();
            dto.queued = entity.isQueued();
        } catch (Exception e) {
            // If any computed field fails, use safe defaults
            dto.durationFormatted = "Unknown";
            dto.scoreFormatted = "N/A";
            dto.progressFormatted = "0%";
            dto.completed = entity.getStatus() == EvaluationStatus.COMPLETED;
            dto.failed = entity.getStatus() == EvaluationStatus.FAILED;
            dto.running = entity.getStatus() == EvaluationStatus.RUNNING;
            dto.queued = entity.getStatus() == EvaluationStatus.QUEUED;
        }
        
        return dto;
    }
    
    /**
     * Create minimal DTO for list views (with truncated text fields)
     */
    public static ModelEvaluationDTO fromEntityMinimal(ModelEvaluation entity) {
        if (entity == null) {
            return null;
        }
        
        ModelEvaluationDTO dto = ModelEvaluationDTO.builder()
            .evaluationId(entity.getEvaluationId())
            .modelName(entity.getModelName())
            .modelProvider(entity.getModelProvider())
            .benchmarkName(entity.getBenchmarkName())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .startedAt(entity.getStartedAt())
            .completedAt(entity.getCompletedAt())
            .totalTasks(entity.getTotalTasks())
            .completedTasks(entity.getCompletedTasks())
            .successfulTasks(entity.getSuccessfulTasks())
            .progressPercent(entity.getProgressPercent())
            .overallScore(entity.getOverallScore())
            .successRate(entity.getSuccessRate())
            .totalExecutionTimeSeconds(entity.getTotalExecutionTimeSeconds())
            .initiatedBy(entity.getInitiatedBy())
            // Skip large text fields for minimal version
            .build();
        
        // Set basic computed fields safely
        try {
            dto.durationFormatted = entity.getDurationFormatted();
            dto.scoreFormatted = entity.getScoreFormatted();
            dto.progressFormatted = dto.getProgressPercentFormatted();
            dto.completed = entity.isCompleted();
            dto.failed = entity.isFailed();
            dto.running = entity.isRunning();
            dto.queued = entity.isQueued();
        } catch (Exception e) {
            // Use safe defaults if computation fails
            dto.durationFormatted = computeDurationSafely(entity);
            dto.scoreFormatted = computeScoreSafely(entity);
            dto.progressFormatted = computeProgressSafely(entity);
            dto.completed = entity.getStatus() == EvaluationStatus.COMPLETED;
            dto.failed = entity.getStatus() == EvaluationStatus.FAILED;
            dto.running = entity.getStatus() == EvaluationStatus.RUNNING;
            dto.queued = entity.getStatus() == EvaluationStatus.QUEUED;
        }
        
        return dto;
    }
    
    /**
     * Get execution time in human readable format
     */
    public String getExecutionTimeFormatted() {
        if (totalExecutionTimeSeconds == null || totalExecutionTimeSeconds == 0) {
            return "N/A";
        }
        
        long seconds = totalExecutionTimeSeconds;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }
    
    /**
     * Get progress percentage as string
     */
    public String getProgressPercentFormatted() {
        if (progressPercent == null) {
            return "0%";
        }
        return String.format("%.1f%%", progressPercent);
    }
    
    /**
     * Check if evaluation is in a terminal state
     */
    public boolean isTerminal() {
        return status != null && (
            status == EvaluationStatus.COMPLETED ||
            status == EvaluationStatus.FAILED ||
            status == EvaluationStatus.CANCELLED
        );
    }
    
    /**
     * Get task completion ratio as string
     */
    public String getTaskCompletionRatio() {
        if (totalTasks == null || totalTasks == 0) {
            return "0/0";
        }
        int completed = completedTasks != null ? completedTasks : 0;
        return completed + "/" + totalTasks;
    }
    
    private static String truncateConfiguration(String config) {
        if (config == null) return null;
        return config.length() > 200 ? config.substring(0, 200) + "..." : config;
    }
    
    private static String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 500 ? error.substring(0, 500) + "..." : error;
    }
    
    private static String computeDurationSafely(ModelEvaluation entity) {
        if (entity.getTotalExecutionTimeSeconds() == null) {
            return "N/A";
        }
        long seconds = entity.getTotalExecutionTimeSeconds();
        return seconds < 60 ? seconds + "s" : (seconds / 60) + "m " + (seconds % 60) + "s";
    }
    
    private static String computeScoreSafely(ModelEvaluation entity) {
        if (entity.getOverallScore() == null) {
            return "N/A";
        }
        return String.format("%.1f", entity.getOverallScore());
    }
    
    private static String computeProgressSafely(ModelEvaluation entity) {
        double progressPercent = entity.getProgressPercent();
        return String.format("%.1f%%", progressPercent);
    }
}