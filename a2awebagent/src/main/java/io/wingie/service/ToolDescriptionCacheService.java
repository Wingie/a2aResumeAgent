package io.wingie.service;

import io.wingie.entity.ToolDescription;
import io.wingie.repository.ToolDescriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage PostgreSQL caching of AI-generated tool descriptions
 * Reduces startup time by avoiding repeated AI calls for the same provider/tool combinations
 */
@Service
@Slf4j
public class ToolDescriptionCacheService {

    @Autowired
    private ToolDescriptionRepository repository;

    @Value("${task.processor.provider:openrouter}")
    private String taskProcessorProvider;

    @Value("${task.processor.modelName:deepseek/deepseek-r1:free}")
    private String taskProcessorModelName;

    /**
     * Get cached tool description or return null if not found
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public Optional<ToolDescription> getCachedDescription(String providerModel, String toolName) {
        log.debug("🔍 Checking cache for provider: {}, tool: {}", providerModel, toolName);
        
        Optional<ToolDescription> cached = repository.findByProviderModelAndToolName(providerModel, toolName);
        
        if (cached.isPresent()) {
            log.info("✅ Cache HIT: {} - {} (used {} times)", providerModel, toolName, cached.get().getUsageCount());
            // Update usage statistics
            updateUsageStats(cached.get());
            return cached;
        } else {
            log.info("❌ Cache MISS: {} - {}", providerModel, toolName);
            return Optional.empty();
        }
    }

    /**
     * Cache a newly generated tool description
     */
    @Transactional(transactionManager = "transactionManager")
    public ToolDescription cacheDescription(String providerModel, String toolName, 
                                          String description, String parametersInfo, 
                                          String toolProperties, Long generationTimeMs) {
        log.info("💾 Caching description: {} - {} (generated in {}ms)", 
            providerModel, toolName, generationTimeMs);

        ToolDescription toolDescription = new ToolDescription(
            providerModel, toolName, description, parametersInfo, 
            toolProperties, generationTimeMs
        );

        try {
            ToolDescription saved = repository.save(toolDescription);
            log.debug("✅ Saved to PostgreSQL with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ Failed to cache description for {} - {}: {}", 
                providerModel, toolName, e.getMessage());
            return toolDescription; // Return unsaved entity
        }
    }

    /**
     * Update usage statistics for a cached description
     */
    @Transactional(transactionManager = "transactionManager")
    public void updateUsageStats(ToolDescription description) {
        try {
            repository.incrementUsageCount(description.getId(), LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to update usage stats for {}: {}", description.getToolName(), e.getMessage());
        }
    }

    /**
     * Get current task processor configuration
     */
    public String getCurrentProviderModel() {
        return taskProcessorModelName;
    }

    /**
     * Check if description exists in cache
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public boolean isDescriptionCached(String providerModel, String toolName) {
        return repository.existsByProviderModelAndToolName(providerModel, toolName);
    }

    /**
     * Get all descriptions for current provider
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<ToolDescription> getCurrentProviderDescriptions() {
        return repository.findByProviderModel(getCurrentProviderModel());
    }

    /**
     * Get provider comparison statistics
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<Object[]> getProviderStatistics() {
        return repository.getProviderStatistics();
    }

    /**
     * Get cache effectiveness report
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public String getCacheReport() {
        long totalDescriptions = repository.countTotalDescriptions();
        List<Object[]> providerCounts = repository.countByProvider();
        List<Object[]> mostUsed = repository.getMostUsedTools();

        StringBuilder report = new StringBuilder();
        report.append("# Tool Description Cache Report\n\n");
        report.append("**Generated:** ").append(LocalDateTime.now()).append("\n");
        report.append("**Total Cached Descriptions:** ").append(totalDescriptions).append("\n\n");

        report.append("## Descriptions by Provider\n");
        for (Object[] row : providerCounts) {
            String provider = (String) row[0];
            Long count = (Long) row[1];
            report.append("- **").append(provider).append(":** ").append(count).append(" descriptions\n");
        }

        report.append("\n## Most Used Tools\n");
        int limit = Math.min(mostUsed.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = mostUsed.get(i);
            String toolName = (String) row[0];
            String provider = (String) row[1];
            Long usageCount = (Long) row[2];
            report.append("- **").append(toolName).append("** (").append(provider)
                  .append("): ").append(usageCount).append(" uses\n");
        }

        return report.toString();
    }

    /**
     * Get cached response by cache key (for CachingAIProcessor)
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public String getCachedResponse(String cacheKey) {
        log.debug("Checking cache for key: {}", cacheKey);
        
        Optional<ToolDescription> cached = repository.findByCacheKey(cacheKey);
        
        if (cached.isPresent()) {
            log.info("Cache HIT for key: {} (used {} times)", cacheKey, cached.get().getUsageCount());
            // Update usage statistics
            updateUsageStats(cached.get());
            return cached.get().getDescription();
        } else {
            log.info("Cache MISS for key: {}", cacheKey);
            return null;
        }
    }
    
    /**
     * Cache response by cache key (for CachingAIProcessor)
     */
    @Transactional(transactionManager = "transactionManager")
    public ToolDescription cacheResponse(String cacheKey, String response, long generationTimeMs) {
        log.info("Caching response for key: {} (generated in {}ms)", cacheKey, generationTimeMs);

        ToolDescription toolDescription = new ToolDescription(
            getCurrentProviderModel(), // providerModel
            "ai-generated", // toolName (generic for cache key based)
            response, // description
            "", // parametersInfo
            cacheKey, // use toolProperties field to store cache key
            generationTimeMs
        );

        try {
            ToolDescription saved = repository.save(toolDescription);
            log.debug("Saved to PostgreSQL with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to cache response for key {}: {}", cacheKey, e.getMessage());
            return toolDescription; // Return unsaved entity
        }
    }
    
    /**
     * Clear cache by model name (for cache management)
     */
    @Transactional(transactionManager = "transactionManager")
    public void clearCacheByModel(String modelName) {
        try {
            repository.deleteByProviderModel(modelName);
            log.info("Cleared cache for model: {}", modelName);
        } catch (Exception e) {
            log.error("Failed to clear cache for model {}: {}", modelName, e.getMessage());
        }
    }
    
    /**
     * Clear cache by tool name pattern (for cache management)
     */
    @Transactional(transactionManager = "transactionManager")
    public void clearCacheByToolPattern(String toolNamePattern) {
        try {
            repository.deleteByToolNameContaining(toolNamePattern);
            log.info("Cleared cache for tool pattern: {}", toolNamePattern);
        } catch (Exception e) {
            log.error("Failed to clear cache for pattern {}: {}", toolNamePattern, e.getMessage());
        }
    }
    
    /**
     * Clean up old descriptions (optional maintenance)
     */
    @Transactional(transactionManager = "transactionManager")
    public void cleanupOldDescriptions(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        try {
            repository.deleteByCreatedAtBefore(cutoff);
            log.info("Cleaned up tool descriptions older than {} days", daysOld);
        } catch (Exception e) {
            log.error("Failed to cleanup old descriptions: {}", e.getMessage());
        }
    }
}