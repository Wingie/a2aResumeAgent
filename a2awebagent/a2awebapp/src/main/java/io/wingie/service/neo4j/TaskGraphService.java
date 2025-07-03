package io.wingie.service.neo4j;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.neo4j.ScreenshotNode;
import io.wingie.entity.neo4j.TaskNode;
import io.wingie.entity.neo4j.WebPageNode;
import io.wingie.repository.neo4j.ScreenshotNodeRepository;
import io.wingie.repository.neo4j.TaskNodeRepository;
import io.wingie.repository.neo4j.WebPageNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing TaskExecution data in Neo4j knowledge graph.
 * Provides async synchronization between PostgreSQL operational data and Neo4j analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskGraphService {
    
    private final TaskNodeRepository taskNodeRepository;
    private final ScreenshotNodeRepository screenshotNodeRepository;
    private final WebPageNodeRepository webPageNodeRepository;
    
    @Autowired(required = false) // Optional dependency for graceful degradation
    private ScreenshotEmbeddingService screenshotEmbeddingService;
    
    /**
     * Asynchronously logs a task execution to Neo4j knowledge graph.
     * Called from TaskExecutionIntegrationService after task completion.
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> logTaskToGraph(TaskExecution taskExecution) {
        try {
            log.debug("🔗 Starting Neo4j graph logging for task: {}", taskExecution.getTaskId());
            long startTime = System.currentTimeMillis();
            
            // Create or update TaskNode
            TaskNode taskNode = createOrUpdateTaskNode(taskExecution);
            
            // Process screenshots if present
            if (taskExecution.getScreenshots() != null && !taskExecution.getScreenshots().isEmpty()) {
                processScreenshots(taskNode, taskExecution.getScreenshots());
            }
            
            // Extract and process visited pages (if we can parse them from context)
            processWebPages(taskNode, taskExecution);
            
            // Establish relationships with similar tasks
            establishTaskRelationships(taskNode);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Neo4j graph logging completed for task {} in {}ms", taskExecution.getTaskId(), duration);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("❌ Failed to log task {} to Neo4j graph: {}", taskExecution.getTaskId(), e.getMessage(), e);
            // Don't propagate exception - Neo4j logging is non-critical
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Creates or updates TaskNode from TaskExecution entity
     */
    private TaskNode createOrUpdateTaskNode(TaskExecution taskExecution) {
        TaskNode taskNode = taskNodeRepository.findByTaskId(taskExecution.getTaskId())
            .orElse(TaskNode.fromTaskExecution(taskExecution));
        
        // Update with latest data
        taskNode.setStatus(taskExecution.getStatus().name());
        taskNode.setCompletedAt(taskExecution.getCompletedAt());
        taskNode.setDurationSeconds(taskExecution.getActualDurationSeconds() != null ? 
            taskExecution.getActualDurationSeconds().longValue() : null);
        taskNode.setScreenshotCount(taskExecution.getScreenshots() != null ? 
            taskExecution.getScreenshots().size() : 0);
        taskNode.setIsSuccessful("COMPLETED".equals(taskExecution.getStatus().name()));
        
        return taskNodeRepository.save(taskNode);
    }
    
    /**
     * Processes screenshots and creates ScreenshotNode entities
     */
    private void processScreenshots(TaskNode taskNode, List<String> screenshotUrls) {
        log.debug("📸 Processing {} screenshots for task {}", screenshotUrls.size(), taskNode.getTaskId());
        
        for (String screenshotUrl : screenshotUrls) {
            try {
                // Generate unique screenshot ID using URL and timestamp
                String screenshotId = generateScreenshotId(taskNode.getTaskId(), screenshotUrl);
                
                // Check if screenshot already exists to avoid duplicates
                if (screenshotNodeRepository.findByScreenshotId(screenshotId).isEmpty()) {
                    
                    // Create ScreenshotNode with enhanced metadata
                    ScreenshotNode screenshotNode = ScreenshotNode.fromScreenshotCapture(
                        taskNode.getTaskId(), 
                        screenshotUrl, 
                        extractUrlFromScreenshot(screenshotUrl), 
                        "task_completion"
                    );
                    
                    // Save to Neo4j
                    screenshotNode = screenshotNodeRepository.save(screenshotNode);
                    
                    // Establish CAPTURES_SCREENSHOT relationship
                    taskNode.getScreenshots().add(screenshotNode);
                    
                    log.info("✅ Created ScreenshotNode {} for task {} with URL: {}", 
                        screenshotId, taskNode.getTaskId(), screenshotUrl);
                    
                    // Trigger async screenshot embedding processing if service available
                    if (screenshotEmbeddingService != null) {
                        screenshotEmbeddingService.processScreenshotEmbedding(screenshotId, screenshotUrl)
                            .thenAccept(result -> log.debug("📊 Screenshot embedding completed for {}", screenshotId))
                            .exceptionally(ex -> {
                                log.warn("⚠️ Screenshot embedding failed for {}: {}", screenshotId, ex.getMessage());
                                return null;
                            });
                    }
                } else {
                    log.debug("📸 Screenshot {} already exists for task {}, skipping", screenshotId, taskNode.getTaskId());
                }
            } catch (Exception e) {
                log.error("❌ Failed to process screenshot {} for task {}: {}", 
                    screenshotUrl, taskNode.getTaskId(), e.getMessage(), e);
            }
        }
        
        // Save TaskNode with updated screenshot relationships
        try {
            taskNodeRepository.save(taskNode);
            log.info("💾 Saved TaskNode {} with {} screenshot relationships", 
                taskNode.getTaskId(), taskNode.getScreenshots().size());
        } catch (Exception e) {
            log.error("❌ Failed to save TaskNode with screenshot relationships: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processes web pages visited during task execution
     */
    private void processWebPages(TaskNode taskNode, TaskExecution taskExecution) {
        // For now, extract URLs from extracted results or progress messages
        // In future, this could be enhanced with more sophisticated URL tracking
        String extractedResults = taskExecution.getExtractedResults();
        if (extractedResults != null) {
            List<String> urls = extractUrlsFromText(extractedResults);
            for (String url : urls) {
                processWebPage(taskNode, url);
            }
        }
    }
    
    /**
     * Creates or updates WebPageNode and establishes relationship with task
     */
    private void processWebPage(TaskNode taskNode, String url) {
        try {
            WebPageNode webPageNode = webPageNodeRepository.findByUrl(url)
                .orElse(new WebPageNode(url));
            
            // Update visit metrics
            webPageNode.recordVisit(
                taskNode.getDurationSeconds() != null ? taskNode.getDurationSeconds() : 0,
                1000.0 // Default load time - could be enhanced with actual measurements
            );
            
            webPageNode = webPageNodeRepository.save(webPageNode);
            
            // Establish relationship
            taskNode.getVisitedPages().add(webPageNode);
            log.debug("🌐 Logged web page visit {} for task {}", url, taskNode.getTaskId());
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to process web page {} for task {}: {}", 
                url, taskNode.getTaskId(), e.getMessage());
        }
    }
    
    /**
     * Establishes relationships between similar tasks
     */
    private void establishTaskRelationships(TaskNode taskNode) {
        try {
            // Find similar tasks by type and query similarity
            List<TaskNode> similarTasks = taskNodeRepository.findTasksByTypeAndQuery(
                taskNode.getTaskType(), 
                extractKeywordsFromQuery(taskNode.getOriginalQuery()),
                5
            );
            
            for (TaskNode similarTask : similarTasks) {
                if (!similarTask.getTaskId().equals(taskNode.getTaskId())) {
                    taskNode.getSimilarTasks().add(similarTask);
                }
            }
            
            // Save relationships
            if (!taskNode.getSimilarTasks().isEmpty()) {
                taskNodeRepository.save(taskNode);
                log.debug("🔗 Established {} similarity relationships for task {}", 
                    taskNode.getSimilarTasks().size(), taskNode.getTaskId());
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to establish relationships for task {}: {}", 
                taskNode.getTaskId(), e.getMessage());
        }
    }
    
    /**
     * Asynchronously processes screenshot embeddings (placeholder for future CLIP integration)
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processScreenshotEmbeddings(String taskId, List<String> screenshotUrls) {
        try {
            log.debug("🧠 Processing screenshot embeddings for task: {}", taskId);
            
            for (String screenshotUrl : screenshotUrls) {
                // Placeholder for CLIP embedding generation
                // This will be implemented in ScreenshotEmbeddingService
                log.debug("📊 Would generate embeddings for screenshot: {}", screenshotUrl);
            }
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("❌ Failed to process embeddings for task {}: {}", taskId, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Gets task analytics from Neo4j graph
     */
    public TaskAnalytics getTaskAnalytics(String taskId) {
        try {
            TaskNode taskNode = taskNodeRepository.findByTaskId(taskId).orElse(null);
            if (taskNode == null) {
                return null;
            }
            
            TaskAnalytics analytics = new TaskAnalytics();
            analytics.setTaskId(taskId);
            analytics.setSimilarTaskCount(taskNode.getSimilarTasks().size());
            analytics.setScreenshotCount(taskNode.getScreenshotCount());
            analytics.setPageVisitCount(taskNode.getPageVisitCount());
            analytics.setSuccessRate(taskNode.getSuccessRate());
            
            return analytics;
            
        } catch (Exception e) {
            log.error("❌ Failed to get analytics for task {}: {}", taskId, e.getMessage());
            return null;
        }
    }
    
    // Helper methods
    private String generateScreenshotId(String taskId, String screenshotUrl) {
        return taskId + "_" + Math.abs(screenshotUrl.hashCode());
    }
    
    private String extractUrlFromScreenshot(String screenshotUrl) {
        // Placeholder - in future, this could extract URL from screenshot metadata
        return "unknown";
    }
    
    private List<String> extractUrlsFromText(String text) {
        // Simple URL extraction - could be enhanced with proper regex
        return List.of(); // Placeholder
    }
    
    private String extractKeywordsFromQuery(String query) {
        if (query == null || query.length() < 10) return "";
        // Simple keyword extraction - take first few words
        String[] words = query.split("\\s+");
        return words.length > 0 ? words[0] : "";
    }
    
    // Analytics DTO
    public static class TaskAnalytics {
        private String taskId;
        private int similarTaskCount;
        private int screenshotCount;
        private int pageVisitCount;
        private double successRate;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public int getSimilarTaskCount() { return similarTaskCount; }
        public void setSimilarTaskCount(int similarTaskCount) { this.similarTaskCount = similarTaskCount; }
        
        public int getScreenshotCount() { return screenshotCount; }
        public void setScreenshotCount(int screenshotCount) { this.screenshotCount = screenshotCount; }
        
        public int getPageVisitCount() { return pageVisitCount; }
        public void setPageVisitCount(int pageVisitCount) { this.pageVisitCount = pageVisitCount; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}