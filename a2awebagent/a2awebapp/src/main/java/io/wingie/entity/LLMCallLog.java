package io.wingie.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking LLM API calls with cache integration for the agentic-harness system.
 * 
 * This entity provides comprehensive logging of all LLM interactions, including:
 * - Cache hit/miss tracking 
 * - Token usage and cost analysis
 * - Performance metrics
 * - Error tracking and retry patterns
 * - Integration with ToolDescription caching system
 * 
 * Used for:
 * - Cost optimization and budget monitoring
 * - Performance analysis and bottleneck identification  
 * - Agent decision tree tracking in agentic workflows
 * - LLM provider comparison and A/B testing
 */
@Entity
@Table(name = "llm_call_logs", indexes = {
    @Index(name = "idx_cache_key", columnList = "cacheKey"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_provider_model", columnList = "provider, modelName"),
    @Index(name = "idx_task_execution", columnList = "taskExecutionId"),
    @Index(name = "idx_session_id", columnList = "sessionId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMCallLog {
    
    @Id
    @Column(length = 36)
    private String callId;
    
    // Cache Integration - Links to ToolDescription system
    @Column(length = 255, nullable = false)
    private String cacheKey;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean cacheHit = false;
    
    // LLM Provider Information (only for cache misses)
    @Column(length = 50)
    private String provider; // "openai", "gemini", "anthropic", "openrouter"
    
    @Column(length = 100)
    private String modelName; // "gpt-4o-mini", "gemini-2.0-flash", etc.
    
    // Request/Response Data (stored as JSON for flexibility)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestPayload;
    
    @Lob
    @Column(columnDefinition = "TEXT") 
    private String responsePayload;
    
    // Performance Metrics
    private Long responseTimeMs;
    
    // Token Usage and Cost Tracking
    private Integer inputTokens;
    private Integer outputTokens;
    
    @Column(precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;
    
    // Error Handling
    @Column(length = 50)
    private String errorCode;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Builder.Default
    private Integer retryAttempt = 0;
    
    // Context and Relationships
    @Column(length = 100, nullable = false)
    private String toolName;
    
    @Column(name = "task_execution_id", length = 36)
    private String taskExecutionId; // Links to TaskExecution for agentic workflows
    
    @Column(length = 100)
    private String sessionId;
    
    @Column(length = 100)
    private String userId;
    
    // Request Context
    @Column(length = 45) // IPv6 max length
    private String requestIp;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    // Timestamps
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime completedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_execution_id", referencedColumnName = "task_id", insertable = false, updatable = false)
    private TaskExecution taskExecution;
    
    @PrePersist
    public void prePersist() {
        if (callId == null) {
            callId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (cacheHit == null) {
            cacheHit = false;
        }
        if (retryAttempt == null) {
            retryAttempt = 0;
        }
    }
    
    /**
     * Convenience method to generate cache key for this LLM call
     */
    public String generateCacheKey() {
        return String.format("%s:%s", modelName, toolName);
    }
    
    /**
     * Calculate total tokens (input + output)
     */
    public Integer getTotalTokens() {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
    }
    
    /**
     * Check if this was a successful LLM call
     */
    public boolean isSuccessful() {
        return errorCode == null && completedAt != null;
    }
    
    /**
     * Get execution duration in milliseconds
     */
    public Long getExecutionDurationMs() {
        if (createdAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(createdAt, completedAt).toMillis();
    }
    
    /**
     * Mark the call as completed successfully
     */
    public void markCompleted() {
        this.completedAt = LocalDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
    }
    
    /**
     * Mark the call as failed with error details
     */
    public void markFailed(String errorCode, String errorMessage) {
        this.completedAt = LocalDateTime.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Mark as cache hit (no actual LLM call made)
     */
    public void markCacheHit() {
        this.cacheHit = true;
        this.completedAt = LocalDateTime.now();
        this.responseTimeMs = 0L; // Cache hits are instant
        this.inputTokens = 0;
        this.outputTokens = 0;
        this.estimatedCostUsd = BigDecimal.ZERO;
    }
}