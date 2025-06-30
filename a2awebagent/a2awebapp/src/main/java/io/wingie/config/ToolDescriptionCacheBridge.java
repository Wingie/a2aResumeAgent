package io.wingie.config;

import io.wingie.service.ToolDescriptionCacheService;
import io.wingie.entity.ToolDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Local cache interface for tool descriptions
 */
interface LocalToolDescriptionCache {
    String getCachedDescription(String providerModel, String toolName);
    void cacheDescription(String providerModel, String toolName, String description);
    boolean isCacheAvailable();
}

/**
 * Bridge implementation that connects a local cache interface
 * with the a2awebagent's ToolDescriptionCacheService.
 * 
 * This allows the CachedMCPToolsController to use PostgreSQL caching
 * without depending on external interface definitions.
 */
@Component
@Primary
@Slf4j
public class ToolDescriptionCacheBridge implements LocalToolDescriptionCache {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @Override
    public String getCachedDescription(String providerModel, String toolName) {
        try {
            Optional<ToolDescription> cached = cacheService.getCachedDescription(providerModel, toolName);
            if (cached.isPresent()) {
                log.debug("Cache HIT for tool: {} (model: {})", toolName, providerModel);
                return cached.get().getDescription();
            } else {
                log.debug("Cache MISS for tool: {} (model: {})", toolName, providerModel);
                return null;
            }
        } catch (Exception e) {
            log.warn("Error retrieving cached description for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
            return null;
        }
    }
    
    @Override
    public void cacheDescription(String providerModel, String toolName, String description) {
        try {
            // Use the actual service method signature
            long startTime = System.currentTimeMillis();
            ToolDescription cached = cacheService.cacheDescription(
                providerModel, 
                toolName, 
                description, 
                "", // parametersInfo - empty for now
                "", // toolProperties - empty for now  
                System.currentTimeMillis() - startTime // generationTimeMs
            );
            log.info("Cached description for tool: {} (model: {}) - {} chars", 
                    toolName, providerModel, description.length());
        } catch (Exception e) {
            log.error("Error caching description for tool: {} (model: {}): {}", 
                    toolName, providerModel, e.getMessage());
        }
    }
    
    @Override
    public boolean isCacheAvailable() {
        return cacheService != null;
    }
    
    public ToolDescriptionCacheService getCacheService() {
        return cacheService;
    }
}