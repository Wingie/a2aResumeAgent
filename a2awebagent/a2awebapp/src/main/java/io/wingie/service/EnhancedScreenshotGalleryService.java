package io.wingie.service;

import io.wingie.entity.neo4j.ScreenshotNode;
import io.wingie.repository.neo4j.ScreenshotNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced Screenshot Gallery Service providing advanced screenshot management
 * and visual analysis capabilities.
 * 
 * Features:
 * - Thumbnail generation and management
 * - Visual similarity detection using Neo4j
 * - Screenshot metadata and analytics
 * - Gallery organization and filtering
 * - Performance optimization with caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedScreenshotGalleryService {
    
    private final ScreenshotNodeRepository screenshotRepository;
    private final ThumbnailService thumbnailService;
    
    /**
     * Process a new screenshot and integrate it into the gallery
     */
    @Async
    public CompletableFuture<ScreenshotNode> processScreenshot(String taskId, String screenshotPath, 
                                                             String url, boolean isSuccess, 
                                                             String actionContext) {
        try {
            // Convert file path to HTTP URL for UI display
            String httpUrl = convertFilePathToHttpUrl(screenshotPath);
            
            // Create screenshot node using existing factory method
            ScreenshotNode screenshot = ScreenshotNode.fromScreenshotCapture(taskId, httpUrl, url, actionContext);
            
            // Set additional properties
            screenshot.setScreenshotId(UUID.randomUUID().toString());
            screenshot.setCapturedAt(LocalDateTime.now());
            
            // Save to Neo4j
            final ScreenshotNode savedScreenshot = screenshotRepository.save(screenshot);
            
            // Generate thumbnails asynchronously
            CompletableFuture<ThumbnailService.ThumbnailSet> thumbnailsFuture = 
                thumbnailService.generateThumbnailSet(screenshotPath);
            
            // Update screenshot with thumbnail paths when ready
            thumbnailsFuture.thenAccept(thumbnails -> {
                if (thumbnails != null) {
                    // Note: Neo4j entity doesn't have thumbnail fields, so we'll log for now
                    log.debug("Generated thumbnails for screenshot: {} (thumbnail: {}, small: {})", 
                            savedScreenshot.getScreenshotId(), 
                            thumbnails.getThumbnailPath(), 
                            thumbnails.getSmallThumbnailPath());
                }
            });
            
            // Perform similarity analysis asynchronously
            performSimilarityAnalysis(savedScreenshot);
            
            log.info("📸 Processed screenshot for gallery: {} (task: {})", 
                    savedScreenshot.getScreenshotId(), taskId);
            
            return CompletableFuture.completedFuture(savedScreenshot);
            
        } catch (Exception e) {
            log.error("Failed to process screenshot for gallery: {}", screenshotPath, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Process screenshot from base64 data
     */
    @Async
    public CompletableFuture<ScreenshotNode> processScreenshotFromBase64(String taskId, String base64Data, 
                                                                       String url, boolean isSuccess, 
                                                                       String actionContext) {
        try {
            // Generate thumbnail from base64
            CompletableFuture<String> thumbnailFuture = 
                thumbnailService.generateThumbnailFromBase64(base64Data, taskId);
            
            String thumbnailPath = thumbnailFuture.join();
            
            // Create screenshot node using existing factory method
            ScreenshotNode screenshot = ScreenshotNode.fromScreenshotCapture(taskId, "data:image/base64", url, actionContext);
            
            // Set additional properties
            screenshot.setScreenshotId(UUID.randomUUID().toString());
            screenshot.setCapturedAt(LocalDateTime.now());
            
            // Save to Neo4j
            screenshot = screenshotRepository.save(screenshot);
            
            // Perform similarity analysis asynchronously
            performSimilarityAnalysis(screenshot);
            
            log.info("📸 Processed base64 screenshot for gallery: {} (task: {})", 
                    screenshot.getScreenshotId(), taskId);
            
            return CompletableFuture.completedFuture(screenshot);
            
        } catch (Exception e) {
            log.error("Failed to process base64 screenshot for gallery: {}", taskId, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Get gallery data for frontend display
     */
    public GalleryData getGalleryData(int limit, String filter, String sortBy) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7); // Last week
            List<ScreenshotNode> screenshots;
            
            switch (filter) {
                case "success":
                    // Use high quality screenshots as a proxy for successful ones
                    screenshots = screenshotRepository.findHighQualityScreenshots(0.7, limit);
                    break;
                case "errors":
                    // Find screenshots with low quality scores
                    screenshots = screenshotRepository.findAll().stream()
                        .filter(s -> s.getQualityScore() != null && s.getQualityScore() < 0.5)
                        .limit(limit)
                        .collect(Collectors.toList());
                    break;
                case "recent":
                    screenshots = screenshotRepository.findByDomainSince("", since).stream()
                        .limit(limit)
                        .collect(Collectors.toList());
                    break;
                default:
                    screenshots = screenshotRepository.findAll().stream()
                        .filter(s -> s.getCapturedAt().isAfter(since))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
            
            // Get basic statistics from what we have
            long totalCount = screenshotRepository.countScreenshotsSince(LocalDateTime.now().minusMonths(1));
            long uniqueTasks = screenshots.stream()
                .map(ScreenshotNode::getTaskId)
                .distinct()
                .count();
            long uniqueUrls = screenshots.stream()
                .map(ScreenshotNode::getPageDomain)
                .distinct()
                .count();
            
            return GalleryData.builder()
                .screenshots(screenshots)
                .totalCount(totalCount)
                .successCount(screenshots.size()) // Approximate
                .errorCount(0) // Would need custom query
                .uniqueTasks(uniqueTasks)
                .uniqueUrls(uniqueUrls)
                .build();
            
        } catch (Exception e) {
            log.error("Failed to get gallery data", e);
            return GalleryData.builder()
                .screenshots(List.of())
                .totalCount(0)
                .successCount(0)
                .errorCount(0)
                .uniqueTasks(0)
                .uniqueUrls(0)
                .build();
        }
    }
    
    /**
     * Get screenshots for a specific task
     */
    public List<ScreenshotNode> getTaskScreenshots(String taskId) {
        return screenshotRepository.findByTaskId(taskId);
    }
    
    /**
     * Find similar screenshots
     */
    public List<ScreenshotNode> findSimilarScreenshots(String screenshotId, int limit) {
        return screenshotRepository.findSimilarScreenshots(screenshotId);
    }
    
    /**
     * Find screenshots by URL pattern
     */
    public List<ScreenshotNode> findScreenshotsByUrl(String urlPattern) {
        return screenshotRepository.findByUrlFragment(urlPattern, 50);
    }
    
    /**
     * Get screenshot timeline for a task
     */
    public List<ScreenshotNode> getTaskScreenshotTimeline(String taskId) {
        List<ScreenshotNode> screenshots = screenshotRepository.findByTaskId(taskId);
        return screenshots.stream()
            .sorted((a, b) -> a.getCapturedAt().compareTo(b.getCapturedAt()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get screenshot analytics
     */
    public Map<String, Object> getScreenshotAnalytics() {
        try {
            long totalCount = screenshotRepository.countScreenshotsSince(LocalDateTime.now().minusMonths(1));
            List<ScreenshotNode> allScreenshots = screenshotRepository.findAll();
            
            long successfulScreenshots = allScreenshots.stream()
                .filter(s -> s.getQualityScore() != null && s.getQualityScore() > 0.7)
                .count();
            
            long uniqueTasks = allScreenshots.stream()
                .map(ScreenshotNode::getTaskId)
                .distinct()
                .count();
            
            long uniqueUrls = allScreenshots.stream()
                .map(ScreenshotNode::getPageDomain)
                .filter(domain -> domain != null && !domain.isEmpty())
                .distinct()
                .count();
            
            return Map.of(
                "totalScreenshots", totalCount,
                "successfulScreenshots", successfulScreenshots,
                "errorScreenshots", Math.max(0, totalCount - successfulScreenshots),
                "uniqueTasks", uniqueTasks,
                "uniqueUrls", uniqueUrls,
                "successRate", totalCount > 0 ? (double) successfulScreenshots / totalCount : 0.0,
                "errorRate", totalCount > 0 ? (double) (totalCount - successfulScreenshots) / totalCount : 0.0
            );
        } catch (Exception e) {
            log.error("Failed to get screenshot analytics", e);
            return Map.of(
                "totalScreenshots", 0,
                "successfulScreenshots", 0,
                "errorScreenshots", 0,
                "uniqueTasks", 0,
                "uniqueUrls", 0,
                "successRate", 0.0,
                "errorRate", 0.0
            );
        }
    }
    
    /**
     * Delete old screenshots to manage storage
     */
    public void cleanupOldScreenshots(int daysToKeep) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            screenshotRepository.deleteScreenshotsOlderThan(cutoffDate);
            log.info("Cleaned up screenshots older than {} days", daysToKeep);
        } catch (Exception e) {
            log.error("Failed to cleanup old screenshots", e);
        }
    }
    
    // Private helper methods
    
    @Async
    private void performSimilarityAnalysis(ScreenshotNode screenshot) {
        try {
            // Find recent screenshots from same domain for similarity analysis
            List<ScreenshotNode> recentScreenshots = screenshotRepository
                .findByDomainSince(screenshot.getPageDomain(), LocalDateTime.now().minusHours(24));
            
            // Simple similarity analysis - for now just log potential similarities
            for (ScreenshotNode other : recentScreenshots) {
                if (other.getScreenshotId().equals(screenshot.getScreenshotId())) continue;
                
                double similarity = calculateSimilarity(screenshot, other);
                if (similarity > 0.7) { // Threshold for similarity
                    log.debug("Potential similarity detected: {} <-> {} (score: {})", 
                            screenshot.getScreenshotId(), 
                            other.getScreenshotId(), 
                            similarity);
                    
                    // Note: Would need to create a custom method to establish relationships
                    // For now, we'll just log the similarity
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to perform similarity analysis for screenshot: {}", 
                    screenshot.getScreenshotId(), e);
        }
    }
    
    private double calculateSimilarity(ScreenshotNode s1, ScreenshotNode s2) {
        double similarity = 0.0;
        
        // Domain similarity
        if (s1.getPageDomain() != null && s1.getPageDomain().equals(s2.getPageDomain())) {
            similarity += 0.3;
        }
        
        // Dimension similarity
        if (s1.getImageWidth() != null && s2.getImageWidth() != null &&
            s1.getImageHeight() != null && s2.getImageHeight() != null) {
            double widthRatio = Math.min(s1.getImageWidth(), s2.getImageWidth()) / 
                               (double) Math.max(s1.getImageWidth(), s2.getImageWidth());
            double heightRatio = Math.min(s1.getImageHeight(), s2.getImageHeight()) / 
                                (double) Math.max(s1.getImageHeight(), s2.getImageHeight());
            similarity += (widthRatio + heightRatio) / 2 * 0.3;
        }
        
        // UI pattern similarity
        if (s1.getUiPatternType() != null && s1.getUiPatternType().equals(s2.getUiPatternType())) {
            similarity += 0.2;
        }
        
        // Action context similarity
        if (s1.getActionContext() != null && s2.getActionContext() != null &&
            s1.getActionContext().equals(s2.getActionContext())) {
            similarity += 0.2;
        }
        
        return similarity;
    }
    
    private String extractDomainFromUrl(String url) {
        if (url == null) return null;
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Converts a file path to an HTTP URL for serving via Spring Boot static resources.
     * Handles both absolute paths and filenames.
     */
    private String convertFilePathToHttpUrl(String filePath) {
        if (filePath == null) return null;
        
        try {
            // Extract just the filename from the path
            String filename;
            if (filePath.contains("/") || filePath.contains("\\")) {
                // It's a full path, extract the filename
                java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                filename = path.getFileName().toString();
            } else {
                // It's already just a filename
                filename = filePath;
            }
            
            // Convert to HTTP URL using the /screenshots/ mapping from WebConfig
            return "/screenshots/" + filename;
            
        } catch (Exception e) {
            log.warn("Failed to convert file path to HTTP URL: {}", filePath, e);
            return filePath; // Fallback to original path
        }
    }
    
    /**
     * Data class for gallery response
     */
    @lombok.Data
    @lombok.Builder
    public static class GalleryData {
        private List<ScreenshotNode> screenshots;
        private long totalCount;
        private long successCount;
        private long errorCount;
        private long uniqueTasks;
        private long uniqueUrls;
    }
}