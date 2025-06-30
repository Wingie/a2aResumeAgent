package io.wingie.dto;

import io.wingie.entity.TaskStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive DTO for admin dashboard statistics
 * Aggregates data from TaskExecution, ModelEvaluation, and ToolDescription tables
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatisticsDTO {
    
    // Overall system health and activity
    private SystemHealthMetrics systemHealth;
    
    // Tool usage and performance metrics
    private ToolMetrics toolMetrics;
    
    // Model-specific statistics
    private ModelPerformanceMetrics modelPerformance;
    
    // Recent activity feed
    private ActivityFeed activityFeed;
    
    // Cache efficiency metrics
    private CacheMetrics cacheMetrics;
    
    // Real-time statistics
    private RealTimeStats realTimeStats;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemHealthMetrics {
        private long activeTasks;
        private long queuedTasks;
        private long stuckTasks;
        private double systemLoadPercent;
        private boolean browserHealthy;
        private String overallStatus; // "healthy", "warning", "critical"
        private LocalDateTime lastUpdate;
        private long uptimeSeconds;
        private Map<String, Object> healthDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolMetrics {
        private long totalToolCalls;
        private long successfulCalls;
        private long failedCalls;
        private double successRate;
        private double averageExecutionTimeSeconds;
        private List<ToolCallMetric> topUsedTools;
        private List<ToolCallMetric> slowestTools;
        private List<ToolCallMetric> mostFailedTools;
        private Map<String, Long> callsByHour;
        private Map<String, Long> errorsByType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCallMetric {
        private String toolName;
        private String taskType;
        private long callCount;
        private long successCount;
        private long failureCount;
        private double successRate;
        private double averageExecutionTime;
        private String lastError;
        private LocalDateTime lastUsed;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelPerformanceMetrics {
        private String currentModel;
        private List<ModelMetric> modelComparison;
        private Map<String, Double> modelSuccessRates;
        private Map<String, Double> modelAverageResponseTime;
        private Map<String, Long> modelUsageCount;
        private List<ModelErrorPattern> errorPatterns;
        private ModelRecommendation recommendation;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelMetric {
        private String modelName;
        private String provider;
        private long totalCalls;
        private long successfulCalls;
        private double successRate;
        private double averageResponseTimeMs;
        private long totalErrors;
        private String mostCommonError;
        private LocalDateTime lastUsed;
        private boolean isRecommended;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelErrorPattern {
        private String modelName;
        private String errorType;
        private String errorMessage;
        private long occurrenceCount;
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;
        private String suggestedFix;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelRecommendation {
        private String recommendedModel;
        private String currentModel;
        private String reason;
        private double confidenceScore;
        private String[] benefits;
        private String[] drawbacks;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityFeed {
        private List<ActivityItem> recentActivities;
        private long totalActivities;
        private LocalDateTime oldestActivity;
        private LocalDateTime newestActivity;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        private String id;
        private String type; // "TASK_EXECUTION", "MODEL_EVALUATION", "TOOL_CALL", "ERROR"
        private String title;
        private String description;
        private TaskStatus status;
        private String taskType;
        private String modelUsed;
        private String toolName;
        private LocalDateTime timestamp;
        private String duration;
        private String errorDetails;
        private String severity; // "info", "warning", "error", "success"
        private Map<String, Object> metadata;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CacheMetrics {
        private long totalCachedItems;
        private long cacheHits;
        private long cacheMisses;
        private double hitRate;
        private long totalCacheSizeBytes;
        private double averageGenerationTimeMs;
        private List<CacheProviderMetric> providerMetrics;
        private List<CacheUsageMetric> mostUsedCachedItems;
        private LocalDateTime lastCacheClean;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CacheProviderMetric {
        private String providerModel;
        private long itemCount;
        private long usageCount;
        private double averageGenerationTime;
        private LocalDateTime lastUsed;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CacheUsageMetric {
        private String toolName;
        private String providerModel;
        private long usageCount;
        private double generationTimeMs;
        private LocalDateTime lastUsed;
        private long cacheSizeBytes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RealTimeStats {
        private long tasksPerMinute;
        private long errorsPerMinute;
        private double currentSystemLoad;
        private LocalDateTime timestamp;
        private Map<String, Object> instantMetrics;
        private List<String> activeTaskIds;
        private List<String> recentErrors;
        private Map<String, Long> statusCounts;
    }
}