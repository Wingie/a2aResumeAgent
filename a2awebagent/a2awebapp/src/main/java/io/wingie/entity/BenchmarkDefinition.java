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
import java.util.UUID;

/**
 * Entity representing a reusable evaluation benchmark template.
 * 
 * Benchmark definitions provide structured templates for model evaluation,
 * containing a collection of tasks that can be executed against different
 * AI models to assess their performance consistently.
 * 
 * Used for:
 * - Standardized model evaluation across different AI providers
 * - Performance comparison and A/B testing
 * - Quality assurance and regression testing
 * - Capability assessment for different task categories
 */
@Entity
@Table(name = "benchmark_definitions", indexes = {
    @Index(name = "idx_benchmark_name", columnList = "benchmarkName"),
    @Index(name = "idx_benchmark_active", columnList = "isActive"),
    @Index(name = "idx_benchmark_category", columnList = "category"),
    @Index(name = "idx_benchmark_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkDefinition {
    
    @Id
    @Column(name = "benchmark_id", length = 36)
    private String benchmarkId;
    
    @Column(name = "benchmark_name", nullable = false, length = 100, unique = true)
    @NotBlank
    private String benchmarkName;
    
    @Column(name = "benchmark_version", nullable = false, length = 20)
    @NotBlank
    @Builder.Default
    private String benchmarkVersion = "1.0";
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    // Configuration
    @Column(name = "total_tasks", nullable = false)
    @Builder.Default
    private Integer totalTasks = 0;
    
    @Column(name = "expected_duration_seconds")
    private Integer expectedDurationSeconds;
    
    @Column(name = "difficulty_rating")
    private Integer difficultyRating; // 1-5 overall difficulty
    
    // Metadata
    @Column(name = "category", length = 50)
    private String category; // 'web_automation', 'data_extraction', 'reasoning'
    
    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated tags
    
    @Column(name = "author", length = 100)
    private String author;
    
    // Status
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;
    
    // Note: BenchmarkTask relationships managed separately to avoid circular dependency
    
    @PrePersist
    protected void onCreate() {
        if (benchmarkId == null) {
            benchmarkId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (benchmarkVersion == null) {
            benchmarkVersion = "1.0";
        }
        if (totalTasks == null) {
            totalTasks = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Convenience methods
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTaskCount(int count) {
        this.totalTasks = count;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFormattedDuration() {
        if (expectedDurationSeconds == null) {
            return "Unknown";
        }
        
        int minutes = expectedDurationSeconds / 60;
        int seconds = expectedDurationSeconds % 60;
        
        if (minutes == 0) {
            return seconds + "s";
        } else if (minutes < 60) {
            return minutes + "m" + (seconds > 0 ? " " + seconds + "s" : "");
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return hours + "h" + (remainingMinutes > 0 ? " " + remainingMinutes + "m" : "");
        }
    }
    
    public String getDifficultyLabel() {
        if (difficultyRating == null) {
            return "Unknown";
        }
        return switch (difficultyRating) {
            case 1 -> "Very Easy";
            case 2 -> "Easy";
            case 3 -> "Medium";
            case 4 -> "Hard";
            case 5 -> "Very Hard";
            default -> "Unknown";
        };
    }
    
    public List<String> getTagsList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }
        return List.of(tags.split(","))
            .stream()
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .toList();
    }
    
    public void setTagsList(List<String> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            this.tags = null;
        } else {
            this.tags = String.join(",", tagsList);
        }
    }
    
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        List<String> currentTags = new ArrayList<>(getTagsList());
        String normalizedTag = tag.trim().toLowerCase();
        
        if (!currentTags.contains(normalizedTag)) {
            currentTags.add(normalizedTag);
            setTagsList(currentTags);
        }
    }
    
    public void removeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        List<String> currentTags = new ArrayList<>(getTagsList());
        currentTags.remove(tag.trim().toLowerCase());
        setTagsList(currentTags);
    }
    
    public String getSummary() {
        return String.format("%s v%s - %d tasks, %s difficulty, %s", 
            benchmarkName, 
            benchmarkVersion, 
            totalTasks, 
            getDifficultyLabel(), 
            getFormattedDuration()
        );
    }
    
    /**
     * Create a new benchmark definition with default values
     */
    public static BenchmarkDefinition createDefault(String name, String description, String category) {
        return BenchmarkDefinition.builder()
            .benchmarkName(name)
            .description(description)
            .category(category)
            .benchmarkVersion("1.0")
            .difficultyRating(3) // Medium difficulty by default
            .author("a2aTravelAgent System")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Check if benchmark definition is valid for execution
     */
    public boolean isValidForExecution() {
        return isActive && 
               benchmarkName != null && !benchmarkName.trim().isEmpty() &&
               totalTasks != null && totalTasks > 0;
    }
    
    /**
     * Get execution readiness status
     */
    public String getExecutionStatus() {
        if (!isActive) {
            return "Inactive";
        }
        if (totalTasks == null || totalTasks == 0) {
            return "No tasks defined";
        }
        return "Ready";
    }
}