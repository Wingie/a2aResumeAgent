package io.wingie.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

/**
 * Neo4j node representing state transitions within task execution.
 * Tracks how tasks progress through different states for pattern analysis.
 */
@Node("TaskTransition")
public class TaskTransitionNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("taskId")
    private String taskId;
    
    @Property("fromStatus")
    private String fromStatus;
    
    @Property("toStatus")
    private String toStatus;
    
    @Property("transitionTime")
    private LocalDateTime transitionTime;
    
    @Property("progressPercent")
    private Integer progressPercent;
    
    @Property("progressMessage")
    private String progressMessage;
    
    @Property("transitionCause")
    private String transitionCause; // USER_ACTION, SYSTEM_EVENT, ERROR, TIMEOUT, etc.
    
    @Property("durationInState")
    private Long durationInStateSeconds;
    
    @Property("isSuccessfulTransition")
    private Boolean isSuccessfulTransition;
    
    @Property("errorMessage")
    private String errorMessage;
    
    @Property("contextData")
    private String contextData; // JSON string for additional context
    
    // Constructors
    public TaskTransitionNode() {}
    
    public TaskTransitionNode(String taskId, String fromStatus, String toStatus, String transitionCause) {
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.transitionCause = transitionCause;
        this.transitionTime = LocalDateTime.now();
        this.isSuccessfulTransition = !isFailureTransition(toStatus);
    }
    
    // Helper methods
    public boolean isFailureTransition(String status) {
        return "FAILED".equals(status) || "TIMEOUT".equals(status) || "CANCELLED".equals(status);
    }
    
    public boolean isCompletionTransition() {
        return "COMPLETED".equals(toStatus);
    }
    
    public boolean isProgressTransition() {
        return "RUNNING".equals(fromStatus) && "RUNNING".equals(toStatus);
    }
    
    public String getTransitionType() {
        if (isCompletionTransition()) return "COMPLETION";
        if (isFailureTransition(toStatus)) return "FAILURE";
        if (isProgressTransition()) return "PROGRESS";
        if ("QUEUED".equals(fromStatus) && "RUNNING".equals(toStatus)) return "START";
        return "OTHER";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    
    public LocalDateTime getTransitionTime() { return transitionTime; }
    public void setTransitionTime(LocalDateTime transitionTime) { this.transitionTime = transitionTime; }
    
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    
    public String getProgressMessage() { return progressMessage; }
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    
    public String getTransitionCause() { return transitionCause; }
    public void setTransitionCause(String transitionCause) { this.transitionCause = transitionCause; }
    
    public Long getDurationInStateSeconds() { return durationInStateSeconds; }
    public void setDurationInStateSeconds(Long durationInStateSeconds) { this.durationInStateSeconds = durationInStateSeconds; }
    
    public Boolean getIsSuccessfulTransition() { return isSuccessfulTransition; }
    public void setIsSuccessfulTransition(Boolean isSuccessfulTransition) { this.isSuccessfulTransition = isSuccessfulTransition; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }
}