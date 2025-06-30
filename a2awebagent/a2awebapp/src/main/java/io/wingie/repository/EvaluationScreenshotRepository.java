package io.wingie.repository;

import io.wingie.entity.EvaluationScreenshot;
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
public interface EvaluationScreenshotRepository extends JpaRepository<EvaluationScreenshot, Long> {
    
    // Find screenshots by task ID
    List<EvaluationScreenshot> findByTaskId(String taskId);
    
    // Find screenshots by task ID ordered by step number
    List<EvaluationScreenshot> findByTaskIdOrderByStepNumber(String taskId);
    
    // Find screenshots by task ID and step number range
    List<EvaluationScreenshot> findByTaskIdAndStepNumberBetween(String taskId, Integer startStep, Integer endStep);
    
    // Find screenshots taken before or after actions
    List<EvaluationScreenshot> findByTaskIdAndBeforeAction(String taskId, Boolean beforeAction);
    
    // Find success indicator screenshots
    List<EvaluationScreenshot> findByTaskIdAndSuccessIndicatorTrue(String taskId);
    
    // Find error indicator screenshots
    List<EvaluationScreenshot> findByTaskIdAndErrorIndicatorTrue(String taskId);
    
    // Find screenshots within date range
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.timestamp BETWEEN :startDate AND :endDate ORDER BY s.timestamp DESC")
    List<EvaluationScreenshot> findScreenshotsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find latest screenshot for a task
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.taskId = :taskId ORDER BY s.stepNumber DESC, s.timestamp DESC")
    Optional<EvaluationScreenshot> findLatestScreenshotForTask(@Param("taskId") String taskId);
    
    // Find first screenshot for a task
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.taskId = :taskId ORDER BY s.stepNumber ASC, s.timestamp ASC")
    Optional<EvaluationScreenshot> findFirstScreenshotForTask(@Param("taskId") String taskId);
    
    // Count screenshots by task
    long countByTaskId(String taskId);
    
    // Count success/error screenshots by task
    long countByTaskIdAndSuccessIndicatorTrue(String taskId);
    
    long countByTaskIdAndErrorIndicatorTrue(String taskId);
    
    // Find screenshots by action taken
    @Query("SELECT s FROM EvaluationScreenshot s WHERE LOWER(s.actionTaken) LIKE LOWER(CONCAT('%', :action, '%'))")
    List<EvaluationScreenshot> findByActionContaining(@Param("action") String action);
    
    // Find screenshots with specific file size range
    List<EvaluationScreenshot> findByFileSizeBytesBetween(Long minSize, Long maxSize);
    
    // Find screenshots with specific resolution
    List<EvaluationScreenshot> findByImageWidthAndImageHeight(Integer width, Integer height);
    
    // Statistics queries
    @Query("SELECT COUNT(s), AVG(s.fileSizeBytes), AVG(s.imageWidth), AVG(s.imageHeight) " +
           "FROM EvaluationScreenshot s WHERE s.taskId = :taskId")
    List<Object[]> getScreenshotStatisticsForTask(@Param("taskId") String taskId);
    
    @Query("SELECT SUM(s.fileSizeBytes) FROM EvaluationScreenshot s WHERE s.taskId = :taskId")
    Optional<Long> getTotalFileSizeForTask(@Param("taskId") String taskId);
    
    // Find screenshots for cleanup (older than specified time)
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.timestamp < :cutoffTime")
    List<EvaluationScreenshot> findScreenshotsForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find screenshots by file path pattern
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.screenshotPath LIKE :pathPattern")
    List<EvaluationScreenshot> findByScreenshotPathLike(@Param("pathPattern") String pathPattern);
    
    // Find orphaned screenshots (no corresponding task)
    @Query("SELECT s FROM EvaluationScreenshot s WHERE NOT EXISTS " +
           "(SELECT 1 FROM EvaluationTask t WHERE t.taskId = s.taskId)")
    List<EvaluationScreenshot> findOrphanedScreenshots();
    
    // Performance monitoring
    @Query("SELECT DATE(s.timestamp), COUNT(s), AVG(s.fileSizeBytes) " +
           "FROM EvaluationScreenshot s WHERE s.timestamp >= :startDate " +
           "GROUP BY DATE(s.timestamp) ORDER BY DATE(s.timestamp)")
    List<Object[]> getDailyScreenshotStatistics(@Param("startDate") LocalDateTime startDate);
    
    // Find screenshots for specific evaluation
    @Query("SELECT s FROM EvaluationScreenshot s " +
           "JOIN EvaluationTask t ON s.taskId = t.taskId " +
           "WHERE t.evaluationId = :evaluationId " +
           "ORDER BY t.executionOrder, s.stepNumber")
    List<EvaluationScreenshot> findScreenshotsForEvaluation(@Param("evaluationId") String evaluationId);
    
    // Storage analysis
    @Query("SELECT COUNT(s), SUM(s.fileSizeBytes) FROM EvaluationScreenshot s")
    List<Object[]> getTotalStorageUsage();
    
    @Query("SELECT DATE(s.timestamp), SUM(s.fileSizeBytes) " +
           "FROM EvaluationScreenshot s " +
           "GROUP BY DATE(s.timestamp) ORDER BY DATE(s.timestamp)")
    List<Object[]> getDailyStorageUsage();
    
    // Find screenshots by step number across all tasks
    @Query("SELECT s FROM EvaluationScreenshot s WHERE s.stepNumber = :stepNumber ORDER BY s.timestamp DESC")
    List<EvaluationScreenshot> findByStepNumber(@Param("stepNumber") Integer stepNumber);
    
    // Find screenshots with similar dimensions
    @Query("SELECT s FROM EvaluationScreenshot s WHERE " +
           "s.imageWidth BETWEEN :width - :tolerance AND :width + :tolerance AND " +
           "s.imageHeight BETWEEN :height - :tolerance AND :height + :tolerance")
    List<EvaluationScreenshot> findSimilarResolutionScreenshots(@Param("width") Integer width, 
                                                               @Param("height") Integer height, 
                                                               @Param("tolerance") Integer tolerance);
    
    // Recent screenshots for monitoring
    @Query("SELECT s FROM EvaluationScreenshot s ORDER BY s.timestamp DESC")
    Page<EvaluationScreenshot> findRecentScreenshots(Pageable pageable);
}