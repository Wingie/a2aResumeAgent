package io.wingie.controller;

import io.wingie.entity.neo4j.ScreenshotNode;
import io.wingie.service.EnhancedScreenshotGalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Enhanced Screenshot Gallery functionality.
 * 
 * Provides endpoints for screenshot gallery management, similarity search,
 * analytics, and thumbnail generation.
 */
@RestController
@RequestMapping("/api/screenshots")
@RequiredArgsConstructor
@Slf4j
public class EnhancedScreenshotGalleryController {
    
    private final EnhancedScreenshotGalleryService galleryService;
    
    /**
     * Get gallery data with filtering and pagination
     */
    @GetMapping("/gallery")
    public ResponseEntity<EnhancedScreenshotGalleryService.GalleryData> getGallery(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "recent") String sortBy) {
        
        try {
            EnhancedScreenshotGalleryService.GalleryData galleryData = 
                galleryService.getGalleryData(limit, filter, sortBy);
            
            log.debug("Retrieved gallery data: {} screenshots", galleryData.getScreenshots().size());
            return ResponseEntity.ok(galleryData);
            
        } catch (Exception e) {
            log.error("Failed to get gallery data", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get screenshots for a specific task
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<ScreenshotNode>> getTaskScreenshots(@PathVariable String taskId) {
        try {
            List<ScreenshotNode> screenshots = galleryService.getTaskScreenshots(taskId);
            log.debug("Retrieved {} screenshots for task: {}", screenshots.size(), taskId);
            return ResponseEntity.ok(screenshots);
            
        } catch (Exception e) {
            log.error("Failed to get screenshots for task: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get screenshot timeline for a task
     */
    @GetMapping("/task/{taskId}/timeline")
    public ResponseEntity<List<ScreenshotNode>> getTaskScreenshotTimeline(@PathVariable String taskId) {
        try {
            List<ScreenshotNode> timeline = galleryService.getTaskScreenshotTimeline(taskId);
            log.debug("Retrieved screenshot timeline for task: {} ({} screenshots)", 
                     taskId, timeline.size());
            return ResponseEntity.ok(timeline);
            
        } catch (Exception e) {
            log.error("Failed to get screenshot timeline for task: {}", taskId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Find similar screenshots
     */
    @GetMapping("/{screenshotId}/similar")
    public ResponseEntity<List<ScreenshotNode>> findSimilarScreenshots(
            @PathVariable String screenshotId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<ScreenshotNode> similarScreenshots = 
                galleryService.findSimilarScreenshots(screenshotId, limit);
            
            log.debug("Found {} similar screenshots for: {}", similarScreenshots.size(), screenshotId);
            return ResponseEntity.ok(similarScreenshots);
            
        } catch (Exception e) {
            log.error("Failed to find similar screenshots for: {}", screenshotId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Find screenshots by URL pattern
     */
    @GetMapping("/search/url")
    public ResponseEntity<List<ScreenshotNode>> findScreenshotsByUrl(
            @RequestParam String urlPattern) {
        
        try {
            List<ScreenshotNode> screenshots = galleryService.findScreenshotsByUrl(urlPattern);
            log.debug("Found {} screenshots matching URL pattern: {}", screenshots.size(), urlPattern);
            return ResponseEntity.ok(screenshots);
            
        } catch (Exception e) {
            log.error("Failed to find screenshots by URL pattern: {}", urlPattern, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get screenshot analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getScreenshotAnalytics() {
        try {
            Map<String, Object> analytics = galleryService.getScreenshotAnalytics();
            log.debug("Retrieved screenshot analytics: {} total screenshots", 
                     analytics.get("totalScreenshots"));
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            log.error("Failed to get screenshot analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Manual screenshot processing endpoint (for testing)
     */
    @PostMapping("/process")
    public ResponseEntity<String> processScreenshot(
            @RequestParam String taskId,
            @RequestParam String screenshotPath,
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "true") boolean isSuccess,
            @RequestParam(required = false) String actionContext) {
        
        try {
            galleryService.processScreenshot(taskId, screenshotPath, url, isSuccess, actionContext);
            log.info("Manually processed screenshot: {} for task: {}", screenshotPath, taskId);
            return ResponseEntity.ok("Screenshot processed successfully");
            
        } catch (Exception e) {
            log.error("Failed to process screenshot: {}", screenshotPath, e);
            return ResponseEntity.internalServerError().body("Failed to process screenshot");
        }
    }
    
    /**
     * Manual screenshot processing from base64 (for testing)
     */
    @PostMapping("/process/base64")
    public ResponseEntity<String> processScreenshotFromBase64(
            @RequestParam String taskId,
            @RequestBody String base64Data,
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "true") boolean isSuccess,
            @RequestParam(required = false) String actionContext) {
        
        try {
            galleryService.processScreenshotFromBase64(taskId, base64Data, url, isSuccess, actionContext);
            log.info("Manually processed base64 screenshot for task: {}", taskId);
            return ResponseEntity.ok("Base64 screenshot processed successfully");
            
        } catch (Exception e) {
            log.error("Failed to process base64 screenshot for task: {}", taskId, e);
            return ResponseEntity.internalServerError().body("Failed to process base64 screenshot");
        }
    }
    
    /**
     * Get screenshot gallery page
     */
    @GetMapping("/gallery-page")
    public String getGalleryPage() {
        return "screenshot-gallery";
    }
    
    /**
     * Cleanup old screenshots
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldScreenshots(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        
        try {
            galleryService.cleanupOldScreenshots(daysToKeep);
            log.info("Cleaned up screenshots older than {} days", daysToKeep);
            return ResponseEntity.ok("Screenshots cleaned up successfully");
            
        } catch (Exception e) {
            log.error("Failed to cleanup old screenshots", e);
            return ResponseEntity.internalServerError().body("Failed to cleanup screenshots");
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> analytics = galleryService.getScreenshotAnalytics();
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "totalScreenshots", analytics.get("totalScreenshots"),
                "successRate", analytics.get("successRate"),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Screenshot gallery health check failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}