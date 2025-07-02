package io.wingie.controller;

import io.wingie.entity.neo4j.ScreenshotNode;
import io.wingie.entity.neo4j.TaskNode;
import io.wingie.entity.neo4j.WebPageNode;
import io.wingie.repository.neo4j.ScreenshotNodeRepository;
import io.wingie.repository.neo4j.TaskNodeRepository;
import io.wingie.repository.neo4j.WebPageNodeRepository;
import io.wingie.service.neo4j.ScreenshotEmbeddingService;
import io.wingie.service.neo4j.TaskGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for Neo4j knowledge graph analytics.
 * Provides endpoints for screenshot similarity, task relationships, and navigation patterns.
 */
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
@Slf4j
public class GraphAnalyticsController {
    
    private final ScreenshotNodeRepository screenshotRepository;
    private final TaskNodeRepository taskRepository;
    private final WebPageNodeRepository webPageRepository;
    private final ScreenshotEmbeddingService embeddingService;
    private final TaskGraphService taskGraphService;
    
    /**
     * Get similar screenshots for visual pattern analysis
     */
    @GetMapping("/screenshots/{screenshotId}/similar")
    public ResponseEntity<List<ScreenshotNode>> getSimilarScreenshots(
            @PathVariable String screenshotId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ScreenshotNode> similarScreenshots = embeddingService.findSimilarScreenshots(screenshotId, limit);
            return ResponseEntity.ok(similarScreenshots);
        } catch (Exception e) {
            log.error("Failed to find similar screenshots for {}: {}", screenshotId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get task analytics including relationships and performance metrics
     */
    @GetMapping("/tasks/{taskId}/analytics")
    public ResponseEntity<TaskGraphService.TaskAnalytics> getTaskAnalytics(@PathVariable String taskId) {
        try {
            TaskGraphService.TaskAnalytics analytics = taskGraphService.getTaskAnalytics(taskId);
            if (analytics == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Failed to get task analytics for {}: {}", taskId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get navigation patterns for a specific domain
     */
    @GetMapping("/navigation/{domain}")
    public ResponseEntity<Map<String, Object>> getNavigationPatterns(
            @PathVariable String domain,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<WebPageNode> pages = webPageRepository.findPagesByDomainSortedByVisits(domain);
            Map<String, Object> navigationData = new HashMap<>();
            navigationData.put("domain", domain);
            navigationData.put("totalPages", pages.size());
            navigationData.put("pages", pages.stream().limit(limit).toList());
            
            // Get top performing pages
            List<WebPageNode> topPages = webPageRepository.findBestPerformingPagesByDomain(domain, 5);
            navigationData.put("topPerformingPages", topPages);
            
            return ResponseEntity.ok(navigationData);
        } catch (Exception e) {
            log.error("Failed to get navigation patterns for domain {}: {}", domain, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get task flow analysis - sequences and patterns
     */
    @GetMapping("/tasks/{taskId}/flow")
    public ResponseEntity<Map<String, Object>> getTaskFlow(@PathVariable String taskId) {
        try {
            TaskNode task = taskRepository.findByTaskId(taskId).orElse(null);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> flowData = new HashMap<>();
            flowData.put("task", task);
            
            // Get subsequent tasks
            List<TaskNode> subsequentTasks = taskRepository.findSubsequentTasks(taskId);
            flowData.put("subsequentTasks", subsequentTasks);
            
            // Get previous tasks
            List<TaskNode> previousTasks = taskRepository.findPreviousTasks(taskId);
            flowData.put("previousTasks", previousTasks);
            
            // Get similar tasks
            List<TaskNode> similarTasks = taskRepository.findSimilarTasks(taskId);
            flowData.put("similarTasks", similarTasks);
            
            return ResponseEntity.ok(flowData);
        } catch (Exception e) {
            log.error("Failed to get task flow for {}: {}", taskId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get screenshot statistics and patterns
     */
    @GetMapping("/screenshots/stats")
    public ResponseEntity<Map<String, Object>> getScreenshotStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get basic counts
            long totalScreenshots = screenshotRepository.count();
            stats.put("totalScreenshots", totalScreenshots);
            
            // Get embedding statistics
            ScreenshotEmbeddingService.EmbeddingStats embeddingStats = embeddingService.getEmbeddingStats();
            stats.put("embeddingStats", embeddingStats);
            
            // Get domain distribution
            List<ScreenshotNodeRepository.DomainCount> domainCounts = screenshotRepository.getTopDomainsByScreenshotCount(10);
            stats.put("domainDistribution", domainCounts);
            
            // Get UI pattern distribution
            List<ScreenshotNodeRepository.PatternCount> patternCounts = screenshotRepository.getUIPatternStatistics();
            stats.put("uiPatternDistribution", patternCounts);
            
            // Get high quality screenshots
            List<ScreenshotNode> highQualityScreenshots = screenshotRepository.findHighQualityScreenshots(0.8, 5);
            stats.put("highQualityExamples", highQualityScreenshots);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get screenshot statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get task performance analytics by type
     */
    @GetMapping("/tasks/performance")
    public ResponseEntity<Map<String, Object>> getTaskPerformance(
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> performance = new HashMap<>();
            
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            if (taskType != null) {
                // Get specific task type statistics
                TaskNodeRepository.TaskTypeStats stats = taskRepository.getTaskTypeStatistics(taskType);
                performance.put("taskTypeStats", stats);
                
                // Get recent successful tasks
                List<TaskNode> recentSuccessful = taskRepository.findFastestSuccessfulTasks(taskType, 10);
                performance.put("recentSuccessfulTasks", recentSuccessful);
                
                // Get recent failed tasks
                List<TaskNode> recentFailed = taskRepository.findRecentFailedTasks(taskType, 5);
                performance.put("recentFailedTasks", recentFailed);
            } else {
                // Get overall statistics
                TaskNodeRepository.PeriodStats periodStats = taskRepository.getPeriodStatistics(since);
                performance.put("periodStats", periodStats);
                
                // Get task type success rates
                List<TaskNodeRepository.TaskTypeSuccessRate> successRates = taskRepository.getTaskTypeSuccessRates();
                performance.put("taskTypeSuccessRates", successRates);
            }
            
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            log.error("Failed to get task performance analytics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get web page effectiveness analysis
     */
    @GetMapping("/pages/effectiveness")
    public ResponseEntity<Map<String, Object>> getPageEffectiveness(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Map<String, Object> effectiveness = new HashMap<>();
            
            // Get high performing pages
            List<WebPageNode> highPerforming = webPageRepository.findHighPerformingPages(0.7, 3, limit);
            effectiveness.put("highPerformingPages", highPerforming);
            
            // Get problematic pages
            List<WebPageNode> problematic = webPageRepository.findProblematicPages(0.5, 0.2, limit);
            effectiveness.put("problematicPages", problematic);
            
            // Get entry pages
            List<WebPageNode> entryPages = webPageRepository.findEntryPages();
            effectiveness.put("entryPages", entryPages.stream().limit(limit).toList());
            
            // Get exit pages
            List<WebPageNode> exitPages = webPageRepository.findExitPages();
            effectiveness.put("exitPages", exitPages.stream().limit(limit).toList());
            
            // Get domain statistics
            List<WebPageNodeRepository.DomainStats> domainStats = webPageRepository.getDomainStatistics(10);
            effectiveness.put("domainStats", domainStats);
            
            return ResponseEntity.ok(effectiveness);
        } catch (Exception e) {
            log.error("Failed to get page effectiveness analysis: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get knowledge graph overview - high-level statistics
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getKnowledgeGraphOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // Task statistics
            long totalTasks = taskRepository.count();
            overview.put("totalTasks", totalTasks);
            
            // Screenshot statistics
            long totalScreenshots = screenshotRepository.count();
            overview.put("totalScreenshots", totalScreenshots);
            
            // Web page statistics
            long totalPages = webPageRepository.count();
            overview.put("totalPages", totalPages);
            
            // Recent activity - handle empty database gracefully
            LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
            try {
                List<TaskNode> recentTasks = taskRepository.findTasksSince(last24Hours);
                overview.put("tasksLast24Hours", recentTasks.size());
            } catch (Exception e) {
                log.debug("No recent tasks found: {}", e.getMessage());
                overview.put("tasksLast24Hours", 0);
            }
            
            try {
                long recentScreenshots = screenshotRepository.countScreenshotsSince(last24Hours);
                overview.put("screenshotsLast24Hours", recentScreenshots);
            } catch (Exception e) {
                log.debug("No recent screenshots found: {}", e.getMessage());
                overview.put("screenshotsLast24Hours", 0);
            }
            
            // Performance metrics - handle empty database and NULL values
            try {
                // Use custom query that handles NULL values properly
                LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                List<TaskNode> recentTasks = taskRepository.findTasksSince(weekAgo);
                
                Map<String, Object> weeklyData = new HashMap<>();
                weeklyData.put("totalTasks", (long) recentTasks.size());
                
                long successfulTasks = recentTasks.stream()
                    .mapToLong(t -> Boolean.TRUE.equals(t.getIsSuccessful()) ? 1 : 0)
                    .sum();
                weeklyData.put("successfulTasks", successfulTasks);
                
                long failedTasks = recentTasks.stream()
                    .mapToLong(t -> "FAILED".equals(t.getStatus()) ? 1 : 0)
                    .sum();
                weeklyData.put("failedTasks", failedTasks);
                
                // Calculate average duration only for tasks that have duration
                double avgDuration = recentTasks.stream()
                    .filter(t -> t.getDurationSeconds() != null)
                    .mapToLong(TaskNode::getDurationSeconds)
                    .average()
                    .orElse(0.0);
                weeklyData.put("avgDuration", avgDuration);
                
                overview.put("weeklyPerformance", weeklyData);
            } catch (Exception e) {
                log.debug("No weekly performance data: {}", e.getMessage());
                overview.put("weeklyPerformance", createEmptyStats());
            }
            
            // Database status
            overview.put("databaseInitialized", totalTasks > 0 || totalScreenshots > 0 || totalPages > 0);
            overview.put("status", "operational");
            
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Failed to get knowledge graph overview: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private Map<String, Object> createEmptyStats() {
        Map<String, Object> emptyStats = new HashMap<>();
        emptyStats.put("totalTasks", 0L);
        emptyStats.put("successfulTasks", 0L);
        emptyStats.put("failedTasks", 0L);
        emptyStats.put("avgDuration", 0.0);
        return emptyStats;
    }
    
    /**
     * Search screenshots by extracted text
     */
    @GetMapping("/screenshots/search")
    public ResponseEntity<List<ScreenshotNode>> searchScreenshots(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ScreenshotNode> results = screenshotRepository.findByExtractedText(query, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to search screenshots for '{}': {}", query, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}