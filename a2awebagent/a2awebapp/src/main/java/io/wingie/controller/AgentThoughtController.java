package io.wingie.controller;

import io.wingie.dto.AgentThoughtEvent;
import io.wingie.entity.AgentDecisionStep;
import io.wingie.service.AgentThoughtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Live Agent Reasoning Display feature.
 * 
 * Provides endpoints for:
 * - Real-time thought streaming via SSE
 * - Decision history retrieval
 * - Performance metrics and analytics
 * - Agent reasoning transparency
 * 
 * Part of the Personal Superintelligence System (Wingie) for real-time AI agent observation.
 */
@RestController
@RequestMapping("/api/agent-thoughts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow CORS for frontend integration
public class AgentThoughtController {
    
    private final AgentThoughtService agentThoughtService;
    
    /**
     * Stream real-time agent thoughts via Server-Sent Events.
     * 
     * Frontend JavaScript can connect to this endpoint to receive live updates
     * of the agent's reasoning process, tool selections, and decision-making.
     * 
     * @return SSE stream of agent thoughts
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentThoughts() {
        log.info("🧠 New SSE connection established for agent thought streaming");
        return agentThoughtService.createThoughtStream();
    }
    
    /**
     * Get recent agent thoughts for initial page load or history display.
     * 
     * @param limit Maximum number of thoughts to retrieve (default: 20)
     * @return List of recent agent thoughts
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AgentThoughtEvent>> getRecentThoughts(
            @RequestParam(defaultValue = "20") int limit) {
        
        List<AgentThoughtEvent> recentThoughts = agentThoughtService.getRecentThoughts(limit);
        
        log.debug("🧠 Retrieved {} recent agent thoughts", recentThoughts.size());
        return ResponseEntity.ok(recentThoughts);
    }
    
    /**
     * Get decision history for a specific task.
     * 
     * @param taskId The task execution ID
     * @return List of decision steps for the task
     */
    @GetMapping("/task/{taskId}/decisions")
    public ResponseEntity<List<AgentDecisionStep>> getTaskDecisionHistory(
            @PathVariable String taskId) {
        
        List<AgentDecisionStep> decisions = agentThoughtService.getTaskDecisionHistory(taskId);
        
        log.debug("🧠 Retrieved {} decision steps for task {}", decisions.size(), taskId);
        return ResponseEntity.ok(decisions);
    }
    
    /**
     * Get currently active decisions across all tasks.
     * 
     * @return List of active decision steps
     */
    @GetMapping("/active")
    public ResponseEntity<List<AgentDecisionStep>> getActiveDecisions() {
        
        List<AgentDecisionStep> activeDecisions = agentThoughtService.getActiveDecisions();
        
        log.debug("🧠 Retrieved {} active decisions", activeDecisions.size());
        return ResponseEntity.ok(activeDecisions);
    }
    
    /**
     * Get performance statistics and metrics for the agent reasoning system.
     * 
     * @return Performance metrics including success rates, confidence levels, etc.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getDecisionMetrics() {
        
        Map<String, Object> metrics = agentThoughtService.getDecisionStatistics();
        
        log.debug("🧠 Retrieved decision metrics: {} decisions processed", 
                 metrics.get("totalDecisions"));
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Manually trigger a test thought event for debugging/testing purposes.
     * 
     * @param taskId The task ID to associate with the test thought
     * @return Success response
     */
    @PostMapping("/test-thought")
    public ResponseEntity<String> triggerTestThought(
            @RequestParam String taskId) {
        
        // Create a test thought event
        AgentThoughtEvent testThought = AgentThoughtEvent.reasoning(
            taskId, 
            "This is a test thought event for debugging the Live Agent Reasoning Display", 
            "test_context"
        );
        
        agentThoughtService.broadcastThought(testThought);
        
        log.info("🧠 Test thought broadcasted for task {}", taskId);
        return ResponseEntity.ok("Test thought broadcasted successfully");
    }
    
    /**
     * Record a tool selection decision (typically called by the MCP integration).
     * 
     * @param request The tool selection request
     * @return The created decision step
     */
    @PostMapping("/tool-selection")
    public ResponseEntity<AgentDecisionStep> recordToolSelection(
            @RequestBody ToolSelectionRequest request) {
        
        AgentDecisionStep decision = agentThoughtService.recordToolSelection(
            request.getTaskId(),
            request.getReasoning(),
            request.getToolSelected(),
            request.getConfidence(),
            request.getAlternatives()
        );
        
        log.info("🧠 Tool selection recorded: {} -> {}", 
                request.getTaskId(), request.getToolSelected());
        return ResponseEntity.ok(decision);
    }
    
    /**
     * Record a reasoning thought (typically called by the MCP integration).
     * 
     * @param request The reasoning request
     * @return Success response
     */
    @PostMapping("/reasoning")
    public ResponseEntity<String> recordReasoning(
            @RequestBody ReasoningRequest request) {
        
        agentThoughtService.recordReasoning(
            request.getTaskId(),
            request.getReasoning(),
            request.getContext()
        );
        
        log.debug("🧠 Reasoning recorded for task {}", request.getTaskId());
        return ResponseEntity.ok("Reasoning recorded successfully");
    }
    
    /**
     * Record tool execution start (typically called by the MCP integration).
     * 
     * @param request The execution start request
     * @return Success response
     */
    @PostMapping("/execution-start")
    public ResponseEntity<String> recordExecutionStart(
            @RequestBody ExecutionStartRequest request) {
        
        agentThoughtService.recordExecutionStart(
            request.getTaskId(),
            request.getToolSelected(),
            request.getReasoning()
        );
        
        log.debug("🧠 Execution start recorded: {} -> {}", 
                 request.getTaskId(), request.getToolSelected());
        return ResponseEntity.ok("Execution start recorded successfully");
    }
    
    /**
     * Record a reflection thought (typically called by the MCP integration).
     * 
     * @param request The reflection request
     * @return Success response
     */
    @PostMapping("/reflection")
    public ResponseEntity<String> recordReflection(
            @RequestBody ReflectionRequest request) {
        
        agentThoughtService.recordReflection(
            request.getTaskId(),
            request.getReasoning(),
            request.getConfidence()
        );
        
        log.debug("🧠 Reflection recorded for task {}", request.getTaskId());
        return ResponseEntity.ok("Reflection recorded successfully");
    }
    
    /**
     * Record an error in the reasoning process (typically called by the MCP integration).
     * 
     * @param request The error request
     * @return Success response
     */
    @PostMapping("/error")
    public ResponseEntity<String> recordError(
            @RequestBody ErrorRequest request) {
        
        agentThoughtService.recordError(
            request.getTaskId(),
            request.getReasoning(),
            request.getContext()
        );
        
        log.warn("🧠 Error recorded for task {}: {}", 
                request.getTaskId(), request.getReasoning());
        return ResponseEntity.ok("Error recorded successfully");
    }
    
    /**
     * Mark a decision as completed (typically called by the MCP integration).
     * 
     * @param request The completion request
     * @return Success response
     */
    @PostMapping("/decision-completed")
    public ResponseEntity<String> markDecisionCompleted(
            @RequestBody DecisionCompletedRequest request) {
        
        agentThoughtService.markDecisionCompleted(
            request.getTaskId(),
            request.getToolSelected(),
            request.isSuccessful(),
            request.getExecutionTimeMs(),
            request.getStatusMessage()
        );
        
        log.info("🧠 Decision completed: {} -> {} ({})", 
                request.getTaskId(), request.getToolSelected(), 
                request.isSuccessful() ? "success" : "failed");
        return ResponseEntity.ok("Decision completion recorded successfully");
    }
    
    // Request DTOs
    
    public static class ToolSelectionRequest {
        private String taskId;
        private String reasoning;
        private String toolSelected;
        private Double confidence;
        private List<String> alternatives;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public String getToolSelected() { return toolSelected; }
        public void setToolSelected(String toolSelected) { this.toolSelected = toolSelected; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public List<String> getAlternatives() { return alternatives; }
        public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }
    }
    
    public static class ReasoningRequest {
        private String taskId;
        private String reasoning;
        private String context;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
    
    public static class ExecutionStartRequest {
        private String taskId;
        private String toolSelected;
        private String reasoning;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getToolSelected() { return toolSelected; }
        public void setToolSelected(String toolSelected) { this.toolSelected = toolSelected; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }
    
    public static class ReflectionRequest {
        private String taskId;
        private String reasoning;
        private Double confidence;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
    
    public static class ErrorRequest {
        private String taskId;
        private String reasoning;
        private String context;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
    
    public static class DecisionCompletedRequest {
        private String taskId;
        private String toolSelected;
        private boolean successful;
        private Long executionTimeMs;
        private String statusMessage;
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getToolSelected() { return toolSelected; }
        public void setToolSelected(String toolSelected) { this.toolSelected = toolSelected; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public Long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    }
}