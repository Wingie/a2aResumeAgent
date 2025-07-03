package io.wingie.service;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.repository.TaskExecutionRepository;
import io.wingie.service.neo4j.TaskGraphService;
import io.wingie.service.neo4j.ScreenshotEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Integration service that wraps MCP tool executions with TaskExecution tracking
 * and real-time SSE broadcasting for the Agent Observatory dashboard.
 * 
 * This service bridges the gap between MCP tool calls and the evaluation tracking system,
 * enabling real-time visibility into all tool executions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionIntegrationService {
    
    private final TaskExecutionRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired(required = false) // Optional dependency - graceful degradation if Neo4j unavailable
    private TaskGraphService taskGraphService;
    
    @Autowired(required = false) // Optional dependency
    private ScreenshotEmbeddingService screenshotEmbeddingService;
    
    /**
     * Wraps a tool execution with TaskExecution tracking and real-time SSE broadcasting.
     * 
     * @param toolName The name of the tool being executed
     * @param arguments The tool arguments as a JSON string
     * @param toolExecution The actual tool execution logic
     * @return The result of the tool execution
     */
    public Object executeWithTracking(String toolName, String arguments, Supplier<Object> toolExecution) {
        // Create TaskExecution record
        TaskExecution task = createTaskExecution(toolName, arguments);
        
        try {
            // Set task context for LLM call correlation
            TaskContext.setContext(task.getTaskId(), generateSessionId(), "mcp-user");
            
            // Mark task as started and broadcast initial state
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            task.setProgressPercent(0);
            task.setProgressMessage("Starting " + toolName + " execution...");
            task = taskRepository.save(task);
            
            // Trigger immediate SSE broadcast for task start
            broadcastTaskUpdate(task, "tool-started");
            
            log.info("🚀 Started tool execution: {} ({})", toolName, task.getTaskId());
            
            // Update progress to indicate tool is running
            updateTaskProgress(task, 25, "Tool " + toolName + " is executing...");
            
            // Execute the actual tool
            Object result = toolExecution.get();
            
            // Mark as completed successfully
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgressPercent(100);
            task.setProgressMessage("Completed successfully");
            task.setCompletedAt(LocalDateTime.now());
            task.setExtractedResults(result != null ? result.toString() : "");
            
            // Calculate and store actual duration
            if (task.getStartedAt() != null && task.getCompletedAt() != null) {
                long durationSeconds = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toSeconds();
                task.setActualDurationSeconds((int) durationSeconds);
            }
            
            task = taskRepository.save(task);
            
            // Trigger immediate SSE broadcast for completion
            broadcastTaskUpdate(task, "tool-completed");
            
            // 🔗 Neo4j Knowledge Graph Integration - Async logging
            if (taskGraphService != null) {
                final TaskExecution finalTask = task; // Make effectively final for lambda
                taskGraphService.logTaskToGraph(finalTask)
                    .thenRun(() -> log.debug("🔗 Neo4j graph logging initiated for task {}", finalTask.getTaskId()))
                    .exceptionally(ex -> {
                        log.warn("⚠️ Neo4j graph logging failed for task {}: {}", finalTask.getTaskId(), ex.getMessage());
                        return null;
                    });
            }
            
            long durationMs = task.getStartedAt() != null ? 
                java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis() : 0;
            log.info("✅ Completed tool execution: {} in {}ms", toolName, durationMs);
            
            return result;
            
        } catch (Exception e) {
            // Mark as failed and broadcast error
            task.setStatus(TaskStatus.FAILED);
            task.setProgressPercent(100);
            task.setProgressMessage("Failed: " + e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorDetails(e.getMessage());
            
            // Calculate and store actual duration even for failed tasks
            if (task.getStartedAt() != null && task.getCompletedAt() != null) {
                long durationSeconds = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toSeconds();
                task.setActualDurationSeconds((int) durationSeconds);
            }
            
            task = taskRepository.save(task);
            
            // Trigger immediate SSE broadcast for failure
            broadcastTaskUpdate(task, "tool-failed");
            
            log.error("❌ Tool execution failed: {} ({})", toolName, task.getTaskId(), e);
            
            throw e; // Re-throw to maintain original behavior
        } finally {
            // Clear task context
            TaskContext.clear();
        }
    }
    
    /**
     * Updates task progress and triggers real-time SSE broadcast.
     */
    public void updateTaskProgress(TaskExecution task, int progressPercent, String progressMessage) {
        task.setProgressPercent(progressPercent);
        task.setProgressMessage(progressMessage);
        task.setUpdated(LocalDateTime.now());
        task = taskRepository.save(task);
        
        // Trigger immediate SSE broadcast for progress update
        broadcastTaskUpdate(task, "tool-progress");
        
        log.debug("📊 Progress update: {} - {}% - {}", task.getTaskId(), progressPercent, progressMessage);
    }
    
    /**
     * Adds a screenshot to the task and triggers real-time SSE broadcast.
     * Also initiates async screenshot embedding processing for knowledge graph.
     */
    public void addTaskScreenshot(String taskId, String screenshotPath) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.getScreenshots().add(screenshotPath);
            task.setUpdated(LocalDateTime.now());
            task = taskRepository.save(task);
            
            // Trigger immediate SSE broadcast for new screenshot
            broadcastTaskUpdate(task, "screenshot-captured");
            
            // 📸 Screenshot Embedding Processing - Async
            if (screenshotEmbeddingService != null) {
                String screenshotId = taskId + "_" + System.currentTimeMillis();
                screenshotEmbeddingService.processScreenshotEmbedding(screenshotId, screenshotPath)
                    .thenAccept(result -> log.debug("📊 Screenshot embedding processing completed: {}", result))
                    .exceptionally(ex -> {
                        log.warn("⚠️ Screenshot embedding failed for {}: {}", screenshotPath, ex.getMessage());
                        return null;
                    });
            }
            
            log.info("📸 Screenshot added to task {}: {}", taskId, screenshotPath);
        });
    }
    
    /**
     * Creates a new TaskExecution record for MCP tool call tracking.
     */
    private TaskExecution createTaskExecution(String toolName, String arguments) {
        TaskExecution task = new TaskExecution();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTaskType("mcp_tool_execution");
        task.setOriginalQuery("MCP Tool Call: " + toolName);
        task.setStatus(TaskStatus.QUEUED);
        task.setProgressPercent(0);
        task.setProgressMessage("Queued for execution");
        task.setCreated(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        task.setRequesterId("mcp-integration");
        
        // Store tool arguments as metadata
        if (arguments != null && !arguments.isEmpty()) {
            task.setOriginalQuery("MCP Tool Call: " + toolName + " with args: " + 
                (arguments.length() > 200 ? arguments.substring(0, 200) + "..." : arguments));
        }
        
        return taskRepository.save(task);
    }
    
    /**
     * Generates a session ID for grouping related tool calls.
     */
    private String generateSessionId() {
        return "mcp-session-" + System.currentTimeMillis();
    }
    
    /**
     * Triggers immediate SSE broadcast for task updates using Spring Events.
     */
    private void broadcastTaskUpdate(TaskExecution task, String eventType) {
        try {
            // Publish event instead of direct controller call to avoid circular dependency
            TaskUpdateEvent event = new TaskUpdateEvent(task, eventType);
            eventPublisher.publishEvent(event);
            log.debug("📡 Published task update event: {} for task {}", eventType, task.getTaskId());
        } catch (Exception e) {
            log.warn("Failed to publish task update event: {}", e.getMessage());
            // Don't fail the main execution if event publishing fails
        }
    }
    
    /**
     * Event class for task updates to avoid circular dependencies
     */
    public static class TaskUpdateEvent {
        private final TaskExecution task;
        private final String eventType;
        
        public TaskUpdateEvent(TaskExecution task, String eventType) {
            this.task = task;
            this.eventType = eventType;
        }
        
        public TaskExecution getTask() { return task; }
        public String getEventType() { return eventType; }
    }
}