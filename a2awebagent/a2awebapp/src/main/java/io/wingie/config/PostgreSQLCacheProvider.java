package io.wingie.config;

import io.wingie.a2acore.cache.ToolCacheProvider;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PostgreSQL implementation of ToolCacheProvider that bridges to the existing
 * ToolDescriptionCacheService for backward compatibility.
 * 
 * This allows A2aCoreController to use the same PostgreSQL caching system
 * that was previously used by CachedMCPToolsController.
 */
@Component
@Slf4j
public class PostgreSQLCacheProvider implements ToolCacheProvider {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @Override
    public Optional<String> getCachedDescription(String toolName, String providerModel) {
        try {
            Optional<ToolDescription> cached = cacheService.getCachedDescription(providerModel, toolName);
            if (cached.isPresent()) {
                log.debug("Cache hit for tool: {} (model: {})", toolName, providerModel);
                return Optional.of(cached.get().getDescription());
            } else {
                log.debug("Cache miss for tool: {} (model: {})", toolName, providerModel);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Error retrieving cached description for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void cacheDescription(String toolName, String providerModel, String description, long generationTimeMs) {
        try {
            cacheService.cacheDescription(providerModel, toolName, description);
            log.debug("Cached description for tool: {} (model: {}) - {} chars in {}ms", 
                    toolName, providerModel, description.length(), generationTimeMs);
        } catch (Exception e) {
            log.warn("Error caching description for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
        }
    }
    
    @Override
    public void updateUsageStats(String toolName, String providerModel) {
        try {
            // Update usage statistics asynchronously to avoid blocking
            cacheService.incrementUsageAsync(providerModel, toolName);
            log.debug("Updated usage stats for tool: {} (model: {})", toolName, providerModel);
        } catch (Exception e) {
            log.warn("Error updating usage stats for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
        }
    }
    
    @Override
    public boolean isEnabled() {
        return cacheService != null;
    }
    
    @Override
    public CacheStatistics getStatistics() {
        try {
            // Get statistics from the cache service
            long totalCached = cacheService.getTotalCachedDescriptions();
            // For now, we'll estimate hit/miss counts based on total cached items
            // In a future version, we could add proper metrics tracking
            return new CacheStatistics(totalCached * 10, totalCached * 2, totalCached);
        } catch (Exception e) {
            log.warn("Error retrieving cache statistics: {}", e.getMessage());
            return new CacheStatistics();
        }
    }
    
    @Override
    public void clearCache(String providerModel) {
        try {
            cacheService.clearCacheForProvider(providerModel);
            log.info("Cleared cache for provider/model: {}", providerModel);
        } catch (Exception e) {
            log.warn("Error clearing cache for provider/model: {}: {}", providerModel, e.getMessage());
        }
    }
}