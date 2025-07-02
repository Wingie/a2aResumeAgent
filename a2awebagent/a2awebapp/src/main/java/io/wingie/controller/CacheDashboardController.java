package io.wingie.controller;

import io.wingie.entity.ToolDescription;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the cache dashboard UI - displays PostgreSQL tool description cache
 */
@Controller
@RequestMapping("/cache")
@Slf4j
public class CacheDashboardController {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    /**
     * Display the cache dashboard with detailed PostgreSQL cache data
     */
    @GetMapping
    public String cacheDashboard(Model model) {
        log.info("Cache dashboard accessed");
        
        try {
            // Get current cache data
            List<ToolDescription> descriptions = cacheService.getCurrentProviderDescriptions();
            String currentModel = cacheService.getCurrentProviderModel();
            List<Object[]> providerStats = cacheService.getProviderStatistics();
            
            // Calculate cache metrics
            Map<String, Object> metrics = calculateCacheMetrics(descriptions);
            
            // Add data to model for the template
            model.addAttribute("descriptions", descriptions);
            model.addAttribute("currentModel", currentModel);
            model.addAttribute("providerStats", providerStats);
            model.addAttribute("metrics", metrics);
            model.addAttribute("cacheReport", cacheService.getCacheReport());
            
            return "cache-dashboard";
            
        } catch (Exception e) {
            log.error("Error loading cache dashboard: {}", e.getMessage());
            model.addAttribute("error", "Failed to load cache data: " + e.getMessage());
            return "cache-dashboard";
        }
    }
    
    private Map<String, Object> calculateCacheMetrics(List<ToolDescription> descriptions) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (descriptions.isEmpty()) {
            metrics.put("totalTools", 0);
            metrics.put("averageGenerationTime", 0);
            metrics.put("totalUsage", 0);
            metrics.put("cacheEfficiency", 0);
            metrics.put("fastestTool", null);
            metrics.put("slowestTool", null);
            metrics.put("mostUsedTool", null);
            return metrics;
        }
        
        // Calculate statistics
        int totalTools = descriptions.size();
        long totalGenerationTime = descriptions.stream()
            .mapToLong(d -> d.getGenerationTimeMs() != null ? d.getGenerationTimeMs() : 0)
            .sum();
        int totalUsage = descriptions.stream()
            .mapToInt(ToolDescription::getUsageCount)
            .sum();
        
        double averageGenerationTime = totalGenerationTime / (double) totalTools;
        double cacheEfficiency = totalUsage > 0 ? (totalUsage / (double) totalTools) : 0;
        
        // Find fastest and slowest tools
        ToolDescription fastestTool = descriptions.stream()
            .filter(d -> d.getGenerationTimeMs() != null)
            .min((a, b) -> Long.compare(a.getGenerationTimeMs(), b.getGenerationTimeMs()))
            .orElse(null);
            
        ToolDescription slowestTool = descriptions.stream()
            .filter(d -> d.getGenerationTimeMs() != null)
            .max((a, b) -> Long.compare(a.getGenerationTimeMs(), b.getGenerationTimeMs()))
            .orElse(null);
            
        ToolDescription mostUsedTool = descriptions.stream()
            .max((a, b) -> Integer.compare(a.getUsageCount(), b.getUsageCount()))
            .orElse(null);
        
        metrics.put("totalTools", totalTools);
        metrics.put("averageGenerationTime", Math.round(averageGenerationTime));
        metrics.put("totalUsage", totalUsage);
        metrics.put("cacheEfficiency", Math.round(cacheEfficiency * 100) / 100.0);
        metrics.put("fastestTool", fastestTool);
        metrics.put("slowestTool", slowestTool);
        metrics.put("mostUsedTool", mostUsedTool);
        
        return metrics;
    }
}