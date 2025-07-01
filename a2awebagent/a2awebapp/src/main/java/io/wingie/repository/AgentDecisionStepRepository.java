package io.wingie.repository;

import io.wingie.entity.AgentDecisionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing agent decision steps in agentic workflows
 * 
 * Provides querying capabilities for:
 * - Agent workflow analysis and optimization
 * - Decision quality assessment
 * - Cost tracking for multi-step processes
 * - Performance monitoring and debugging
 */
@Repository
public interface AgentDecisionStepRepository extends JpaRepository<AgentDecisionStep, String> {

    // ==================== WORKFLOW ANALYSIS ====================
    
    /**
     * Find all decision steps for a specific task execution (complete agent workflow)
     */
    List<AgentDecisionStep> findByTaskExecutionIdOrderByStepNumber(String taskExecutionId);
    
    /**
     * Find the latest decision step for a task execution
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.taskExecutionId = :taskExecutionId ORDER BY ads.stepNumber DESC LIMIT 1")
    Optional<AgentDecisionStep> findLatestStepByTaskExecution(@Param("taskExecutionId") String taskExecutionId);
    
    /**
     * Find all active (pending or in-progress) decision steps
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.status IN ('PENDING', 'IN_PROGRESS') ORDER BY ads.createdAt")
    List<AgentDecisionStep> findActiveDecisionSteps();
    
    /**
     * Count total decision steps for a task execution
     */
    long countByTaskExecutionId(String taskExecutionId);
    
    /**
     * Find the maximum step number for a task execution
     */
    @Query("SELECT MAX(ads.stepNumber) FROM AgentDecisionStep ads WHERE ads.taskExecutionId = :taskExecutionId")
    Optional<Integer> findMaxStepNumberByTaskId(@Param("taskExecutionId") String taskExecutionId);

    // ==================== DECISION QUALITY ANALYSIS ====================
    
    /**
     * Find decision steps by tool and success rate
     */
    @Query("SELECT ads.toolSelected, COUNT(ads), " +
           "SUM(CASE WHEN ads.successful = true THEN 1 ELSE 0 END), " +
           "AVG(ads.confidenceScore) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "ORDER BY COUNT(ads) DESC")
    List<Object[]> getToolSuccessRateAnalysis();
    
    /**
     * Find decision steps with low confidence scores (potential quality issues)
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.confidenceScore < :threshold ORDER BY ads.confidenceScore ASC")
    List<AgentDecisionStep> findLowConfidenceDecisions(@Param("threshold") BigDecimal threshold);
    
    /**
     * Get average confidence score by tool
     */
    @Query("SELECT ads.toolSelected, AVG(ads.confidenceScore), COUNT(ads) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL AND ads.confidenceScore IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "ORDER BY AVG(ads.confidenceScore) DESC")
    List<Object[]> getAverageConfidenceByTool();
    
    /**
     * Find failed decision steps for debugging
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.status = 'FAILED' ORDER BY ads.createdAt DESC")
    List<AgentDecisionStep> findFailedDecisions();

    // ==================== COST ANALYSIS ====================
    
    /**
     * Calculate total cost for a specific task execution
     */
    @Query("SELECT SUM(ads.stepCostUsd) FROM AgentDecisionStep ads WHERE ads.taskExecutionId = :taskExecutionId")
    BigDecimal getTotalCostByTaskExecution(@Param("taskExecutionId") String taskExecutionId);
    
    /**
     * Get cost breakdown by tool
     */
    @Query("SELECT ads.toolSelected, SUM(ads.stepCostUsd), COUNT(ads), AVG(ads.stepCostUsd) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL AND ads.stepCostUsd IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "ORDER BY SUM(ads.stepCostUsd) DESC")
    List<Object[]> getCostBreakdownByTool();
    
    /**
     * Find most expensive decision steps
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.stepCostUsd IS NOT NULL ORDER BY ads.stepCostUsd DESC")
    List<AgentDecisionStep> findMostExpensiveDecisions(@Param("limit") int limit);
    
    /**
     * Get daily cost trends for agent workflows
     */
    @Query("SELECT DATE(ads.createdAt), SUM(ads.stepCostUsd), COUNT(ads) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.stepCostUsd IS NOT NULL AND ads.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(ads.createdAt) " +
           "ORDER BY DATE(ads.createdAt)")
    List<Object[]> getDailyCostTrends(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ==================== PERFORMANCE ANALYSIS ====================
    
    /**
     * Get average execution time by tool
     */
    @Query("SELECT ads.toolSelected, AVG(ads.executionTimeMs), COUNT(ads) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL AND ads.executionTimeMs IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "ORDER BY AVG(ads.executionTimeMs) DESC")
    List<Object[]> getAverageExecutionTimeByTool();
    
    /**
     * Find slowest decision steps
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.executionTimeMs IS NOT NULL ORDER BY ads.executionTimeMs DESC")
    List<AgentDecisionStep> findSlowestDecisions(@Param("limit") int limit);
    
    /**
     * Get token usage statistics by tool
     */
    @Query("SELECT ads.toolSelected, SUM(ads.tokensUsed), AVG(ads.tokensUsed), COUNT(ads) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL AND ads.tokensUsed IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "ORDER BY SUM(ads.tokensUsed) DESC")
    List<Object[]> getTokenUsageByTool();

    // ==================== WORKFLOW PATTERNS ====================
    
    /**
     * Find most common tool sequences in agent workflows
     */
    @Query("SELECT ads1.toolSelected, ads2.toolSelected, COUNT(*) " +
           "FROM AgentDecisionStep ads1 " +
           "JOIN AgentDecisionStep ads2 ON ads1.taskExecutionId = ads2.taskExecutionId " +
           "WHERE ads1.stepNumber = ads2.stepNumber - 1 " +
           "AND ads1.toolSelected IS NOT NULL AND ads2.toolSelected IS NOT NULL " +
           "GROUP BY ads1.toolSelected, ads2.toolSelected " +
           "ORDER BY COUNT(*) DESC")
    List<Object[]> getMostCommonToolSequences();
    
    /**
     * Find workflows by step count (simple vs complex workflows)
     */
    @Query("SELECT ads.taskExecutionId, COUNT(ads), SUM(ads.stepCostUsd), AVG(ads.confidenceScore) " +
           "FROM AgentDecisionStep ads " +
           "GROUP BY ads.taskExecutionId " +
           "ORDER BY COUNT(ads) DESC")
    List<Object[]> getWorkflowComplexityAnalysis();
    
    /**
     * Find decision steps that required retries or alternatives
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.alternativesConsidered IS NOT NULL AND ads.alternativesConsidered != '' ORDER BY ads.createdAt DESC")
    List<AgentDecisionStep> findDecisionsWithAlternatives();

    // ==================== TIME-BASED QUERIES ====================
    
    /**
     * Find recent decision steps
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.createdAt >= :sinceTime ORDER BY ads.createdAt DESC")
    List<AgentDecisionStep> findRecentDecisions(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * Count decision steps today
     */
    @Query("SELECT COUNT(ads) FROM AgentDecisionStep ads WHERE CAST(ads.createdAt AS date) = CURRENT_DATE")
    long countDecisionsToday();
    
    /**
     * Get today's total agent cost
     */
    @Query("SELECT COALESCE(SUM(ads.stepCostUsd), 0) FROM AgentDecisionStep ads WHERE CAST(ads.createdAt AS date) = CURRENT_DATE")
    BigDecimal getTotalAgentCostToday();

    // ==================== LLM INTEGRATION ====================
    
    /**
     * Find decision steps by LLM call ID
     */
    List<AgentDecisionStep> findByLlmCallId(String llmCallId);
    
    /**
     * Find decision steps without linked LLM calls (potential data consistency issues)
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.llmCallId IS NULL ORDER BY ads.createdAt DESC")
    List<AgentDecisionStep> findDecisionsWithoutLLMCalls();
    
    /**
     * Get LLM usage patterns in agent workflows
     */
    @Query("SELECT lcl.provider, lcl.modelName, COUNT(ads), AVG(ads.confidenceScore) " +
           "FROM AgentDecisionStep ads " +
           "JOIN LLMCallLog lcl ON ads.llmCallId = lcl.callId " +
           "GROUP BY lcl.provider, lcl.modelName " +
           "ORDER BY COUNT(ads) DESC")
    List<Object[]> getLLMUsagePatternsInAgentWorkflows();

    // ==================== OPTIMIZATION QUERIES ====================
    
    /**
     * Find tools with poor success rates (candidates for improvement)
     * TEMPORARILY DISABLED due to HQL validation issues
     */
    // @Query("SELECT ads.toolSelected, " +
    //        "COUNT(ads) as total, " +
    //        "SUM(CASE WHEN ads.successful = true THEN 1 ELSE 0 END) as successes, " +
    //        "(SUM(CASE WHEN ads.successful = true THEN 1 ELSE 0 END) * 100.0 / COUNT(ads)) as success_rate " +
    //        "FROM AgentDecisionStep ads " +
    //        "WHERE ads.toolSelected IS NOT NULL " +
    //        "GROUP BY ads.toolSelected " +
    //        "HAVING COUNT(ads) > :minimumUsage AND success_rate < :maxSuccessRate " +
    //        "ORDER BY success_rate ASC")
    default List<Object[]> findToolsWithPoorSuccessRates(long minimumUsage, double maxSuccessRate) {
        return java.util.Collections.emptyList();
    }
    
    /**
     * Find tools that consistently take too long (performance optimization candidates)
     */
    @Query("SELECT ads.toolSelected, AVG(ads.executionTimeMs), COUNT(ads) " +
           "FROM AgentDecisionStep ads " +
           "WHERE ads.toolSelected IS NOT NULL AND ads.executionTimeMs IS NOT NULL " +
           "GROUP BY ads.toolSelected " +
           "HAVING AVG(ads.executionTimeMs) > :thresholdMs AND COUNT(ads) > :minimumUsage " +
           "ORDER BY AVG(ads.executionTimeMs) DESC")
    List<Object[]> findSlowTools(@Param("thresholdMs") long thresholdMs, @Param("minimumUsage") long minimumUsage);

    // ==================== MAINTENANCE ====================
    
    /**
     * Delete old decision steps for cleanup
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * Count total records for maintenance monitoring
     */
    @Query("SELECT COUNT(ads) FROM AgentDecisionStep ads")
    long countTotalRecords();
    
    /**
     * Find orphaned decision steps (task execution no longer exists)
     */
    @Query("SELECT ads FROM AgentDecisionStep ads WHERE ads.taskExecutionId NOT IN (SELECT te.taskId FROM TaskExecution te)")
    List<AgentDecisionStep> findOrphanedDecisionSteps();
}