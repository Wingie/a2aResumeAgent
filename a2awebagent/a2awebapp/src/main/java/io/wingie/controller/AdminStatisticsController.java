package io.wingie.controller;

import io.wingie.dto.AdminStatisticsDTO;
import io.wingie.dto.AdminStatisticsDTO.*;
import io.wingie.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    // Initialize real-time updates
    {
        // Send updates every 30 seconds
        scheduler.scheduleAtFixedRate(this::broadcastStatistics, 10, 30, TimeUnit.SECONDS);
        // Send activity feed updates every 10 seconds
        scheduler.scheduleAtFixedRate(this::broadcastActivityFeed, 5, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Get complete admin dashboard statistics
     */
    @GetMapping("/complete")
    public ResponseEntity<AdminStatisticsDTO> getCompleteStatistics() {
        log.debug("Admin dashboard requesting complete statistics");
        
        try {
            AdminStatisticsDTO statistics = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
            return ResponseEntity.ok(stats.getActivityFeed());
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO initialStats = statisticsService.getCompleteStatistics();
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
            emitter.send(SseEmitter.event()
                .name("activity")
                .data(stats.getActivityFeed()));
        } catch (Exception e) {
            log.error("Error sending initial activity feed", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Manual refresh endpoint for statistics
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshStatistics() {
        log.info("Manual statistics refresh requested");
        
        try {
            // Trigger immediate broadcast to all connected clients
            broadcastStatistics();
            broadcastActivityFeed();
            
            return ResponseEntity.ok(Map.of(
                "message", "Statistics refreshed successfully",
                "timestamp", LocalDateTime.now(),
                "connectedClients", sseEmitters.size()
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
            AdminStatisticsDTO statistics = statisticsService.getCompleteStatistics();
            
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
            AdminStatisticsDTO stats = statisticsService.getCompleteStatistics();
            ActivityFeed activityFeed = stats.getActivityFeed();
            
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