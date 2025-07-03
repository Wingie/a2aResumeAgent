package io.wingie.service;

import io.wingie.dto.AdminStatisticsDTO;
import io.wingie.dto.AdminStatisticsDTO.*;
import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.entity.ModelEvaluation;
import io.wingie.entity.ToolDescription;
import io.wingie.repository.TaskExecutionRepository;
import io.wingie.repository.ModelEvaluationRepository;
import io.wingie.repository.ToolDescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to aggregate comprehensive statistics for the admin dashboard
 * Provides insights into system performance, tool usage, model effectiveness, and cache efficiency
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsService {
    
    private final TaskExecutionRepository taskRepository;
    private final ModelEvaluationRepository modelEvaluationRepository;
    private final ToolDescriptionRepository toolDescriptionRepository;
    
    @Value("${task.processor.modelName:deepseek/deepseek-r1:free}")
    private String currentModelName;
    
    private final Map<String, Long> requestCountHistory = new HashMap<>();
    private final Map<String, Long> errorCountHistory = new HashMap<>();
    
    /**
     * Get complete admin dashboard statistics
     */
    public AdminStatisticsDTO getCompleteStatistics() {
        // log.debug("Generating complete admin statistics");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last24Hours = now.minusDays(1);
            LocalDateTime lastWeek = now.minusDays(7);
            
            return AdminStatisticsDTO.builder()
                .systemHealth(buildSystemHealthMetrics(now))
                .toolMetrics(buildToolMetrics(last24Hours, now))
                .modelPerformance(buildModelPerformanceMetrics(lastWeek, now))
                .activityFeed(buildActivityFeed(now))
                .cacheMetrics(buildCacheMetrics())
                .realTimeStats(buildRealTimeStats(now))
                .build();
                
        } catch (Exception e) {
            log.error("Error generating admin statistics", e);
            throw new RuntimeException("Failed to generate statistics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build system health metrics
     */
    private SystemHealthMetrics buildSystemHealthMetrics(LocalDateTime now) {
        long activeTasks = taskRepository.countByStatus(TaskStatus.RUNNING);
        long queuedTasks = taskRepository.countByStatus(TaskStatus.QUEUED);
        long stuckTasks = taskRepository.countStuckTasks(now.minusMinutes(30));
        
        // Calculate system load based on active/queued tasks
        double systemLoad = Math.min(100.0, (activeTasks + queuedTasks) * 10.0);
        
        // Determine overall status
        String overallStatus;
        if (stuckTasks > 0 || systemLoad > 80) {
            overallStatus = "critical";
        } else if (systemLoad > 50 || activeTasks > 10) {
            overallStatus = "warning";
        } else {
            overallStatus = "healthy";
        }
        
        Map<String, Object> healthDetails = Map.of(
            "maxConcurrentTasks", 20,
            "averageResponseTime", calculateAverageResponseTime(),
            "memoryUsage", "N/A", // Could integrate with JVM metrics
            "diskUsage", "N/A"
        );
        
        return SystemHealthMetrics.builder()
            .activeTasks(activeTasks)
            .queuedTasks(queuedTasks)
            .stuckTasks(stuckTasks)
            .systemLoadPercent(systemLoad)
            .browserHealthy(true) // Playwright always healthy
            .overallStatus(overallStatus)
            .lastUpdate(now)
            .uptimeSeconds(calculateUptimeSeconds())
            .healthDetails(healthDetails)
            .build();
    }
    
    /**
     * Build tool usage and performance metrics
     */
    private ToolMetrics buildToolMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        // log.debug("Building tool metrics for period: {} to {}", startDate, endDate);
        
        List<TaskExecution> recentTasks = taskRepository.findCompletedTasksBetween(startDate, endDate);
        // log.debug("Found {} recent tasks for tool metrics", recentTasks.size());
        
        long totalCalls = recentTasks.size();
        long successfulCalls = recentTasks.stream()
            .filter(TaskExecution::isCompleted)
            .mapToLong(t -> 1)
            .sum();
        long failedCalls = totalCalls - successfulCalls;
        
        double successRate = totalCalls > 0 ? (successfulCalls * 100.0) / totalCalls : 0.0;
        
        double avgExecutionTime = recentTasks.stream()
            .filter(t -> t.getActualDurationSeconds() != null)
            .mapToInt(TaskExecution::getActualDurationSeconds)
            .average()
            .orElse(0.0);
        
        // Group by task type for tool metrics
        Map<String, List<TaskExecution>> tasksByType = recentTasks.stream()
            .collect(Collectors.groupingBy(TaskExecution::getTaskType));
        
        // log.debug("Task types found: {}", tasksByType.keySet());
        
        List<ToolCallMetric> topUsedTools = tasksByType.entrySet().stream()
            .map(entry -> buildToolCallMetric(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Long.compare(b.getCallCount(), a.getCallCount()))
            .limit(10)
            .collect(Collectors.toList());
        
        List<ToolCallMetric> slowestTools = topUsedTools.stream()
            .sorted((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()))
            .limit(5)
            .collect(Collectors.toList());
        
        List<ToolCallMetric> mostFailedTools = topUsedTools.stream()
            .filter(t -> t.getFailureCount() > 0)
            .sorted((a, b) -> Long.compare(b.getFailureCount(), a.getFailureCount()))
            .limit(5)
            .collect(Collectors.toList());
        
        Map<String, Long> callsByHour = buildHourlyCallsMap(recentTasks);
        Map<String, Long> errorsByType = buildErrorTypesMap(recentTasks);
        
        // Ensure lists are never null
        if (topUsedTools == null) topUsedTools = new ArrayList<>();
        if (slowestTools == null) slowestTools = new ArrayList<>();
        if (mostFailedTools == null) mostFailedTools = new ArrayList<>();
        if (callsByHour == null) callsByHour = new HashMap<>();
        if (errorsByType == null) errorsByType = new HashMap<>();
        
        
        return ToolMetrics.builder()
            .totalToolCalls(totalCalls)
            .successfulCalls(successfulCalls)
            .failedCalls(failedCalls)
            .successRate(successRate)
            .averageExecutionTimeSeconds(avgExecutionTime)
            .topUsedTools(topUsedTools)
            .slowestTools(slowestTools)
            .mostFailedTools(mostFailedTools)
            .callsByHour(callsByHour)
            .errorsByType(errorsByType)
            .build();
    }
    
    /**
     * Build model performance metrics with comparisons
     */
    private ModelPerformanceMetrics buildModelPerformanceMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        List<ModelEvaluation> evaluations = modelEvaluationRepository.findAll();
        List<TaskExecution> recentTasks = taskRepository.findCompletedTasksBetween(startDate, endDate);
        
        // Group tasks by estimated model used (based on time periods)
        Map<String, List<TaskExecution>> tasksByModel = groupTasksByModel(recentTasks);
        
        List<ModelMetric> modelComparison = tasksByModel.entrySet().stream()
            .map(entry -> buildModelMetric(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Double.compare(b.getSuccessRate(), a.getSuccessRate()))
            .collect(Collectors.toList());
        
        Map<String, Double> successRates = modelComparison.stream()
            .collect(Collectors.toMap(ModelMetric::getModelName, ModelMetric::getSuccessRate));
        
        Map<String, Double> avgResponseTimes = modelComparison.stream()
            .collect(Collectors.toMap(ModelMetric::getModelName, ModelMetric::getAverageResponseTimeMs));
        
        Map<String, Long> usageCounts = modelComparison.stream()
            .collect(Collectors.toMap(ModelMetric::getModelName, ModelMetric::getTotalCalls));
        
        List<ModelErrorPattern> errorPatterns = buildModelErrorPatterns(recentTasks);
        
        ModelRecommendation recommendation = buildModelRecommendation(modelComparison);
        
        return ModelPerformanceMetrics.builder()
            .currentModel(currentModelName)
            .modelComparison(modelComparison)
            .modelSuccessRates(successRates)
            .modelAverageResponseTime(avgResponseTimes)
            .modelUsageCount(usageCounts)
            .errorPatterns(errorPatterns)
            .recommendation(recommendation)
            .build();
    }
    
    /**
     * Build recent activity feed
     */
    private ActivityFeed buildActivityFeed(LocalDateTime now) {
        LocalDateTime last2Hours = now.minusHours(2);
        
        Pageable pageable = PageRequest.of(0, 50, Sort.by("updated").descending());
        List<TaskExecution> recentTasks = taskRepository.findRecentlyUpdatedTasks(last2Hours);
        
        List<ActivityItem> activities = recentTasks.stream()
            .map(this::buildActivityItem)
            .collect(Collectors.toList());
        
        LocalDateTime oldestActivity = activities.isEmpty() ? now : 
            activities.stream().map(ActivityItem::getTimestamp).min(LocalDateTime::compareTo).orElse(now);
        LocalDateTime newestActivity = activities.isEmpty() ? now :
            activities.stream().map(ActivityItem::getTimestamp).max(LocalDateTime::compareTo).orElse(now);
        
        return ActivityFeed.builder()
            .recentActivities(activities)
            .totalActivities(activities.size())
            .oldestActivity(oldestActivity)
            .newestActivity(newestActivity)
            .build();
    }
    
    /**
     * Build cache efficiency metrics
     */
    private CacheMetrics buildCacheMetrics() {
        List<ToolDescription> allCachedItems = toolDescriptionRepository.findAll();
        
        long totalItems = allCachedItems.size();
        long totalUsage = allCachedItems.stream()
            .filter(t -> t.getUsageCount() != null)
            .mapToLong(ToolDescription::getUsageCount)
            .sum();
        
        // Estimate cache hits vs misses (usage count indicates hits)
        long cacheHits = totalUsage;
        long cacheMisses = Math.max(0, totalItems); // Conservative estimate
        
        double hitRate = (cacheHits + cacheMisses) > 0 ? 
            (cacheHits * 100.0) / (cacheHits + cacheMisses) : 0.0;
        
        double avgGenerationTime = allCachedItems.stream()
            .filter(t -> t.getGenerationTimeMs() != null)
            .mapToLong(ToolDescription::getGenerationTimeMs)
            .average()
            .orElse(0.0);
        
        // Group by provider
        Map<String, List<ToolDescription>> byProvider = allCachedItems.stream()
            .collect(Collectors.groupingBy(ToolDescription::getProviderModel));
        
        List<CacheProviderMetric> providerMetrics = byProvider.entrySet().stream()
            .map(entry -> buildCacheProviderMetric(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        List<CacheUsageMetric> mostUsed = allCachedItems.stream()
            .filter(t -> t.getUsageCount() != null && t.getUsageCount() > 0)
            .sorted((a, b) -> Long.compare(b.getUsageCount(), a.getUsageCount()))
            .limit(10)
            .map(this::buildCacheUsageMetric)
            .collect(Collectors.toList());
        
        return CacheMetrics.builder()
            .totalCachedItems(totalItems)
            .cacheHits(cacheHits)
            .cacheMisses(cacheMisses)
            .hitRate(hitRate)
            .totalCacheSizeBytes(estimateCacheSize(allCachedItems))
            .averageGenerationTimeMs(avgGenerationTime)
            .providerMetrics(providerMetrics)
            .mostUsedCachedItems(mostUsed)
            .lastCacheClean(LocalDateTime.now().minusDays(1)) // Placeholder
            .build();
    }
    
    /**
     * Build real-time statistics
     */
    private RealTimeStats buildRealTimeStats(LocalDateTime now) {
        LocalDateTime lastMinute = now.minusMinutes(1);
        
        List<TaskExecution> recentTasks = taskRepository.findRecentlyUpdatedTasks(lastMinute);
        List<TaskExecution> activeTasks = taskRepository.findActiveTasks();
        
        long tasksPerMinute = recentTasks.size();
        long errorsPerMinute = recentTasks.stream()
            .filter(TaskExecution::isFailed)
            .mapToLong(t -> 1)
            .sum();
        
        List<String> activeTaskIds = activeTasks.stream()
            .map(TaskExecution::getTaskId)
            .collect(Collectors.toList());
        
        List<String> recentErrors = recentTasks.stream()
            .filter(TaskExecution::isFailed)
            .map(t -> t.getErrorDetails() != null ? t.getErrorDetails() : "Unknown error")
            .limit(5)
            .collect(Collectors.toList());
        
        Map<String, Long> statusCounts = Arrays.stream(TaskStatus.values())
            .collect(Collectors.toMap(
                Enum::name,
                status -> taskRepository.countByStatus(status)
            ));
        
        double currentLoad = Math.min(100.0, activeTasks.size() * 10.0);
        
        return RealTimeStats.builder()
            .tasksPerMinute(tasksPerMinute)
            .errorsPerMinute(errorsPerMinute)
            .currentSystemLoad(currentLoad)
            .timestamp(now)
            .instantMetrics(Map.of(
                "browserInstances", 1,
                "memoryUsageMB", "N/A",
                "cpuUsagePercent", "N/A"
            ))
            .activeTaskIds(activeTaskIds)
            .recentErrors(recentErrors)
            .statusCounts(statusCounts)
            .build();
    }
    
    // Helper methods
    
    private ToolCallMetric buildToolCallMetric(String taskType, List<TaskExecution> tasks) {
        long total = tasks.size();
        long successful = tasks.stream().filter(TaskExecution::isCompleted).mapToLong(t -> 1).sum();
        long failed = total - successful;
        double successRate = total > 0 ? (successful * 100.0) / total : 0.0;
        
        double avgTime = tasks.stream()
            .filter(t -> t.getActualDurationSeconds() != null)
            .mapToInt(TaskExecution::getActualDurationSeconds)
            .average()
            .orElse(0.0);
        
        String lastError = tasks.stream()
            .filter(TaskExecution::isFailed)
            .map(TaskExecution::getErrorDetails)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        
        LocalDateTime lastUsed = tasks.stream()
            .map(TaskExecution::getUpdated)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        return ToolCallMetric.builder()
            .toolName(taskType)
            .taskType(taskType)
            .callCount(total)
            .successCount(successful)
            .failureCount(failed)
            .successRate(successRate)
            .averageExecutionTime(avgTime)
            .lastError(lastError)
            .lastUsed(lastUsed)
            .build();
    }
    
    private ModelMetric buildModelMetric(String modelName, List<TaskExecution> tasks) {
        long total = tasks.size();
        long successful = tasks.stream().filter(TaskExecution::isCompleted).mapToLong(t -> 1).sum();
        double successRate = total > 0 ? (successful * 100.0) / total : 0.0;
        
        double avgResponseTime = tasks.stream()
            .filter(t -> t.getActualDurationSeconds() != null)
            .mapToInt(TaskExecution::getActualDurationSeconds)
            .average()
            .orElse(0.0) * 1000; // Convert to milliseconds
        
        long totalErrors = total - successful;
        
        String mostCommonError = tasks.stream()
            .filter(TaskExecution::isFailed)
            .map(TaskExecution::getErrorDetails)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("No errors");
        
        LocalDateTime lastUsed = tasks.stream()
            .map(TaskExecution::getUpdated)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        return ModelMetric.builder()
            .modelName(modelName)
            .provider(extractProvider(modelName))
            .totalCalls(total)
            .successfulCalls(successful)
            .successRate(successRate)
            .averageResponseTimeMs(avgResponseTime)
            .totalErrors(totalErrors)
            .mostCommonError(mostCommonError)
            .lastUsed(lastUsed)
            .isRecommended(successRate > 80.0 && avgResponseTime < 10000)
            .build();
    }
    
    private ActivityItem buildActivityItem(TaskExecution task) {
        String severity = task.isFailed() ? "error" : 
                         task.isCompleted() ? "success" : 
                         task.isRunning() ? "info" : "warning";
        
        String title = String.format("%s - %s", 
            task.getTaskType().toUpperCase(), 
            task.getStatus().name());
        
        String description = task.getProgressMessage() != null ? 
            task.getProgressMessage() : 
            (task.getOriginalQuery() != null ? 
                task.getOriginalQuery().substring(0, Math.min(100, task.getOriginalQuery().length())) : 
                "No query available");
        
        // Create structured tool result for frontend consumption
        Map<String, Object> toolResult = Map.of(
            "taskId", task.getTaskId() != null ? task.getTaskId() : "",
            "originalQuery", task.getOriginalQuery() != null ? task.getOriginalQuery() : "",
            "extractedResults", task.getExtractedResults() != null ? task.getExtractedResults() : "",
            "screenshots", task.getScreenshots() != null ? task.getScreenshots() : List.of(),
            "progressMessage", task.getProgressMessage() != null ? task.getProgressMessage() : "",
            "progressPercent", task.getProgressPercent() != null ? task.getProgressPercent() : 0,
            "startedAt", task.getStartedAt() != null ? task.getStartedAt() : "",
            "completedAt", task.getCompletedAt() != null ? task.getCompletedAt() : "",
            "actualDurationSeconds", task.getActualDurationSeconds() != null ? task.getActualDurationSeconds() : 0
        );
        
        return ActivityItem.builder()
            .id(task.getTaskId() != null ? task.getTaskId() : "")
            .type("TASK_EXECUTION")
            .title(title)
            .description(description)
            .status(task.getStatus())
            .taskType(task.getTaskType() != null ? task.getTaskType() : "UNKNOWN")
            .modelUsed(currentModelName) // Simplified
            .toolName(task.getTaskType() != null ? task.getTaskType() : "UNKNOWN")
            .timestamp(task.getUpdated() != null ? task.getUpdated() : LocalDateTime.now())
            .duration(task.getDurationFormatted() != null ? task.getDurationFormatted() : "0s")
            .errorDetails(task.getErrorDetails())
            .severity(severity)
            .metadata(Map.of(
                "screenshots", task.getScreenshots() != null ? task.getScreenshots().size() : 0,
                "retryCount", task.getRetryCount() != null ? task.getRetryCount() : 0,
                "requesterId", task.getRequesterId() != null ? task.getRequesterId() : "anonymous"
            ))
            // CRITICAL: Map the missing fields that frontend expects
            .results(task.getExtractedResults()) // Direct mapping for frontend
            .screenshots(task.getScreenshots() != null ? task.getScreenshots() : List.of()) // Direct mapping
            .toolResult(toolResult) // Structured object for detailed view
            .progressMessage(task.getProgressMessage()) // For in-progress tasks
            .progressPercent(task.getProgressPercent()) // For progress display
            .build();
    }
    
    private CacheProviderMetric buildCacheProviderMetric(String providerModel, List<ToolDescription> items) {
        long usageCount = items.stream()
            .filter(t -> t.getUsageCount() != null)
            .mapToLong(ToolDescription::getUsageCount)
            .sum();
        
        double avgGenerationTime = items.stream()
            .filter(t -> t.getGenerationTimeMs() != null)
            .mapToLong(ToolDescription::getGenerationTimeMs)
            .average()
            .orElse(0.0);
        
        LocalDateTime lastUsed = items.stream()
            .map(ToolDescription::getLastUsedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        return CacheProviderMetric.builder()
            .providerModel(providerModel)
            .itemCount(items.size())
            .usageCount(usageCount)
            .averageGenerationTime(avgGenerationTime)
            .lastUsed(lastUsed)
            .build();
    }
    
    private CacheUsageMetric buildCacheUsageMetric(ToolDescription tool) {
        return CacheUsageMetric.builder()
            .toolName(tool.getToolName())
            .providerModel(tool.getProviderModel())
            .usageCount(tool.getUsageCount() != null ? tool.getUsageCount() : 0)
            .generationTimeMs(tool.getGenerationTimeMs() != null ? tool.getGenerationTimeMs() : 0)
            .lastUsed(tool.getLastUsedAt())
            .cacheSizeBytes(estimateItemSize(tool))
            .build();
    }
    
    // Utility methods
    
    private double calculateAverageResponseTime() {
        LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
        return taskRepository.findCompletedTasksBetween(last24Hours, LocalDateTime.now())
            .stream()
            .filter(t -> t.getActualDurationSeconds() != null)
            .mapToInt(TaskExecution::getActualDurationSeconds)
            .average()
            .orElse(0.0);
    }
    
    private long calculateUptimeSeconds() {
        // Simplified - could track actual application start time
        return ChronoUnit.SECONDS.between(LocalDateTime.now().minusDays(1), LocalDateTime.now());
    }
    
    private Map<String, Long> buildHourlyCallsMap(List<TaskExecution> tasks) {
        return tasks.stream()
            .collect(Collectors.groupingBy(
                t -> String.valueOf(t.getCreated().getHour()),
                Collectors.counting()
            ));
    }
    
    private Map<String, Long> buildErrorTypesMap(List<TaskExecution> tasks) {
        return tasks.stream()
            .filter(TaskExecution::isFailed)
            .map(t -> extractErrorType(t.getErrorDetails()))
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }
    
    private Map<String, List<TaskExecution>> groupTasksByModel(List<TaskExecution> tasks) {
        // Simplified - group by time periods to estimate model usage
        // In a real implementation, you'd track model per task
        return tasks.stream()
            .collect(Collectors.groupingBy(t -> currentModelName));
    }
    
    private List<ModelErrorPattern> buildModelErrorPatterns(List<TaskExecution> tasks) {
        Map<String, List<TaskExecution>> errorGroups = tasks.stream()
            .filter(TaskExecution::isFailed)
            .filter(t -> t.getErrorDetails() != null)
            .collect(Collectors.groupingBy(t -> extractErrorType(t.getErrorDetails())));
        
        return errorGroups.entrySet().stream()
            .map(entry -> {
                String errorType = entry.getKey();
                List<TaskExecution> errorTasks = entry.getValue();
                
                LocalDateTime firstSeen = errorTasks.stream()
                    .map(TaskExecution::getCreated)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                LocalDateTime lastSeen = errorTasks.stream()
                    .map(TaskExecution::getUpdated)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                return ModelErrorPattern.builder()
                    .modelName(currentModelName)
                    .errorType(errorType)
                    .errorMessage(errorTasks.get(0).getErrorDetails())
                    .occurrenceCount(errorTasks.size())
                    .firstSeen(firstSeen)
                    .lastSeen(lastSeen)
                    .suggestedFix(suggestErrorFix(errorType))
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    private ModelRecommendation buildModelRecommendation(List<ModelMetric> models) {
        if (models.isEmpty()) {
            return ModelRecommendation.builder()
                .recommendedModel(currentModelName)
                .currentModel(currentModelName)
                .reason("No comparison data available")
                .confidenceScore(0.0)
                .benefits(new String[]{"Current model"})
                .drawbacks(new String[]{"No alternatives evaluated"})
                .build();
        }
        
        ModelMetric bestModel = models.stream()
            .max((a, b) -> {
                // Weighted score: success rate (70%) + speed (30%)
                double scoreA = (a.getSuccessRate() * 0.7) + ((10000.0 - a.getAverageResponseTimeMs()) / 10000.0 * 30);
                double scoreB = (b.getSuccessRate() * 0.7) + ((10000.0 - b.getAverageResponseTimeMs()) / 10000.0 * 30);
                return Double.compare(scoreA, scoreB);
            })
            .orElse(models.get(0));
        
        boolean shouldSwitchModels = !bestModel.getModelName().equals(currentModelName) && 
                                   bestModel.getSuccessRate() > 90.0;
        
        return ModelRecommendation.builder()
            .recommendedModel(shouldSwitchModels ? bestModel.getModelName() : currentModelName)
            .currentModel(currentModelName)
            .reason(shouldSwitchModels ? 
                String.format("Better success rate: %.1f%% vs current", bestModel.getSuccessRate()) :
                "Current model performing adequately")
            .confidenceScore(shouldSwitchModels ? 0.85 : 0.5)
            .benefits(shouldSwitchModels ? 
                new String[]{"Higher success rate", "Better reliability"} :
                new String[]{"Stable performance", "Known behavior"})
            .drawbacks(shouldSwitchModels ?
                new String[]{"Cache invalidation", "Learning curve"} :
                new String[]{"May not be optimal"})
            .build();
    }
    
    private String extractErrorType(String errorDetails) {
        if (errorDetails == null) return "Unknown";
        if (errorDetails.contains("timeout")) return "Timeout";
        if (errorDetails.contains("connection")) return "Connection";
        if (errorDetails.contains("parsing")) return "Parsing";
        if (errorDetails.contains("browser")) return "Browser";
        return "General";
    }
    
    private String extractProvider(String modelName) {
        if (modelName == null) return "unknown";
        if (modelName.contains("openrouter")) return "OpenRouter";
        if (modelName.contains("openai")) return "OpenAI";
        if (modelName.contains("anthropic")) return "Anthropic";
        if (modelName.contains("google")) return "Google";
        return "unknown";
    }
    
    private String suggestErrorFix(String errorType) {
        return switch (errorType.toLowerCase()) {
            case "timeout" -> "Increase timeout values or optimize task complexity";
            case "connection" -> "Check network connectivity and API endpoints";
            case "parsing" -> "Validate input format and model response structure";
            case "browser" -> "Restart browser instances or check Playwright configuration";
            default -> "Review logs and error patterns for specific guidance";
        };
    }
    
    private long estimateCacheSize(List<ToolDescription> items) {
        return items.stream()
            .mapToLong(this::estimateItemSize)
            .sum();
    }
    
    private long estimateItemSize(ToolDescription item) {
        // Rough estimate of memory usage
        int baseSize = 100; // Object overhead
        int descriptionSize = item.getDescription() != null ? item.getDescription().length() * 2 : 0;
        int paramsSize = item.getParametersInfo() != null ? item.getParametersInfo().length() * 2 : 0;
        int propsSize = item.getToolProperties() != null ? item.getToolProperties().length() * 2 : 0;
        
        return baseSize + descriptionSize + paramsSize + propsSize;
    }
}