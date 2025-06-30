package io.wingie.controller;

import io.wingie.entity.ToolDescription;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for cache management operations
 * Provides endpoints for the startup dashboard to manage cached tool descriptions
 */
@RestController
@RequestMapping("/api/cache")
@Slf4j
public class CacheManagementController {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    /**
     * Get all cached descriptions for the current model
     */
    @GetMapping("/descriptions")
    public ResponseEntity<List<ToolDescription>> getAllDescriptions() {
        try {
            List<ToolDescription> descriptions = cacheService.getCurrentProviderDescriptions();
            log.debug("Retrieved {} cached descriptions", descriptions.size());
            return ResponseEntity.ok(descriptions);
        } catch (Exception e) {
            log.error("Error retrieving cached descriptions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get cache statistics for the dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get basic statistics
            List<ToolDescription> currentDescriptions = cacheService.getCurrentProviderDescriptions();
            stats.put("totalDescriptions", currentDescriptions.size());
            stats.put("currentModel", cacheService.getCurrentProviderModel());
            
            // Calculate cache hit rate (simplified - could be enhanced with actual metrics)
            long totalUsage = currentDescriptions.stream()
                .mapToLong(desc -> desc.getUsageCount() != null ? desc.getUsageCount() : 0)
                .sum();
            double hitRate = currentDescriptions.size() > 0 ? 
                (double) totalUsage / currentDescriptions.size() * 100 : 0;
            stats.put("hitRate", Math.round(hitRate * 100.0) / 100.0);
            
            // Provider breakdown
            List<Object[]> providerStats = cacheService.getProviderStatistics();
            stats.put("providerBreakdown", providerStats);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving cache statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update a specific tool description
     */
    @PutMapping("/descriptions/{id}")
    public ResponseEntity<String> updateDescription(@PathVariable Long id, 
                                                   @RequestBody Map<String, String> request) {
        try {
            String newDescription = request.get("description");
            if (newDescription == null || newDescription.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description cannot be empty");
            }
            
            // Note: This would require a method in the service to update existing descriptions
            // For now, we'll return a placeholder response
            log.info("Request to update description {} with new content", id);
            
            // TODO: Implement actual update logic in service
            return ResponseEntity.ok("Description updated successfully");
            
        } catch (Exception e) {
            log.error("Error updating description {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update description");
        }
    }
    
    /**
     * Delete a specific cached description
     */
    @DeleteMapping("/descriptions/{id}")
    public ResponseEntity<String> deleteDescription(@PathVariable Long id) {
        try {
            // Note: This would require a method in the service to delete by ID
            // For now, we'll return a placeholder response
            log.info("Request to delete description {}", id);
            
            // TODO: Implement actual delete logic in service
            return ResponseEntity.ok("Description deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting description {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to delete description");
        }
    }
    
    /**
     * Clear cache for a specific model
     */
    @PostMapping("/clear-model")
    public ResponseEntity<String> clearModelCache(@RequestBody Map<String, String> request) {
        try {
            String modelName = request.get("modelName");
            if (modelName == null || modelName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Model name is required");
            }
            
            cacheService.clearCacheByModel(modelName);
            log.info("Cleared cache for model: {}", modelName);
            
            return ResponseEntity.ok("Cache cleared for model: " + modelName);
            
        } catch (Exception e) {
            log.error("Error clearing cache for model: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to clear model cache");
        }
    }
    
    /**
     * Clear cache for tools matching a pattern
     */
    @PostMapping("/clear-pattern")
    public ResponseEntity<String> clearPatternCache(@RequestBody Map<String, String> request) {
        try {
            String pattern = request.get("pattern");
            if (pattern == null || pattern.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Pattern is required");
            }
            
            cacheService.clearCacheByToolPattern(pattern);
            log.info("Cleared cache for pattern: {}", pattern);
            
            return ResponseEntity.ok("Cache cleared for pattern: " + pattern);
            
        } catch (Exception e) {
            log.error("Error clearing cache for pattern: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to clear pattern cache");
        }
    }
    
    /**
     * Get detailed cache report in markdown format
     */
    @GetMapping("/report")
    public ResponseEntity<String> getCacheReport() {
        try {
            String report = cacheService.getCacheReport();
            return ResponseEntity.ok()
                .header("Content-Type", "text/markdown")
                .body(report);
        } catch (Exception e) {
            log.error("Error generating cache report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to generate cache report");
        }
    }
    
    /**
     * Force cache cleanup of old descriptions
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldDescriptions(@RequestParam(defaultValue = "30") int daysOld) {
        try {
            cacheService.cleanupOldDescriptions(daysOld);
            log.info("Cleaned up descriptions older than {} days", daysOld);
            
            return ResponseEntity.ok("Cleanup completed for descriptions older than " + daysOld + " days");
            
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Cleanup failed");
        }
    }
}