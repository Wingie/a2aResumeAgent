package io.wingie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a template task within a benchmark definition.
 * 
 * Benchmark tasks are reusable task templates that can be instantiated
 * as evaluation tasks when running model evaluations. They define the
 * expected behavior, scoring criteria, and execution parameters.
 * 
 * Used for:
 * - Template-based task creation for consistent evaluation
 * - Standardized scoring and evaluation criteria
 * - Reusable task definitions across multiple evaluations
 * - Performance comparison baseline establishment
 */
@Entity
@Table(name = "benchmark_tasks", indexes = {
    @Index(name = "idx_benchmark_task_benchmark", columnList = "benchmarkId"),
    @Index(name = "idx_benchmark_task_order", columnList = "benchmarkId, executionOrder"),
    @Index(name = "idx_benchmark_task_category", columnList = "taskCategory"),
    @Index(name = "idx_benchmark_task_difficulty", columnList = "difficultyLevel")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkTask {
    
    @Id
    @Column(name = "benchmark_task_id", length = 36)
    private String benchmarkTaskId;
    
    // Relationship to benchmark definition
    @Column(name = "benchmark_id", length = 36, nullable = false)
    @NotBlank
    private String benchmarkId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benchmark_id", insertable = false, updatable = false)
    private BenchmarkDefinition benchmarkDefinition;
    
    // Task Template Information
    @Column(name = "task_name", nullable = false, length = 200)
    @NotBlank
    private String taskName;
    
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;
    
    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String promptTemplate;
    
    // Scoring Configuration
    @Column(name = "max_score", nullable = false)
    @NotNull
    @Builder.Default
    private Double maxScore = 1.0;
    
    @Column(name = "evaluation_criteria", columnDefinition = "TEXT")
    private String evaluationCriteria;
    
    // Execution Configuration
    @Column(name = "execution_order", nullable = false)
    @NotNull
    private Integer executionOrder;
    
    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 2;
    
    // Categorization
    @Column(name = "task_category", length = 50)
    private String taskCategory; // 'navigation', 'form_filling', 'data_extraction'
    
    @Column(name = "difficulty_level")
    private Integer difficultyLevel; // 1-5 scale
    
    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated tags
    
    // Expected Result (optional)
    @Column(name = "expected_result_pattern", columnDefinition = "TEXT")
    private String expectedResultPattern; // Regex or exact match pattern
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (benchmarkTaskId == null) {
            benchmarkTaskId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (maxScore == null) {
            maxScore = 1.0;
        }
        if (timeoutSeconds == null) {
            timeoutSeconds = 300;
        }
        if (maxRetries == null) {
            maxRetries = 2;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Convenience methods
    public boolean isValidTask() {
        return taskName != null && !taskName.trim().isEmpty() &&
               promptTemplate != null && !promptTemplate.trim().isEmpty() &&
               maxScore != null && maxScore > 0 &&
               executionOrder != null && executionOrder > 0;
    }
    
    public String getDifficultyLabel() {
        if (difficultyLevel == null) {
            return "Unknown";
        }
        return switch (difficultyLevel) {
            case 1 -> "Very Easy";
            case 2 -> "Easy";
            case 3 -> "Medium";
            case 4 -> "Hard";
            case 5 -> "Very Hard";
            default -> "Unknown";
        };
    }
    
    public String getFormattedTimeout() {
        if (timeoutSeconds == null) {
            return "No timeout";
        }
        
        if (timeoutSeconds < 60) {
            return timeoutSeconds + "s";
        } else if (timeoutSeconds < 3600) {
            return (timeoutSeconds / 60) + "m " + (timeoutSeconds % 60) + "s";
        } else {
            int hours = timeoutSeconds / 3600;
            int minutes = (timeoutSeconds % 3600) / 60;
            int seconds = timeoutSeconds % 60;
            return hours + "h " + minutes + "m " + seconds + "s";
        }
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
        
        List<String> currentTags = new java.util.ArrayList<>(getTagsList());
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
        
        List<String> currentTags = new java.util.ArrayList<>(getTagsList());
        currentTags.remove(tag.trim().toLowerCase());
        setTagsList(currentTags);
    }
    
    public String getSummary() {
        return String.format("Task %d: %s (%s, %s difficulty, %.1f points)", 
            executionOrder, 
            taskName, 
            taskCategory != null ? taskCategory : "uncategorized", 
            getDifficultyLabel(), 
            maxScore
        );
    }
    
    /**
     * Create a new benchmark task with default values
     */
    public static BenchmarkTask createDefault(String benchmarkId, String name, String promptTemplate, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .promptTemplate(promptTemplate)
            .executionOrder(order)
            .maxScore(1.0)
            .difficultyLevel(3) // Medium difficulty by default
            .taskCategory("general")
            .timeoutSeconds(300) // 5 minutes
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a navigation task template
     */
    public static BenchmarkTask createNavigationTask(String benchmarkId, String name, String url, String expectedElement, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .taskDescription("Navigate to a website and verify specific elements")
            .promptTemplate("Navigate to " + url + " and verify that the page contains: " + expectedElement)
            .expectedResultPattern(expectedElement)
            .maxScore(10.0)
            .evaluationCriteria("Page loads successfully and expected element is present")
            .executionOrder(order)
            .taskCategory("navigation")
            .difficultyLevel(1)
            .tags("navigation,basic,web")
            .timeoutSeconds(60)
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a search task template
     */
    public static BenchmarkTask createSearchTask(String benchmarkId, String name, String searchEngine, String query, String expectedResult, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .taskDescription("Perform a search and verify results")
            .promptTemplate("Go to " + searchEngine + " and search for '" + query + "'. Verify that the results contain: " + expectedResult)
            .expectedResultPattern(expectedResult)
            .maxScore(15.0)
            .evaluationCriteria("Search is performed successfully and expected result appears in search results")
            .executionOrder(order)
            .taskCategory("search")
            .difficultyLevel(2)
            .tags("search,interaction,web")
            .timeoutSeconds(90)
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a form interaction task template
     */
    public static BenchmarkTask createFormTask(String benchmarkId, String name, String url, String formData, String expectedOutcome, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .taskDescription("Fill out and submit a form")
            .promptTemplate("Navigate to " + url + " and fill out the form with: " + formData + ". Submit the form and verify: " + expectedOutcome)
            .expectedResultPattern(expectedOutcome)
            .maxScore(20.0)
            .evaluationCriteria("Form is filled correctly, submitted successfully, and expected outcome is achieved")
            .executionOrder(order)
            .taskCategory("form_filling")
            .difficultyLevel(3)
            .tags("forms,interaction,complex")
            .timeoutSeconds(120)
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a data extraction task template
     */
    public static BenchmarkTask createDataExtractionTask(String benchmarkId, String name, String url, String dataToExtract, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .taskDescription("Extract specific data from a webpage")
            .promptTemplate("Navigate to " + url + " and extract the following information: " + dataToExtract)
            .expectedResultPattern(dataToExtract)
            .maxScore(15.0)
            .evaluationCriteria("Correct data is extracted from the specified webpage")
            .executionOrder(order)
            .taskCategory("data_extraction")
            .difficultyLevel(2)
            .tags("extraction,scraping,data")
            .timeoutSeconds(90)
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a complex interaction task template
     */
    public static BenchmarkTask createComplexInteractionTask(String benchmarkId, String name, String description, String prompt, 
                                                           String expectedResult, Integer difficulty, int order) {
        return BenchmarkTask.builder()
            .benchmarkId(benchmarkId)
            .taskName(name)
            .taskDescription(description)
            .promptTemplate(prompt)
            .expectedResultPattern(expectedResult)
            .maxScore(25.0)
            .evaluationCriteria("Complex multi-step interaction completed successfully")
            .executionOrder(order)
            .taskCategory("complex_interaction")
            .difficultyLevel(difficulty)
            .tags("complex,multi-step,advanced")
            .timeoutSeconds(300)
            .maxRetries(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}