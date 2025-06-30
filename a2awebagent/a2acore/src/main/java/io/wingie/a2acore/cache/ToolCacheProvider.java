package io.wingie.a2acore.cache;

import io.wingie.a2acore.domain.Tool;

import java.util.Optional;

/**
 * Interface for tool description caching providers.
 * 
 * Allows integration with existing PostgreSQL caching system in a2awebagent
 * while keeping a2acore independent of specific cache implementations.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public interface ToolCacheProvider {
    
    /**
     * Retrieves cached enhanced description for a tool.
     * 
     * @param toolName The name of the tool
     * @param providerModel The AI provider/model combination (e.g., "gemini-2.0-flash")
     * @return Enhanced description if found in cache
     */
    Optional<String> getCachedDescription(String toolName, String providerModel);
    
    /**
     * Caches an enhanced tool description.
     * 
     * @param toolName The name of the tool
     * @param providerModel The AI provider/model combination
     * @param description The enhanced description to cache
     * @param generationTimeMs Time taken to generate the description
     */
    void cacheDescription(String toolName, String providerModel, String description, long generationTimeMs);
    
    /**
     * Updates usage statistics for a cached tool.
     * 
     * @param toolName The name of the tool
     * @param providerModel The AI provider/model combination
     */
    void updateUsageStats(String toolName, String providerModel);
    
    /**
     * Caches the complete tool definition including schemas and annotations.
     * 
     * @param tool The complete tool definition
     * @param providerModel The AI provider/model combination
     */
    default void cacheToolDefinition(Tool tool, String providerModel) {
        // Default implementation just caches the description
        cacheDescription(tool.getName(), providerModel, tool.getDescription(), 0);
    }
    
    /**
     * Retrieves a cached complete tool definition.
     * 
     * @param toolName The name of the tool
     * @param providerModel The AI provider/model combination
     * @return Complete tool definition if found in cache
     */
    default Optional<Tool> getCachedTool(String toolName, String providerModel) {
        // Default implementation returns empty - providers can override
        return Optional.empty();
    }
    
    /**
     * Checks if caching is enabled for this provider.
     * 
     * @return true if caching is enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Gets cache statistics for monitoring.
     * 
     * @return Cache statistics
     */
    default CacheStatistics getStatistics() {
        return new CacheStatistics();
    }
    
    /**
     * Clears cached data for a specific provider/model.
     * 
     * @param providerModel The AI provider/model combination to clear
     */
    default void clearCache(String providerModel) {
        // Default implementation does nothing
    }
    
    /**
     * Statistics about cache performance.
     */
    class CacheStatistics {
        private long hitCount = 0;
        private long missCount = 0;
        private long totalSize = 0;
        
        public CacheStatistics() {}
        
        public CacheStatistics(long hitCount, long missCount, long totalSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.totalSize = totalSize;
        }
        
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getTotalSize() { return totalSize; }
        
        public double getHitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{hits=%d, misses=%d, size=%d, hitRate=%.2f%%}", 
                hitCount, missCount, totalSize, getHitRate() * 100);
        }
    }
}