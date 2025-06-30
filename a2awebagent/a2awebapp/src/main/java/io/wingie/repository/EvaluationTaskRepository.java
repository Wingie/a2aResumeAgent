package io.wingie.repository;

import io.wingie.entity.EvaluationTask;
import io.wingie.entity.EvaluationTaskStatus;
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
public interface EvaluationTaskRepository extends JpaRepository<EvaluationTask, String> {
    
    // Find tasks by evaluation ID
    List<EvaluationTask> findByEvaluationId(String evaluationId);
    
    // Find tasks by evaluation ID ordered by execution order
    List<EvaluationTask> findByEvaluationIdOrderByExecutionOrder(String evaluationId);
    
    // Find tasks by status
    List<EvaluationTask> findByStatus(EvaluationTaskStatus status);
    
    Page<EvaluationTask> findByStatus(EvaluationTaskStatus status, Pageable pageable);
    
    // Find tasks by evaluation and status
    List<EvaluationTask> findByEvaluationIdAndStatus(String evaluationId, EvaluationTaskStatus status);
    
    // Find active tasks (pending or running)
    @Query("SELECT t FROM EvaluationTask t WHERE t.status IN ('PENDING', 'RUNNING') ORDER BY t.createdAt ASC")
    List<EvaluationTask> findActiveTasks();
    
    // Find tasks by task name or category
    List<EvaluationTask> findByTaskName(String taskName);
    
    List<EvaluationTask> findByTaskCategory(String taskCategory);
    
    // Find tasks that can be retried
    @Query("SELECT t FROM EvaluationTask t WHERE t.status IN ('FAILED', 'TIMEOUT') AND t.retryCount < t.maxRetries")
    List<EvaluationTask> findRetryableTasks();
    
    // Find tasks that have timed out
    @Query("SELECT t FROM EvaluationTask t WHERE t.status = 'RUNNING' AND t.startedAt < :timeoutThreshold")
    List<EvaluationTask> findTimedOutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    // Count tasks by status
    long countByStatus(EvaluationTaskStatus status);
    
    // Count tasks by evaluation and status
    long countByEvaluationIdAndStatus(String evaluationId, EvaluationTaskStatus status);
    
    // Statistics for an evaluation
    @Query("SELECT COUNT(t) FROM EvaluationTask t WHERE t.evaluationId = :evaluationId")
    long countTasksInEvaluation(@Param("evaluationId") String evaluationId);
    
    @Query("SELECT COUNT(t) FROM EvaluationTask t WHERE t.evaluationId = :evaluationId AND t.status = 'COMPLETED' AND t.success = true")
    long countSuccessfulTasksInEvaluation(@Param("evaluationId") String evaluationId);
    
    @Query("SELECT COUNT(t) FROM EvaluationTask t WHERE t.evaluationId = :evaluationId AND (t.status = 'FAILED' OR (t.status = 'COMPLETED' AND t.success = false))")
    long countFailedTasksInEvaluation(@Param("evaluationId") String evaluationId);
    
    @Query("SELECT AVG(t.executionTimeSeconds) FROM EvaluationTask t WHERE t.evaluationId = :evaluationId AND t.executionTimeSeconds IS NOT NULL")
    Optional<Double> getAverageExecutionTimeForEvaluation(@Param("evaluationId") String evaluationId);
    
    @Query("SELECT AVG(t.score) FROM EvaluationTask t WHERE t.evaluationId = :evaluationId AND t.score IS NOT NULL")
    Optional<Double> getAverageScoreForEvaluation(@Param("evaluationId") String evaluationId);
    
    // Find tasks with screenshots
    @Query("SELECT t FROM EvaluationTask t WHERE EXISTS (SELECT 1 FROM EvaluationScreenshot s WHERE s.taskId = t.taskId)")
    List<EvaluationTask> findTasksWithScreenshots();
    
    // Find tasks by difficulty level
    List<EvaluationTask> findByDifficultyLevel(Integer difficultyLevel);
    
    // Find tasks by tags (contains search)
    @Query("SELECT t FROM EvaluationTask t WHERE LOWER(t.tags) LIKE LOWER(CONCAT('%', :tag, '%'))")
    List<EvaluationTask> findTasksByTag(@Param("tag") String tag);
    
    // Search tasks by prompt content
    @Query("SELECT t FROM EvaluationTask t WHERE LOWER(t.prompt) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.taskName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.taskDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<EvaluationTask> searchTasks(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Performance analysis queries
    @Query("SELECT t.taskCategory, AVG(t.executionTimeSeconds), AVG(CASE WHEN t.success = true THEN 1.0 ELSE 0.0 END) " +
           "FROM EvaluationTask t WHERE t.status = 'COMPLETED' AND t.createdAt >= :startDate " +
           "GROUP BY t.taskCategory ORDER BY t.taskCategory")
    List<Object[]> getTaskCategoryPerformance(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT t.difficultyLevel, AVG(t.executionTimeSeconds), AVG(CASE WHEN t.success = true THEN 1.0 ELSE 0.0 END) " +
           "FROM EvaluationTask t WHERE t.status = 'COMPLETED' AND t.createdAt >= :startDate " +
           "GROUP BY t.difficultyLevel ORDER BY t.difficultyLevel")
    List<Object[]> getDifficultyLevelPerformance(@Param("startDate") LocalDateTime startDate);
    
    // Find next task to execute in an evaluation
    @Query("SELECT t FROM EvaluationTask t WHERE t.evaluationId = :evaluationId AND t.status = 'PENDING' " +
           "ORDER BY t.executionOrder ASC, t.createdAt ASC")
    Optional<EvaluationTask> findNextTaskToExecute(@Param("evaluationId") String evaluationId);
    
    // Find tasks that are currently running
    @Query("SELECT t FROM EvaluationTask t WHERE t.status = 'RUNNING' ORDER BY t.startedAt ASC")
    List<EvaluationTask> findRunningTasks();
    
    // Find completed tasks within date range
    @Query("SELECT t FROM EvaluationTask t WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :startDate AND :endDate ORDER BY t.completedAt DESC")
    List<EvaluationTask> findCompletedTasksBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Error analysis
    @Query("SELECT t.errorMessage, COUNT(t) FROM EvaluationTask t WHERE t.status = 'FAILED' AND t.createdAt >= :startDate " +
           "GROUP BY t.errorMessage ORDER BY COUNT(t) DESC")
    List<Object[]> getCommonErrors(@Param("startDate") LocalDateTime startDate);
    
    // Health monitoring
    @Query("SELECT COUNT(t) FROM EvaluationTask t WHERE t.status = 'RUNNING' AND t.startedAt < :stuckThreshold")
    long countStuckTasks(@Param("stuckThreshold") LocalDateTime stuckThreshold);
    
    // Benchmark comparison
    @Query("SELECT t.taskName, " +
           "AVG(CASE WHEN evaluation.modelName = :model1 THEN t.score END) as model1Score, " +
           "AVG(CASE WHEN evaluation.modelName = :model2 THEN t.score END) as model2Score " +
           "FROM EvaluationTask t JOIN t.evaluation evaluation " +
           "WHERE evaluation.modelName IN (:model1, :model2) AND t.status = 'COMPLETED' AND t.score IS NOT NULL " +
           "GROUP BY t.taskName ORDER BY t.taskName")
    List<Object[]> compareModelsOnTasks(@Param("model1") String model1, @Param("model2") String model2);
    
    // Find tasks for cleanup
    @Query("SELECT t FROM EvaluationTask t WHERE t.status IN ('COMPLETED', 'FAILED', 'TIMEOUT', 'SKIPPED') AND t.completedAt < :cutoffTime")
    List<EvaluationTask> findTasksForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
}