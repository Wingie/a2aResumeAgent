package io.wingie.service;

import io.wingie.a2acore.domain.ExecutionParameters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Common infrastructure for step control and progress tracking across
 * MCP tool flow and evaluation system.
 * 
 * Provides unified handling of:
 * - Step counting and limits
 * - Early completion detection
 * - Progress tracking with step awareness
 * - Execution context management
 */
@Service
@Slf4j
public class StepControlService {
    
    private final ConcurrentMap<String, StepContext> activeContexts = new ConcurrentHashMap<>();
    
    /**
     * Initialize step control context for a task.
     */
    public String initializeStepControl(String taskId, ExecutionParameters params) {
        StepContext context = new StepContext(taskId, params);
        activeContexts.put(taskId, context);
        
        log.info("Initialized step control for task {}: {}", taskId, params);
        return taskId;
    }
    
    /**
     * Advance to next step and check if execution should continue.
     */
    public StepResult advanceStep(String taskId, String stepDescription, double confidenceScore) {
        StepContext context = activeContexts.get(taskId);
        if (context == null) {
            log.warn("No step context found for task {}", taskId);
            return StepResult.error("No step context found");
        }
        
        context.incrementStep();
        context.addStep(stepDescription, confidenceScore);
        
        log.debug("Step {}/{} for task {}: {} (confidence: {:.2f})", 
                 context.getCurrentStep(), context.getMaxSteps(), taskId, stepDescription, confidenceScore);
        
        // Check if we should continue
        boolean shouldContinue = shouldContinueExecution(context, confidenceScore);
        boolean reachedLimit = context.getCurrentStep() >= context.getMaxSteps();
        boolean earlyCompletion = !shouldContinue && !reachedLimit;
        
        StepResult result = new StepResult(
            context.getCurrentStep(),
            context.getMaxSteps(),
            shouldContinue,
            reachedLimit,
            earlyCompletion,
            confidenceScore,
            stepDescription
        );
        
        if (!shouldContinue) {
            if (earlyCompletion) {
                log.info("Early completion triggered for task {} at step {}/{} (confidence: {:.2f})", 
                        taskId, context.getCurrentStep(), context.getMaxSteps(), confidenceScore);
            } else {
                log.info("Reached step limit for task {} at step {}/{}", 
                        taskId, context.getCurrentStep(), context.getMaxSteps());
            }
        }
        
        return result;
    }
    
    /**
     * Get current step information for a task.
     */
    public StepStatus getStepStatus(String taskId) {
        StepContext context = activeContexts.get(taskId);
        if (context == null) {
            return null;
        }
        
        return new StepStatus(
            context.getCurrentStep(),
            context.getMaxSteps(),
            context.getExecutionMode(),
            context.getStepHistory(),
            context.isEarlyCompletionAllowed()
        );
    }
    
    /**
     * Complete step control for a task and return execution summary.
     */
    public ExecutionSummary completeStepControl(String taskId) {
        StepContext context = activeContexts.remove(taskId);
        if (context == null) {
            log.warn("No step context found for task {} during completion", taskId);
            return new ExecutionSummary(0, 0, false, new ArrayList<>());
        }
        
        boolean earlyCompletion = context.getCurrentStep() < context.getMaxSteps() && 
                                 !context.getStepHistory().isEmpty();
        
        ExecutionSummary summary = new ExecutionSummary(
            context.getCurrentStep(),
            context.getMaxSteps(),
            earlyCompletion,
            context.getStepHistory()
        );
        
        log.info("Completed step control for task {}: {}", taskId, summary);
        return summary;
    }
    
    /**
     * Check if execution should continue based on current context and confidence.
     */
    private boolean shouldContinueExecution(StepContext context, double confidenceScore) {
        // Always stop if we've reached the maximum steps
        if (context.getCurrentStep() >= context.getMaxSteps()) {
            return false;
        }
        
        // For ONE_SHOT mode, stop after first step
        if (context.getExecutionMode() == ExecutionParameters.ExecutionMode.ONE_SHOT) {
            return context.getCurrentStep() < 1;
        }
        
        // For MULTI_STEP mode, continue until max steps unless early completion is triggered
        if (context.getExecutionMode() == ExecutionParameters.ExecutionMode.MULTI_STEP) {
            return !shouldTriggerEarlyCompletion(context, confidenceScore);
        }
        
        // For AUTO mode, use intelligent stopping based on confidence
        if (context.getExecutionMode() == ExecutionParameters.ExecutionMode.AUTO) {
            return !shouldTriggerEarlyCompletion(context, confidenceScore);
        }
        
        return true;
    }
    
    /**
     * Determine if early completion should be triggered.
     */
    private boolean shouldTriggerEarlyCompletion(StepContext context, double confidenceScore) {
        if (!context.isEarlyCompletionAllowed()) {
            return false;
        }
        
        // Check confidence threshold
        if (confidenceScore >= context.getEarlyCompletionThreshold()) {
            return true;
        }
        
        // For AUTO mode, also check if we've achieved consistent high confidence
        if (context.getExecutionMode() == ExecutionParameters.ExecutionMode.AUTO) {
            return hasConsistentHighConfidence(context);
        }
        
        return false;
    }
    
    /**
     * Check if recent steps have shown consistent high confidence.
     */
    private boolean hasConsistentHighConfidence(StepContext context) {
        List<StepInfo> history = context.getStepHistory();
        if (history.size() < 2) {
            return false;
        }
        
        // Check last 2-3 steps for consistent confidence above threshold
        int checkCount = Math.min(3, history.size());
        double threshold = context.getEarlyCompletionThreshold();
        
        for (int i = history.size() - checkCount; i < history.size(); i++) {
            if (history.get(i).getConfidenceScore() < threshold) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get execution statistics for all active contexts.
     */
    public ExecutionStatistics getExecutionStatistics() {
        int totalActive = activeContexts.size();
        int oneShotTasks = 0;
        int multiStepTasks = 0;
        int autoTasks = 0;
        
        for (StepContext context : activeContexts.values()) {
            switch (context.getExecutionMode()) {
                case ONE_SHOT -> oneShotTasks++;
                case MULTI_STEP -> multiStepTasks++;
                case AUTO -> autoTasks++;
            }
        }
        
        return new ExecutionStatistics(totalActive, oneShotTasks, multiStepTasks, autoTasks);
    }
    
    // Data classes for step control
    
    @Data
    public static class StepContext {
        private final String taskId;
        private final ExecutionParameters parameters;
        private int currentStep = 0;
        private final List<StepInfo> stepHistory = new ArrayList<>();
        private final LocalDateTime startTime = LocalDateTime.now();
        
        public StepContext(String taskId, ExecutionParameters parameters) {
            this.taskId = taskId;
            this.parameters = parameters;
        }
        
        public void incrementStep() {
            currentStep++;
        }
        
        public void addStep(String description, double confidenceScore) {
            stepHistory.add(new StepInfo(currentStep, description, confidenceScore, LocalDateTime.now()));
        }
        
        public int getMaxSteps() {
            return parameters.getMaxSteps();
        }
        
        public ExecutionParameters.ExecutionMode getExecutionMode() {
            return parameters.getExecutionMode();
        }
        
        public boolean isEarlyCompletionAllowed() {
            return parameters.getAllowEarlyCompletion();
        }
        
        public double getEarlyCompletionThreshold() {
            return parameters.getEarlyCompletionThreshold();
        }
    }
    
    @Data
    public static class StepInfo {
        private final int stepNumber;
        private final String description;
        private final double confidenceScore;
        private final LocalDateTime timestamp;
    }
    
    @Data
    public static class StepResult {
        private final int currentStep;
        private final int maxSteps;
        private final boolean shouldContinue;
        private final boolean reachedLimit;
        private final boolean earlyCompletion;
        private final double confidenceScore;
        private final String stepDescription;
        private final String errorMessage;
        
        public StepResult(int currentStep, int maxSteps, boolean shouldContinue, 
                         boolean reachedLimit, boolean earlyCompletion, 
                         double confidenceScore, String stepDescription) {
            this.currentStep = currentStep;
            this.maxSteps = maxSteps;
            this.shouldContinue = shouldContinue;
            this.reachedLimit = reachedLimit;
            this.earlyCompletion = earlyCompletion;
            this.confidenceScore = confidenceScore;
            this.stepDescription = stepDescription;
            this.errorMessage = null;
        }
        
        public static StepResult error(String errorMessage) {
            return new StepResult(0, 0, false, false, false, 0.0, "", errorMessage);
        }
        
        private StepResult(int currentStep, int maxSteps, boolean shouldContinue,
                          boolean reachedLimit, boolean earlyCompletion,
                          double confidenceScore, String stepDescription, String errorMessage) {
            this.currentStep = currentStep;
            this.maxSteps = maxSteps;
            this.shouldContinue = shouldContinue;
            this.reachedLimit = reachedLimit;
            this.earlyCompletion = earlyCompletion;
            this.confidenceScore = confidenceScore;
            this.stepDescription = stepDescription;
            this.errorMessage = errorMessage;
        }
        
        public boolean isError() {
            return errorMessage != null;
        }
    }
    
    @Data
    public static class StepStatus {
        private final int currentStep;
        private final int maxSteps;
        private final ExecutionParameters.ExecutionMode executionMode;
        private final List<StepInfo> stepHistory;
        private final boolean earlyCompletionAllowed;
        
        public double getProgress() {
            return maxSteps > 0 ? (double) currentStep / maxSteps : 0.0;
        }
        
        public String getProgressFormatted() {
            return String.format("%d/%d (%.1f%%)", currentStep, maxSteps, getProgress() * 100);
        }
    }
    
    @Data
    public static class ExecutionSummary {
        private final int stepsCompleted;
        private final int maxSteps;
        private final boolean earlyCompletion;
        private final List<StepInfo> stepHistory;
        
        public double getEfficiency() {
            return maxSteps > 0 ? (double) stepsCompleted / maxSteps : 0.0;
        }
        
        public String getEfficiencyFormatted() {
            return String.format("%.1f%% (%d/%d steps)", getEfficiency() * 100, stepsCompleted, maxSteps);
        }
        
        @Override
        public String toString() {
            return String.format("ExecutionSummary{steps=%d/%d, efficiency=%.1f%%, earlyCompletion=%s}", 
                               stepsCompleted, maxSteps, getEfficiency() * 100, earlyCompletion);
        }
    }
    
    @Data
    public static class ExecutionStatistics {
        private final int totalActiveTasks;
        private final int oneShotTasks;
        private final int multiStepTasks;
        private final int autoTasks;
        
        public String getSummary() {
            return String.format("Active: %d (OneShot: %d, MultiStep: %d, Auto: %d)", 
                               totalActiveTasks, oneShotTasks, multiStepTasks, autoTasks);
        }
    }
}