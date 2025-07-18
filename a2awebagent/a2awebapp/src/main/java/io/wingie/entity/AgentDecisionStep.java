package io.wingie.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.wingie.converter.JsonbConverter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for tracking individual decision steps in agentic workflows.
 * 
 * This entity captures the decision-making process of AI agents, linking:
 * - Agent reasoning and tool selection
 * - LLM calls that generated the decisions
 * - Cost and performance metrics per step
 * - Alternative options considered
 * 
 * Used for:
 * - Agent workflow optimization and debugging
 * - Decision quality analysis and improvement
 * - Cost tracking for multi-step agent processes
 * - A/B testing different agent strategies
 */
@Entity
@Table(name = "agent_decision_steps", indexes = {
    @Index(name = "idx_agent_steps_task_execution", columnList = "taskExecutionId"),
    @Index(name = "idx_agent_steps_step_number", columnList = "taskExecutionId, stepNumber"),
    @Index(name = "idx_agent_steps_llm_call", columnList = "llmCallId"),
    @Index(name = "idx_agent_steps_created_at", columnList = "createdAt"),
    @Index(name = "idx_agent_steps_tool_selected", columnList = "toolSelected"),
    @Index(name = "idx_agent_steps_confidence", columnList = "confidenceScore")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentDecisionStep {
    
    @Id
    @Column(length = 36)
    private String stepId;
    
    // Relationship to main task execution
    @Column(name = "task_execution_id", length = 36, nullable = false)
    private String taskExecutionId;
    
    @Column(nullable = false)
    private Integer stepNumber;
    
    // Decision Information
    @Column(length = 100)
    private String toolSelected;
    
    @Column(columnDefinition = "TEXT")
    private String reasoningText;
    
    @Column(precision = 3, scale = 2)
    private BigDecimal confidenceScore; // 0.00 to 1.00
    
    // LLM Call Integration
    @Column(name = "llm_call_id", length = 36)
    private String llmCallId;
    
    // Cost and Performance per Step
    private Integer tokensUsed;
    
    @Column(precision = 8, scale = 4)
    private BigDecimal stepCostUsd;
    
    // Alternative Analysis (stored as JSONB)
    @JdbcTypeCode(java.sql.Types.OTHER)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> alternativesConsidered;
    
    @JdbcTypeCode(java.sql.Types.OTHER)
    @Column(columnDefinition = "jsonb")  
    private Map<String, Object> executionContext;
    
    // Execution Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DecisionStatus status = DecisionStatus.PENDING;
    
    @Column(length = 500)
    private String statusMessage;
    
    // Performance Metrics
    private Long executionTimeMs;
    private Boolean successful;
    
    // Timestamps
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_execution_id", referencedColumnName = "task_id", insertable = false, updatable = false)
    private TaskExecution taskExecution;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_call_id", referencedColumnName = "callId", insertable = false, updatable = false)
    private LLMCallLog llmCallLog;
    
    @PrePersist
    public void prePersist() {
        if (stepId == null) {
            stepId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DecisionStatus.PENDING;
        }
    }
    
    /**
     * Decision execution status
     */
    public enum DecisionStatus {
        PENDING,        // Decision made but not yet executed
        IN_PROGRESS,    // Currently executing the decided action
        COMPLETED,      // Successfully completed
        FAILED,         // Failed during execution
        CANCELLED       // Cancelled before completion
    }
    
    /**
     * Mark the decision step as started
     */
    public void markStarted() {
        this.status = DecisionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }
    
    /**
     * Mark the decision step as completed successfully
     */
    public void markCompleted(boolean successful) {
        this.status = successful ? DecisionStatus.COMPLETED : DecisionStatus.FAILED;
        this.successful = successful;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }
    
    /**
     * Mark the decision step as failed with error message
     */
    public void markFailed(String errorMessage) {
        this.status = DecisionStatus.FAILED;
        this.successful = false;
        this.statusMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }
    
    /**
     * Mark the decision step as cancelled
     */
    public void markCancelled(String reason) {
        this.status = DecisionStatus.CANCELLED;
        this.statusMessage = reason;
        this.completedAt = LocalDateTime.now();
        this.executionTimeMs = calculateExecutionTime();
    }
    
    /**
     * Calculate execution time from start to completion
     */
    private Long calculateExecutionTime() {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, completedAt).toMillis();
    }
    
    /**
     * Get total execution time (from creation to completion if no explicit start)
     */
    public Long getTotalExecutionTime() {
        if (completedAt == null) {
            return null;
        }
        LocalDateTime start = startedAt != null ? startedAt : createdAt;
        return java.time.Duration.between(start, completedAt).toMillis();
    }
    
    /**
     * Check if this decision step is currently active
     */
    public boolean isActive() {
        return status == DecisionStatus.PENDING || status == DecisionStatus.IN_PROGRESS;
    }
    
    /**
     * Check if this decision step completed successfully
     */
    public boolean isSuccessful() {
        return status == DecisionStatus.COMPLETED && Boolean.TRUE.equals(successful);
    }
    
    /**
     * Set alternatives considered (direct Map assignment)
     */
    public void setAlternativesMap(Map<String, Object> alternatives) {
        this.alternativesConsidered = alternatives;
    }
    
    /**
     * Set execution context (direct Map assignment)
     */
    public void setExecutionContextMap(Map<String, Object> context) {
        this.executionContext = context;
    }
    
    /**
     * Convenience method to get alternatives as Map (now directly available)
     */
    public Map<String, Object> getAlternativesMap() {
        return alternativesConsidered != null ? alternativesConsidered : new HashMap<>();
    }
    
    /**
     * Get confidence score as percentage (0-100)
     */
    public Double getConfidencePercentage() {
        if (confidenceScore == null) {
            return null;
        }
        return confidenceScore.multiply(new BigDecimal(100)).doubleValue();
    }
    
    /**
     * Set confidence score from percentage (0-100)
     */
    public void setConfidencePercentage(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Confidence percentage must be between 0 and 100");
        }
        this.confidenceScore = new BigDecimal(percentage).divide(new BigDecimal(100));
    }
    
    /**
     * Generate a summary of this decision step for logging/display
     */
    public String getSummary() {
        return String.format("Step %d: %s -> %s (confidence: %.1f%%, cost: $%.4f, %s)",
                stepNumber,
                toolSelected,
                status,
                getConfidencePercentage() != null ? getConfidencePercentage() : 0.0,
                stepCostUsd != null ? stepCostUsd.doubleValue() : 0.0,
                successful != null ? (successful ? "success" : "failed") : "pending"
        );
    }
}