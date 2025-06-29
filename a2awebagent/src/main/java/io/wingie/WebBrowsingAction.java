package io.wingie;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.detect.ActionCallback;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.GeminiV2ActionProcessor;
import com.t4a.processor.scripts.SeleniumScriptProcessor;
import com.t4a.processor.selenium.SeleniumOpenAIProcessor;
import lombok.extern.java.Log;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Log
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching and validation of web pages ")
public class WebBrowsingAction {
    private SeleniumScriptProcessor script;
    private boolean initialized = false;
    
    @Autowired
    @Lazy
    private WebDriver webDriver;
    
    /**
     * Initialize script processor using the Spring-managed WebDriver.
     */
    private synchronized void initializeIfNeeded() {
        if (!initialized) {
            log.info("=== WebBrowsingAction Initialization Starting ===");
            log.info("Operating System: " + System.getProperty("os.name"));
            log.info("Java Version: " + System.getProperty("java.version"));
            
            try {
                if (webDriver == null) {
                    throw new IllegalStateException("WebDriver bean not available from Spring context");
                }
                
                log.info("Using Spring-managed WebDriver: " + webDriver.getClass().getName());
                
                // Test the driver with a simple operation
                log.info("Testing WebDriver with simple operation...");
                String currentUrl = webDriver.getCurrentUrl();
                log.info("WebDriver test successful. Current URL: " + currentUrl);
                
                log.info("Creating SeleniumScriptProcessor...");
                script = new SeleniumScriptProcessor(new SeleniumOpenAIProcessor(webDriver));
                initialized = true;
                log.info("=== WebBrowsingAction Initialization Completed Successfully ===");
                
            } catch (Exception e) {
                log.severe("=== WebBrowsingAction Initialization Failed ===");
                log.severe("Exception type: " + e.getClass().getName());
                log.severe("Error message: " + e.getMessage());
                log.severe("Full stack trace:");
                e.printStackTrace();
                
                initialized = false;
                throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
            }
        }
    }

    private ActionCallback callback;
    private AIProcessor processor;
    
    private AIProcessor getProcessor() {
        if (processor == null) {
            processor = new GeminiV2ActionProcessor();
        }
        return processor;
    }
    @Action(description = "perform actions on the web with selenium and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) throws IOException {
        initializeIfNeeded();
        CustomScriptResult result = new CustomScriptResult();
        A2ASeleniumCallBack seleniumCallBack = new A2ASeleniumCallBack(result,getProcessor());
        if(getProcessor() != null) {
            try {
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(getProcessor().query("Separate the web browsing steps into individual steps  just give me steps without any additional text or bracket. MOST IMP - make sure each step can be processed by selenium webdriver, urls should always start with http or https {"+ webBrowsingSteps+"}"));
                //you can create your own selenium processor which implements SeleniumProcessor
                //SeleniumScriptProcessor script = new SeleniumScriptProcessor(new MyOwnSeleniumScriptProcessor());

                script.process(seperatedWebBrowsingSteps,seleniumCallBack);
                                return result.getLastData();
            } catch (AIProcessingException e) {
                throw new RuntimeException(e);
            }


        }
        return "processing issues";// Process the file

    }

    @Action(description = "perform actions on the web with selenium and return image file path")
    public String browseWebAndReturnImage(String webBrowsingSteps) throws IOException {
        initializeIfNeeded();
        CustomScriptResult result = new CustomScriptResult();
        A2ASeleniumCallBack seleniumCallBack = new A2ASeleniumCallBack(result,getProcessor());
        if(getProcessor() != null) {
            try {
                log.info("=== Starting Web Browsing Action for Image Capture ===");
                log.info("Original steps: " + webBrowsingSteps);
                
                StringBuffer seperatedWebBrowsingSteps = new StringBuffer(getProcessor().query("Separate the web browsing steps into individual steps  just give me steps without any additional text or brackets {"+ webBrowsingSteps+"}"));
                log.info("Separated steps: " + seperatedWebBrowsingSteps.toString());

                script.process(seperatedWebBrowsingSteps,seleniumCallBack);
                
                String screenshotPath = result.getLastScreenshotAsFile();
                log.info("=== Web Browsing Action Completed ===");
                log.info("Screenshot saved to: " + screenshotPath);
                
                return "Screenshot saved to: " + screenshotPath;
            } catch (AIProcessingException e) {
                log.severe("AI Processing error in browseWebAndReturnImage: " + e.getMessage());
                throw new RuntimeException("Failed to process web browsing steps: " + e.getMessage(), e);
            } catch (Exception e) {
                log.severe("Unexpected error in browseWebAndReturnImage: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Web browsing action failed: " + e.getMessage(), e);
            }
        }
        log.severe("Processor is null - cannot execute web browsing steps");
        return "processing issues - AI processor not available";
    }

    @Action(description = "take a screenshot of the current page without performing any actions")
    public String takeCurrentPageScreenshot() throws IOException {
        initializeIfNeeded();
        try {
            log.info("=== Taking Current Page Screenshot ===");
            
            // Get current URL for context
            String currentUrl = webDriver.getCurrentUrl();
            log.info("Current URL: " + currentUrl);
            
            // Use enhanced screenshot capture with SafeWebDriverWrapper
            byte[] screenshot = ScreenshotUtils.captureScreenshotWithFallbacks(webDriver, "Current page screenshot");
            
            if (screenshot != null) {
                // Save screenshot using CustomScriptResult
                CustomScriptResult result = new CustomScriptResult();
                result.addScreenshot(screenshot);
                String screenshotPath = result.getLastScreenshotAsFile();
                
                log.info("Current page screenshot saved to: " + screenshotPath);
                return "Screenshot saved to: " + screenshotPath;
            } else {
                log.severe("Failed to capture screenshot of current page");
                return "Failed to capture screenshot";
            }
            
        } catch (Exception e) {
            log.severe("Error taking current page screenshot: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Screenshot capture failed: " + e.getMessage(), e);
        }
    }

}
