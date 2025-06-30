package io.wingie.controller;

import io.wingie.dto.ModelEvaluationDTO;
import io.wingie.entity.*;
import io.wingie.repository.ModelEvaluationRepository;
import io.wingie.repository.EvaluationTaskRepository;
import io.wingie.repository.EvaluationScreenshotRepository;
import io.wingie.service.ModelEvaluationService;
import io.wingie.service.BenchmarkDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/evaluations")
public class EvaluationController {

    private final ModelEvaluationRepository evaluationRepository;
    private final EvaluationTaskRepository taskRepository;
    private final EvaluationScreenshotRepository screenshotRepository;
    private final ModelEvaluationService evaluationService;
    private final BenchmarkDefinitionService benchmarkService;
    
    // Store SSE emitters for real-time updates
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Main evaluation dashboard page
     */
    @GetMapping("")
    public String evaluationDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String benchmarkName,
            Model model) {
        
        log.debug("Loading evaluation dashboard - page: {}, size: {}, status: {}", page, size, status);
        
        try {
            // Get system statistics
            Map<String, Object> stats = evaluationService.getEvaluationStats();
            model.addAttribute("stats", stats);
            
            // Get active evaluations
            List<ModelEvaluation> activeEvaluations = evaluationRepository.findActiveEvaluations();
            model.addAttribute("activeEvaluations", activeEvaluations);
            
            // Get recent evaluations with pagination
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ModelEvaluation> recentEvaluations = evaluationRepository.findRecentEvaluations(pageable);
            model.addAttribute("recentEvaluations", recentEvaluations);
            
            // Add available benchmarks and models
            model.addAttribute("availableBenchmarks", benchmarkService.getAllBenchmarksInfo());
            model.addAttribute("availableModels", getAvailableModels());
            model.addAttribute("evaluationStatuses", EvaluationStatus.values());
            
            // Add filter options
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentModelName", modelName);
            model.addAttribute("currentBenchmarkName", benchmarkName);
            model.addAttribute("currentSortBy", sortBy);
            model.addAttribute("currentSortDir", sortDir);
            
            // Add pagination info
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", recentEvaluations.getTotalPages());
            model.addAttribute("totalElements", recentEvaluations.getTotalElements());
            
            return "evaluation-dashboard";
            
        } catch (Exception e) {
            log.error("Error loading evaluation dashboard", e);
            model.addAttribute("error", "Failed to load dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Start a new evaluation
     */
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startEvaluation(
            @RequestParam String modelName,
            @RequestParam String modelProvider,
            @RequestParam String benchmarkName,
            @RequestParam(defaultValue = "unknown") String initiatedBy,
            @RequestParam(required = false) Map<String, Object> configuration) {
        
        try {
            log.info("Starting evaluation: model={}, benchmark={}, initiatedBy={}", modelName, benchmarkName, initiatedBy);
            
            // Validate benchmark exists
            if (!benchmarkService.benchmarkExists(benchmarkName)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unknown benchmark: " + benchmarkName
                ));
            }
            
            String evaluationId = evaluationService.startEvaluation(modelName, modelProvider, benchmarkName, initiatedBy, configuration);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "evaluationId", evaluationId,
                "message", "Evaluation started successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error starting evaluation", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get evaluation details
     */
    @GetMapping("/{evaluationId}")
    public String evaluationDetails(@PathVariable String evaluationId, Model model) {
        log.debug("Loading evaluation details for: {}", evaluationId);
        
        return evaluationRepository.findById(evaluationId)
            .map(evaluation -> {
                model.addAttribute("evaluation", evaluation);
                
                // Get tasks for this evaluation
                List<EvaluationTask> tasks = taskRepository.findByEvaluationIdOrderByExecutionOrder(evaluationId);
                model.addAttribute("tasks", tasks);
                
                // Get benchmark info
                String benchmarkName = evaluation.getBenchmarkName();
                model.addAttribute("benchmarkInfo", benchmarkService.getBenchmarkInfo(benchmarkName));
                
                return "evaluation-details";
            })
            .orElse("redirect:/evaluations?error=Evaluation not found");
    }

    /**
     * Get evaluation task details
     */
    @GetMapping("/{evaluationId}/tasks/{taskId}")
    public String taskDetails(@PathVariable String evaluationId, @PathVariable String taskId, Model model) {
        log.debug("Loading task details for evaluation: {}, task: {}", evaluationId, taskId);
        
        return taskRepository.findById(taskId)
            .map(task -> {
                // Verify task belongs to evaluation
                if (!task.getEvaluationId().equals(evaluationId)) {
                    return "redirect:/evaluations/" + evaluationId + "?error=Task not found";
                }
                
                model.addAttribute("task", task);
                
                // Get screenshots for this task
                List<EvaluationScreenshot> screenshots = screenshotRepository.findByTaskIdOrderByStepNumber(taskId);
                model.addAttribute("screenshots", screenshots);
                
                // Get evaluation info
                evaluationRepository.findById(evaluationId).ifPresent(evaluation -> {
                    model.addAttribute("evaluation", evaluation);
                });
                
                return "evaluation-task-details";
            })
            .orElse("redirect:/evaluations/" + evaluationId + "?error=Task not found");
    }

    /**
     * Cancel a running evaluation
     */
    @DeleteMapping("/{evaluationId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cancelEvaluation(@PathVariable String evaluationId) {
        try {
            log.info("Cancelling evaluation: {}", evaluationId);
            evaluationService.cancelEvaluation(evaluationId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Evaluation cancelled successfully"
            ));
        } catch (Exception e) {
            log.error("Error cancelling evaluation {}", evaluationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get evaluation progress
     */
    @GetMapping("/{evaluationId}/progress")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEvaluationProgress(@PathVariable String evaluationId) {
        try {
            return evaluationRepository.findById(evaluationId)
                .map(evaluation -> {
                    Map<String, Object> progress = Map.of(
                        "evaluationId", evaluationId,
                        "status", evaluation.getStatus().name(),
                        "progressPercent", evaluation.getProgressPercent(),
                        "completedTasks", evaluation.getCompletedTasks(),
                        "totalTasks", evaluation.getTotalTasks(),
                        "successfulTasks", evaluation.getSuccessfulTasks(),
                        "failedTasks", evaluation.getFailedTasks(),
                        "duration", evaluation.getDurationFormatted(),
                        "score", evaluation.getScoreFormatted()
                    );
                    return ResponseEntity.ok(progress);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting progress for evaluation {}", evaluationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get evaluation results
     */
    @GetMapping("/{evaluationId}/results")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEvaluationResults(@PathVariable String evaluationId) {
        try {
            return evaluationRepository.findById(evaluationId)
                .map(evaluation -> {
                    List<EvaluationTask> tasks = taskRepository.findByEvaluationIdOrderByExecutionOrder(evaluationId);
                    
                    Map<String, Object> results = Map.of(
                        "evaluation", evaluation,
                        "tasks", tasks,
                        "summary", Map.of(
                            "totalTasks", tasks.size(),
                            "completedTasks", tasks.stream().filter(t -> t.getStatus().isTerminal()).count(),
                            "successfulTasks", tasks.stream().filter(t -> Boolean.TRUE.equals(t.getSuccess())).count(),
                            "averageScore", tasks.stream().filter(t -> t.getScore() != null).mapToDouble(EvaluationTask::getScore).average().orElse(0.0),
                            "totalDuration", evaluation.getDurationFormatted()
                        )
                    );
                    return ResponseEntity.ok(results);
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting results for evaluation {}", evaluationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get screenshots for an evaluation
     */
    @GetMapping("/{evaluationId}/screenshots")
    @ResponseBody
    public ResponseEntity<List<EvaluationScreenshot>> getEvaluationScreenshots(@PathVariable String evaluationId) {
        try {
            List<EvaluationScreenshot> screenshots = screenshotRepository.findScreenshotsForEvaluation(evaluationId);
            return ResponseEntity.ok(screenshots);
        } catch (Exception e) {
            log.error("Error getting screenshots for evaluation {}", evaluationId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * API endpoints for stats and data
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStatsApi() {
        return evaluationService.getEvaluationStats();
    }

    @GetMapping("/api/active")
    @ResponseBody
    public List<ModelEvaluationDTO> getActiveEvaluationsApi() {
        // Use DTOs to avoid lazy loading issues
        return evaluationRepository.findActiveEvaluations()
                .stream()
                .map(ModelEvaluationDTO::fromEntityMinimal)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/recent")
    @ResponseBody
    public Page<ModelEvaluation> getRecentEvaluationsApi(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return evaluationRepository.findRecentEvaluations(pageable);
    }

    @GetMapping("/api/benchmarks")
    @ResponseBody
    public List<Map<String, Object>> getAvailableBenchmarksApi() {
        return benchmarkService.getAllBenchmarksInfo();
    }

    @GetMapping("/api/models")
    @ResponseBody
    public List<Map<String, String>> getAvailableModelsApi() {
        return getAvailableModels();
    }

    /**
     * Server-Sent Events endpoint for real-time updates
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvaluationUpdates() {
        SseEmitter emitter = new SseEmitter(600000L); // 10 minutes timeout
        
        emitters.add(emitter);
        log.debug("New SSE client connected for evaluations. Total clients: {}", emitters.size());
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected from evaluations. Remaining clients: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client timed out for evaluations. Remaining clients: {}", emitters.size());
        });
        
        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.debug("SSE client error for evaluations. Remaining clients: {}", emitters.size());
        });
        
        // Send initial data using DTOs to avoid lazy loading issues
        try {
            List<ModelEvaluationDTO> activeEvaluationDTOs = evaluationRepository.findActiveEvaluations()
                    .stream()
                    .map(ModelEvaluationDTO::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            Map<String, Object> initialData = Map.of(
                "type", "initial",
                "stats", evaluationService.getEvaluationStats(),
                "activeEvaluations", activeEvaluationDTOs,
                "timestamp", LocalDateTime.now()
            );
            emitter.send(SseEmitter.event().name("evaluation-update").data(initialData));
        } catch (Exception e) {
            log.error("Error sending initial SSE data for evaluations", e);
        }
        
        return emitter;
    }

    /**
     * Manual refresh endpoint
     */
    @PostMapping("/refresh")
    public String refreshDashboard() {
        broadcastUpdate();
        return "redirect:/evaluations";
    }

    /**
     * Compare evaluations/models
     */
    @GetMapping("/compare")
    public String compareEvaluations(
            @RequestParam List<String> evaluationIds,
            Model model) {
        
        try {
            List<ModelEvaluation> evaluations = evaluationRepository.findAllById(evaluationIds);
            model.addAttribute("evaluations", evaluations);
            
            // Get comparison data
            Map<String, Object> comparisonData = generateComparisonData(evaluations);
            model.addAttribute("comparisonData", comparisonData);
            
            return "evaluation-comparison";
            
        } catch (Exception e) {
            log.error("Error loading evaluation comparison", e);
            return "redirect:/evaluations?error=Failed to load comparison";
        }
    }

    // Helper methods
    
    private List<Map<String, String>> getAvailableModels() {
        // This would typically come from configuration or a model registry
        return List.of(
            Map.of("name", "gemma-3b", "provider", "openrouter", "description", "Google Gemma 3B"),
            Map.of("name", "gpt-4o-mini", "provider", "openai", "description", "OpenAI GPT-4 Optimized Mini"),
            Map.of("name", "claude-3-haiku", "provider", "anthropic", "description", "Anthropic Claude 3 Haiku"),
            Map.of("name", "mistral-large", "provider", "mistral", "description", "Mistral Large"),
            Map.of("name", "gemini-2.0-flash", "provider", "google", "description", "Google Gemini 2.0 Flash")
        );
    }
    
    private Map<String, Object> generateComparisonData(List<ModelEvaluation> evaluations) {
        // Generate comparison charts and statistics
        return Map.of(
            "scoreComparison", evaluations.stream()
                .collect(java.util.stream.Collectors.toMap(
                    ModelEvaluation::getModelName,
                    e -> e.getOverallScore() != null ? e.getOverallScore() : 0.0
                )),
            "successRateComparison", evaluations.stream()
                .collect(java.util.stream.Collectors.toMap(
                    ModelEvaluation::getModelName,
                    e -> e.getSuccessRate() != null ? e.getSuccessRate() : 0.0
                )),
            "durationComparison", evaluations.stream()
                .collect(java.util.stream.Collectors.toMap(
                    ModelEvaluation::getModelName,
                    e -> e.getTotalExecutionTimeSeconds() != null ? e.getTotalExecutionTimeSeconds() : 0L
                ))
        );
    }
    
    private void broadcastUpdate() {
        if (emitters.isEmpty()) {
            return;
        }
        
        try {
            // Use DTOs to avoid lazy loading issues in SSE
            List<ModelEvaluationDTO> activeEvaluationDTOs = evaluationRepository.findActiveEvaluations()
                    .stream()
                    .map(ModelEvaluationDTO::fromEntityMinimal)
                    .collect(Collectors.toList());
            
            Map<String, Object> updateData = Map.of(
                "type", "update",
                "stats", evaluationService.getEvaluationStats(),
                "activeEvaluations", activeEvaluationDTOs,
                "timestamp", LocalDateTime.now()
            );
            
            // Send to all connected clients
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("evaluation-update").data(updateData));
                } catch (Exception e) {
                    log.debug("Failed to send SSE update, removing client", e);
                    emitters.remove(emitter);
                }
            });
            
        } catch (Exception e) {
            log.error("Error broadcasting evaluation update", e);
        }
    }
    
    // Scheduled method to broadcast periodic updates
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 15000) // Every 15 seconds
    public void scheduledBroadcast() {
        if (!emitters.isEmpty()) {
            broadcastUpdate();
        }
    }
}