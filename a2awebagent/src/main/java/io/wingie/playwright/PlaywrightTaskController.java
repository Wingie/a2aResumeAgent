package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.t4a.processor.scripts.BaseScriptProcessor;
import io.github.vishalmysore.a2a.server.DyanamicTaskContoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Playwright-based task controller
 * Provides intelligent web automation task management using Microsoft Playwright
 */
@Service
@Lazy
public class PlaywrightTaskController extends DyanamicTaskContoller {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightTaskController.class);
    
    private BaseScriptProcessor baseScriptProcessor;
    private boolean initializationFailed = false;
    private String initializationError;
    
    @Autowired
    private Browser playwrightBrowser;
    
    @Autowired
    private BrowserContext playwrightContext;
    
    public PlaywrightTaskController() {
        logger.info("PlaywrightTaskController constructor called - lazy initialization will occur on first use");
    }

    private synchronized void initializePlaywrightIfNeeded() {
        if (baseScriptProcessor != null || initializationFailed) {
            return; // Already initialized or failed
        }
        
        try {
            logger.info("Initializing PlaywrightScriptProcessor with Spring-managed Playwright...");
            
            if (playwrightBrowser == null) {
                throw new IllegalStateException("Playwright Browser bean not available from Spring context");
            }
            
            if (playwrightContext == null) {
                throw new IllegalStateException("Playwright BrowserContext bean not available from Spring context");
            }
            
            logger.info("Using Spring-managed Playwright Browser: {}", playwrightBrowser.getClass().getName());
            
            // Create a custom Playwright script processor
            this.baseScriptProcessor = new PlaywrightScriptProcessor(playwrightBrowser, playwrightContext);
            
            logger.info("PlaywrightScriptProcessor initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Playwright components", e);
            initializationFailed = true;
            initializationError = e.getMessage();
            throw new RuntimeException("Playwright initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public BaseScriptProcessor getScriptProcessor() {
        try {
            initializePlaywrightIfNeeded();
            
            if (initializationFailed) {
                throw new RuntimeException("Playwright initialization previously failed: " + initializationError);
            }
            
            return baseScriptProcessor;
            
        } catch (Exception e) {
            logger.error("Error getting PlaywrightScriptProcessor", e);
            throw new RuntimeException("Failed to get Playwright script processor: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Playwright is properly initialized
     */
    public boolean isPlaywrightReady() {
        try {
            return playwrightBrowser != null && 
                   playwrightContext != null && 
                   !playwrightBrowser.isConnected() == false;
        } catch (Exception e) {
            logger.warn("Error checking Playwright readiness", e);
            return false;
        }
    }

    /**
     * Get initialization status for health checks
     */
    public String getInitializationStatus() {
        if (initializationFailed) {
            return "FAILED: " + initializationError;
        } else if (baseScriptProcessor != null) {
            return "READY";
        } else {
            return "NOT_INITIALIZED";
        }
    }
}