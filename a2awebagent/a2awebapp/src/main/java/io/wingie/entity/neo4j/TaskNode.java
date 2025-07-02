package io.wingie.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j representation of a task execution for graph-based analytics.
 * Bridges TaskExecution entities to knowledge graph relationships.
 */
@Node("Task")
public class TaskNode {
    
    @Id 
    @GeneratedValue
    private Long id;
    
    @Property("taskId")
    private String taskId;
    
    @Property("taskType")
    private String taskType;
    
    @Property("status")
    private String status;
    
    @Property("originalQuery")
    private String originalQuery;
    
    @Property("startedAt")
    private LocalDateTime startedAt;
    
    @Property("completedAt")
    private LocalDateTime completedAt;
    
    @Property("durationSeconds")
    private Long durationSeconds;
    
    @Property("screenshotCount")
    private Integer screenshotCount;
    
    @Property("pageVisitCount")
    private Integer pageVisitCount;
    
    @Property("actionCount")
    private Integer actionCount;
    
    @Property("successfulActions")
    private Integer successfulActions;
    
    @Property("isSuccessful")
    private Boolean isSuccessful;
    
    @Property("sessionId")
    private String sessionId;
    
    @Property("userId")
    private String userId;
    
    // Relationships for task flow analysis
    @Relationship(type = "CAPTURES_SCREENSHOT", direction = Relationship.Direction.OUTGOING)
    private List<ScreenshotNode> screenshots = new ArrayList<>();
    
    @Relationship(type = "VISITS_PAGE", direction = Relationship.Direction.OUTGOING)
    private List<WebPageNode> visitedPages = new ArrayList<>();
    
    @Relationship(type = "FOLLOWS_TASK", direction = Relationship.Direction.OUTGOING)
    private List<TaskNode> subsequentTasks = new ArrayList<>();
    
    @Relationship(type = "PRECEDED_BY", direction = Relationship.Direction.INCOMING)
    private List<TaskNode> previousTasks = new ArrayList<>();
    
    @Relationship(type = "SIMILAR_TASK", direction = Relationship.Direction.OUTGOING)
    private List<TaskNode> similarTasks = new ArrayList<>();
    
    @Relationship(type = "EXECUTES_WORKFLOW", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowNode> workflows = new ArrayList<>();
    
    // Constructors
    public TaskNode() {}
    
    public TaskNode(String taskId, String taskType, String status) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.status = status;
        this.startedAt = LocalDateTime.now();
        this.screenshotCount = 0;
        this.pageVisitCount = 0;
        this.actionCount = 0;
        this.successfulActions = 0;
        this.isSuccessful = false;
    }
    
    // Factory method to create from TaskExecution entity
    public static TaskNode fromTaskExecution(io.wingie.entity.TaskExecution task) {
        TaskNode node = new TaskNode();
        node.taskId = task.getTaskId();
        node.taskType = task.getTaskType();
        node.status = task.getStatus() != null ? task.getStatus().name() : "UNKNOWN";
        node.originalQuery = task.getOriginalQuery();
        node.startedAt = task.getStartedAt();
        node.completedAt = task.getCompletedAt();
        node.durationSeconds = task.getActualDurationSeconds() != null ? task.getActualDurationSeconds().longValue() : null;
        node.screenshotCount = task.getScreenshots() != null ? task.getScreenshots().size() : 0;
        node.isSuccessful = "COMPLETED".equals(node.status);
        return node;
    }
    
    // Helper methods
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(status) || "TIMEOUT".equals(status) || "CANCELLED".equals(status);
    }
    
    public boolean isLongRunning() {
        return durationSeconds != null && durationSeconds > 300; // 5 minutes
    }
    
    public double getSuccessRate() {
        if (actionCount == null || actionCount == 0) return 0.0;
        return (double) (successfulActions != null ? successfulActions : 0) / actionCount;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public Integer getScreenshotCount() { return screenshotCount; }
    public void setScreenshotCount(Integer screenshotCount) { this.screenshotCount = screenshotCount; }
    
    public Integer getPageVisitCount() { return pageVisitCount; }
    public void setPageVisitCount(Integer pageVisitCount) { this.pageVisitCount = pageVisitCount; }
    
    public Integer getActionCount() { return actionCount; }
    public void setActionCount(Integer actionCount) { this.actionCount = actionCount; }
    
    public Integer getSuccessfulActions() { return successfulActions; }
    public void setSuccessfulActions(Integer successfulActions) { this.successfulActions = successfulActions; }
    
    public Boolean getIsSuccessful() { return isSuccessful; }
    public void setIsSuccessful(Boolean isSuccessful) { this.isSuccessful = isSuccessful; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public List<ScreenshotNode> getScreenshots() { return screenshots; }
    public void setScreenshots(List<ScreenshotNode> screenshots) { this.screenshots = screenshots; }
    
    public List<WebPageNode> getVisitedPages() { return visitedPages; }
    public void setVisitedPages(List<WebPageNode> visitedPages) { this.visitedPages = visitedPages; }
    
    public List<TaskNode> getSubsequentTasks() { return subsequentTasks; }
    public void setSubsequentTasks(List<TaskNode> subsequentTasks) { this.subsequentTasks = subsequentTasks; }
    
    public List<TaskNode> getPreviousTasks() { return previousTasks; }
    public void setPreviousTasks(List<TaskNode> previousTasks) { this.previousTasks = previousTasks; }
    
    public List<TaskNode> getSimilarTasks() { return similarTasks; }
    public void setSimilarTasks(List<TaskNode> similarTasks) { this.similarTasks = similarTasks; }
    
    public List<WorkflowNode> getWorkflows() { return workflows; }
    public void setWorkflows(List<WorkflowNode> workflows) { this.workflows = workflows; }
}