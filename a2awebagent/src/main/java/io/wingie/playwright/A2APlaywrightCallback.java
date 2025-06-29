package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import io.wingie.CustomScriptResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Playwright callback implementation for A2A web automation
 * Replaces A2ASeleniumCallBack with Playwright functionality
 */
@Slf4j
public class A2APlaywrightCallback implements PlaywrightCallback {

    private static final int SCREENSHOT_WAIT_MS = 2000; // 2 seconds for rendering
    private static final int MAX_RETRIES = 3;

    private final CustomScriptResult customResult;
    private final AIProcessor processor;
    private final String context;
    private final StringBuffer allSteps;

    public A2APlaywrightCallback(String context, StringBuffer allSteps, CustomScriptResult customResult, AIProcessor processor) {
        this.customResult = customResult;
        this.processor = processor;
        this.context = context;
        this.allSteps = allSteps;
    }

    @Override
    public boolean beforeWebAction(String lineToBeProcessed, Browser browser, BrowserContext context) {
        if (lineToBeProcessed.contains("browser")) {
            return false; // Skip browser-related commands
        }

        log.info("Processing line (before): {}", lineToBeProcessed);
        
        try {
            if (context.pages().isEmpty()) {
                log.debug("No pages in context - may be first action");
                return true;
            }

            Page page = context.pages().get(0);
            
            // Wait briefly for page to be stable
            try {
                Thread.sleep(SCREENSHOT_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Capture page content and screenshot
            String html = page.content();
            customResult.addBeforeHtml(html);
            
            byte[] screenshot = takeEnhancedScreenshot(page, "BEFORE action: " + lineToBeProcessed);
            if (screenshot != null) {
                customResult.addScreenshot(screenshot);
                log.debug("Screenshot captured before action");
            }

        } catch (Exception e) {
            log.warn("Error in beforeWebAction: {}", e.getMessage());
        }
        
        return true;
    }

    @Override
    public void afterWebAction(String lineProcessed, Browser browser, BrowserContext context) {
        log.info("Processed line (after): {}", lineProcessed);
        
        try {
            if (context.pages().isEmpty()) {
                log.debug("No pages in context after action");
                return;
            }

            Page page = context.pages().get(0);
            
            // Wait briefly for page changes to complete
            try {
                Thread.sleep(SCREENSHOT_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Capture updated page content and screenshot
            String html = page.content();
            customResult.addAfterHtml(html);
            
            byte[] screenshot = takeEnhancedScreenshot(page, "AFTER action: " + lineProcessed);
            if (screenshot != null) {
                customResult.addScreenshot(screenshot);
                log.debug("Screenshot captured after action");
            }

            // Add action result to data
            customResult.addData("Completed action: " + lineProcessed);

        } catch (Exception e) {
            log.warn("Error in afterWebAction: {}", e.getMessage());
        }
    }

    @Override
    public String handleError(String line, String errorMessage, Browser browser, BrowserContext context, int retryCount) {
        log.error("Error processing line: {} | Error: {} | Retry: {}", line, errorMessage, retryCount);
        
        if (retryCount > MAX_RETRIES) {
            log.error("Max retries ({}) reached for line: {}", MAX_RETRIES, line);
            customResult.addData("FAILED after " + MAX_RETRIES + " retries: " + line + " - Error: " + errorMessage);
            return null;
        }

        if (processor != null) {
            try {
                String prompt = "You are an automated Playwright web script correction assistant. " +
                        "Your task is to correct the following line of code: " + line +
                        ". The error message is: " + errorMessage +
                        ". Please provide a corrected version of the line that will work with Playwright. " +
                        "Overall context is: " + this.context + 
                        " the steps were broken down to: " + allSteps.toString();
                
                String correctedLine = processor.query(prompt);
                log.info("AI suggested correction for '{}': '{}'", line, correctedLine);
                
                customResult.addData("AI correction attempt " + retryCount + " for: " + line + " -> " + correctedLine);
                return correctedLine;
                
            } catch (AIProcessingException e) {
                log.error("Failed to query AI for correction", e);
                customResult.addData("AI correction failed for: " + line + " - " + e.getMessage());
            }
        } else {
            log.warn("No AI processor available for error correction");
        }

        return null;
    }

    /**
     * Take an enhanced screenshot with proper waiting and error handling
     */
    private byte[] takeEnhancedScreenshot(Page page, String description) {
        try {
            // Wait for page to be in a ready state
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            
            // Take full page screenshot
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setType(com.microsoft.playwright.options.ScreenshotType.PNG));
            
            // Save screenshot to file for debugging
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = "/app/screenshots/playwright_" + timestamp + ".png";
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get(filename))
                    .setFullPage(true));
            
            log.debug("Enhanced screenshot captured: {} - {}", description, filename);
            return screenshot;
            
        } catch (Exception e) {
            log.error("Failed to capture enhanced screenshot: {}", e.getMessage());
            return null;
        }
    }
}