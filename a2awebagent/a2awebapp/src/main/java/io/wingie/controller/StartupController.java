package io.wingie.controller;

import io.wingie.service.ToolDescriptionCacheService;
import io.wingie.service.ModelManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for the startup admin dashboard that provides model management and cache management UI
 */
@Controller
@RequestMapping("/startup")
@Slf4j
public class StartupController {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @Autowired
    private ModelManagementService modelManagementService;
    
    /**
     * Display the startup admin dashboard with model management and cache statistics
     */
    @GetMapping
    public String startup(Model model) {
        log.info("Startup admin dashboard accessed");
        
        try {
            // Add model management data
            model.addAttribute("currentModel", modelManagementService.getCurrentModel());
            model.addAttribute("availableModels", modelManagementService.getAvailableModels());
            model.addAttribute("modelStatistics", modelManagementService.getModelStatistics());
            
            // Add cache statistics
            model.addAttribute("cacheReport", cacheService.getCacheReport());
            model.addAttribute("providerStats", cacheService.getProviderStatistics());
            
            // Return the template name (will resolve to startup.html)
            return "startup";
            
        } catch (Exception e) {
            log.error("Error loading startup dashboard: {}", e.getMessage());
            model.addAttribute("error", "Failed to load dashboard data: " + e.getMessage());
            return "startup";
        }
    }
    
    /**
     * API endpoint to get all available models
     */
    @GetMapping("/api/models")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        try {
            Map<String, Object> response = Map.of(
                "currentModel", modelManagementService.getCurrentModel(),
                "availableModels", modelManagementService.getAvailableModels(),
                "statistics", modelManagementService.getModelStatistics()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting available models: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API endpoint to switch models
     */
    @PostMapping("/api/models/switch")
    @ResponseBody
    public ResponseEntity<ModelManagementService.ModelSwitchResult> switchModel(@RequestBody Map<String, String> request) {
        String modelKey = request.get("modelKey");
        
        if (modelKey == null || modelKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ModelManagementService.ModelSwitchResult(false, "Model key is required", null, null)
            );
        }
        
        try {
            log.info("Admin requested model switch to: {}", modelKey);
            ModelManagementService.ModelSwitchResult result = modelManagementService.switchModel(modelKey);
            
            if (result.isSuccess()) {
                log.info("Model switched successfully: {}", result.getMessage());
                // Optionally clear old model cache
                if (request.containsKey("clearOldCache") && "true".equals(request.get("clearOldCache"))) {
                    modelManagementService.clearOldModelCache(result.getOldModel());
                }
            } else {
                log.warn("Model switch failed: {}", result.getMessage());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error switching model: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                new ModelManagementService.ModelSwitchResult(false, "Internal error: " + e.getMessage(), null, null)
            );
        }
    }
    
    /**
     * API endpoint to validate model configuration
     */
    @GetMapping("/api/models/validate/{modelKey}")
    @ResponseBody
    public ResponseEntity<ModelManagementService.ModelValidationResult> validateModel(@PathVariable String modelKey) {
        try {
            ModelManagementService.ModelValidationResult result = modelManagementService.validateModel(modelKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating model {}: {}", modelKey, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API endpoint to get model statistics
     */
    @GetMapping("/api/models/statistics")
    @ResponseBody
    public ResponseEntity<ModelManagementService.ModelStatistics> getModelStatistics() {
        try {
            ModelManagementService.ModelStatistics statistics = modelManagementService.getModelStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting model statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}