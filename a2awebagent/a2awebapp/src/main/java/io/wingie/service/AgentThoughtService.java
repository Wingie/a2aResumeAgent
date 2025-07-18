package io.wingie.service;

import io.wingie.dto.AgentThoughtEvent;
import io.wingie.entity.AgentDecisionStep;
import io.wingie.repository.AgentDecisionStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing real-time agent thought streaming and decision step tracking.
 * 
 * Provides the backend for the Live Agent Reasoning Display feature,
 * enabling real-time visibility into AI agent decision-making processes.
 * 
 * Key capabilities:
 * - Real-time thought streaming via SSE
 * - Decision step persistence and tracking
 * - Performance metrics collection
 * - Reasoning pattern analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentThoughtService {
    
    private final AgentDecisionStepRepository decisionStepRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // SSE management for real-time thought streaming
    private final List<SseEmitter> thoughtStreamEmitters = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> taskStepCounters = new ConcurrentHashMap<>();
    
    // Thought event buffer for recent activity
    private final List<AgentThoughtEvent> recentThoughts = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_THOUGHTS = 100;
    
    /**
     * Register a new SSE emitter for thought streaming
     */
    public SseEmitter createThoughtStream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        thoughtStreamEmitters.add(emitter);
        
        emitter.onCompletion(() -> {
            thoughtStreamEmitters.remove(emitter);
            log.debug("🧠 Thought stream emitter completed");
        });
        
        emitter.onTimeout(() -> {
            thoughtStreamEmitters.remove(emitter);
            log.debug("🧠 Thought stream emitter timed out");
        });
        
        emitter.onError((throwable) -> {
            thoughtStreamEmitters.remove(emitter);
            log.warn("🧠 Thought stream emitter error: {}", throwable.getMessage());
        });
        
        log.info("🧠 New thought stream emitter registered. Active streams: {}", thoughtStreamEmitters.size());
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("Connected to agent thought stream"));
        } catch (Exception e) {
            log.warn("Failed to send initial thought stream message", e);
        }
        
        return emitter;
    }
    
    /**
     * Broadcast a thought event to all connected SSE streams
     */
    public void broadcastThought(AgentThoughtEvent thought) {
        // Add to recent thoughts buffer
        addToRecentThoughts(thought);
        
        // Broadcast to all connected SSE streams
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : thoughtStreamEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("agent-thought")
                        .data(thought));
                        
            } catch (Exception e) {
                log.debug("Failed to send thought to SSE emitter: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        
        // Remove dead emitters
        thoughtStreamEmitters.removeAll(deadEmitters);
        
        log.debug("🧠 Broadcasted thought event to {} active streams: {}", 
                 thoughtStreamEmitters.size(), thought.getThoughtType());
    }
    
    /**
     * Record a tool selection thought and persist as decision step
     */
    public AgentDecisionStep recordToolSelection(String taskId, String reasoning, 
                                               String toolSelected, Double confidence, 
                                               List<String> alternatives) {
        
        // Create and broadcast thought event
        AgentThoughtEvent thought = AgentThoughtEvent.toolSelection(
            taskId, reasoning, toolSelected, confidence, alternatives);
        broadcastThought(thought);
        
        // Create and persist decision step
        Integer stepNumber = getNextStepNumber(taskId);
        AgentDecisionStep decisionStep = AgentDecisionStep.builder()
                .taskExecutionId(taskId)
                .stepNumber(stepNumber)
                .toolSelected(toolSelected)
                .reasoningText(reasoning)
                .confidenceScore(confidence != null ? BigDecimal.valueOf(confidence) : null)
                .createdAt(LocalDateTime.now())
                .status(AgentDecisionStep.DecisionStatus.PENDING)
                .build();
                
        // Set alternatives if provided
        if (alternatives != null && !alternatives.isEmpty()) {
            Map<String, Object> alternativesMap = new ConcurrentHashMap<>();
            for (int i = 0; i < alternatives.size(); i++) {
                alternativesMap.put("option_" + i, alternatives.get(i));
            }
            decisionStep.setAlternativesMap(alternativesMap);
        }
        
        decisionStep = decisionStepRepository.save(decisionStep);
        
        log.info("🧠 Recorded tool selection: {} -> {} (confidence: {}%)", 
                taskId, toolSelected, confidence != null ? confidence * 100 : "unknown");
                
        return decisionStep;
    }
    
    /**
     * Record a reasoning thought (no persistence, just streaming)
     */
    public void recordReasoning(String taskId, String reasoning, String context) {
        AgentThoughtEvent thought = AgentThoughtEvent.reasoning(taskId, reasoning, context);
        broadcastThought(thought);
        
        log.debug("🧠 Recorded reasoning: {} - {}", taskId, reasoning);
    }
    
    /**
     * Record a tool execution start
     */
    public void recordExecutionStart(String taskId, String toolSelected, String reasoning) {
        AgentThoughtEvent thought = AgentThoughtEvent.execution(taskId, toolSelected, reasoning);
        broadcastThought(thought);
        
        // Update corresponding decision step status
        updateDecisionStepStatus(taskId, toolSelected, AgentDecisionStep.DecisionStatus.IN_PROGRESS);
        
        log.debug("🧠 Recorded execution start: {} -> {}", taskId, toolSelected);
    }
    
    /**
     * Record a reflection thought after tool execution
     */
    public void recordReflection(String taskId, String reasoning, Double confidence) {
        AgentThoughtEvent thought = AgentThoughtEvent.reflection(taskId, reasoning, confidence);
        broadcastThought(thought);
        
        log.debug("🧠 Recorded reflection: {} - {}", taskId, reasoning);
    }
    
    /**
     * Record an error in the reasoning process
     */
    public void recordError(String taskId, String reasoning, String context) {
        AgentThoughtEvent thought = AgentThoughtEvent.error(taskId, reasoning, context);
        broadcastThought(thought);
        
        log.warn("🧠 Recorded reasoning error: {} - {}", taskId, reasoning);
    }
    
    /**
     * Mark a decision step as completed
     */
    @Transactional
    public void markDecisionCompleted(String taskId, String toolSelected, boolean successful, 
                                    Long executionTimeMs, String statusMessage) {
        
        AgentDecisionStep latestStep = decisionStepRepository
            .findLatestStepByTaskExecution(taskId)
            .orElse(null);
            
        if (latestStep != null && toolSelected.equals(latestStep.getToolSelected())) {
            latestStep.markCompleted(successful);
            latestStep.setExecutionTimeMs(executionTimeMs);
            latestStep.setStatusMessage(statusMessage);
            decisionStepRepository.save(latestStep);
            
            log.info("🧠 Marked decision completed: {} -> {} ({})", 
                    taskId, toolSelected, successful ? "success" : "failed");
        }
    }
    
    /**
     * Mark a decision step as failed
     */
    @Transactional
    public void markDecisionFailed(String taskId, String toolSelected, String errorMessage) {
        AgentDecisionStep latestStep = decisionStepRepository
            .findLatestStepByTaskExecution(taskId)
            .orElse(null);
            
        if (latestStep != null && toolSelected.equals(latestStep.getToolSelected())) {
            latestStep.markFailed(errorMessage);
            decisionStepRepository.save(latestStep);
            
            // Also broadcast error thought
            recordError(taskId, "Tool execution failed: " + errorMessage, "tool_execution_error");
            
            log.warn("🧠 Marked decision failed: {} -> {} - {}", taskId, toolSelected, errorMessage);
        }
    }
    
    /**
     * Get recent thoughts for activity feed
     */
    public List<AgentThoughtEvent> getRecentThoughts(int limit) {
        return recentThoughts.stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Get decision steps for a specific task
     */
    public List<AgentDecisionStep> getTaskDecisionHistory(String taskId) {
        return decisionStepRepository.findByTaskExecutionIdOrderByStepNumber(taskId);
    }
    
    /**
     * Get currently active decision steps across all tasks
     */
    public List<AgentDecisionStep> getActiveDecisions() {
        return decisionStepRepository.findActiveDecisionSteps();
    }
    
    /**
     * Get decision statistics for performance monitoring
     */
    public Map<String, Object> getDecisionStatistics() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        
        List<AgentDecisionStep> recentDecisions = decisionStepRepository.findRecentDecisions(since);
        long totalDecisions = recentDecisions.size();
        long successfulDecisions = recentDecisions.stream()
                .mapToLong(step -> Boolean.TRUE.equals(step.getSuccessful()) ? 1 : 0)
                .sum();
                
        double averageConfidence = recentDecisions.stream()
                .filter(step -> step.getConfidenceScore() != null)
                .mapToDouble(step -> step.getConfidenceScore().doubleValue())
                .average()
                .orElse(0.0);
                
        return Map.of(
            "totalDecisions", totalDecisions,
            "successfulDecisions", successfulDecisions,
            "successRate", totalDecisions > 0 ? (double) successfulDecisions / totalDecisions : 0.0,
            "averageConfidence", averageConfidence,
            "activeStreams", thoughtStreamEmitters.size(),
            "recentThoughts", recentThoughts.size()
        );
    }
    
    // Private helper methods
    
    private Integer getNextStepNumber(String taskId) {
        return taskStepCounters.computeIfAbsent(taskId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }
    
    @Transactional
    private void updateDecisionStepStatus(String taskId, String toolSelected, 
                                        AgentDecisionStep.DecisionStatus status) {
        AgentDecisionStep latestStep = decisionStepRepository
            .findLatestStepByTaskExecution(taskId)
            .orElse(null);
            
        if (latestStep != null && toolSelected.equals(latestStep.getToolSelected())) {
            if (status == AgentDecisionStep.DecisionStatus.IN_PROGRESS) {
                latestStep.markStarted();
            }
            decisionStepRepository.save(latestStep);
        }
    }
    
    private void addToRecentThoughts(AgentThoughtEvent thought) {
        recentThoughts.add(0, thought); // Add to beginning
        
        // Trim to max size
        while (recentThoughts.size() > MAX_RECENT_THOUGHTS) {
            recentThoughts.remove(recentThoughts.size() - 1);
        }
    }
}