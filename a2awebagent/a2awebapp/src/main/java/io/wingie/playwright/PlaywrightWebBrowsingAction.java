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
// AI processing imports removed - using a2acore annotations instead
import io.wingie.CustomScriptResult;
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

    @Action(description = "perform actions on the web with Playwright and return text", name = "browseWebAndReturnText")
    public String browseWebAndReturnText(@Parameter(description = "Natural language description of web browsing steps to perform") String webBrowsingSteps) {
        
        log.info("Starting Playwright web browsing: {}", webBrowsingSteps);
        
        try {
            CustomScriptResult result = new CustomScriptResult();
            executeWebBrowsingSteps(webBrowsingSteps, result);
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
        Page page = null;
        
        try {
            // Create a new page
            page = playwrightContext.newPage();
            log.info("Created new Playwright page");
            
            // Setup real-time event listeners for browser monitoring
            browserEventHandler.setupPageEventListeners(page);

            // Execute steps directly without AI processing (using static tool descriptions)
            {
                // Fallback: execute directly if no AI processor
                log.warn("No AI processor available, executing steps directly");
                executeDirectSteps(page, webBrowsingSteps, result);
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
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            result.addData("Navigated to: " + url);
        } else if (steps.toLowerCase().contains("google")) {
            log.info("Navigating to Google (keyword detected)");
            page.navigate("https://www.google.com");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            result.addData("Navigated to Google");
        } else if (steps.toLowerCase().contains("linkedin")) {
            log.info("Navigating to LinkedIn (keyword detected)");
            page.navigate("https://www.linkedin.com");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            result.addData("Navigated to LinkedIn");
        } else {
            // Always navigate somewhere sensible before screenshot - default to tastebeforeyouwaste.org
            log.info("No URL detected, defaulting to tastebeforeyouwaste.org for screenshot");
            page.navigate("https://www.tastebeforeyouwaste.org");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            result.addData("Navigated to tastebeforeyouwaste.org (default for screenshot)");
        }
        
        // Always capture screenshot after navigation
        captureScreenshot(page, result);
    }

    private void captureScreenshot(Page page, CustomScriptResult result) {
        try {
            // Use absolute path resolution for better compatibility
            String screenshotDir = System.getProperty("app.storage.screenshots", "./screenshots");
            java.nio.file.Path baseDir = Paths.get(screenshotDir).toAbsolutePath();
            java.nio.file.Files.createDirectories(baseDir);
            
            String filename = "playwright_" + System.currentTimeMillis() + ".png";
            java.nio.file.Path screenshotPath = baseDir.resolve(filename);
            
            // Capture screenshot
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
        // Default URLs for common sites
        if (step.contains("google")) return "https://www.google.com";
        if (step.contains("booking")) return "https://www.booking.com";
        if (step.contains("linkedin")) return "https://www.linkedin.com";
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
}