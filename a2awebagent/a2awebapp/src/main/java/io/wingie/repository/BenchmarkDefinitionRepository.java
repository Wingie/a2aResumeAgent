package io.wingie.repository;

import io.wingie.entity.BenchmarkDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BenchmarkDefinition entities with specialized queries for
 * benchmark management and analytics.
 */
@Repository
public interface BenchmarkDefinitionRepository extends JpaRepository<BenchmarkDefinition, String> {
    
    // =====================================
    // Basic Queries
    // =====================================
    
    /**
     * Find benchmark by name
     */
    Optional<BenchmarkDefinition> findByBenchmarkName(String benchmarkName);
    
    /**
     * Find all active benchmarks
     */
    List<BenchmarkDefinition> findByIsActiveTrue();
    
    /**
     * Find all inactive benchmarks
     */
    List<BenchmarkDefinition> findByIsActiveFalse();
    
    /**
     * Find benchmarks by category
     */
    List<BenchmarkDefinition> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * Find benchmarks by difficulty rating
     */
    List<BenchmarkDefinition> findByDifficultyRatingAndIsActiveTrue(Integer difficultyRating);
    
    /**
     * Find benchmarks by author
     */
    List<BenchmarkDefinition> findByAuthorAndIsActiveTrue(String author);
    
    // =====================================
    // Advanced Search Queries
    // =====================================
    
    /**
     * Search benchmarks by name pattern (case-insensitive)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "LOWER(bd.benchmarkName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND bd.isActive = true ORDER BY bd.benchmarkName")
    List<BenchmarkDefinition> searchByNameContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Search benchmarks by description pattern (case-insensitive)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "LOWER(bd.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND bd.isActive = true ORDER BY bd.benchmarkName")
    List<BenchmarkDefinition> searchByDescriptionContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find benchmarks with specific tags
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "LOWER(bd.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "AND bd.isActive = true ORDER BY bd.benchmarkName")
    List<BenchmarkDefinition> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * Find benchmarks with task count in range
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "bd.totalTasks BETWEEN :minTasks AND :maxTasks " +
           "AND bd.isActive = true ORDER BY bd.totalTasks")
    List<BenchmarkDefinition> findByTaskCountRange(@Param("minTasks") Integer minTasks, 
                                                   @Param("maxTasks") Integer maxTasks);
    
    /**
     * Find benchmarks with expected duration in range (seconds)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "bd.expectedDurationSeconds BETWEEN :minDuration AND :maxDuration " +
           "AND bd.isActive = true ORDER BY bd.expectedDurationSeconds")
    List<BenchmarkDefinition> findByDurationRange(@Param("minDuration") Integer minDuration, 
                                                  @Param("maxDuration") Integer maxDuration);
    
    // =====================================
    // Analytics and Statistics Queries
    // =====================================
    
    /**
     * Get benchmark statistics by category
     */
    @Query("SELECT bd.category, COUNT(bd), AVG(bd.totalTasks), AVG(bd.difficultyRating) " +
           "FROM BenchmarkDefinition bd WHERE bd.isActive = true " +
           "GROUP BY bd.category ORDER BY bd.category")
    List<Object[]> getBenchmarkStatsByCategory();
    
    /**
     * Get difficulty distribution
     */
    @Query("SELECT bd.difficultyRating, COUNT(bd) " +
           "FROM BenchmarkDefinition bd WHERE bd.isActive = true " +
           "GROUP BY bd.difficultyRating ORDER BY bd.difficultyRating")
    List<Object[]> getDifficultyDistribution();
    
    /**
     * Get task count statistics
     */
    @Query("SELECT MIN(bd.totalTasks), MAX(bd.totalTasks), AVG(bd.totalTasks), COUNT(bd) " +
           "FROM BenchmarkDefinition bd WHERE bd.isActive = true")
    Object[] getTaskCountStatistics();
    
    /**
     * Get most popular benchmarks (by task count)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE bd.isActive = true " +
           "ORDER BY bd.totalTasks DESC")
    List<BenchmarkDefinition> findMostPopularBenchmarks();
    
    /**
     * Get recently created benchmarks
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE bd.isActive = true " +
           "ORDER BY bd.createdAt DESC")
    List<BenchmarkDefinition> findRecentlyCreated();
    
    /**
     * Get recently updated benchmarks
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE bd.isActive = true " +
           "ORDER BY bd.updatedAt DESC")
    List<BenchmarkDefinition> findRecentlyUpdated();
    
    // =====================================
    // Execution Readiness Queries
    // =====================================
    
    /**
     * Find benchmarks ready for execution
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "bd.isActive = true AND bd.totalTasks > 0 " +
           "ORDER BY bd.benchmarkName")
    List<BenchmarkDefinition> findReadyForExecution();
    
    /**
     * Find benchmarks needing attention (no tasks)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "bd.isActive = true AND (bd.totalTasks IS NULL OR bd.totalTasks = 0) " +
           "ORDER BY bd.createdAt DESC")
    List<BenchmarkDefinition> findNeedingAttention();
    
    /**
     * Find benchmarks by complexity (task count and difficulty)
     */
    @Query("SELECT bd FROM BenchmarkDefinition bd WHERE " +
           "bd.isActive = true AND " +
           "bd.totalTasks >= :minTasks AND " +
           "bd.difficultyRating >= :minDifficulty " +
           "ORDER BY bd.difficultyRating DESC, bd.totalTasks DESC")
    List<BenchmarkDefinition> findByComplexity(@Param("minTasks") Integer minTasks, 
                                               @Param("minDifficulty") Integer minDifficulty);
    
    // =====================================
    // Maintenance Queries
    // =====================================
    
    /**
     * Find benchmarks created after date
     */
    List<BenchmarkDefinition> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find benchmarks updated after date
     */
    List<BenchmarkDefinition> findByUpdatedAtAfter(LocalDateTime date);
    
    /**
     * Count active benchmarks
     */
    @Query("SELECT COUNT(bd) FROM BenchmarkDefinition bd WHERE bd.isActive = true")
    Long countActiveBenchmarks();
    
    /**
     * Count benchmarks by category
     */
    @Query("SELECT COUNT(bd) FROM BenchmarkDefinition bd WHERE " +
           "bd.category = :category AND bd.isActive = true")
    Long countByCategory(@Param("category") String category);
    
    /**
     * Check if benchmark name exists
     */
    boolean existsByBenchmarkName(String benchmarkName);
    
    /**
     * Check if benchmark name exists for different ID (for updates)
     */
    @Query("SELECT COUNT(bd) > 0 FROM BenchmarkDefinition bd WHERE " +
           "bd.benchmarkName = :benchmarkName AND bd.benchmarkId != :excludeId")
    boolean existsByBenchmarkNameAndIdNot(@Param("benchmarkName") String benchmarkName, 
                                          @Param("excludeId") String excludeId);
    
    // =====================================
    // Bulk Operations
    // =====================================
    
    /**
     * Bulk activate benchmarks by category
     */
    @Query("UPDATE BenchmarkDefinition bd SET bd.isActive = true, bd.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bd.category = :category")
    int activateByCategory(@Param("category") String category);
    
    /**
     * Bulk deactivate benchmarks by category
     */
    @Query("UPDATE BenchmarkDefinition bd SET bd.isActive = false, bd.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bd.category = :category")
    int deactivateByCategory(@Param("category") String category);
    
    /**
     * Update task count for specific benchmark
     */
    @Query("UPDATE BenchmarkDefinition bd SET bd.totalTasks = :taskCount, bd.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE bd.benchmarkId = :benchmarkId")
    int updateTaskCount(@Param("benchmarkId") String benchmarkId, @Param("taskCount") Integer taskCount);
}