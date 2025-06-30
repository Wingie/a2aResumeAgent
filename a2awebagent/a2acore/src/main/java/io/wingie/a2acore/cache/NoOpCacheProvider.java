package io.wingie.a2acore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * No-operation cache provider that disables caching.
 * 
 * Used as the default when no specific cache provider is configured.
 * This ensures a2acore works without any external dependencies.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(ToolCacheProvider.class)
public class NoOpCacheProvider implements ToolCacheProvider {
    
    private static final Logger log = LoggerFactory.getLogger(NoOpCacheProvider.class);
    
    private volatile boolean warningLogged = false;
    
    @Override
    public Optional<String> getCachedDescription(String toolName, String providerModel) {
        logCacheDisabledWarning();
        return Optional.empty();
    }
    
    @Override
    public void cacheDescription(String toolName, String providerModel, String description, long generationTimeMs) {
        logCacheDisabledWarning();
        // No-op - caching disabled
    }
    
    @Override
    public void updateUsageStats(String toolName, String providerModel) {
        // No-op - no stats to update
    }
    
    @Override
    public boolean isEnabled() {
        return false;
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return new CacheStatistics(0, 0, 0);
    }
    
    /**
     * Logs a warning once that caching is disabled.
     */
    private void logCacheDisabledWarning() {
        if (!warningLogged) {
            log.info("Tool description caching is disabled (using NoOpCacheProvider). " +
                    "Consider configuring PostgreSQL cache provider for better performance.");
            warningLogged = true;
        }
    }
}