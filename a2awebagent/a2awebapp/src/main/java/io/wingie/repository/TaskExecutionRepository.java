package io.wingie.repository;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
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
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, String> {
    
    // Find tasks by status
    List<TaskExecution> findByStatus(TaskStatus status);
    
    Page<TaskExecution> findByStatus(TaskStatus status, Pageable pageable);
    
    // Find tasks by type
    List<TaskExecution> findByTaskType(String taskType);
    
    Page<TaskExecution> findByTaskType(String taskType, Pageable pageable);
    
    // Find tasks by requester
    List<TaskExecution> findByRequesterId(String requesterId);
    
    Page<TaskExecution> findByRequesterId(String requesterId, Pageable pageable);
    
    // Find active tasks (queued or running)
    @Query("SELECT t FROM TaskExecution t WHERE t.status IN ('QUEUED', 'RUNNING') ORDER BY t.created ASC")
    List<TaskExecution> findActiveTasks();
    
    // Find completed tasks within date range
    @Query("SELECT t FROM TaskExecution t WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :startDate AND :endDate ORDER BY t.completedAt DESC")
    List<TaskExecution> findCompletedTasksBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find failed tasks that can be retried
    @Query("SELECT t FROM TaskExecution t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetries ORDER BY t.updated DESC")
    List<TaskExecution> findRetryableTasks();
    
    // Find tasks older than specified time for cleanup
    @Query("SELECT t FROM TaskExecution t WHERE t.status IN ('COMPLETED', 'FAILED', 'CANCELLED', 'TIMEOUT') AND t.completedAt < :cutoffTime")
    List<TaskExecution> findTasksForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find running tasks that might have timed out
    @Query("SELECT t FROM TaskExecution t WHERE t.status = 'RUNNING' AND t.startedAt < :timeoutThreshold")
    List<TaskExecution> findTimedOutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    // Count tasks by status
    long countByStatus(TaskStatus status);
    
    // Count tasks by type and status
    long countByTaskTypeAndStatus(String taskType, TaskStatus status);
    
    // Find recent tasks for dashboard
    @Query("SELECT t FROM TaskExecution t ORDER BY t.created DESC")
    Page<TaskExecution> findRecentTasks(Pageable pageable);
    
    // Find tasks with screenshots
    @Query("SELECT t FROM TaskExecution t WHERE SIZE(t.screenshots) > 0 ORDER BY t.updated DESC")
    List<TaskExecution> findTasksWithScreenshots();
    
    // Search tasks by query content
    @Query("SELECT t FROM TaskExecution t WHERE LOWER(t.originalQuery) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.created DESC")
    Page<TaskExecution> searchTasksByQuery(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT AVG(t.actualDurationSeconds) FROM TaskExecution t WHERE t.status = 'COMPLETED' AND t.taskType = :taskType")
    Optional<Double> getAverageDurationForTaskType(@Param("taskType") String taskType);
    
    @Query("SELECT COUNT(t), t.status FROM TaskExecution t WHERE t.created >= :startDate GROUP BY t.status")
    List<Object[]> getTaskStatusCounts(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(t), t.taskType FROM TaskExecution t WHERE t.created >= :startDate GROUP BY t.taskType")
    List<Object[]> getTaskTypeCounts(@Param("startDate") LocalDateTime startDate);
    
    // Find tasks by status with sorting
    @Query("SELECT t FROM TaskExecution t WHERE t.status = :status ORDER BY " +
           "CASE WHEN :sortBy = 'created' THEN t.created END DESC, " +
           "CASE WHEN :sortBy = 'updated' THEN t.updated END DESC, " +
           "CASE WHEN :sortBy = 'duration' THEN t.actualDurationSeconds END DESC")
    List<TaskExecution> findByStatusSorted(@Param("status") TaskStatus status, @Param("sortBy") String sortBy);
    
    // Health check query - find any stuck tasks
    @Query("SELECT COUNT(t) FROM TaskExecution t WHERE t.status = 'RUNNING' AND t.startedAt < :stuckThreshold")
    long countStuckTasks(@Param("stuckThreshold") LocalDateTime stuckThreshold);
    
    // Performance monitoring
    @Query("SELECT MIN(t.actualDurationSeconds), MAX(t.actualDurationSeconds), AVG(t.actualDurationSeconds) " +
           "FROM TaskExecution t WHERE t.status = 'COMPLETED' AND t.created >= :startDate")
    List<Object[]> getDurationStatistics(@Param("startDate") LocalDateTime startDate);
    
    // DTO-optimized queries for SSE endpoints (avoiding lazy loading issues)
    
    // Find active tasks without loading screenshots collection
    @Query("SELECT t FROM TaskExecution t WHERE t.status IN ('QUEUED', 'RUNNING') ORDER BY t.created ASC")
    List<TaskExecution> findActiveTasksForSSE();
    
    // Find recent tasks for dashboard without screenshots (for performance)
    @Query("SELECT t FROM TaskExecution t ORDER BY t.created DESC")
    Page<TaskExecution> findRecentTasksForDashboard(Pageable pageable);
    
    // Find task with eagerly loaded screenshots for details view
    @Query("SELECT t FROM TaskExecution t LEFT JOIN FETCH t.screenshots WHERE t.taskId = :taskId")
    Optional<TaskExecution> findByIdWithScreenshots(@Param("taskId") String taskId);
    
    // Count screenshots for a task without loading the collection
    @Query("SELECT SIZE(t.screenshots) FROM TaskExecution t WHERE t.taskId = :taskId")
    Optional<Integer> countScreenshotsForTask(@Param("taskId") String taskId);
    
    // Find tasks with screenshot count for list views
    @Query("SELECT t.taskId, t.status, t.taskType, t.originalQuery, t.progressMessage, t.progressPercent, " +
           "t.created, t.updated, t.startedAt, t.completedAt, t.requesterId, t.actualDurationSeconds, " +
           "t.retryCount, SIZE(t.screenshots) as screenshotCount " +
           "FROM TaskExecution t WHERE t.status IN ('QUEUED', 'RUNNING') ORDER BY t.created ASC")
    List<Object[]> findActiveTasksWithScreenshotCount();
    
    // Statistics query optimized for SSE (no screenshots loading)
    @Query("SELECT COUNT(t) FROM TaskExecution t WHERE t.status = :status")
    long countByStatusForSSE(@Param("status") TaskStatus status);
    
    // Find latest tasks for SSE without screenshots
    @Query("SELECT t FROM TaskExecution t WHERE t.updated >= :sinceTime ORDER BY t.updated DESC")
    List<TaskExecution> findRecentlyUpdatedTasks(@Param("sinceTime") LocalDateTime sinceTime);
}