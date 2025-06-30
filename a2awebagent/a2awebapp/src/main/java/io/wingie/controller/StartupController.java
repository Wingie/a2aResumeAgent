package io.wingie.controller;

import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the startup dashboard that provides cache management UI
 */
@Controller
@RequestMapping("/startup")
@Slf4j
public class StartupController {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    /**
     * Display the startup dashboard with cache statistics and management UI
     */
    @GetMapping
    public String startup(Model model) {
        log.info("Startup dashboard accessed");
        
        try {
            // Add cache statistics to the model
            model.addAttribute("currentModel", cacheService.getCurrentProviderModel());
            model.addAttribute("cacheReport", cacheService.getCacheReport());
            model.addAttribute("providerStats", cacheService.getProviderStatistics());
            
            // Return the template name (will resolve to startup.html)
            return "startup";
            
        } catch (Exception e) {
            log.error("Error loading startup dashboard: {}", e.getMessage());
            model.addAttribute("error", "Failed to load cache statistics: " + e.getMessage());
            return "startup";
        }
    }
}