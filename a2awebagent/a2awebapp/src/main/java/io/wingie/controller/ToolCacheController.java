package io.wingie.controller;

import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Simple REST controller for monitoring tool description cache
 */
@RestController
@RequestMapping("/api/tool-cache")
@Slf4j
public class ToolCacheController {

    @Autowired
    private ToolDescriptionCacheService cacheService;

    /**
     * Get cache effectiveness report
     */
    @GetMapping("/report")
    public String getCacheReport() {
        log.info("📊 Cache report requested");
        return cacheService.getCacheReport();
    }

    /**
     * Get provider statistics
     */
    @GetMapping("/stats")
    public List<Object[]> getProviderStats() {
        log.info("📈 Provider statistics requested");
        return cacheService.getProviderStatistics();
    }

    /**
     * Get current provider configuration
     */
    @GetMapping("/config")
    public Map<String, String> getCurrentConfig() {
        return Map.of(
            "currentProvider", cacheService.getCurrentProviderModel(),
            "status", "PostgreSQL caching enabled"
        );
    }

    /**
     * Check if specific tool is cached
     */
    @GetMapping("/check")
    public Map<String, Object> checkCache(
            @RequestParam String providerModel,
            @RequestParam String toolName) {
        
        boolean cached = cacheService.isDescriptionCached(providerModel, toolName);
        
        return Map.of(
            "providerModel", providerModel,
            "toolName", toolName,
            "cached", cached
        );
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "Tool Description Cache",
            "provider", cacheService.getCurrentProviderModel()
        );
    }
}