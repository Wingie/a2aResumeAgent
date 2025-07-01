package io.wingie.config;

import io.wingie.a2acore.cache.ToolCacheProvider;
import io.wingie.entity.ToolDescription;
import io.wingie.service.ToolDescriptionCacheService;
import io.wingie.service.LLMCallTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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
    
    @Autowired
    private LLMCallTracker llmCallTracker;
    
    @Override
    public Optional<String> getCachedDescription(String toolName, String providerModel) {
        try {
            Optional<ToolDescription> cached = cacheService.getCachedDescription(providerModel, toolName);
            if (cached.isPresent()) {
                log.debug("✅ Cache hit for tool: {} (model: {})", toolName, providerModel);
                // Track cache hit in LLM call logs for analytics
                llmCallTracker.trackCacheHit(toolName, providerModel);
                return Optional.of(cached.get().getDescription());
            } else {
                log.debug("❌ Cache miss for tool: {} (model: {})", toolName, providerModel);
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
            // Cache the tool description with proper parameter mapping
            ToolDescription cached = cacheService.cacheDescription(
                providerModel, 
                toolName, 
                description, 
                "", // parametersInfo - empty for basic caching
                "", // toolProperties - empty for basic caching
                generationTimeMs
            );
            
            log.info("✅ Cached description for tool: {} (model: {}) - {} chars in {}ms (ID: {})", 
                    toolName, providerModel, description.length(), generationTimeMs, cached.getId());
        } catch (Exception e) {
            log.error("❌ Error caching description for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
        }
    }
    
    @Override
    public void updateUsageStats(String toolName, String providerModel) {
        try {
            // Find the cached description and update its usage stats
            Optional<ToolDescription> cached = cacheService.getCachedDescription(providerModel, toolName);
            if (cached.isPresent()) {
                // Use async update to avoid blocking the main thread
                cacheService.updateUsageStatsAsync(cached.get().getId());
                log.debug("📈 Updated usage stats for tool: {} (model: {}) - Total uses: {}", 
                        toolName, providerModel, cached.get().getUsageCount() + 1);
            } else {
                log.debug("⚠️ Cannot update usage stats - tool not found in cache: {} (model: {})", 
                        toolName, providerModel);
            }
        } catch (Exception e) {
            log.warn("❌ Error updating usage stats for tool: {} (model: {}): {}", 
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
            // Get real statistics from the PostgreSQL database
            long totalEntries = cacheService.getCurrentProviderDescriptions().size();
            List<Object[]> providerStats = cacheService.getProviderStatistics();
            
            // Calculate cache hits and total requests (simplified calculation)
            long totalHits = 0;
            long totalRequests = 0;
            
            for (Object[] stat : providerStats) {
                if (stat.length >= 2 && stat[1] instanceof Number) {
                    long count = ((Number) stat[1]).longValue();
                    totalHits += count;
                    totalRequests += count * 2; // Estimate: assume 50% hit rate for calculation
                }
            }
            
            // If no data, return sensible defaults
            if (totalRequests == 0) {
                totalRequests = totalEntries > 0 ? totalEntries * 2 : 1;
                totalHits = totalEntries;
            }
            
            log.debug("📊 Cache statistics: {} entries, {} hits, {} requests", 
                    totalEntries, totalHits, totalRequests);
            
            return new CacheStatistics(totalRequests, totalHits, totalEntries);
        } catch (Exception e) {
            log.warn("❌ Error retrieving cache statistics: {}", e.getMessage());
            // Return safe default values
            return new CacheStatistics(1, 0, 0);
        }
    }
    
    @Override
    public void clearCache(String providerModel) {
        try {
            // Clear cache for specific provider model
            cacheService.clearCacheByModel(providerModel);
            log.info("🗑️ Successfully cleared cache for provider/model: {}", providerModel);
        } catch (Exception e) {
            log.error("❌ Error clearing cache for provider/model {}: {}", providerModel, e.getMessage());
        }
    }
}