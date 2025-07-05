package io.wingie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_executions", indexes = {
    @Index(name = "idx_task_status", columnList = "status"),
    @Index(name = "idx_task_type", columnList = "taskType"),
    @Index(name = "idx_requester", columnList = "requesterId"),
    @Index(name = "idx_created", columnList = "created")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskExecution {
    
    @Id
    @Column(name = "task_id", length = 36)
    private String taskId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private TaskStatus status;
    
    @Column(name = "task_type", nullable = false, length = 50)
    @NotBlank
    private String taskType;
    
    @Column(name = "original_query", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String originalQuery;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "task_screenshots", 
        joinColumns = @JoinColumn(name = "task_id"),
        indexes = @Index(name = "idx_task_screenshots", columnList = "task_id")
    )
    @Column(name = "screenshot_path")
    @Builder.Default
    private List<String> screenshots = new ArrayList<>();
    
    @Column(name = "extracted_results", columnDefinition = "TEXT")
    private String extractedResults;
    
    @Column(name = "progress_message", length = 500)
    private String progressMessage;
    
    @Column(name = "progress_percent")
    private Integer progressPercent;
    
    @Column(name = "created", nullable = false)
    @NotNull
    private LocalDateTime created;
    
    @Column(name = "updated", nullable = false)
    @NotNull
    private LocalDateTime updated;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
    
    @Column(name = "requester_id", length = 100)
    private String requesterId;
    
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;
    
    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default
    
    @PrePersist
    protected void onCreate() {
        if (created == null) {
            created = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (updated == null) {
            updated = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (status == null) {
            status = TaskStatus.QUEUED;
        }
        if (progressPercent == null) {
            progressPercent = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updated = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
        
        // Auto-set timestamps based on status changes
        if (status == TaskStatus.RUNNING && startedAt == null) {
            startedAt = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
        }
        
        if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) && completedAt == null) {
            completedAt = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
            
            // Calculate actual duration
            if (startedAt != null) {
                actualDurationSeconds = (int) java.time.Duration.between(startedAt, completedAt).getSeconds();
            }
        }
        
        // Update progress percentage based on status
        if (status == TaskStatus.COMPLETED) {
            progressPercent = 100;
        } else if (status == TaskStatus.FAILED) {
            // Keep current progress percentage for failed tasks
        }
    }
    
    // Convenience methods
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == TaskStatus.FAILED;
    }
    
    public boolean isRunning() {
        return status == TaskStatus.RUNNING;
    }
    
    public boolean isQueued() {
        return status == TaskStatus.QUEUED;
    }
    
    public boolean canRetry() {
        return isFailed() && retryCount < maxRetries;
    }
    
    public void addScreenshot(String screenshotPath) {
        if (screenshots == null) {
            screenshots = new ArrayList<>();
        }
        screenshots.add(screenshotPath);
    }
    
    public void updateProgress(String message, Integer percent) {
        this.progressMessage = message;
        if (percent != null && percent >= 0 && percent <= 100) {
            this.progressPercent = percent;
        }
        this.updated = LocalDateTime.now();
    }
    
    public void markAsStarted() {
        this.status = TaskStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.updated = LocalDateTime.now();
    }
    
    public void markAsCompleted(String results) {
        this.status = TaskStatus.COMPLETED;
        this.extractedResults = results;
        this.progressPercent = 100;
        this.progressMessage = "Task completed successfully";
        this.completedAt = LocalDateTime.now();
        this.updated = LocalDateTime.now();
        
        if (startedAt != null) {
            this.actualDurationSeconds = (int) java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorDetails = errorMessage;
        this.progressMessage = "Task failed: " + errorMessage;
        this.completedAt = LocalDateTime.now();
        this.updated = LocalDateTime.now();
        
        if (startedAt != null) {
            this.actualDurationSeconds = (int) java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updated = LocalDateTime.now();
    }
    
    public String getDurationFormatted() {
        if (actualDurationSeconds == null) {
            if (startedAt != null) {
                // Task is still running
                int runningSeconds = (int) java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
                return formatDuration(runningSeconds) + " (running)";
            }
            return "Not started";
        }
        return formatDuration(actualDurationSeconds);
    }
    
    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }
}