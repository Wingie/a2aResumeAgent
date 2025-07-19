package io.wingie.controller;

import io.wingie.service.TaskProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for real-time task progress streaming via Server-Sent Events
 * Bridges Redis pub/sub messages from TaskProgressService to HTTP SSE endpoints
 */
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaskProgressController {

    private final TaskProgressService taskProgressService;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired(required = false)
    private RedisMessageListenerContainer redisMessageListenerContainer;
    
    // SSE emitters for all task progress updates
    private final List<SseEmitter> allTaskEmitters = new CopyOnWriteArrayList<>();
    
    // SSE emitters per task ID
    private final Map<String, List<SseEmitter>> taskSpecificEmitters = new ConcurrentHashMap<>();
    
    // Heartbeat scheduler for keeping connections alive
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    
    @PostConstruct
    public void initialize() {
        if (redisTemplate != null && redisMessageListenerContainer != null) {
            setupRedisListener();
            startHeartbeat();
            log.info("Task progress SSE controller initialized with Redis support");
        } else {
            log.warn("Redis not available - task progress SSE will use database polling fallback");
        }
    }
    
    @PreDestroy
    public void cleanup() {
        heartbeatScheduler.shutdown();
        allTaskEmitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing SSE emitter during cleanup", e);
            }
        });
        taskSpecificEmitters.values().forEach(emitters -> 
            emitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("Error completing task-specific SSE emitter during cleanup", e);
                }
            })
        );
    }
    
    /**
     * Server-Sent Events endpoint for all task progress updates
     * GET /v1/tasks/progress-stream
     */
    @GetMapping(value = "/progress-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAllTaskProgress() {
        log.info("New SSE connection established for all task progress");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        allTaskEmitters.add(emitter);
        
        setupEmitterCallbacks(emitter, () -> allTaskEmitters.remove(emitter));
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data(Map.of(
                    "status", "connected",
                    "timestamp", System.currentTimeMillis(),
                    "message", "Listening for all task progress updates"
                )));
        } catch (IOException e) {
            log.error("Error sending initial connection event", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Server-Sent Events endpoint for specific task progress updates
     * GET /v1/tasks/{taskId}/progress-stream
     */
    @GetMapping(value = "/{taskId}/progress-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskProgress(@PathVariable String taskId) {
        log.info("New SSE connection established for task progress: {}", taskId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // Add to task-specific emitters
        taskSpecificEmitters.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        setupEmitterCallbacks(emitter, () -> {
            List<SseEmitter> emitters = taskSpecificEmitters.get(taskId);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    taskSpecificEmitters.remove(taskId);
                }
            }
        });
        
        // Send initial connection confirmation and current task status
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data(Map.of(
                    "status", "connected",
                    "taskId", taskId,
                    "timestamp", System.currentTimeMillis(),
                    "message", "Listening for task progress updates"
                )));
                
            // Try to send current progress if available
            sendCurrentTaskProgress(emitter, taskId);
            
        } catch (IOException e) {
            log.error("Error sending initial task progress event for {}", taskId, e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    private void setupEmitterCallbacks(SseEmitter emitter, Runnable onRemove) {
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed");
            onRemove.run();
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out");
            onRemove.run();
        });
        
        emitter.onError((ex) -> {
            log.warn("SSE connection error: {}", ex.getMessage());
            onRemove.run();
        });
    }
    
    private void setupRedisListener() {
        if (redisMessageListenerContainer == null) return;
        
        Topic topic = new ChannelTopic("task:progress");
        
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    // Parse the Redis message
                    String messageBody = new String(message.getBody());
                    log.debug("Received Redis task progress message: {}", messageBody);
                    
                    // Convert Redis message to progress data
                    @SuppressWarnings("unchecked")
                    Map<String, Object> progressData = (Map<String, Object>) redisTemplate
                        .getValueSerializer().deserialize(message.getBody());
                    
                    if (progressData != null) {
                        broadcastProgressUpdate(progressData);
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing Redis task progress message", e);
                }
            }
        };
        
        redisMessageListenerContainer.addMessageListener(listener, topic);
        log.info("Redis message listener configured for task progress updates");
    }
    
    private void broadcastProgressUpdate(Map<String, Object> progressData) {
        String taskId = (String) progressData.get("taskId");
        
        // Create SSE event
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
            .name("progress")
            .data(progressData);
        
        // Send to all task emitters
        sendToEmitters(allTaskEmitters, eventBuilder, "all tasks");
        
        // Send to task-specific emitters
        if (taskId != null) {
            List<SseEmitter> taskEmitters = taskSpecificEmitters.get(taskId);
            if (taskEmitters != null && !taskEmitters.isEmpty()) {
                sendToEmitters(taskEmitters, eventBuilder, "task " + taskId);
            }
        }
    }
    
    private void sendToEmitters(List<SseEmitter> emitters, SseEmitter.SseEventBuilder eventBuilder, String description) {
        if (emitters.isEmpty()) return;
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(eventBuilder);
            } catch (IOException e) {
                log.debug("SSE emitter failed for {}, removing: {}", description, e.getMessage());
                deadEmitters.add(emitter);
            } catch (Exception e) {
                log.warn("Unexpected error sending SSE event for {}: {}", description, e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        
        // Remove dead emitters
        emitters.removeAll(deadEmitters);
        
        if (!deadEmitters.isEmpty()) {
            log.debug("Removed {} dead SSE emitters for {}", deadEmitters.size(), description);
        }
    }
    
    private void sendCurrentTaskProgress(SseEmitter emitter, String taskId) {
        try {
            // Try to get current progress from Redis first
            if (redisTemplate != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentProgress = (Map<String, Object>) redisTemplate
                    .opsForValue().get("task:progress:" + taskId);
                
                if (currentProgress != null) {
                    emitter.send(SseEmitter.event()
                        .name("current-progress")
                        .data(currentProgress));
                    return;
                }
            }
            
            // Fallback: get from database via TaskProgressService
            // This would require extending TaskProgressService with a getCurrentProgress method
            log.debug("No current progress found in Redis for task {}", taskId);
            
        } catch (Exception e) {
            log.debug("Error sending current task progress for {}: {}", taskId, e.getMessage());
        }
    }
    
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> heartbeat = Map.of(
                    "type", "heartbeat",
                    "timestamp", System.currentTimeMillis(),
                    "activeConnections", allTaskEmitters.size() + taskSpecificEmitters.values()
                        .stream().mapToInt(List::size).sum()
                );
                
                SseEmitter.SseEventBuilder heartbeatEvent = SseEmitter.event()
                    .name("heartbeat")
                    .data(heartbeat);
                
                // Send heartbeat to all connections
                sendToEmitters(allTaskEmitters, heartbeatEvent, "heartbeat (all)");
                taskSpecificEmitters.forEach((taskId, emitters) -> 
                    sendToEmitters(emitters, heartbeatEvent, "heartbeat (" + taskId + ")")
                );
                
            } catch (Exception e) {
                log.warn("Error sending heartbeat", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}