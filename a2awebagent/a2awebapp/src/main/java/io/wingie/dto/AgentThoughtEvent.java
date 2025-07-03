package io.wingie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a real-time agent thought event for streaming to the frontend.
 * 
 * This DTO captures the AI agent's reasoning process, including:
 * - What the agent is thinking about
 * - Which tool it's considering or selecting  
 * - Confidence levels in its decisions
 * - Alternative options being considered
 * - Performance metrics
 * 
 * Used for the Live Agent Reasoning Display feature in the Personal Superintelligence System.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentThoughtEvent {
    
    /**
     * Unique identifier for the task this thought relates to
     */
    private String taskId;
    
    /**
     * Type of thought event (e.g., "tool_selection", "reasoning", "execution", "reflection")
     */
    private String thoughtType;
    
    /**
     * The agent's reasoning or thought process in human-readable form
     */
    private String reasoning;
    
    /**
     * The tool that was selected or is being considered
     */
    private String toolSelected;
    
    /**
     * Confidence level in the decision (0.0 to 1.0)
     */
    private Double confidence;
    
    /**
     * Alternative tools or options that were considered but not selected
     */
    private List<String> alternatives;
    
    /**
     * Current step number in the decision process
     */
    private Integer stepNumber;
    
    /**
     * Total estimated steps for completion
     */
    private Integer totalSteps;
    
    /**
     * Performance metrics associated with this thought
     */
    private PerformanceMetrics performance;
    
    /**
     * Timestamp when this thought occurred
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Additional context or metadata
     */
    private String context;
    
    /**
     * Whether this represents an error in the reasoning process
     */
    @Builder.Default
    private Boolean isError = false;
    
    /**
     * Performance metrics nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PerformanceMetrics {
        
        /**
         * Time taken for this reasoning step (milliseconds)
         */
        private Long thinkingTimeMs;
        
        /**
         * Memory or context size being processed
         */
        private Integer contextSize;
        
        /**
         * Number of options evaluated
         */
        private Integer optionsEvaluated;
        
        /**
         * Cost estimate for this decision (if applicable)
         */
        private Double estimatedCost;
    }
    
    // Convenience factory methods
    
    public static AgentThoughtEvent toolSelection(String taskId, String reasoning, String toolSelected, Double confidence, List<String> alternatives) {
        return AgentThoughtEvent.builder()
                .taskId(taskId)
                .thoughtType("tool_selection")
                .reasoning(reasoning)
                .toolSelected(toolSelected)
                .confidence(confidence)
                .alternatives(alternatives)
                .build();
    }
    
    public static AgentThoughtEvent reasoning(String taskId, String reasoning, String context) {
        return AgentThoughtEvent.builder()
                .taskId(taskId)
                .thoughtType("reasoning")
                .reasoning(reasoning)
                .context(context)
                .build();
    }
    
    public static AgentThoughtEvent execution(String taskId, String toolSelected, String reasoning) {
        return AgentThoughtEvent.builder()
                .taskId(taskId)
                .thoughtType("execution")
                .reasoning(reasoning)
                .toolSelected(toolSelected)
                .build();
    }
    
    public static AgentThoughtEvent reflection(String taskId, String reasoning, Double confidence) {
        return AgentThoughtEvent.builder()
                .taskId(taskId)
                .thoughtType("reflection")
                .reasoning(reasoning)
                .confidence(confidence)
                .build();
    }
    
    public static AgentThoughtEvent error(String taskId, String reasoning, String context) {
        return AgentThoughtEvent.builder()
                .taskId(taskId)
                .thoughtType("error")
                .reasoning(reasoning)
                .context(context)
                .isError(true)
                .build();
    }
}