package io.wingie.repository;

import io.wingie.entity.ModelEvaluation;
import io.wingie.entity.EvaluationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelEvaluationRepository extends JpaRepository<ModelEvaluation, String> {
    
    // Find evaluations by status
    List<ModelEvaluation> findByStatus(EvaluationStatus status);
    
    Page<ModelEvaluation> findByStatus(EvaluationStatus status, Pageable pageable);
    
    // Find evaluations by model name
    List<ModelEvaluation> findByModelName(String modelName);
    
    Page<ModelEvaluation> findByModelName(String modelName, Pageable pageable);
    
    // Find evaluations by benchmark name
    List<ModelEvaluation> findByBenchmarkName(String benchmarkName);
    
    Page<ModelEvaluation> findByBenchmarkName(String benchmarkName, Pageable pageable);
    
    // Find evaluations by model and benchmark
    List<ModelEvaluation> findByModelNameAndBenchmarkName(String modelName, String benchmarkName);
    
    // Find active evaluations (queued or running)
    @Query("SELECT e FROM ModelEvaluation e WHERE e.status IN ('QUEUED', 'RUNNING') ORDER BY e.createdAt ASC")
    List<ModelEvaluation> findActiveEvaluations();
    
    // Find completed evaluations within date range
    @Query("SELECT e FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.completedAt BETWEEN :startDate AND :endDate ORDER BY e.completedAt DESC")
    List<ModelEvaluation> findCompletedEvaluationsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find evaluations ordered by overall score
    @Query("SELECT e FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.overallScore IS NOT NULL ORDER BY e.overallScore DESC")
    List<ModelEvaluation> findCompletedEvaluationsOrderedByScore();
    
    // Find evaluations for cleanup (older than specified time)
    @Query("SELECT e FROM ModelEvaluation e WHERE e.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND e.completedAt < :cutoffTime")
    List<ModelEvaluation> findEvaluationsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find running evaluations that might have timed out
    @Query("SELECT e FROM ModelEvaluation e WHERE e.status = 'RUNNING' AND e.startedAt < :timeoutThreshold")
    List<ModelEvaluation> findTimedOutEvaluations(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    // Count evaluations by status
    long countByStatus(EvaluationStatus status);
    
    // Count evaluations by model and status
    long countByModelNameAndStatus(String modelName, EvaluationStatus status);
    
    // Find recent evaluations for dashboard
    @Query("SELECT e FROM ModelEvaluation e ORDER BY e.createdAt DESC")
    Page<ModelEvaluation> findRecentEvaluations(Pageable pageable);
    
    // Find best performing evaluations by model
    @Query("SELECT e FROM ModelEvaluation e WHERE e.modelName = :modelName AND e.status = 'COMPLETED' " +
           "AND e.overallScore IS NOT NULL ORDER BY e.overallScore DESC")
    List<ModelEvaluation> findBestEvaluationsByModel(@Param("modelName") String modelName, Pageable pageable);
    
    // Find evaluations by model provider
    List<ModelEvaluation> findByModelProvider(String modelProvider);
    
    // Statistics queries
    @Query("SELECT AVG(e.overallScore) FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.modelName = :modelName")
    Optional<Double> getAverageScoreForModel(@Param("modelName") String modelName);
    
    @Query("SELECT AVG(e.successRate) FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.benchmarkName = :benchmarkName")
    Optional<Double> getAverageSuccessRateForBenchmark(@Param("benchmarkName") String benchmarkName);
    
    @Query("SELECT AVG(e.totalExecutionTimeSeconds) FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.modelName = :modelName")
    Optional<Double> getAverageExecutionTimeForModel(@Param("modelName") String modelName);
    
    @Query("SELECT COUNT(e), e.status FROM ModelEvaluation e WHERE e.createdAt >= :startDate GROUP BY e.status")
    List<Object[]> getEvaluationStatusCounts(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(e), e.modelName FROM ModelEvaluation e WHERE e.createdAt >= :startDate GROUP BY e.modelName")
    List<Object[]> getModelEvaluationCounts(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(e), e.benchmarkName FROM ModelEvaluation e WHERE e.createdAt >= :startDate GROUP BY e.benchmarkName")
    List<Object[]> getBenchmarkEvaluationCounts(@Param("startDate") LocalDateTime startDate);
    
    // Performance comparison queries
    @Query("SELECT e.modelName, AVG(e.overallScore), AVG(e.successRate), AVG(e.totalExecutionTimeSeconds) " +
           "FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.benchmarkName = :benchmarkName " +
           "GROUP BY e.modelName ORDER BY AVG(e.overallScore) DESC")
    List<Object[]> getModelPerformanceComparison(@Param("benchmarkName") String benchmarkName);
    
    // Find evaluations initiated by user
    List<ModelEvaluation> findByInitiatedBy(String initiatedBy);
    
    Page<ModelEvaluation> findByInitiatedBy(String initiatedBy, Pageable pageable);
    
    // Search evaluations
    @Query("SELECT e FROM ModelEvaluation e WHERE " +
           "LOWER(e.modelName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.benchmarkName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.modelProvider) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY e.createdAt DESC")
    Page<ModelEvaluation> searchEvaluations(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Health check query - find any stuck evaluations
    @Query("SELECT COUNT(e) FROM ModelEvaluation e WHERE e.status = 'RUNNING' AND e.startedAt < :stuckThreshold")
    long countStuckEvaluations(@Param("stuckThreshold") LocalDateTime stuckThreshold);
    
    // Performance monitoring
    @Query("SELECT MIN(e.totalExecutionTimeSeconds), MAX(e.totalExecutionTimeSeconds), AVG(e.totalExecutionTimeSeconds) " +
           "FROM ModelEvaluation e WHERE e.status = 'COMPLETED' AND e.createdAt >= :startDate")
    List<Object[]> getExecutionTimeStatistics(@Param("startDate") LocalDateTime startDate);
    
    // Find latest evaluation for each model and benchmark combination
    @Query("SELECT e FROM ModelEvaluation e WHERE e.createdAt = " +
           "(SELECT MAX(e2.createdAt) FROM ModelEvaluation e2 WHERE e2.modelName = e.modelName AND e2.benchmarkName = e.benchmarkName) " +
           "ORDER BY e.modelName, e.benchmarkName")
    List<ModelEvaluation> findLatestEvaluationPerModelBenchmark();
}