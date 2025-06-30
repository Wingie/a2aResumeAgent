package io.wingie.controller;

import io.wingie.dto.TaskExecutionDTO;
import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.repository.TaskExecutionRepository;
import io.wingie.service.TaskExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AgentDashboardController {

    private final TaskExecutionRepository taskRepository;
    private final TaskExecutorService taskExecutorService;
    
    // Store SSE emitters for real-time updates
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/agents")
    public String agentsDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            Model model) {
        
        log.debug("Loading agents dashboard - page: {}, size: {}, status: {}", page, size, status);
        
        try {
            // Get system statistics
            Map<String, Object> stats = getSystemStats();
            model.addAttribute("stats", stats);
            
            // Get active tasks
            List<TaskExecution> activeTasks = taskRepository.findActiveTasks();
            model.addAttribute("activeTasks", activeTasks);
            
            // Get recent tasks with pagination
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<TaskExecution> recentTasks = taskRepository.findRecentTasks(pageable);
            model.addAttribute("recentTasks", recentTasks);
            
            // Add filter options
            model.addAttribute("taskStatuses", TaskStatus.values());
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentTaskType", taskType);
            model.addAttribute("currentSortBy", sortBy);
            model.addAttribute("currentSortDir", sortDir);
            
            // Add pagination info
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", recentTasks.getTotalPages());
            model.addAttribute("totalElements", recentTasks.getTotalElements());
            
            return "agents-dashboard";
            
        } catch (Exception e) {
            log.error("Error loading agents dashboard", e);
            model.addAttribute("error", "Failed to load dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/agents/task/{taskId}")
    public String taskDetails(@PathVariable String taskId, Model model) {
        log.debug("Loading task details for: {}", taskId);
        
        return taskRepository.findById(taskId)
            .map(task -> {
                model.addAttribute("task", task);
                model.addAttribute("screenshots", task.getScreenshots());
                return "task-details";
            })
            .orElse("redirect:/agents?error=Task not found");
    }

    @GetMapping("/agents/api/stats")
    @ResponseBody
    public Map<String, Object> getStatsApi() {
        return getSystemStats();
    }

    @GetMapping("/agents/api/active")
    @ResponseBody
    public List<TaskExecutionDTO> getActiveTasksApi() {
        // Use DTO-safe method to avoid lazy loading issues
        return taskRepository.findActiveTasksForSSE()
                .stream()
                .map(TaskExecutionDTO::fromEntityMinimal)
                .collect(Collectors.toList());
    }

    @GetMapping("/agents/api/recent")
    @ResponseBody
    public Page<TaskExecution> getRecentTasksApi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return taskRepository.findRecentTasks(pageable);
    }

    // Server-Sent Events endpoint for real-time updates
    @GetMapping(value = "/agents/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskUpdates() {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        emitters.add(emitter);
        log.debug("New SSE client connected. Total clients: {}", emitters.size());
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected. Remaining clients: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client timed out. Remaining clients: {}", emitters.size());
        });
        
        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.debug("SSE client error. Remaining clients: {}", emitters.size());
        });
        
        // Send initial data using DTOs to avoid lazy loading issues
        try {
            List<TaskExecutionDTO> activeTaskDTOs = taskRepository.findActiveTasksForSSE()
                    .stream()
                    .map(TaskExecutionDTO::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            Map<String, Object> initialData = Map.of(
                "type", "initial",
                "stats", getSystemStats(),
                "activeTasks", activeTaskDTOs
            );
            emitter.send(SseEmitter.event().name("dashboard-update").data(initialData));
        } catch (Exception e) {
            log.error("Error sending initial SSE data", e);
        }
        
        return emitter;
    }

    // Manual refresh endpoints
    @PostMapping("/agents/refresh")
    public String refreshDashboard() {
        // Trigger refresh for all connected SSE clients
        broadcastUpdate();
        return "redirect:/agents";
    }

    @DeleteMapping("/agents/task/{taskId}")
    @ResponseBody
    public Map<String, String> cancelTask(@PathVariable String taskId) {
        try {
            taskExecutorService.cancelTask(taskId);
            broadcastUpdate();
            return Map.of("status", "success", "message", "Task cancelled");
        } catch (Exception e) {
            log.error("Error cancelling task {}", taskId, e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    // Helper methods
    private Map<String, Object> getSystemStats() {
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
            
            return Map.of(
                "activeTasks", taskRepository.countByStatus(TaskStatus.RUNNING),
                "queuedTasks", taskRepository.countByStatus(TaskStatus.QUEUED),
                "completedToday", taskRepository.findCompletedTasksBetween(last24Hours, LocalDateTime.now()).size(),
                "failedToday", taskRepository.countByStatus(TaskStatus.FAILED),
                "totalTasks", taskRepository.count(),
                "retryableTasks", taskRepository.findRetryableTasks().size(),
                "lastUpdated", LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Error getting system stats", e);
            return Map.of("error", e.getMessage());
        }
    }

    private void broadcastUpdate() {
        if (emitters.isEmpty()) {
            return;
        }
        
        try {
            // Use DTOs to avoid lazy loading issues in SSE
            List<TaskExecutionDTO> activeTaskDTOs = taskRepository.findActiveTasksForSSE()
                    .stream()
                    .map(TaskExecutionDTO::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            Map<String, Object> updateData = Map.of(
                "type", "update",
                "stats", getSystemStats(),
                "activeTasks", activeTaskDTOs,
                "timestamp", LocalDateTime.now()
            );
            
            // Send to all connected clients
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("dashboard-update").data(updateData));
                } catch (Exception e) {
                    log.debug("Failed to send SSE update, removing client", e);
                    emitters.remove(emitter);
                }
            });
            
        } catch (Exception e) {
            log.error("Error broadcasting update", e);
        }
    }

    // Scheduled method to broadcast periodic updates
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 10000) // Every 10 seconds
    public void scheduledBroadcast() {
        if (!emitters.isEmpty()) {
            broadcastUpdate();
        }
    }
}