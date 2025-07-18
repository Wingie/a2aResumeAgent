package io.wingie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluation_tasks", indexes = {
    @Index(name = "idx_eval_task_status", columnList = "status"),
    @Index(name = "idx_eval_task_name", columnList = "taskName"),
    @Index(name = "idx_eval_task_evaluation", columnList = "evaluationId"),
    @Index(name = "idx_eval_task_order", columnList = "executionOrder")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationTask {
    
    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;
    
    @Column(name = "evaluation_id", length = 36, nullable = false)
    @NotBlank
    private String evaluationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", insertable = false, updatable = false)
    private ModelEvaluation evaluation;
    
    @Column(name = "task_name", nullable = false, length = 200)
    @NotBlank
    private String taskName;
    
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;
    
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String prompt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private EvaluationTaskStatus status;
    
    @Column(name = "execution_order")
    private Integer executionOrder;
    
    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;
    
    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;
    
    @Column(name = "success")
    private Boolean success;
    
    @Column(name = "score")
    private Double score;
    
    @Column(name = "max_score")
    private Double maxScore;
    
    @Column(name = "execution_time_seconds")
    private Long executionTimeSeconds;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "evaluationTask", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvaluationScreenshot> screenshots = new ArrayList<>();
    
    // Scoring criteria and evaluation metadata
    @Column(name = "evaluation_criteria", columnDefinition = "TEXT")
    private String evaluationCriteria;
    
    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 2;
    
    @Column(name = "task_category", length = 50)
    private String taskCategory; // e.g., "navigation", "form_filling", "data_extraction"
    
    @Column(name = "difficulty_level")
    private Integer difficultyLevel; // 1-5 scale
    
    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated tags for categorization
    
    // User-controlled execution parameters
    @Column(name = "execution_parameters", columnDefinition = "TEXT")
    private String executionParametersJson; // JSON string of ExecutionParameters
    
    @Column(name = "max_steps")
    @Builder.Default
    private Integer maxSteps = 10; // Quick access field for queries
    
    @Column(name = "execution_mode", length = 20)
    @Builder.Default
    private String executionMode = "MULTI_STEP"; // ONE_SHOT, MULTI_STEP, AUTO
    
    @Column(name = "early_completion_allowed")
    @Builder.Default
    private Boolean earlyCompletionAllowed = true;
    
    @Column(name = "steps_completed")
    private Integer stepsCompleted; // Track actual steps executed
    
    @Column(name = "early_completion_triggered")
    private Boolean earlyCompletionTriggered; // Whether task completed early
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = EvaluationTaskStatus.PENDING;
        }
        if (success == null) {
            success = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 2;
        }
        if (timeoutSeconds == null) {
            timeoutSeconds = 300;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Auto-set timestamps based on status changes
        if (status == EvaluationTaskStatus.RUNNING && startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        
        if (status.isTerminal() && completedAt == null) {
            completedAt = LocalDateTime.now();
            
            // Calculate execution time
            if (startedAt != null) {
                executionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
            }
        }
    }
    
    // Convenience methods
    public boolean isCompleted() {
        return status == EvaluationTaskStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == EvaluationTaskStatus.FAILED;
    }
    
    public boolean isRunning() {
        return status == EvaluationTaskStatus.RUNNING;
    }
    
    public boolean isPending() {
        return status == EvaluationTaskStatus.PENDING;
    }
    
    public boolean canRetry() {
        return (isFailed() || status == EvaluationTaskStatus.TIMEOUT) && retryCount < maxRetries;
    }
    
    public void markAsStarted() {
        this.status = EvaluationTaskStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted(String result, boolean success, Double score) {
        this.status = EvaluationTaskStatus.COMPLETED;
        this.actualResult = result;
        this.success = success;
        this.score = score;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (startedAt != null) {
            this.executionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = EvaluationTaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.success = false;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (startedAt != null) {
            this.executionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public void markAsTimedOut() {
        this.status = EvaluationTaskStatus.TIMEOUT;
        this.errorMessage = "Task execution timed out after " + timeoutSeconds + " seconds";
        this.success = false;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (startedAt != null) {
            this.executionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addScreenshot(String screenshotPath, int stepNumber) {
        if (screenshots == null) {
            screenshots = new ArrayList<>();
        }
        EvaluationScreenshot screenshot = EvaluationScreenshot.builder()
            .taskId(this.taskId)
            .screenshotPath(screenshotPath)
            .stepNumber(stepNumber)
            .timestamp(LocalDateTime.now())
            .build();
        screenshots.add(screenshot);
    }
    
    public String getDurationFormatted() {
        if (executionTimeSeconds == null) {
            if (startedAt != null && status == EvaluationTaskStatus.RUNNING) {
                // Task is still running
                long runningSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
                return formatDuration(runningSeconds) + " (running)";
            }
            return "Not started";
        }
        return formatDuration(executionTimeSeconds);
    }
    
    private String formatDuration(long seconds) {
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
    
    public String getScoreFormatted() {
        if (score == null || maxScore == null) {
            return success ? "Pass" : "Fail";
        }
        return String.format("%.2f / %.2f", score, maxScore);
    }
    
    public double getScorePercentage() {
        if (score == null || maxScore == null || maxScore == 0) {
            return success ? 100.0 : 0.0;
        }
        return (score / maxScore) * 100.0;
    }
    
    /**
     * Parse execution parameters from JSON or return defaults.
     */
    public io.wingie.a2acore.domain.ExecutionParameters getExecutionParameters() {
        if (executionParametersJson == null || executionParametersJson.trim().isEmpty()) {
            // Build from individual fields
            return io.wingie.a2acore.domain.ExecutionParameters.builder()
                .maxSteps(maxSteps != null ? maxSteps : 10)
                .executionMode(parseExecutionMode())
                .allowEarlyCompletion(earlyCompletionAllowed != null ? earlyCompletionAllowed : true)
                .build();
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(executionParametersJson, io.wingie.a2acore.domain.ExecutionParameters.class);
        } catch (Exception e) {
            // Fallback to individual fields
            return io.wingie.a2acore.domain.ExecutionParameters.builder()
                .maxSteps(maxSteps != null ? maxSteps : 10)
                .executionMode(parseExecutionMode())
                .allowEarlyCompletion(earlyCompletionAllowed != null ? earlyCompletionAllowed : true)
                .build();
        }
    }
    
    /**
     * Set execution parameters from object.
     */
    public void setExecutionParameters(io.wingie.a2acore.domain.ExecutionParameters params) {
        if (params == null) return;
        
        // Set individual fields for easy querying
        this.maxSteps = params.getMaxSteps();
        this.executionMode = params.getExecutionMode().name();
        this.earlyCompletionAllowed = params.getAllowEarlyCompletion();
        
        // Store complete JSON for full parameter access
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.executionParametersJson = mapper.writeValueAsString(params);
        } catch (Exception e) {
            // If JSON serialization fails, clear the field
            this.executionParametersJson = null;
        }
    }
    
    private io.wingie.a2acore.domain.ExecutionParameters.ExecutionMode parseExecutionMode() {
        if (executionMode == null) {
            return io.wingie.a2acore.domain.ExecutionParameters.ExecutionMode.MULTI_STEP;
        }
        
        try {
            return io.wingie.a2acore.domain.ExecutionParameters.ExecutionMode.valueOf(executionMode);
        } catch (IllegalArgumentException e) {
            return io.wingie.a2acore.domain.ExecutionParameters.ExecutionMode.MULTI_STEP;
        }
    }
    
    /**
     * Update execution results after task completion.
     */
    public void updateExecutionResults(int stepsCompleted, boolean earlyCompletion) {
        this.stepsCompleted = stepsCompleted;
        this.earlyCompletionTriggered = earlyCompletion;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get execution efficiency as percentage of steps used vs. maximum allowed.
     */
    public double getExecutionEfficiency() {
        if (stepsCompleted == null || maxSteps == null || maxSteps == 0) {
            return 0.0;
        }
        return (double) stepsCompleted / maxSteps * 100.0;
    }
    
    /**
     * Check if task executed within expected parameters.
     */
    public boolean isExecutionEfficient() {
        return getExecutionEfficiency() <= 80.0; // Used 80% or less of allocated steps
    }
    
    /**
     * Get formatted execution summary.
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Mode: %s, ", executionMode));
        summary.append(String.format("Steps: %d/%d", 
                                    stepsCompleted != null ? stepsCompleted : 0, 
                                    maxSteps != null ? maxSteps : 0));
        
        if (earlyCompletionTriggered != null && earlyCompletionTriggered) {
            summary.append(" (early completion)");
        }
        
        summary.append(String.format(", Efficiency: %.1f%%", getExecutionEfficiency()));
        
        return summary.toString();
    }
}