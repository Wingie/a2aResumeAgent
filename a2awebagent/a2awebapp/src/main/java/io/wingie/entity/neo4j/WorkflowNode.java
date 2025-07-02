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
 * Neo4j node representing common workflows or sequences of actions.
 * Identifies patterns in how users/agents navigate through tasks.
 */
@Node("Workflow")
public class WorkflowNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("workflowName")
    private String workflowName;
    
    @Property("workflowType")
    private String workflowType; // BOOKING, SEARCH, LOGIN, CHECKOUT, etc.
    
    @Property("description")
    private String description;
    
    @Property("stepCount")
    private Integer stepCount;
    
    @Property("avgDuration")
    private Double avgDuration; // Average duration in seconds
    
    @Property("successRate")
    private Double successRate;
    
    @Property("occurrenceCount")
    private Integer occurrenceCount;
    
    @Property("firstSeen")
    private LocalDateTime firstSeen;
    
    @Property("lastSeen")
    private LocalDateTime lastSeen;
    
    @Property("domains")
    private String domains; // JSON list of domains where workflow occurs
    
    // Relationships
    @Relationship(type = "CONTAINS_PATTERN", direction = Relationship.Direction.OUTGOING)
    private List<UIPatternNode> patterns = new ArrayList<>();
    
    @Relationship(type = "EXECUTED_IN", direction = Relationship.Direction.INCOMING)
    private List<TaskNode> tasks = new ArrayList<>();
    
    // Constructors and basic methods
    public WorkflowNode() {}
    
    public WorkflowNode(String workflowName, String workflowType) {
        this.workflowName = workflowName;
        this.workflowType = workflowType;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.occurrenceCount = 1;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    
    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    
    public Double getAvgDuration() { return avgDuration; }
    public void setAvgDuration(Double avgDuration) { this.avgDuration = avgDuration; }
    
    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    
    public String getDomains() { return domains; }
    public void setDomains(String domains) { this.domains = domains; }
    
    public List<UIPatternNode> getPatterns() { return patterns; }
    public void setPatterns(List<UIPatternNode> patterns) { this.patterns = patterns; }
    
    public List<TaskNode> getTasks() { return tasks; }
    public void setTasks(List<TaskNode> tasks) { this.tasks = tasks; }
}