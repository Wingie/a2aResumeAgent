package io.wingie.controller;

import io.wingie.dto.AdminStatisticsDTO;
import io.wingie.dto.AdminStatisticsDTO.*;
import io.wingie.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST Controller for admin dashboard statistics
 * Provides comprehensive system metrics, real-time updates via SSE, and diagnostic information
 * Perfect for debugging model issues like Gemma failures
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow CORS for admin dashboard
public class AdminStatisticsController {
    
    private final AdminStatisticsService statisticsService;
    
    // SSE emitters for real-time updates
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Cache for statistics to prevent connection pool exhaustion
    private final AtomicReference<AdminStatisticsDTO> cachedStatistics = new AtomicReference<>();
    private final AtomicReference<ActivityFeed> cachedActivityFeed = new AtomicReference<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Cache timestamps
    private volatile LocalDateTime lastStatisticsCacheTime = LocalDateTime.now().minusMinutes(5);
    private volatile LocalDateTime lastActivityCacheTime = LocalDateTime.now().minusMinutes(5);
    
    // Cache durations (in seconds)
    private static final long STATISTICS_CACHE_DURATION = 45; // 45 seconds for main statistics
    private static final long ACTIVITY_CACHE_DURATION = 15;   // 15 seconds for activity feed
    
    // Initialize real-time updates with longer intervals to reduce DB load
    {
        // Send statistics updates every 60 seconds (reduced from 30)
        scheduler.scheduleAtFixedRate(this::broadcastStatistics, 15, 60, TimeUnit.SECONDS);
        // Send activity feed updates every 20 seconds (reduced from 10)
        scheduler.scheduleAtFixedRate(this::broadcastActivityFeed, 10, 20, TimeUnit.SECONDS);
    }
    
    /**
     * Get complete admin dashboard statistics with caching
     */
    @GetMapping("/complete")
    public ResponseEntity<AdminStatisticsDTO> getCompleteStatistics() {
        log.debug("Admin dashboard requesting complete statistics");
        
        try {
            AdminStatisticsDTO statistics = getCachedStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error retrieving complete statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get system health metrics only
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthMetrics> getSystemHealth() {
        log.debug("Requesting system health metrics");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            return ResponseEntity.ok(stats.getSystemHealth());
        } catch (Exception e) {
            log.error("Error retrieving system health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get tool performance metrics only
     */
    @GetMapping("/tools")
    public ResponseEntity<ToolMetrics> getToolMetrics() {
        log.debug("Requesting tool performance metrics");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            return ResponseEntity.ok(stats.getToolMetrics());
        } catch (Exception e) {
            log.error("Error retrieving tool metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get model performance comparison - excellent for debugging Gemma issues
     */
    @GetMapping("/models")
    public ResponseEntity<ModelPerformanceMetrics> getModelPerformance() {
        log.debug("Requesting model performance metrics");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            return ResponseEntity.ok(stats.getModelPerformance());
        } catch (Exception e) {
            log.error("Error retrieving model performance", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent activity feed - perfect for debugging recent failures
     */
    @GetMapping("/activity")
    public ResponseEntity<ActivityFeed> getActivityFeed() {
        log.debug("Requesting activity feed");
        
        try {
            ActivityFeed activityFeed = getCachedActivityFeed();
            return ResponseEntity.ok(activityFeed);
        } catch (Exception e) {
            log.error("Error retrieving activity feed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get cache efficiency metrics
     */
    @GetMapping("/cache")
    public ResponseEntity<CacheMetrics> getCacheMetrics() {
        log.debug("Requesting cache metrics");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            return ResponseEntity.ok(stats.getCacheMetrics());
        } catch (Exception e) {
            log.error("Error retrieving cache metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get real-time statistics snapshot
     */
    @GetMapping("/realtime")
    public ResponseEntity<RealTimeStats> getRealTimeStats() {
        log.debug("Requesting real-time statistics");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            return ResponseEntity.ok(stats.getRealTimeStats());
        } catch (Exception e) {
            log.error("Error retrieving real-time stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get error patterns for debugging - especially useful for model failures
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrorAnalysis() {
        log.debug("Requesting error analysis");
        
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            ModelPerformanceMetrics modelMetrics = stats.getModelPerformance();
            ToolMetrics toolMetrics = stats.getToolMetrics();
            
            Map<String, Object> errorAnalysis = Map.of(
                "modelErrorPatterns", modelMetrics.getErrorPatterns(),
                "toolErrorsByType", toolMetrics.getErrorsByType(),
                "mostFailedTools", toolMetrics.getMostFailedTools(),
                "recentErrors", stats.getRealTimeStats().getRecentErrors(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(errorAnalysis);
        } catch (Exception e) {
            log.error("Error retrieving error analysis", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Server-Sent Events endpoint for real-time statistics updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStatistics() {
        log.info("New SSE connection established for admin statistics");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed");
            sseEmitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out");
            sseEmitters.remove(emitter);
        });
        
        emitter.onError((ex) -> {
            log.warn("SSE connection error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
        
        // Send initial data immediately
        try {
            AdminStatisticsDTO initialStats = getCachedStatistics();
            emitter.send(SseEmitter.event()
                .name("statistics")
                .data(initialStats));
        } catch (Exception e) {
            log.error("Error sending initial statistics", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Server-Sent Events endpoint for activity feed only (faster updates)
     */
    @GetMapping(value = "/stream/activity", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamActivityFeed() {
        log.info("New SSE connection established for activity feed");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        
        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((ex) -> {
            log.warn("Activity feed SSE error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
        
        // Send initial activity feed
        try {
            ActivityFeed activityFeed = getCachedActivityFeed();
            emitter.send(SseEmitter.event()
                .name("activity")
                .data(activityFeed));
        } catch (Exception e) {
            log.error("Error sending initial activity feed", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * NEW: Server-Sent Events endpoint for live agent reasoning thoughts
     */
    @GetMapping(value = "/stream/thoughts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentThoughts() {
        log.info("🧠 New SSE connection established for agent thought streaming");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        
        emitter.onCompletion(() -> {
            log.debug("Agent thoughts SSE connection completed");
            sseEmitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("Agent thoughts SSE connection timed out");
            sseEmitters.remove(emitter);
        });
        emitter.onError((ex) -> {
            log.warn("Agent thoughts SSE error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
        
        // Send initial thought stream welcome message
        try {
            Map<String, Object> welcomeMessage = Map.of(
                "type", "welcome",
                "message", "Connected to agent thought stream",
                "timestamp", LocalDateTime.now(),
                "capabilities", List.of("reasoning", "tool-selection", "confidence-scoring", "alternatives")
            );
            emitter.send(SseEmitter.event()
                .name("agent-thought")
                .data(welcomeMessage));
        } catch (Exception e) {
            log.error("Error sending initial agent thought message", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * NEW: Server-Sent Events endpoint for real-time performance metrics
     */
    @GetMapping(value = "/stream/performance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPerformanceMetrics() {
        log.info("📊 New SSE connection established for performance metrics streaming");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        
        emitter.onCompletion(() -> {
            log.debug("Performance metrics SSE connection completed");
            sseEmitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("Performance metrics SSE connection timed out");
            sseEmitters.remove(emitter);
        });
        emitter.onError((ex) -> {
            log.warn("Performance metrics SSE error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
        
        // Send initial performance metrics
        try {
            AdminStatisticsDTO stats = getCachedStatistics();
            Map<String, Object> performanceData = Map.of(
                "systemHealth", stats.getSystemHealth(),
                "modelPerformance", stats.getModelPerformance(),
                "cacheMetrics", stats.getCacheMetrics(),
                "realTimeStats", stats.getRealTimeStats(),
                "timestamp", LocalDateTime.now()
            );
            emitter.send(SseEmitter.event()
                .name("performance-metrics")
                .data(performanceData));
        } catch (Exception e) {
            log.error("Error sending initial performance metrics", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * NEW: Server-Sent Events endpoint for debug traces
     */
    @GetMapping(value = "/stream/debug", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDebugTraces() {
        log.info("🐛 New SSE connection established for debug trace streaming");
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.add(emitter);
        
        emitter.onCompletion(() -> {
            log.debug("Debug traces SSE connection completed");
            sseEmitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("Debug traces SSE connection timed out");
            sseEmitters.remove(emitter);
        });
        emitter.onError((ex) -> {
            log.warn("Debug traces SSE error: {}", ex.getMessage());
            sseEmitters.remove(emitter);
        });
        
        // Send initial debug welcome message
        try {
            Map<String, Object> debugWelcome = Map.of(
                "type", "debug-welcome",
                "message", "Connected to debug trace stream",
                "timestamp", LocalDateTime.now(),
                "features", List.of("llm-calls", "execution-flow", "error-analysis", "performance-bottlenecks")
            );
            emitter.send(SseEmitter.event()
                .name("debug-trace")
                .data(debugWelcome));
        } catch (Exception e) {
            log.error("Error sending initial debug message", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Manual refresh endpoint for statistics - clears cache and refreshes data
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshStatistics() {
        log.info("Manual statistics refresh requested - clearing cache");
        
        try {
            // Clear cache to force fresh data
            clearCache();
            
            // Trigger immediate broadcast to all connected clients
            broadcastStatistics();
            broadcastActivityFeed();
            
            return ResponseEntity.ok(Map.of(
                "message", "Statistics refreshed successfully",
                "timestamp", LocalDateTime.now(),
                "connectedClients", sseEmitters.size(),
                "cacheCleared", true
            ));
        } catch (Exception e) {
            log.error("Error refreshing statistics", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to refresh statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get SSE connection status
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        return ResponseEntity.ok(Map.of(
            "activeConnections", sseEmitters.size(),
            "timestamp", LocalDateTime.now()
        ));
    }
    
    // Private methods for SSE broadcasting
    
    private void broadcastStatistics() {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        try {
            AdminStatisticsDTO statistics = getCachedStatistics();
            
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            
            sseEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("statistics")
                        .data(statistics));
                } catch (Exception e) {
                    log.debug("Failed to send to SSE client, marking for removal: {}", e.getMessage());
                    deadEmitters.add(emitter);
                }
            });
            
            // Remove dead connections
            sseEmitters.removeAll(deadEmitters);
            
            if (!deadEmitters.isEmpty()) {
                log.debug("Removed {} dead SSE connections", deadEmitters.size());
            }
            
        } catch (Exception e) {
            log.error("Error broadcasting statistics", e);
        }
    }
    
    private void broadcastActivityFeed() {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        try {
            ActivityFeed activityFeed = getCachedActivityFeed();
            
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            
            sseEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("activity")
                        .data(activityFeed));
                } catch (Exception e) {
                    log.debug("Failed to send activity feed to SSE client: {}", e.getMessage());
                    deadEmitters.add(emitter);
                }
            });
            
            sseEmitters.removeAll(deadEmitters);
            
        } catch (Exception e) {
            log.error("Error broadcasting activity feed", e);
        }
    }
    
    /**
     * NEW: Broadcasts agent thought events to all connected SSE clients
     */
    public void broadcastAgentThought(Map<String, Object> thoughtData) {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        sseEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("agent-thought")
                    .data(thoughtData));
            } catch (Exception e) {
                log.debug("Failed to send agent thought to SSE client: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        });
        
        sseEmitters.removeAll(deadEmitters);
        
        if (!deadEmitters.isEmpty()) {
            log.debug("Removed {} dead SSE connections during agent thought broadcast", deadEmitters.size());
        }
    }
    
    /**
     * NEW: Broadcasts performance metrics to all connected SSE clients
     */
    public void broadcastPerformanceMetrics(Map<String, Object> performanceData) {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        sseEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("performance-metrics")
                    .data(performanceData));
            } catch (Exception e) {
                log.debug("Failed to send performance metrics to SSE client: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        });
        
        sseEmitters.removeAll(deadEmitters);
        
        if (!deadEmitters.isEmpty()) {
            log.debug("Removed {} dead SSE connections during performance metrics broadcast", deadEmitters.size());
        }
    }
    
    /**
     * NEW: Broadcasts debug trace events to all connected SSE clients
     */
    public void broadcastDebugTrace(Map<String, Object> debugData) {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        sseEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("debug-trace")
                    .data(debugData));
            } catch (Exception e) {
                log.debug("Failed to send debug trace to SSE client: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        });
        
        sseEmitters.removeAll(deadEmitters);
        
        if (!deadEmitters.isEmpty()) {
            log.debug("Removed {} dead SSE connections during debug trace broadcast", deadEmitters.size());
        }
    }
    
    /**
     * NEW: Broadcasts task execution updates to all connected SSE clients
     * Called by TaskExecutionIntegrationService for real-time task updates
     */
    public void broadcastTaskUpdate(io.wingie.entity.TaskExecution task, String eventType) {
        if (sseEmitters.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Object> taskUpdate = Map.of(
                "eventType", eventType,
                "taskId", task.getTaskId(),
                "status", task.getStatus().name(),
                "progress", task.getProgressPercent(),
                "message", task.getProgressMessage() != null ? task.getProgressMessage() : "",
                "screenshots", task.getScreenshots() != null ? task.getScreenshots() : List.of(),
                "timestamp", LocalDateTime.now()
            );
            
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            
            sseEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("task-update")
                        .data(taskUpdate));
                } catch (Exception e) {
                    log.debug("Failed to send task update to SSE client: {}", e.getMessage());
                    deadEmitters.add(emitter);
                }
            });
            
            sseEmitters.removeAll(deadEmitters);
            
            log.debug("📡 Broadcasted task update {} for task {} to {} clients", 
                eventType, task.getTaskId(), sseEmitters.size());
            
        } catch (Exception e) {
            log.error("Error broadcasting task update for task {}: {}", task.getTaskId(), e.getMessage());
        }
    }
    
    /**
     * Event listener for task update events to avoid circular dependencies
     */
    @EventListener
    public void handleTaskUpdateEvent(io.wingie.service.TaskExecutionIntegrationService.TaskUpdateEvent event) {
        try {
            broadcastTaskUpdate(event.getTask(), event.getEventType());
        } catch (Exception e) {
            log.error("Error handling task update event: {}", e.getMessage());
        }
    }
    
    // Cache management methods
    
    /**
     * Get cached statistics with thread-safe caching to prevent connection pool exhaustion
     */
    private AdminStatisticsDTO getCachedStatistics() {
        cacheLock.readLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            AdminStatisticsDTO cached = cachedStatistics.get();
            
            // Check if cache is valid
            if (cached != null && lastStatisticsCacheTime.plusSeconds(STATISTICS_CACHE_DURATION).isAfter(now)) {
                log.debug("Returning cached statistics (age: {}s)", 
                    java.time.Duration.between(lastStatisticsCacheTime, now).getSeconds());
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // Cache is stale or missing, need to refresh
        return refreshStatisticsCache();
    }
    
    /**
     * Get cached activity feed with thread-safe caching
     */
    private ActivityFeed getCachedActivityFeed() {
        cacheLock.readLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            ActivityFeed cached = cachedActivityFeed.get();
            
            // Check if cache is valid
            if (cached != null && lastActivityCacheTime.plusSeconds(ACTIVITY_CACHE_DURATION).isAfter(now)) {
                log.debug("Returning cached activity feed (age: {}s)", 
                    java.time.Duration.between(lastActivityCacheTime, now).getSeconds());
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // Cache is stale or missing, need to refresh
        return refreshActivityCache();
    }
    
    /**
     * Refresh statistics cache with write lock
     */
    private AdminStatisticsDTO refreshStatisticsCache() {
        cacheLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            AdminStatisticsDTO cached = cachedStatistics.get();
            
            // Double-check pattern: another thread might have refreshed while we waited
            if (cached != null && lastStatisticsCacheTime.plusSeconds(STATISTICS_CACHE_DURATION).isAfter(now)) {
                log.debug("Statistics cache was refreshed by another thread");
                return cached;
            }
            
            log.debug("Refreshing statistics cache from database");
            AdminStatisticsDTO fresh = statisticsService.getCompleteStatistics();
            cachedStatistics.set(fresh);
            lastStatisticsCacheTime = now;
            
            return fresh;
        } catch (Exception e) {
            log.error("Error refreshing statistics cache", e);
            // Return cached data if available, even if stale
            AdminStatisticsDTO cached = cachedStatistics.get();
            if (cached != null) {
                log.warn("Returning stale cached statistics due to error");
                return cached;
            }
            throw new RuntimeException("Failed to refresh statistics cache", e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Refresh activity cache with write lock
     */
    private ActivityFeed refreshActivityCache() {
        cacheLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            ActivityFeed cached = cachedActivityFeed.get();
            
            // Double-check pattern: another thread might have refreshed while we waited
            if (cached != null && lastActivityCacheTime.plusSeconds(ACTIVITY_CACHE_DURATION).isAfter(now)) {
                log.debug("Activity cache was refreshed by another thread");
                return cached;
            }
            
            log.debug("Refreshing activity cache from database");
            AdminStatisticsDTO fresh = statisticsService.getCompleteStatistics();
            ActivityFeed freshActivity = fresh.getActivityFeed();
            cachedActivityFeed.set(freshActivity);
            lastActivityCacheTime = now;
            
            return freshActivity;
        } catch (Exception e) {
            log.error("Error refreshing activity cache", e);
            // Return cached data if available, even if stale
            ActivityFeed cached = cachedActivityFeed.get();
            if (cached != null) {
                log.warn("Returning stale cached activity feed due to error");
                return cached;
            }
            throw new RuntimeException("Failed to refresh activity cache", e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Clear all caches to force fresh data
     */
    private void clearCache() {
        cacheLock.writeLock().lock();
        try {
            log.info("Clearing all statistics caches");
            cachedStatistics.set(null);
            cachedActivityFeed.set(null);
            lastStatisticsCacheTime = LocalDateTime.now().minusMinutes(5);
            lastActivityCacheTime = LocalDateTime.now().minusMinutes(5);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Cleanup method for scheduler shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down admin statistics SSE scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}