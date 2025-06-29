package io.wingie;

import com.t4a.processor.scripts.BaseScriptProcessor;
import com.t4a.processor.scripts.SeleniumScriptProcessor;
import com.t4a.processor.selenium.SeleniumOpenAIProcessor;
import io.github.vishalmysore.a2a.server.DyanamicTaskContoller;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class SeleniumTaskController extends DyanamicTaskContoller {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumTaskController.class);
    
    private BaseScriptProcessor baseScriptProcessor;
    private boolean initializationFailed = false;
    private String initializationError;
    
    @Autowired
    private WebDriver webDriver;
    
    public SeleniumTaskController() {
        logger.info("SeleniumTaskController constructor called - lazy initialization will occur on first use");
        // Don't initialize WebDriver here - do it lazily
    }

    private synchronized void initializeWebDriverIfNeeded() {
        if (baseScriptProcessor != null || initializationFailed) {
            return; // Already initialized or failed
        }
        
        try {
            logger.info("Initializing SeleniumScriptProcessor with Spring-managed WebDriver...");
            
            if (webDriver == null) {
                throw new IllegalStateException("WebDriver bean not available from Spring context");
            }
            
            logger.info("Using Spring-managed WebDriver: " + webDriver.getClass().getName());
            
            // Create the script processor using Spring-managed WebDriver
            this.baseScriptProcessor = new SeleniumScriptProcessor(new SeleniumOpenAIProcessor(webDriver));
            
            logger.info("SeleniumScriptProcessor initialized successfully");
            
        } catch (Exception e) {
            initializationFailed = true;
            initializationError = "Failed to initialize SeleniumScriptProcessor: " + e.getMessage();
            logger.error("Failed to initialize SeleniumScriptProcessor", e);
            logger.error("WebDriver might not be available. Application will continue without Selenium functionality.");
            
            // Don't throw the exception - let the application start without Selenium
        }
    }

    @Override
    public BaseScriptProcessor getScriptProcessor() {
        initializeWebDriverIfNeeded();
        
        if (initializationFailed) {
            logger.warn("Selenium functionality is not available due to initialization failure: {}", initializationError);
            throw new RuntimeException("Selenium functionality is not available: " + initializationError);
        }
        
        return baseScriptProcessor;
    }
    
    /**
     * Check if Selenium is available without triggering initialization
     */
    public boolean isSeleniumAvailable() {
        return !initializationFailed && (baseScriptProcessor != null);
    }
    
    /**
     * Get the initialization error if any
     */
    public String getInitializationError() {
        return initializationError;
    }
    
    /**
     * Cleanup method to reset script processor
     * Note: WebDriver is managed by Spring and should not be closed here
     */
    public void cleanup() {
        if (baseScriptProcessor != null) {
            logger.info("Resetting SeleniumScriptProcessor...");
            baseScriptProcessor = null;
            logger.info("SeleniumScriptProcessor reset successfully");
        }
    }
}
