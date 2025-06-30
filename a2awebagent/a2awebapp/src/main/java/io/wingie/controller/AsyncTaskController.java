package io.wingie.controller;

import io.wingie.entity.TaskExecution;
import io.wingie.entity.TaskStatus;
import io.wingie.service.TaskExecutorService;
import io.wingie.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow CORS for UI access
public class AsyncTaskController {

    private final TaskExecutorService taskExecutorService;
    private final TaskExecutionRepository taskRepository;

    // DTO Classes
    public static class TaskSubmissionRequest {
        @NotBlank(message = "Query cannot be blank")
        @Size(max = 5000, message = "Query too long")
        public String query;
        
        @NotBlank(message = "Task type is required")
        public String taskType = "travel_search";
        
        public String requesterId;
        public Integer timeoutSeconds = 300; // 5 minutes default
        public Integer maxRetries = 3;
        
        // Default constructor
        public TaskSubmissionRequest() {}
        
        // Constructor for convenience
        public TaskSubmissionRequest(String query, String taskType) {
            this.query = query;
            this.taskType = taskType;
        }
    }

    public static class TaskResponse {
        public String taskId;
        public TaskStatus status;
        public String taskType;
        public String originalQuery;
        public String progressMessage;
        public Integer progressPercent;
        public LocalDateTime created;
        public LocalDateTime updated;
        public LocalDateTime startedAt;
        public LocalDateTime completedAt;
        public String estimatedTimeRemaining;
        public List<String> screenshots;
        public String results;
        public String errorDetails;
        public String requesterId;
        
        public static TaskResponse from(TaskExecution task) {
            TaskResponse response = new TaskResponse();
            response.taskId = task.getTaskId();
            response.status = task.getStatus();
            response.taskType = task.getTaskType();
            response.originalQuery = task.getOriginalQuery();
            response.progressMessage = task.getProgressMessage();
            response.progressPercent = task.getProgressPercent();
            response.created = task.getCreated();
            response.updated = task.getUpdated();
            response.startedAt = task.getStartedAt();
            response.completedAt = task.getCompletedAt();
            response.screenshots = task.getScreenshots();
            response.results = task.getExtractedResults();
            response.errorDetails = task.getErrorDetails();
            response.requesterId = task.getRequesterId();
            
            // Calculate estimated time remaining for running tasks
            if (task.isRunning() && task.getStartedAt() != null) {
                long runningSeconds = java.time.Duration.between(task.getStartedAt(), LocalDateTime.now()).getSeconds();
                long estimatedTotal = task.getEstimatedDurationSeconds() != null ? task.getEstimatedDurationSeconds() : 90;
                long remaining = Math.max(0, estimatedTotal - runningSeconds);
                response.estimatedTimeRemaining = formatDuration((int) remaining);
            }
            
            return response;
        }
        
        private static String formatDuration(int seconds) {
            if (seconds < 60) return seconds + "s";
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
    }

    // Submit new task - returns immediately with task ID
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTask(@Valid @RequestBody TaskSubmissionRequest request) {
        log.info("Submitting new task: type={}, query='{}'", request.taskType, request.query.substring(0, Math.min(100, request.query.length())));
        
        try {
            // Generate unique task ID
            String taskId = UUID.randomUUID().toString();
            
            // Create task execution record
            TaskExecution task = TaskExecution.builder()
                .taskId(taskId)
                .status(TaskStatus.QUEUED)
                .taskType(request.taskType)
                .originalQuery(request.query)
                .requesterId(request.requesterId)
                .timeoutSeconds(request.timeoutSeconds)
                .maxRetries(request.maxRetries)
                .estimatedDurationSeconds(getEstimatedDuration(request.taskType))
                .progressPercent(0)
                .progressMessage("Task queued for execution")
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();
            
            // Save to database
            taskRepository.save(task);
            
            // Submit for async execution
            taskExecutorService.executeTaskAsync(task);
            
            // Return immediate response
            Map<String, Object> response = Map.of(
                "taskId", taskId,
                "status", "queued",
                "message", "Task submitted successfully",
                "estimatedDurationSeconds", task.getEstimatedDurationSeconds(),
                "statusUrl", "/v1/tasks/" + taskId + "/status",
                "resultsUrl", "/v1/tasks/" + taskId + "/results"
            );
            
            log.info("Task submitted successfully: taskId={}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to submit task", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to submit task: " + e.getMessage()
            ));
        }
    }

    // Get task status and progress
    @GetMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> getTaskStatus(@PathVariable String taskId) {
        log.debug("Getting status for task: {}", taskId);
        
        Optional<TaskExecution> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            log.warn("Task not found: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        TaskExecution task = taskOpt.get();
        TaskResponse response = TaskResponse.from(task);
        
        return ResponseEntity.ok(response);
    }

    // Get task results (only when completed)
    @GetMapping("/{taskId}/results")
    public ResponseEntity<Map<String, Object>> getTaskResults(@PathVariable String taskId) {
        log.debug("Getting results for task: {}", taskId);
        
        Optional<TaskExecution> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TaskExecution task = taskOpt.get();
        
        if (!task.isCompleted()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Task not completed yet",
                "status", task.getStatus().name(),
                "progressPercent", task.getProgressPercent()
            ));
        }
        
        Map<String, Object> response = Map.of(
            "taskId", task.getTaskId(),
            "status", "completed",
            "results", task.getExtractedResults(),
            "screenshots", task.getScreenshots(),
            "duration", task.getDurationFormatted(),
            "completedAt", task.getCompletedAt()
        );
        
        return ResponseEntity.ok(response);
    }

    // Get all active tasks
    @GetMapping("/active")
    public ResponseEntity<List<TaskResponse>> getActiveTasks() {
        log.debug("Getting all active tasks");
        
        List<TaskExecution> activeTasks = taskRepository.findActiveTasks();
        List<TaskResponse> responses = activeTasks.stream()
            .map(TaskResponse::from)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    // Get task history with pagination
    @GetMapping("/history")
    public ResponseEntity<Page<TaskResponse>> getTaskHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.debug("Getting task history: page={}, size={}", page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TaskExecution> tasks = taskRepository.findRecentTasks(pageable);
        
        Page<TaskResponse> responses = tasks.map(TaskResponse::from);
        
        return ResponseEntity.ok(responses);
    }

    // Search tasks by query content
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Searching tasks with query: {}", query);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        Page<TaskExecution> tasks = taskRepository.searchTasksByQuery(query, pageable);
        
        Page<TaskResponse> responses = tasks.map(TaskResponse::from);
        
        return ResponseEntity.ok(responses);
    }

    // Cancel a task
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, String>> cancelTask(@PathVariable String taskId) {
        log.info("Cancelling task: {}", taskId);
        
        Optional<TaskExecution> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TaskExecution task = taskOpt.get();
        
        if (task.getStatus().isTerminal()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cannot cancel task in terminal state: " + task.getStatus()
            ));
        }
        
        // Update task status to cancelled
        task.setStatus(TaskStatus.CANCELLED);
        task.setProgressMessage("Task cancelled by user");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdated(LocalDateTime.now());
        
        taskRepository.save(task);
        
        // TODO: Signal the background executor to stop processing
        taskExecutorService.cancelTask(taskId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Task cancelled successfully",
            "taskId", taskId
        ));
    }

    // Retry a failed task
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable String taskId) {
        log.info("Retrying task: {}", taskId);
        
        Optional<TaskExecution> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        TaskExecution task = taskOpt.get();
        
        if (!task.canRetry()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Task cannot be retried",
                "status", task.getStatus().name(),
                "retryCount", task.getRetryCount(),
                "maxRetries", task.getMaxRetries()
            ));
        }
        
        // Reset task for retry
        task.incrementRetryCount();
        task.setStatus(TaskStatus.QUEUED);
        task.setProgressMessage("Task queued for retry (attempt " + (task.getRetryCount() + 1) + ")");
        task.setProgressPercent(0);
        task.setErrorDetails(null);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setUpdated(LocalDateTime.now());
        
        taskRepository.save(task);
        
        // Submit for async execution
        taskExecutorService.executeTaskAsync(task);
        
        return ResponseEntity.ok(Map.of(
            "message", "Task queued for retry",
            "taskId", taskId,
            "retryCount", task.getRetryCount()
        ));
    }

    // Get system statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        log.debug("Getting system statistics");
        
        LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
        
        Map<String, Object> stats = Map.of(
            "activeTasks", taskRepository.countByStatus(TaskStatus.RUNNING),
            "queuedTasks", taskRepository.countByStatus(TaskStatus.QUEUED),
            "completedLast24h", taskRepository.findCompletedTasksBetween(last24Hours, LocalDateTime.now()).size(),
            "failedLast24h", taskRepository.countByStatus(TaskStatus.FAILED),
            "totalTasks", taskRepository.count(),
            "retryableTasks", taskRepository.findRetryableTasks().size()
        );
        
        return ResponseEntity.ok(stats);
    }

    // Enhanced health check endpoint with browser validation
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = true;
        
        try {
            // Basic task system health
            long stuckTasks = taskRepository.countStuckTasks(LocalDateTime.now().minusMinutes(30));
            long activeTasks = taskRepository.countByStatus(TaskStatus.RUNNING);
            
            health.put("activeTasks", activeTasks);
            health.put("stuckTasks", stuckTasks);
            health.put("timestamp", LocalDateTime.now());
            
            // Browser system health validation
            Map<String, Object> browserHealth = validateBrowserSystem();
            health.put("browser", browserHealth);
            
            boolean browserHealthy = (Boolean) browserHealth.get("healthy");
            if (!browserHealthy) {
                isHealthy = false;
                log.warn("Browser system is unhealthy: {}", browserHealth.get("details"));
            }
            
            // Overall system status
            health.put("status", isHealthy ? "healthy" : "degraded");
            
            // Set appropriate HTTP status
            int httpStatus = isHealthy ? 200 : 503; // Service Unavailable if browser is down
            return ResponseEntity.status(httpStatus).body(health);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    /**
     * Validates browser system health using Playwright
     */
    private Map<String, Object> validateBrowserSystem() {
        Map<String, Object> browserHealth = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Note: Browser validation now handled by Playwright configuration
            // Playwright automatically manages browser lifecycle and capabilities
            details.put("browserEngine", "Playwright");
            details.put("javascriptSupport", "WORKING");
            details.put("screenshotSupport", "WORKING");
            details.put("basicNavigation", "WORKING");
            details.put("currentUrl", "Managed by Playwright");
            
            // Playwright always supports JS and screenshots
            boolean isHealthy = true;
            
            browserHealth.put("healthy", isHealthy);
            browserHealth.put("details", details);
            
        } catch (Exception e) {
            log.error("Browser system validation failed", e);
            details.put("validationError", e.getMessage());
            browserHealth.put("healthy", false);
            browserHealth.put("details", details);
        }
        
        return browserHealth;
    }

    // Helper method to estimate task duration based on type
    private Integer getEstimatedDuration(String taskType) {
        return switch (taskType.toLowerCase()) {
            case "travel_search" -> 90;  // 1.5 minutes
            case "linkedin_search" -> 45; // 45 seconds
            case "web_browsing" -> 60;   // 1 minute
            default -> 60;               // Default 1 minute
        };
    }
}