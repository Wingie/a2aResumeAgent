package io.wingie.repository;

import io.wingie.entity.BenchmarkTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BenchmarkTask entities with specialized queries for
 * task management and template operations.
 */
@Repository
public interface BenchmarkTaskRepository extends JpaRepository<BenchmarkTask, String> {
    
    // =====================================
    // Basic Queries
    // =====================================
    
    /**
     * Find all tasks for a specific benchmark
     */
    List<BenchmarkTask> findByBenchmarkIdOrderByExecutionOrder(String benchmarkId);
    
    /**
     * Find task by name within a benchmark
     */
    Optional<BenchmarkTask> findByBenchmarkIdAndTaskName(String benchmarkId, String taskName);
    
    /**
     * Find tasks by category
     */
    List<BenchmarkTask> findByTaskCategoryOrderByExecutionOrder(String taskCategory);
    
    /**
     * Find tasks by difficulty level
     */
    List<BenchmarkTask> findByDifficultyLevelOrderByExecutionOrder(Integer difficultyLevel);
    
    /**
     * Find tasks by difficulty range
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.difficultyLevel BETWEEN :minDifficulty AND :maxDifficulty " +
           "ORDER BY bt.difficultyLevel, bt.executionOrder")
    List<BenchmarkTask> findByDifficultyRange(@Param("minDifficulty") Integer minDifficulty, 
                                              @Param("maxDifficulty") Integer maxDifficulty);
    
    /**
     * Find tasks by timeout range
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.timeoutSeconds BETWEEN :minTimeout AND :maxTimeout " +
           "ORDER BY bt.timeoutSeconds, bt.executionOrder")
    List<BenchmarkTask> findByTimeoutRange(@Param("minTimeout") Integer minTimeout, 
                                           @Param("maxTimeout") Integer maxTimeout);
    
    // =====================================
    // Execution Order Queries
    // =====================================
    
    /**
     * Find next available execution order for a benchmark
     */
    @Query("SELECT COALESCE(MAX(bt.executionOrder), 0) + 1 " +
           "FROM BenchmarkTask bt WHERE bt.benchmarkId = :benchmarkId")
    Integer findNextExecutionOrder(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Find task by execution order within benchmark
     */
    Optional<BenchmarkTask> findByBenchmarkIdAndExecutionOrder(String benchmarkId, Integer executionOrder);
    
    /**
     * Find tasks after specific execution order
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.benchmarkId = :benchmarkId AND bt.executionOrder > :afterOrder " +
           "ORDER BY bt.executionOrder")
    List<BenchmarkTask> findTasksAfterOrder(@Param("benchmarkId") String benchmarkId, 
                                            @Param("afterOrder") Integer afterOrder);
    
    /**
     * Find tasks before specific execution order
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.benchmarkId = :benchmarkId AND bt.executionOrder < :beforeOrder " +
           "ORDER BY bt.executionOrder")
    List<BenchmarkTask> findTasksBeforeOrder(@Param("benchmarkId") String benchmarkId, 
                                             @Param("beforeOrder") Integer beforeOrder);
    
    // =====================================
    // Search and Filter Queries
    // =====================================
    
    /**
     * Search tasks by name pattern (case-insensitive)
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "LOWER(bt.taskName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> searchByNameContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Search tasks by description pattern (case-insensitive)
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "LOWER(bt.taskDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> searchByDescriptionContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Search tasks by prompt template pattern (case-insensitive)
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "LOWER(bt.promptTemplate) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> searchByPromptContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find tasks with specific tags
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "LOWER(bt.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * Find tasks with score in range
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.maxScore BETWEEN :minScore AND :maxScore " +
           "ORDER BY bt.maxScore DESC, bt.executionOrder")
    List<BenchmarkTask> findByScoreRange(@Param("minScore") Double minScore, 
                                         @Param("maxScore") Double maxScore);
    
    // =====================================
    // Analytics and Statistics Queries
    // =====================================
    
    /**
     * Get task statistics by benchmark
     */
    @Query("SELECT bt.benchmarkId, COUNT(bt), AVG(bt.maxScore), AVG(bt.difficultyLevel), AVG(bt.timeoutSeconds) " +
           "FROM BenchmarkTask bt GROUP BY bt.benchmarkId ORDER BY bt.benchmarkId")
    List<Object[]> getTaskStatsByBenchmark();
    
    /**
     * Get task statistics by category
     */
    @Query("SELECT bt.taskCategory, COUNT(bt), AVG(bt.maxScore), AVG(bt.difficultyLevel) " +
           "FROM BenchmarkTask bt WHERE bt.taskCategory IS NOT NULL " +
           "GROUP BY bt.taskCategory ORDER BY bt.taskCategory")
    List<Object[]> getTaskStatsByCategory();
    
    /**
     * Get difficulty distribution
     */
    @Query("SELECT bt.difficultyLevel, COUNT(bt) " +
           "FROM BenchmarkTask bt WHERE bt.difficultyLevel IS NOT NULL " +
           "GROUP BY bt.difficultyLevel ORDER BY bt.difficultyLevel")
    List<Object[]> getDifficultyDistribution();
    
    /**
     * Get score distribution
     */
    @Query("SELECT bt.maxScore, COUNT(bt) " +
           "FROM BenchmarkTask bt " +
           "GROUP BY bt.maxScore ORDER BY bt.maxScore")
    List<Object[]> getScoreDistribution();
    
    /**
     * Get timeout distribution
     */
    @Query("SELECT bt.timeoutSeconds, COUNT(bt) " +
           "FROM BenchmarkTask bt WHERE bt.timeoutSeconds IS NOT NULL " +
           "GROUP BY bt.timeoutSeconds ORDER BY bt.timeoutSeconds")
    List<Object[]> getTimeoutDistribution();
    
    /**
     * Get total score for benchmark
     */
    @Query("SELECT SUM(bt.maxScore) FROM BenchmarkTask bt WHERE bt.benchmarkId = :benchmarkId")
    Double getTotalScoreForBenchmark(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Get average difficulty for benchmark
     */
    @Query("SELECT AVG(bt.difficultyLevel) FROM BenchmarkTask bt WHERE " +
           "bt.benchmarkId = :benchmarkId AND bt.difficultyLevel IS NOT NULL")
    Double getAverageDifficultyForBenchmark(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Get estimated duration for benchmark (sum of timeouts)
     */
    @Query("SELECT SUM(bt.timeoutSeconds) FROM BenchmarkTask bt WHERE " +
           "bt.benchmarkId = :benchmarkId AND bt.timeoutSeconds IS NOT NULL")
    Long getEstimatedDurationForBenchmark(@Param("benchmarkId") String benchmarkId);
    
    // =====================================
    // Template and Reuse Queries
    // =====================================
    
    /**
     * Find similar tasks by category and difficulty
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.taskCategory = :category AND bt.difficultyLevel = :difficulty " +
           "AND bt.benchmarkTaskId != :excludeId " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> findSimilarTasks(@Param("category") String category, 
                                         @Param("difficulty") Integer difficulty, 
                                         @Param("excludeId") String excludeId);
    
    /**
     * Find tasks suitable for reuse (by category)
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.taskCategory = :category " +
           "ORDER BY bt.taskName")
    List<BenchmarkTask> findReusableTasksByCategory(@Param("category") String category);
    
    /**
     * Find most commonly used task patterns
     */
    @Query("SELECT bt.taskCategory, bt.difficultyLevel, COUNT(bt) as usage_count " +
           "FROM BenchmarkTask bt WHERE bt.taskCategory IS NOT NULL " +
           "GROUP BY bt.taskCategory, bt.difficultyLevel " +
           "ORDER BY usage_count DESC")
    List<Object[]> findMostCommonPatterns();
    
    // =====================================
    // Validation and Integrity Queries
    // =====================================
    
    /**
     * Check for duplicate execution orders within benchmark
     */
    @Query("SELECT bt.executionOrder, COUNT(bt) " +
           "FROM BenchmarkTask bt WHERE bt.benchmarkId = :benchmarkId " +
           "GROUP BY bt.executionOrder HAVING COUNT(bt) > 1")
    List<Object[]> findDuplicateExecutionOrders(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Check for gaps in execution order sequence
     */
    @Query("SELECT bt.executionOrder FROM BenchmarkTask bt WHERE bt.benchmarkId = :benchmarkId " +
           "ORDER BY bt.executionOrder")
    List<Integer> getExecutionOrderSequence(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Find tasks without evaluation criteria
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.evaluationCriteria IS NULL OR TRIM(bt.evaluationCriteria) = '' " +
           "ORDER BY bt.benchmarkId, bt.executionOrder")
    List<BenchmarkTask> findTasksWithoutCriteria();
    
    /**
     * Find tasks with unusually high scores
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.maxScore > :threshold ORDER BY bt.maxScore DESC")
    List<BenchmarkTask> findHighScoreTasks(@Param("threshold") Double threshold);
    
    /**
     * Find tasks with unusually long timeouts
     */
    @Query("SELECT bt FROM BenchmarkTask bt WHERE " +
           "bt.timeoutSeconds > :threshold ORDER BY bt.timeoutSeconds DESC")
    List<BenchmarkTask> findLongTimeoutTasks(@Param("threshold") Integer threshold);
    
    // =====================================
    // Maintenance Queries
    // =====================================
    
    /**
     * Count tasks by benchmark
     */
    @Query("SELECT COUNT(bt) FROM BenchmarkTask bt WHERE bt.benchmarkId = :benchmarkId")
    Long countByBenchmarkId(@Param("benchmarkId") String benchmarkId);
    
    /**
     * Count tasks by category
     */
    @Query("SELECT COUNT(bt) FROM BenchmarkTask bt WHERE bt.taskCategory = :category")
    Long countByCategory(@Param("category") String category);
    
    /**
     * Find tasks created after date
     */
    List<BenchmarkTask> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find tasks updated after date
     */
    List<BenchmarkTask> findByUpdatedAtAfter(LocalDateTime date);
    
    /**
     * Check if task name exists within benchmark
     */
    boolean existsByBenchmarkIdAndTaskName(String benchmarkId, String taskName);
    
    /**
     * Check if execution order exists within benchmark
     */
    boolean existsByBenchmarkIdAndExecutionOrder(String benchmarkId, Integer executionOrder);
    
    // =====================================
    // Bulk Operations
    // =====================================
    
    /**
     * Update execution orders for tasks after specific order (for reordering)
     */
    @Query("UPDATE BenchmarkTask bt SET bt.executionOrder = bt.executionOrder + :increment, " +
           "bt.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bt.benchmarkId = :benchmarkId AND bt.executionOrder >= :afterOrder")
    int incrementExecutionOrdersAfter(@Param("benchmarkId") String benchmarkId, 
                                      @Param("afterOrder") Integer afterOrder, 
                                      @Param("increment") Integer increment);
    
    /**
     * Update difficulty level for all tasks in category
     */
    @Query("UPDATE BenchmarkTask bt SET bt.difficultyLevel = :newDifficulty, " +
           "bt.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bt.taskCategory = :category")
    int updateDifficultyByCategory(@Param("category") String category, 
                                   @Param("newDifficulty") Integer newDifficulty);
    
    /**
     * Bulk update timeout for benchmark
     */
    @Query("UPDATE BenchmarkTask bt SET bt.timeoutSeconds = :newTimeout, " +
           "bt.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bt.benchmarkId = :benchmarkId")
    int updateTimeoutForBenchmark(@Param("benchmarkId") String benchmarkId, 
                                  @Param("newTimeout") Integer newTimeout);
}