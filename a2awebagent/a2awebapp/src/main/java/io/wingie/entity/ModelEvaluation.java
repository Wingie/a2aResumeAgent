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
@Table(name = "model_evaluations", indexes = {
    @Index(name = "idx_evaluation_status", columnList = "status"),
    @Index(name = "idx_evaluation_model", columnList = "modelName"),
    @Index(name = "idx_evaluation_benchmark", columnList = "benchmarkName"),
    @Index(name = "idx_evaluation_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelEvaluation {
    
    @Id
    @Column(name = "evaluation_id", length = 36)
    private String evaluationId;
    
    @Column(name = "model_name", nullable = false, length = 100)
    @NotBlank
    private String modelName;
    
    @Column(name = "model_provider", nullable = false, length = 50)
    @NotBlank
    private String modelProvider;
    
    @Column(name = "benchmark_name", nullable = false, length = 100)
    @NotBlank
    private String benchmarkName;
    
    @Column(name = "benchmark_version", length = 20)
    private String benchmarkVersion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private EvaluationStatus status;
    
    @Column(name = "overall_score")
    private Double overallScore;
    
    @Column(name = "max_possible_score")
    private Double maxPossibleScore;
    
    @Column(name = "success_rate")
    private Double successRate;
    
    @Column(name = "total_tasks")
    private Integer totalTasks;
    
    @Column(name = "completed_tasks")
    @Builder.Default
    private Integer completedTasks = 0;
    
    @Column(name = "successful_tasks")
    @Builder.Default
    private Integer successfulTasks = 0;
    
    @Column(name = "failed_tasks")
    @Builder.Default
    private Integer failedTasks = 0;
    
    @Column(name = "average_execution_time_seconds")
    private Double averageExecutionTimeSeconds;
    
    @Column(name = "total_execution_time_seconds")
    private Long totalExecutionTimeSeconds;
    
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;
    
    @Column(name = "environment_info", columnDefinition = "TEXT")
    private String environmentInfo;
    
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;
    
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvaluationTask> tasks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = EvaluationStatus.QUEUED;
        }
        if (completedTasks == null) {
            completedTasks = 0;
        }
        if (successfulTasks == null) {
            successfulTasks = 0;
        }
        if (failedTasks == null) {
            failedTasks = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Auto-set timestamps based on status changes
        if (status == EvaluationStatus.RUNNING && startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        
        if ((status == EvaluationStatus.COMPLETED || status == EvaluationStatus.FAILED) && completedAt == null) {
            completedAt = LocalDateTime.now();
            
            // Calculate total execution time
            if (startedAt != null) {
                totalExecutionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
            }
        }
    }
    
    // Convenience methods
    public boolean isCompleted() {
        return status == EvaluationStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == EvaluationStatus.FAILED;
    }
    
    public boolean isRunning() {
        return status == EvaluationStatus.RUNNING;
    }
    
    public boolean isQueued() {
        return status == EvaluationStatus.QUEUED;
    }
    
    public double getProgressPercent() {
        if (totalTasks == null || totalTasks == 0) {
            return 0.0;
        }
        return (completedTasks * 100.0) / totalTasks;
    }
    
    public void updateProgress() {
        if (tasks != null && !tasks.isEmpty()) {
            this.totalTasks = tasks.size();
            this.completedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == EvaluationTaskStatus.COMPLETED || task.getStatus() == EvaluationTaskStatus.FAILED)
                .count();
            this.successfulTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == EvaluationTaskStatus.COMPLETED && Boolean.TRUE.equals(task.getSuccess()))
                .count();
            this.failedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == EvaluationTaskStatus.FAILED || 
                              (task.getStatus() == EvaluationTaskStatus.COMPLETED && !Boolean.TRUE.equals(task.getSuccess())))
                .count();
            
            if (totalTasks > 0) {
                this.successRate = (successfulTasks * 100.0) / totalTasks;
            }
            
            // Calculate average execution time
            double avgTime = tasks.stream()
                .filter(task -> task.getExecutionTimeSeconds() != null)
                .mapToLong(EvaluationTask::getExecutionTimeSeconds)
                .average()
                .orElse(0.0);
            this.averageExecutionTimeSeconds = avgTime;
        }
    }
    
    public void markAsStarted() {
        this.status = EvaluationStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted() {
        this.status = EvaluationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        
        if (startedAt != null) {
            this.totalExecutionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
        
        updateProgress();
        calculateOverallScore();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = EvaluationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        
        if (startedAt != null) {
            this.totalExecutionTimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
        
        updateProgress();
    }
    
    private void calculateOverallScore() {
        if (tasks == null || tasks.isEmpty()) {
            this.overallScore = 0.0;
            return;
        }
        
        double totalScore = tasks.stream()
            .filter(task -> task.getScore() != null)
            .mapToDouble(EvaluationTask::getScore)
            .sum();
        
        double maxScore = tasks.stream()
            .filter(task -> task.getMaxScore() != null)
            .mapToDouble(EvaluationTask::getMaxScore)
            .sum();
        
        this.overallScore = totalScore;
        this.maxPossibleScore = maxScore;
    }
    
    public String getDurationFormatted() {
        if (totalExecutionTimeSeconds == null) {
            if (startedAt != null && status == EvaluationStatus.RUNNING) {
                // Evaluation is still running
                long runningSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
                return formatDuration(runningSeconds) + " (running)";
            }
            return "Not started";
        }
        return formatDuration(totalExecutionTimeSeconds);
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
        if (overallScore == null || maxPossibleScore == null) {
            return "N/A";
        }
        return String.format("%.2f / %.2f (%.1f%%)", overallScore, maxPossibleScore, 
                           (overallScore / maxPossibleScore) * 100);
    }
}