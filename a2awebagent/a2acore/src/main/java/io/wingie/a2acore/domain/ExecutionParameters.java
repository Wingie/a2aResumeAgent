package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * User-controlled execution parameters for web automation tasks.
 * 
 * Provides fine-grained control over task execution behavior including
 * step limits, execution modes, and early completion settings.
 */
@Data
@Builder
@Jacksonized
public class ExecutionParameters {
    
    /**
     * Maximum number of steps/loops allowed in execution.
     * Default: 10 for reasonable automation scope.
     */
    @NotNull
    @Min(1)
    @Builder.Default
    @JsonProperty("maxSteps")
    private Integer maxSteps = 10;
    
    /**
     * Execution mode determining automation behavior.
     */
    @NotNull
    @Builder.Default
    @JsonProperty("executionMode")
    private ExecutionMode executionMode = ExecutionMode.MULTI_STEP;
    
    /**
     * Whether task can complete early if objectives are met.
     * Default: true for efficiency.
     */
    @Builder.Default
    @JsonProperty("allowEarlyCompletion")
    private Boolean allowEarlyCompletion = true;
    
    /**
     * Timeout per individual step in seconds.
     * Default: 30 seconds per step.
     */
    @Min(5)
    @Builder.Default
    @JsonProperty("stepTimeoutSeconds")
    private Integer stepTimeoutSeconds = 30;
    
    /**
     * Whether to capture screenshots after each step.
     * Default: true for debugging and verification.
     */
    @Builder.Default
    @JsonProperty("captureStepScreenshots")
    private Boolean captureStepScreenshots = true;
    
    /**
     * Minimum confidence score (0.0-1.0) required for early completion.
     * Only applies when allowEarlyCompletion is true.
     */
    @Builder.Default
    @JsonProperty("earlyCompletionThreshold")
    private Double earlyCompletionThreshold = 0.8;
    
    public enum ExecutionMode {
        /**
         * Execute single action and return immediately.
         * Suitable for: "One-shot LinkedIn profile check"
         */
        ONE_SHOT,
        
        /**
         * Execute up to maxSteps with user-defined stopping point.
         * Suitable for: "LinkedIn search with 5 steps max"
         */
        MULTI_STEP,
        
        /**
         * Intelligent automation that continues until objective completion.
         * Respects maxSteps as safety limit.
         * Suitable for: "Travel search on booking.com with 10 steps max"
         */
        AUTO
    }
    
    /**
     * Factory method for one-shot execution.
     */
    public static ExecutionParameters oneShot() {
        return ExecutionParameters.builder()
            .maxSteps(1)
            .executionMode(ExecutionMode.ONE_SHOT)
            .allowEarlyCompletion(false)
            .build();
    }
    
    /**
     * Factory method for multi-step execution with specific limits.
     */
    public static ExecutionParameters multiStep(int maxSteps) {
        return ExecutionParameters.builder()
            .maxSteps(maxSteps)
            .executionMode(ExecutionMode.MULTI_STEP)
            .allowEarlyCompletion(true)
            .build();
    }
    
    /**
     * Factory method for intelligent auto execution.
     */
    public static ExecutionParameters auto(int maxSteps) {
        return ExecutionParameters.builder()
            .maxSteps(maxSteps)
            .executionMode(ExecutionMode.AUTO)
            .allowEarlyCompletion(true)
            .earlyCompletionThreshold(0.8)
            .build();
    }
    
    /**
     * Validates parameter consistency and logical constraints.
     */
    public void validate() {
        if (maxSteps == null || maxSteps < 1) {
            throw new IllegalArgumentException("maxSteps must be at least 1");
        }
        
        if (executionMode == ExecutionMode.ONE_SHOT && maxSteps > 1) {
            throw new IllegalArgumentException("ONE_SHOT mode requires maxSteps = 1");
        }
        
        if (earlyCompletionThreshold != null && 
            (earlyCompletionThreshold < 0.0 || earlyCompletionThreshold > 1.0)) {
            throw new IllegalArgumentException("earlyCompletionThreshold must be between 0.0 and 1.0");
        }
        
        if (stepTimeoutSeconds != null && stepTimeoutSeconds < 5) {
            throw new IllegalArgumentException("stepTimeoutSeconds must be at least 5");
        }
    }
    
    /**
     * Calculates total execution timeout based on step parameters.
     */
    public int getTotalTimeoutSeconds() {
        return maxSteps * stepTimeoutSeconds + 30; // +30s buffer for setup/cleanup
    }
    
    /**
     * Determines if execution should stop early based on current step and results.
     */
    public boolean shouldStopEarly(int currentStep, double confidenceScore) {
        if (!allowEarlyCompletion) {
            return false;
        }
        
        // Always allow completion on last step
        if (currentStep >= maxSteps) {
            return true;
        }
        
        // Check confidence threshold for early completion
        if (executionMode == ExecutionMode.AUTO && 
            earlyCompletionThreshold != null && 
            confidenceScore >= earlyCompletionThreshold) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("ExecutionParameters{mode=%s, maxSteps=%d, earlyCompletion=%s, stepTimeout=%ds}", 
                           executionMode, maxSteps, allowEarlyCompletion, stepTimeoutSeconds);
    }
}