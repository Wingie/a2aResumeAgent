package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import io.wingie.CustomScriptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Base64;

/**
 * Playwright-based web browsing service
 * Replaces the problematic Selenium-based WebBrowsingAction
 */
@Service
@Slf4j
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching using Playwright")
public class PlaywrightWebBrowsingAction {

    @Autowired
    private Browser playwrightBrowser;

    @Autowired
    private BrowserContext playwrightContext;

    @Autowired(required = false)
    private AIProcessor aiProcessor;

    @Action(description = "perform actions on the web with Playwright and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) {
        
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

    @Action(description = "perform actions on the web with Playwright and return image")
    public String browseWebAndReturnImage(String webBrowsingSteps) {
        
        log.info("Starting Playwright web browsing with image capture: {}", webBrowsingSteps);
        
        try {
            CustomScriptResult result = new CustomScriptResult();
            executeWebBrowsingSteps(webBrowsingSteps, result);
            return result.getLastScreenshotAsBase64() != null ? 
                   result.getLastScreenshotAsBase64() : 
                   "No screenshot captured";
        } catch (Exception e) {
            log.error("Error during Playwright web browsing with image", e);
            return "Error during web browsing: " + e.getMessage();
        }
    }

    private void executeWebBrowsingSteps(String webBrowsingSteps, CustomScriptResult result) {
        Page page = null;
        
        try {
            // Create a new page
            page = playwrightContext.newPage();
            log.info("Created new Playwright page");

            if (aiProcessor != null) {
                // Use AI processor to break down steps
                String separatedSteps = aiProcessor.query(
                    "Separate the web browsing steps into individual steps just give me steps without any additional text or bracket. " +
                    "MOST IMP - 1) make sure each step can be processed by Playwright browse, " +
                    "2) urls should always start with http or https, " +
                    "3) Do not give steps such as 'open the browser' as i am using headless browser {" + webBrowsingSteps + "}"
                );
                
                log.info("AI-processed steps: {}", separatedSteps);
                
                // Execute each step
                executeIndividualSteps(page, separatedSteps, result);
            } else {
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
        // Simple fallback execution
        if (steps.toLowerCase().contains("google.com")) {
            page.navigate("https://www.google.com");
            result.addData("Navigated to Google");
            captureScreenshot(page, result);
        } else {
            result.addData("Executed web browsing steps: " + steps);
            captureScreenshot(page, result);
        }
    }

    private void captureScreenshot(Page page, CustomScriptResult result) {
        try {
            String screenshotPath = "/app/screenshots/playwright_" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
            
            // Convert to base64 for result
            byte[] screenshotBytes = java.nio.file.Files.readAllBytes(Paths.get(screenshotPath));
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            result.addScreenshot(screenshotPath, base64Screenshot);
            log.info("Screenshot captured: {}", screenshotPath);
            
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