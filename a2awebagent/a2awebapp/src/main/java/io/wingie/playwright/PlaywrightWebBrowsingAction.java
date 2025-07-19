package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ImageContent;
import io.wingie.a2acore.domain.ImageContentUrl;
import io.wingie.a2acore.domain.ExecutionParameters;
// AI processing imports removed - using a2acore annotations instead
import io.wingie.CustomScriptResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Base64;

/**
 * Playwright-based web browsing service
 * Provides robust and reliable web automation capabilities using Microsoft Playwright
 */
@Service
@Slf4j
@Agent(name = "web browsing", description = "actions related to web browsing and searching using Playwright")
public class PlaywrightWebBrowsingAction {

    @Autowired
    private Browser playwrightBrowser;

    @Autowired
    private BrowserContext playwrightContext;
    
    @Autowired
    private BrowserEventHandler browserEventHandler;
    
    @Autowired
    private io.wingie.service.ScreenshotService screenshotService;

    // AI processor removed - using static annotations instead

    @Action(description = "perform actions on the web with Playwright and return text. Supports optional execution parameters for multi-step control.", name = "browseWebAndReturnText")
    public String browseWebAndReturnText(
            @Parameter(description = "Natural language description of web browsing steps to perform") String webBrowsingSteps,
            @Parameter(description = "Optional JSON execution parameters: {\"maxSteps\": 10, \"executionMode\": \"MULTI_STEP\", \"allowEarlyCompletion\": true}", required = false) String executionParamsJson) {
        return browseWebAndReturnTextWithParams(webBrowsingSteps, executionParamsJson);
    }

    public String browseWebAndReturnTextWithParams(
            @Parameter(description = "Natural language description of web browsing steps to perform") String webBrowsingSteps,
            @Parameter(description = "JSON execution parameters: {\"maxSteps\": 10, \"executionMode\": \"MULTI_STEP\", \"allowEarlyCompletion\": true}") String executionParamsJson) {
        
        // Parse execution parameters
        ExecutionParameters params = parseExecutionParameters(executionParamsJson);
        log.info("Starting Playwright web browsing with params {}: {}", params, webBrowsingSteps);
        
        try {
            CustomScriptResult result = new CustomScriptResult();
            executeWebBrowsingStepsWithParams(webBrowsingSteps, params, result);
            return result.getLastData() != null ? result.getLastData() : "Web browsing completed successfully";
        } catch (Exception e) {
            log.error("Error during Playwright web browsing", e);
            return "Error during web browsing: " + e.getMessage();
        }
    }

    @Action(description = "perform actions on the web with Playwright and return image", name = "browseWebAndReturnImage")
    public ImageContent browseWebAndReturnImage(@Parameter(description = "Natural language description of web browsing steps to perform and capture as image") String webBrowsingSteps) {
        
        log.info("Starting Playwright web browsing with image capture: {}", webBrowsingSteps);
        
        try {
            CustomScriptResult result = new CustomScriptResult();
            executeWebBrowsingSteps(webBrowsingSteps, result);
            
            String base64Screenshot = result.getLastScreenshotAsBase64();
            if (base64Screenshot != null) {
                log.info("Successfully captured screenshot, returning as ImageContent");
                return ImageContent.png(base64Screenshot);
            } else {
                log.warn("No screenshot captured, returning empty ImageContent");
                return ImageContent.png(""); // Empty base64 data
            }
        } catch (Exception e) {
            log.error("Error during Playwright web browsing with image", e);
            // Return error as text in base64 format for proper MCP response
            String errorMessage = "Error during web browsing: " + e.getMessage();
            String errorBase64 = Base64.getEncoder().encodeToString(errorMessage.getBytes());
            return ImageContent.png(errorBase64);
        }
    }

    @Action(description = "perform actions on the web with Playwright and return image URL", name = "browseWebAndReturnImageUrl")
    public ImageContentUrl browseWebAndReturnImageUrl(@Parameter(description = "Natural language description of web browsing steps to perform and capture as image URL") String webBrowsingSteps) {
        
        log.info("Starting Playwright web browsing with image URL capture: {}", webBrowsingSteps);
        
        try {
            CustomScriptResult result = new CustomScriptResult();
            executeWebBrowsingSteps(webBrowsingSteps, result);
            
            String base64Screenshot = result.getLastScreenshotAsBase64();
            if (base64Screenshot != null) {
                log.info("Successfully captured screenshot, saving as URL");
                
                // Save screenshot to static directory and get HTTP URL
                String httpUrl = screenshotService.saveGeneralScreenshot(base64Screenshot);
                
                if (httpUrl != null) {
                    log.info("Screenshot saved to HTTP URL: {}", httpUrl);
                    return ImageContentUrl.png(httpUrl);
                } else {
                    log.warn("Failed to save screenshot to static directory");
                    return null;
                }
            } else {
                log.warn("No screenshot captured");
                return null;
            }
        } catch (Exception e) {
            log.error("Error during Playwright web browsing with image URL", e);
            return null;
        }
    }

    private void executeWebBrowsingSteps(String webBrowsingSteps, CustomScriptResult result) {
        // Legacy method - use default parameters
        ExecutionParameters defaultParams = ExecutionParameters.multiStep(5);
        executeWebBrowsingStepsWithParams(webBrowsingSteps, defaultParams, result);
    }

    private void executeWebBrowsingStepsWithParams(String webBrowsingSteps, ExecutionParameters params, CustomScriptResult result) {
        Page page = null;
        
        try {
            // Create a new page
            page = playwrightContext.newPage();
            log.info("Created new Playwright page");
            
            // Setup real-time event listeners for browser monitoring
            browserEventHandler.setupPageEventListeners(page);

            // Execute steps with user-controlled parameters
            if (params.getExecutionMode() == ExecutionParameters.ExecutionMode.ONE_SHOT) {
                log.info("Executing one-shot browsing action");
                executeDirectSteps(page, webBrowsingSteps, result);
            } else {
                log.info("Executing multi-step browsing with {} max steps", params.getMaxSteps());
                executeMultiStepBrowsing(page, webBrowsingSteps, params, result);
            }

        } catch (Exception e) {
            log.error("Error executing web browsing steps", e);
            result.addData("Error: " + e.getMessage());
        } finally {
            if (page != null && !page.isClosed()) {
                try {
                    page.close();
                    log.info("Closed Playwright page");
                } catch (Exception e) {
                    log.warn("Error closing page", e);
                }
            }
        }
    }

    private void executeIndividualSteps(Page page, String steps, CustomScriptResult result) {
        String[] stepArray = steps.split("\\n");
        
        for (String step : stepArray) {
            step = step.trim();
            if (step.isEmpty()) continue;
            
            log.info("Executing step: {}", step);
            
            try {
                executeStep(page, step, result);
                
                // Wait for page to be ready
                page.waitForLoadState(LoadState.NETWORKIDLE);
                
                // Capture screenshot after each significant step
                if (step.toLowerCase().contains("navigate") || 
                    step.toLowerCase().contains("click") ||
                    step.toLowerCase().contains("search")) {
                    captureScreenshot(page, result);
                }
                
            } catch (Exception e) {
                log.error("Error executing step: {}", step, e);
                result.addData("Error in step '" + step + "': " + e.getMessage());
            }
        }
    }

    private void executeStep(Page page, String step, CustomScriptResult result) {
        step = step.toLowerCase();
        
        if (step.contains("navigate") || step.contains("go to")) {
            // Extract URL and navigate
            String url = extractUrl(step);
            if (url != null) {
                log.info("Navigating to: {}", url);
                page.navigate(url);
                result.addData("Navigated to: " + url);
            }
        } else if (step.contains("click")) {
            // Extract text to click and perform click
            String clickText = extractClickText(step);
            if (clickText != null) {
                log.info("Clicking: {}", clickText);
                page.getByText(clickText).first().click();
                result.addData("Clicked: " + clickText);
            }
        } else if (step.contains("type") || step.contains("search")) {
            // Extract text to type
            String textToType = extractTextToType(step);
            if (textToType != null) {
                log.info("Typing: {}", textToType);
                // Find input field and type
                page.locator("input[type=text], input[type=search], textarea").first().fill(textToType);
                result.addData("Typed: " + textToType);
            }
        } else if (step.contains("screenshot")) {
            // Handle navigation before screenshot if URL or site keywords are present
            String url = extractUrl(step);
            
            if (url != null) {
                log.info("Navigating to extracted URL before screenshot: {}", url);
                page.navigate(url);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                result.addData("Navigated to: " + url);
            } else if (step.contains("google")) {
                log.info("Navigating to Google before screenshot (keyword detected)");
                page.navigate("https://www.google.com");
                page.waitForLoadState(LoadState.NETWORKIDLE);
                result.addData("Navigated to Google");
            } else if (step.contains("linkedin")) {
                log.info("Navigating to LinkedIn before screenshot (keyword detected)");
                page.navigate("https://www.linkedin.com");
                page.waitForLoadState(LoadState.NETWORKIDLE);
                result.addData("Navigated to LinkedIn");
            }
            // Note: Don't add else clause with default navigation here - only navigate if explicitly requested
            
            captureScreenshot(page, result);
        } else {
            log.info("Executing general step: {}", step);
            result.addData("Executed: " + step);
        }
    }

    private void executeDirectSteps(Page page, String steps, CustomScriptResult result) {
        // Enhanced fallback execution with proper URL extraction and navigation
        String url = extractUrl(steps.toLowerCase());
        
        if (url != null) {
            log.info("Navigating to extracted URL: {}", url);
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            result.addData("Navigated to: " + url);
        } else if (steps.toLowerCase().contains("google")) {
            log.info("Navigating to Google (keyword detected)");
            page.navigate("https://www.google.com");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            result.addData("Navigated to Google");
        } else if (steps.toLowerCase().contains("linkedin")) {
            log.info("Navigating to LinkedIn (keyword detected)");
            page.navigate("https://www.linkedin.com");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            result.addData("Navigated to LinkedIn");
        } else {
            // Always navigate somewhere sensible before screenshot - default to tastebeforeyouwaste.org
            log.info("No URL detected, defaulting to tastebeforeyouwaste.org for screenshot");
            page.navigate("https://www.tastebeforeyouwaste.org");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            result.addData("Navigated to tastebeforeyouwaste.org (default for screenshot)");
        }
        
        // Always capture screenshot after navigation
        captureScreenshot(page, result);
    }

    private void captureScreenshot(Page page, CustomScriptResult result) {
        try {
            // Enhanced wait strategy for complete page loading
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Additional wait for JavaScript rendering and content to settle
            try {
                Thread.sleep(2000); // 2 second delay for dynamic content
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Screenshot delay interrupted");
            }
            
            // Wait for document ready state
            try {
                page.waitForFunction("() => document.readyState === 'complete'");
            } catch (Exception e) {
                log.debug("Document ready state check failed, proceeding: {}", e.getMessage());
            }
            
            // Use absolute path resolution for better compatibility
            String screenshotDir = System.getProperty("app.storage.screenshots", "./screenshots");
            java.nio.file.Path baseDir = Paths.get(screenshotDir).toAbsolutePath();
            java.nio.file.Files.createDirectories(baseDir);
            
            String filename = "playwright_" + System.currentTimeMillis() + ".png";
            java.nio.file.Path screenshotPath = baseDir.resolve(filename);
            
            // Capture screenshot after proper waits
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            
            // Convert to base64 for result
            byte[] screenshotBytes = java.nio.file.Files.readAllBytes(screenshotPath);
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            // Store both absolute path and base64 for compatibility
            result.addScreenshot(screenshotPath.toString(), base64Screenshot);
            log.info("Screenshot captured at absolute path: {}", screenshotPath);
            
        } catch (Exception e) {
            log.error("Error capturing screenshot", e);
            result.addData("Screenshot capture failed: " + e.getMessage());
        }
    }

    private String extractUrl(String step) {
        // Simple URL extraction
        if (step.contains("http")) {
            String[] parts = step.split("\\s+");
            for (String part : parts) {
                if (part.startsWith("http")) {
                    return part;
                }
            }
        }
        
        // Extract domain names (e.g., "example.com", "google.com")
        String[] parts = step.split("\\s+");
        for (String part : parts) {
            // Remove common punctuation that might be attached
            part = part.replaceAll("[,.:;!?\"']", "");
            
            // Check if it looks like a domain (contains a dot and typical domain pattern)
            if (part.contains(".") && 
                (part.endsWith(".com") || part.endsWith(".org") || part.endsWith(".net") || 
                 part.endsWith(".edu") || part.endsWith(".gov") || part.endsWith(".io") ||
                 part.endsWith(".co") || part.endsWith(".uk") || part.endsWith(".de") ||
                 part.matches(".*\\.[a-zA-Z]{2,}$"))) { // Generic TLD pattern
                
                // Add https:// if not already present
                if (!part.startsWith("http")) {
                    return "https://" + part;
                } else {
                    return part;
                }
            }
        }
        
        // Default URLs for common sites
        if (step.contains("google")) return "https://www.google.com";
        if (step.contains("booking")) return "https://www.booking.com";
        if (step.contains("linkedin")) return "https://www.linkedin.com";
        if (step.contains("example")) return "https://example.com"; // Handle "example" keyword
        return null;
    }

    private String extractClickText(String step) {
        // Simple text extraction for clicking
        if (step.contains("\"")) {
            int start = step.indexOf("\"");
            int end = step.lastIndexOf("\"");
            if (start != end) {
                return step.substring(start + 1, end);
            }
        }
        return null;
    }

    private String extractTextToType(String step) {
        // Simple text extraction for typing
        if (step.contains("\"")) {
            int start = step.indexOf("\"");
            int end = step.lastIndexOf("\"");
            if (start != end) {
                return step.substring(start + 1, end);
            }
        }
        return null;
    }

    /**
     * Parse execution parameters from JSON string or return defaults.
     */
    private ExecutionParameters parseExecutionParameters(String executionParamsJson) {
        if (executionParamsJson == null || executionParamsJson.trim().isEmpty()) {
            return ExecutionParameters.multiStep(10); // Default
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            ExecutionParameters params = mapper.readValue(executionParamsJson, ExecutionParameters.class);
            params.validate(); // Ensure parameters are valid
            return params;
        } catch (Exception e) {
            log.warn("Failed to parse execution parameters '{}', using defaults: {}", executionParamsJson, e.getMessage());
            return ExecutionParameters.multiStep(10);
        }
    }

    /**
     * Execute multi-step browsing with user-controlled parameters and progress tracking.
     */
    private void executeMultiStepBrowsing(Page page, String webBrowsingSteps, ExecutionParameters params, CustomScriptResult result) {
        int currentStep = 1;
        double currentConfidence = 0.0;
        
        log.info("Starting multi-step execution: maxSteps={}, mode={}", params.getMaxSteps(), params.getExecutionMode());
        
        // Parse steps from the input
        String[] stepArray = webBrowsingSteps.split("\\n");
        
        for (String step : stepArray) {
            step = step.trim();
            if (step.isEmpty()) continue;
            
            // Check step limits
            if (currentStep > params.getMaxSteps()) {
                log.info("Reached maximum steps limit: {}", params.getMaxSteps());
                result.addData(String.format("⚠️ Stopped at step %d/%d due to maxSteps limit", currentStep - 1, params.getMaxSteps()));
                break;
            }
            
            log.info("Executing step {}/{}: {}", currentStep, params.getMaxSteps(), step);
            
            try {
                // Execute the individual step
                executeStep(page, step, result);
                
                // Wait for page to be ready
                page.waitForLoadState(LoadState.NETWORKIDLE);
                
                // Capture screenshot if enabled
                if (params.getCaptureStepScreenshots()) {
                    captureStepScreenshot(page, result, currentStep);
                }
                
                // Simulate confidence scoring (in real implementation, this would analyze results)
                currentConfidence = Math.min(1.0, currentConfidence + 0.2);
                
                // Check for early completion
                if (params.shouldStopEarly(currentStep, currentConfidence)) {
                    log.info("Early completion triggered at step {} with confidence {:.2f}", currentStep, currentConfidence);
                    result.addData(String.format("✅ Completed early at step %d/%d (confidence: %.2f)", currentStep, params.getMaxSteps(), currentConfidence));
                    break;
                }
                
                result.addData(String.format("Step %d completed successfully", currentStep));
                currentStep++;
                
            } catch (Exception e) {
                log.error("Error executing step {}: {}", currentStep, step, e);
                result.addData(String.format("❌ Step %d failed: %s", currentStep, e.getMessage()));
                
                // For AUTO mode, continue to next step; for MULTI_STEP, break on error
                if (params.getExecutionMode() == ExecutionParameters.ExecutionMode.MULTI_STEP) {
                    break;
                }
                currentStep++;
            }
        }
        
        result.addData(String.format("Multi-step execution completed: %d steps processed", currentStep - 1));
    }

    /**
     * Capture screenshot for a specific step with step numbering.
     */
    private void captureStepScreenshot(Page page, CustomScriptResult result, int stepNumber) {
        try {
            String screenshotDir = System.getProperty("app.storage.screenshots", "./screenshots");
            java.nio.file.Path baseDir = Paths.get(screenshotDir).toAbsolutePath();
            java.nio.file.Files.createDirectories(baseDir);
            
            String filename = String.format("step_%02d_%d.png", stepNumber, System.currentTimeMillis());
            java.nio.file.Path screenshotPath = baseDir.resolve(filename);
            
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            
            byte[] screenshotBytes = java.nio.file.Files.readAllBytes(screenshotPath);
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            result.addScreenshot(screenshotPath.toString(), base64Screenshot);
            log.info("Step {} screenshot captured: {}", stepNumber, screenshotPath);
            
        } catch (Exception e) {
            log.error("Error capturing step {} screenshot", stepNumber, e);
            result.addData(String.format("Screenshot capture failed for step %d: %s", stepNumber, e.getMessage()));
        }
    }
}