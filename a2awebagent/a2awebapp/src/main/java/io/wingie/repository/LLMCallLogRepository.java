package io.wingie.repository;

import io.wingie.entity.LLMCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing LLM call logs in PostgreSQL
 * 
 * Provides comprehensive querying capabilities for:
 * - Cost analysis and budget monitoring
 * - Performance metrics and optimization
 * - Cache effectiveness analysis
 * - Agent workflow tracking
 * - LLM provider comparison
 */
@Repository
public interface LLMCallLogRepository extends JpaRepository<LLMCallLog, String> {

    // ==================== CACHE INTEGRATION ====================
    
    /**
     * Find all LLM calls for a specific cache key
     */
    List<LLMCallLog> findByCacheKey(String cacheKey);
    
    /**
     * Find the most recent LLM call for a cache key
     */
    @Query("SELECT l FROM LLMCallLog l WHERE l.cacheKey = :cacheKey ORDER BY l.createdAt DESC")
    Optional<LLMCallLog> findMostRecentByCacheKey(@Param("cacheKey") String cacheKey);
    
    /**
     * Count cache hits vs misses for a specific tool
     */
    @Query("SELECT l.cacheHit, COUNT(l) FROM LLMCallLog l WHERE l.toolName = :toolName GROUP BY l.cacheHit")
    List<Object[]> getCacheHitRatioByTool(@Param("toolName") String toolName);
    
    /**
     * Count cache hits vs misses for a specific provider/model
     */
    @Query("SELECT l.cacheHit, COUNT(l) FROM LLMCallLog l WHERE l.provider = :provider AND l.modelName = :modelName GROUP BY l.cacheHit")
    List<Object[]> getCacheHitRatioByProviderModel(@Param("provider") String provider, @Param("modelName") String modelName);

    // ==================== COST ANALYSIS ====================
    
    /**
     * Calculate total costs for a specific provider in a date range
     */
    @Query("SELECT SUM(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.provider = :provider AND l.createdAt BETWEEN :startDate AND :endDate AND l.cacheHit = false")
    BigDecimal getTotalCostByProvider(@Param("provider") String provider, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate total costs for a specific tool in a date range
     */
    @Query("SELECT SUM(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.toolName = :toolName AND l.createdAt BETWEEN :startDate AND :endDate AND l.cacheHit = false")
    BigDecimal getTotalCostByTool(@Param("toolName") String toolName, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get cost breakdown by provider and model
     */
    @Query("SELECT l.provider, l.modelName, SUM(l.estimatedCostUsd), COUNT(l) FROM LLMCallLog l WHERE l.cacheHit = false AND l.createdAt BETWEEN :startDate AND :endDate GROUP BY l.provider, l.modelName ORDER BY SUM(l.estimatedCostUsd) DESC")
    List<Object[]> getCostBreakdownByProviderModel(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get daily cost trends
     */
    @Query("SELECT DATE(l.createdAt), SUM(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.cacheHit = false AND l.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(l.createdAt) ORDER BY DATE(l.createdAt)")
    List<Object[]> getDailyCostTrends(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ==================== PERFORMANCE ANALYSIS ====================
    
    /**
     * Get average response times by provider and model
     */
    @Query("SELECT l.provider, l.modelName, AVG(l.responseTimeMs), COUNT(l) FROM LLMCallLog l WHERE l.cacheHit = false AND l.responseTimeMs IS NOT NULL GROUP BY l.provider, l.modelName ORDER BY AVG(l.responseTimeMs)")
    List<Object[]> getAverageResponseTimesByProviderModel();
    
    /**
     * Get token usage statistics by provider and model
     */
    @Query("SELECT l.provider, l.modelName, AVG(l.inputTokens), AVG(l.outputTokens), SUM(l.inputTokens + l.outputTokens) FROM LLMCallLog l WHERE l.cacheHit = false AND l.inputTokens IS NOT NULL GROUP BY l.provider, l.modelName")
    List<Object[]> getTokenUsageStatistics();
    
    /**
     * Find slowest LLM calls for performance optimization
     */
    @Query("SELECT l FROM LLMCallLog l WHERE l.cacheHit = false AND l.responseTimeMs IS NOT NULL ORDER BY l.responseTimeMs DESC")
    List<LLMCallLog> findSlowestCalls(@Param("limit") int limit);

    // ==================== ERROR ANALYSIS ====================
    
    /**
     * Get error rates by provider and model
     */
    @Query("SELECT l.provider, l.modelName, l.errorCode, COUNT(l) FROM LLMCallLog l WHERE l.errorCode IS NOT NULL GROUP BY l.provider, l.modelName, l.errorCode ORDER BY COUNT(l) DESC")
    List<Object[]> getErrorRatesByProviderModel();
    
    /**
     * Find recent failed calls for debugging
     */
    @Query("SELECT l FROM LLMCallLog l WHERE l.errorCode IS NOT NULL ORDER BY l.createdAt DESC")
    List<LLMCallLog> findRecentFailedCalls(@Param("limit") int limit);
    
    /**
     * Get retry pattern analysis
     */
    @Query("SELECT l.retryAttempt, COUNT(l) FROM LLMCallLog l GROUP BY l.retryAttempt ORDER BY l.retryAttempt")
    List<Object[]> getRetryPatternAnalysis();

    // ==================== AGENT WORKFLOW TRACKING ====================
    
    /**
     * Find all LLM calls for a specific task execution (agentic workflow)
     */
    List<LLMCallLog> findByTaskExecutionIdOrderByCreatedAt(String taskExecutionId);
    
    /**
     * Find all LLM calls for a specific session
     */
    List<LLMCallLog> findBySessionIdOrderByCreatedAt(String sessionId);
    
    /**
     * Get tool usage patterns in agent workflows
     */
    @Query("SELECT l.toolName, COUNT(l), AVG(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.taskExecutionId IS NOT NULL GROUP BY l.toolName ORDER BY COUNT(l) DESC")
    List<Object[]> getToolUsagePatternsInAgentWorkflows();

    // ==================== TIME-BASED QUERIES ====================
    
    /**
     * Find LLM calls in the last N hours
     */
    @Query("SELECT l FROM LLMCallLog l WHERE l.createdAt >= :sinceTime ORDER BY l.createdAt DESC")
    List<LLMCallLog> findRecentCalls(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * Count total LLM calls today
     */
    @Query("SELECT COUNT(l) FROM LLMCallLog l WHERE CAST(l.createdAt AS date) = CURRENT_DATE")
    long countCallsToday();
    
    /**
     * Count cache hits today
     */
    @Query("SELECT COUNT(l) FROM LLMCallLog l WHERE CAST(l.createdAt AS date) = CURRENT_DATE AND l.cacheHit = true")
    long countCacheHitsToday();
    
    /**
     * Get today's total cost
     */
    @Query("SELECT COALESCE(SUM(l.estimatedCostUsd), 0) FROM LLMCallLog l WHERE CAST(l.createdAt AS date) = CURRENT_DATE AND l.cacheHit = false")
    BigDecimal getTotalCostToday();

    // ==================== OPTIMIZATION QUERIES ====================
    
    /**
     * Find tools with low cache hit rates that could benefit from cache warming
     * TEMPORARILY DISABLED due to HQL validation issues
     */
    // @Query("SELECT l.toolName, " +
    //        "SUM(CASE WHEN l.cacheHit = true THEN 1 ELSE 0 END) as hits, " +
    //        "SUM(CASE WHEN l.cacheHit = false THEN 1 ELSE 0 END) as misses, " +
    //        "(SUM(CASE WHEN l.cacheHit = true THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as hitRate " +
    //        "FROM LLMCallLog l " +
    //        "GROUP BY l.toolName " +
    //        "HAVING COUNT(l) > :minimumCalls AND hitRate < :maxHitRate " +
    //        "ORDER BY hitRate ASC")
    default List<Object[]> findToolsWithLowCacheHitRates(long minimumCalls, double maxHitRate) {
        return java.util.Collections.emptyList();
    }
    
    /**
     * Get most expensive tools (good candidates for caching optimization)
     */
    @Query("SELECT l.toolName, SUM(l.estimatedCostUsd), COUNT(l), AVG(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.cacheHit = false GROUP BY l.toolName ORDER BY SUM(l.estimatedCostUsd) DESC")
    List<Object[]> getMostExpensiveTools();

    // ==================== USER AND SESSION ANALYTICS ====================
    
    /**
     * Get usage statistics by user
     */
    @Query("SELECT l.userId, COUNT(l), SUM(l.estimatedCostUsd) FROM LLMCallLog l WHERE l.userId IS NOT NULL GROUP BY l.userId ORDER BY COUNT(l) DESC")
    List<Object[]> getUserUsageStatistics();
    
    /**
     * Find active sessions in the last N hours
     */
    @Query("SELECT DISTINCT l.sessionId FROM LLMCallLog l WHERE l.sessionId IS NOT NULL AND l.createdAt >= :sinceTime")
    List<String> findActiveSessionsSince(@Param("sinceTime") LocalDateTime sinceTime);

    // ==================== CLEANUP AND MAINTENANCE ====================
    
    /**
     * Delete old LLM call logs for cleanup
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * Count total records for maintenance monitoring
     */
    @Query("SELECT COUNT(l) FROM LLMCallLog l")
    long countTotalRecords();
}