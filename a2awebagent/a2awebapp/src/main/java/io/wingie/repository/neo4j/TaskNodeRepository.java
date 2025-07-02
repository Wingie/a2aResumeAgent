package io.wingie.repository.neo4j;

import io.wingie.entity.neo4j.TaskNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Neo4j repository for TaskNode entities.
 * Provides task relationship analysis and workflow pattern queries.
 */
@Repository
public interface TaskNodeRepository extends Neo4jRepository<TaskNode, Long> {
    
    // Basic finders
    Optional<TaskNode> findByTaskId(String taskId);
    List<TaskNode> findByTaskType(String taskType);
    List<TaskNode> findByStatus(String status);
    List<TaskNode> findByIsSuccessful(boolean isSuccessful);
    List<TaskNode> findBySessionId(String sessionId);
    List<TaskNode> findByUserId(String userId);
    
    // Task flow analysis
    @Query("MATCH (t1:Task)-[:FOLLOWS_TASK]->(t2:Task) WHERE t1.taskId = $taskId RETURN t2 ORDER BY t2.startedAt")
    List<TaskNode> findSubsequentTasks(@Param("taskId") String taskId);
    
    @Query("MATCH (t1:Task)<-[:FOLLOWS_TASK]-(t2:Task) WHERE t1.taskId = $taskId RETURN t2 ORDER BY t2.startedAt DESC")
    List<TaskNode> findPreviousTasks(@Param("taskId") String taskId);
    
    @Query("MATCH path = (start:Task)-[:FOLLOWS_TASK*1..5]->(end:Task) " +
           "WHERE start.taskId = $startTaskId AND end.taskId = $endTaskId " +
           "RETURN path ORDER BY length(path) LIMIT 1")
    List<TaskNode> findTaskSequence(@Param("startTaskId") String startTaskId, @Param("endTaskId") String endTaskId);
    
    @Query("MATCH (t:Task)-[:FOLLOWS_TASK*1..3]->(next:Task) WHERE t.taskId = $taskId " +
           "RETURN next, length(path) as distance ORDER BY distance, next.startedAt LIMIT $limit")
    List<TaskNode> findTaskChain(@Param("taskId") String taskId, @Param("limit") int limit);
    
    // Similarity analysis
    @Query("MATCH (t1:Task)-[:SIMILAR_TASK]->(t2:Task) WHERE t1.taskId = $taskId RETURN t2")
    List<TaskNode> findSimilarTasks(@Param("taskId") String taskId);
    
    @Query("MATCH (t:Task) WHERE t.taskType = $taskType AND t.originalQuery CONTAINS $queryFragment " +
           "RETURN t ORDER BY t.startedAt DESC LIMIT $limit")
    List<TaskNode> findTasksByTypeAndQuery(@Param("taskType") String taskType, @Param("queryFragment") String queryFragment, @Param("limit") int limit);
    
    // Performance analysis
    @Query("MATCH (t:Task) WHERE t.taskType = $taskType AND t.isSuccessful = true " +
           "RETURN t ORDER BY t.durationSeconds ASC LIMIT $limit")
    List<TaskNode> findFastestSuccessfulTasks(@Param("taskType") String taskType, @Param("limit") int limit);
    
    @Query("MATCH (t:Task) WHERE t.taskType = $taskType AND t.isSuccessful = false " +
           "RETURN t ORDER BY t.startedAt DESC LIMIT $limit")
    List<TaskNode> findRecentFailedTasks(@Param("taskType") String taskType, @Param("limit") int limit);
    
    @Query("MATCH (t:Task) WHERE t.durationSeconds > $minDuration RETURN t ORDER BY t.durationSeconds DESC")
    List<TaskNode> findLongRunningTasks(@Param("minDuration") long minDuration);
    
    // Session and user analysis
    @Query("MATCH (t:Task) WHERE t.sessionId = $sessionId RETURN t ORDER BY t.startedAt")
    List<TaskNode> findTasksBySession(@Param("sessionId") String sessionId);
    
    @Query("MATCH (t:Task) WHERE t.userId = $userId RETURN t ORDER BY t.startedAt DESC LIMIT $limit")
    List<TaskNode> findRecentTasksByUser(@Param("userId") String userId, @Param("limit") int limit);
    
    @Query("MATCH (t:Task) WHERE t.sessionId = $sessionId " +
           "RETURN COUNT(t) as totalTasks, " +
           "COUNT(CASE WHEN t.isSuccessful = true THEN 1 END) as successfulTasks, " +
           "AVG(t.durationSeconds) as avgDuration")
    SessionStats getSessionStatistics(@Param("sessionId") String sessionId);
    
    // Time-based queries
    @Query("MATCH (t:Task) WHERE t.startedAt >= $since RETURN t ORDER BY t.startedAt DESC")
    List<TaskNode> findTasksSince(@Param("since") LocalDateTime since);
    
    @Query("MATCH (t:Task) WHERE t.startedAt >= $start AND t.startedAt <= $end RETURN t ORDER BY t.startedAt")
    List<TaskNode> findTasksBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Screenshot and page relationships
    @Query("MATCH (t:Task)-[:CAPTURES_SCREENSHOT]->(s:Screenshot) WHERE t.taskId = $taskId " +
           "RETURN COUNT(s) as screenshotCount")
    Long getScreenshotCountForTask(@Param("taskId") String taskId);
    
    @Query("MATCH (t:Task)-[:VISITS_PAGE]->(p:WebPage) WHERE t.taskId = $taskId " +
           "RETURN p ORDER BY p.visitCount DESC")
    List<String> getVisitedPagesForTask(@Param("taskId") String taskId);
    
    @Query("MATCH (t:Task)-[:VISITS_PAGE]->(p:WebPage) WHERE p.domain = $domain " +
           "RETURN t ORDER BY t.startedAt DESC LIMIT $limit")
    List<TaskNode> findTasksByDomain(@Param("domain") String domain, @Param("limit") int limit);
    
    // Workflow analysis
    @Query("MATCH (t:Task)-[:EXECUTES_WORKFLOW]->(w:Workflow) WHERE t.taskId = $taskId RETURN w")
    List<String> getWorkflowsForTask(@Param("taskId") String taskId);
    
    @Query("MATCH (t:Task)-[:EXECUTES_WORKFLOW]->(w:Workflow) WHERE w.workflowType = $workflowType " +
           "RETURN t ORDER BY t.startedAt DESC LIMIT $limit")
    List<TaskNode> findTasksByWorkflowType(@Param("workflowType") String workflowType, @Param("limit") int limit);
    
    // Statistics and analytics
    @Query("MATCH (t:Task) WHERE t.taskType = $taskType " +
           "RETURN COUNT(t) as totalTasks, " +
           "COUNT(CASE WHEN t.isSuccessful = true THEN 1 END) as successfulTasks, " +
           "AVG(t.durationSeconds) as avgDuration, " +
           "AVG(t.screenshotCount) as avgScreenshots")
    TaskTypeStats getTaskTypeStatistics(@Param("taskType") String taskType);
    
    @Query("MATCH (t:Task) " +
           "RETURN t.taskType as taskType, COUNT(t) as count, " +
           "AVG(CASE WHEN t.isSuccessful = true THEN 1.0 ELSE 0.0 END) as successRate " +
           "ORDER BY count DESC")
    List<TaskTypeSuccessRate> getTaskTypeSuccessRates();
    
    @Query("MATCH (t:Task) WHERE t.startedAt >= $since " +
           "RETURN COUNT(t) as totalTasks, " +
           "COUNT(CASE WHEN t.isSuccessful = true THEN 1 END) as successfulTasks, " +
           "COUNT(CASE WHEN t.status = 'FAILED' THEN 1 END) as failedTasks, " +
           "AVG(t.durationSeconds) as avgDuration")
    PeriodStats getPeriodStatistics(@Param("since") LocalDateTime since);
    
    // Pattern detection
    @Query("MATCH (t1:Task)-[:FOLLOWS_TASK]->(t2:Task) " +
           "WHERE t1.taskType = $taskType1 AND t2.taskType = $taskType2 " +
           "RETURN COUNT(*) as occurrences")
    Long countTaskTypeSequences(@Param("taskType1") String taskType1, @Param("taskType2") String taskType2);
    
    @Query("MATCH (t:Task) WHERE t.originalQuery CONTAINS $keyword " +
           "RETURN t.taskType as taskType, COUNT(t) as count " +
           "ORDER BY count DESC LIMIT $limit")
    List<TaskTypeCount> findTaskTypesByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
    
    // Cleanup
    @Query("MATCH (t:Task) WHERE t.startedAt < $cutoffDate DELETE t")
    void deleteTasksOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("MATCH (t:Task) WHERE t.status = 'FAILED' AND t.startedAt < $cutoffDate DELETE t")
    void deleteOldFailedTasks(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Projection interfaces
    interface SessionStats {
        Long getTotalTasks();
        Long getSuccessfulTasks();
        Double getAvgDuration();
    }
    
    interface TaskTypeStats {
        Long getTotalTasks();
        Long getSuccessfulTasks();
        Double getAvgDuration();
        Double getAvgScreenshots();
    }
    
    interface TaskTypeSuccessRate {
        String getTaskType();
        Long getCount();
        Double getSuccessRate();
    }
    
    interface PeriodStats {
        Long getTotalTasks();
        Long getSuccessfulTasks();
        Long getFailedTasks();
        Double getAvgDuration();
    }
    
    interface TaskTypeCount {
        String getTaskType();
        Long getCount();
    }
}