package io.wingie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_screenshots", indexes = {
    @Index(name = "idx_eval_screenshot_task", columnList = "taskId"),
    @Index(name = "idx_eval_screenshot_step", columnList = "stepNumber"),
    @Index(name = "idx_eval_screenshot_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScreenshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screenshot_id")
    private Long screenshotId;
    
    @Column(name = "task_id", length = 36, nullable = false)
    @NotBlank
    private String taskId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private EvaluationTask evaluationTask;
    
    @Column(name = "screenshot_path", nullable = false, length = 500)
    @NotBlank
    private String screenshotPath;
    
    @Column(name = "step_number", nullable = false)
    @NotNull
    private Integer stepNumber;
    
    @Column(name = "step_description", length = 500)
    private String stepDescription;
    
    @Column(name = "timestamp", nullable = false)
    @NotNull
    private LocalDateTime timestamp;
    
    @Column(name = "action_taken", length = 300)
    private String actionTaken;
    
    @Column(name = "before_action")
    private Boolean beforeAction; // true if screenshot taken before action, false if after
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "image_width")
    private Integer imageWidth;
    
    @Column(name = "image_height")
    private Integer imageHeight;
    
    @Column(name = "success_indicator")
    private Boolean successIndicator; // Whether this screenshot shows successful completion
    
    @Column(name = "error_indicator")
    private Boolean errorIndicator; // Whether this screenshot shows an error state
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (beforeAction == null) {
            beforeAction = false; // Default to after action
        }
        if (successIndicator == null) {
            successIndicator = false;
        }
        if (errorIndicator == null) {
            errorIndicator = false;
        }
    }
    
    // Convenience methods
    public String getFileName() {
        if (screenshotPath == null) {
            return null;
        }
        return screenshotPath.substring(screenshotPath.lastIndexOf('/') + 1);
    }
    
    public String getFileSizeFormatted() {
        if (fileSizeBytes == null) {
            return "Unknown";
        }
        
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }
    
    public String getResolutionFormatted() {
        if (imageWidth == null || imageHeight == null) {
            return "Unknown";
        }
        return imageWidth + " x " + imageHeight;
    }
    
    public String getStatusIcon() {
        if (Boolean.TRUE.equals(successIndicator)) {
            return "fas fa-check-circle text-success";
        } else if (Boolean.TRUE.equals(errorIndicator)) {
            return "fas fa-exclamation-circle text-danger";
        } else {
            return "fas fa-image text-muted";
        }
    }
    
    public String getActionTypeIcon() {
        if (Boolean.TRUE.equals(beforeAction)) {
            return "fas fa-eye text-info"; // Before action - observing
        } else {
            return "fas fa-mouse-pointer text-primary"; // After action - interacted
        }
    }
    
    public void updateFileInfo(long sizeBytes, int width, int height) {
        this.fileSizeBytes = sizeBytes;
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    public void markAsSuccessScreenshot() {
        this.successIndicator = true;
        this.errorIndicator = false;
    }
    
    public void markAsErrorScreenshot() {
        this.errorIndicator = true;
        this.successIndicator = false;
    }
}