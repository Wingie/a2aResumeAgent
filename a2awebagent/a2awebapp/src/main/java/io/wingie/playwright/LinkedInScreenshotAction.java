package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ImageContent;
import io.wingie.CustomScriptResult;
import io.wingie.service.TaskExecutionIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Base64;

/**
 * LinkedIn-specific screenshot tool for testing real-time browser monitoring.
 * Demonstrates SSE event broadcasting during browser actions.
 */
@Service
@Slf4j
@Agent(name = "linkedInScreenshot", description = "Search LinkedIn for a person and capture screenshot with real-time monitoring")
public class LinkedInScreenshotAction {

    @Autowired
    private Browser playwrightBrowser;

    @Autowired
    private BrowserContext playwrightContext;
    
    @Autowired
    private TaskExecutionIntegrationService taskExecutionService;
    
    @Autowired
    private BrowserEventHandler browserEventHandler;

    @Action(description = "Search for a person on LinkedIn and capture screenshot with real-time progress updates", 
            name = "linkedScreenShot")
    public ImageContent linkedScreenShot(
            @Parameter(description = "Name of the person to search for on LinkedIn") String personName) {
        
        log.info("🔍 Starting LinkedIn search for: {}", personName);
        
        // Use TaskExecutionIntegrationService for real-time tracking
        return (ImageContent) taskExecutionService.executeWithTracking(
            "linkedScreenShot", 
            "personName=" + personName,
            () -> executeLinkedInSearch(personName)
        );
    }

    private ImageContent executeLinkedInSearch(String personName) {
        Page page = null;
        CustomScriptResult result = new CustomScriptResult();
        
        try {
            // Step 1: Create page and navigate to LinkedIn
            page = playwrightContext.newPage();
            
            // Setup real-time browser event monitoring
            browserEventHandler.setupPageEventListeners(page);
            
            // Navigate with real-time tracking
            String taskId = getCurrentTaskId();
            if (taskId != null) {
                browserEventHandler.trackNavigation(page, "https://www.linkedin.com", taskId);
            } else {
                log.info("📱 Navigating to LinkedIn...");
                page.navigate("https://www.linkedin.com");
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            }
            
            // Step 2: Search for the person
            log.info("🔍 Searching for person: {}", personName);
            
            // Try to find search box (LinkedIn has various selectors)
            try {
                // LinkedIn search box selectors (multiple fallbacks)
                String[] searchSelectors = {
                    "input[placeholder*='Search']",
                    ".search-global-typeahead__input",
                    "input[aria-label*='Search']",
                    "input[data-test-selector='global-typeahead-search-input']"
                };
                
                boolean searchBoxFound = false;
                for (String selector : searchSelectors) {
                    if (page.locator(selector).count() > 0) {
                        // Use browser event handler for tracked input
                        if (taskId != null) {
                            browserEventHandler.trackTextInput(page, selector, personName, "LinkedIn search box", taskId);
                            
                            // Wait a moment then press Enter
                            page.waitForTimeout(500);
                            page.press(selector, "Enter");
                        } else {
                            page.fill(selector, personName);
                            page.press(selector, "Enter");
                        }
                        searchBoxFound = true;
                        break;
                    }
                }
                
                if (!searchBoxFound) {
                    log.warn("⚠️ Search box not found, trying URL-based search");
                    // Fallback: direct URL search with tracking
                    String searchUrl = "https://www.linkedin.com/search/results/people/?keywords=" + 
                                     java.net.URLEncoder.encode(personName, "UTF-8");
                    
                    if (taskId != null) {
                        browserEventHandler.trackNavigation(page, searchUrl, taskId);
                    } else {
                        page.navigate(searchUrl);
                    }
                }
                
                page.waitForLoadState(LoadState.NETWORKIDLE);
                
            } catch (Exception e) {
                log.warn("⚠️ LinkedIn search failed, continuing with current page: {}", e.getMessage());
            }
            
            // Step 3: Wait and capture screenshot
            log.info("📸 Capturing LinkedIn screenshot...");
            
            // Wait a moment for dynamic content to load
            try {
                page.waitForTimeout(2000); // 2 second wait
            } catch (Exception e) {
                log.debug("Timeout during wait, continuing...");
            }
            
            // Capture screenshot with real-time tracking
            if (taskId != null) {
                browserEventHandler.captureAndBroadcastScreenshot(page, "linkedin-search-result", taskId);
            }
            
            // Also use legacy screenshot capture for return value
            String screenshotPath = captureScreenshot(page, result);
            
            // Return the screenshot as ImageContent
            String base64Screenshot = result.getLastScreenshotAsBase64();
            if (base64Screenshot != null) {
                log.info("✅ LinkedIn screenshot completed successfully");
                return ImageContent.png(base64Screenshot);
            } else {
                log.warn("⚠️ No screenshot captured");
                return ImageContent.png(""); // Empty base64 data
            }
            
        } catch (Exception e) {
            log.error("❌ Error during LinkedIn screenshot", e);
            // Return error as text in base64 format
            String errorMessage = "Error during LinkedIn search: " + e.getMessage();
            String errorBase64 = Base64.getEncoder().encodeToString(errorMessage.getBytes());
            return ImageContent.png(errorBase64);
        } finally {
            if (page != null) {
                try {
                    page.close();
                } catch (Exception e) {
                    log.warn("Error closing page: {}", e.getMessage());
                }
            }
        }
    }

    private String captureScreenshot(Page page, CustomScriptResult result) {
        try {
            // Use screenshot directory configuration
            String screenshotDir = System.getProperty("app.storage.screenshots", "./screenshots");
            java.nio.file.Path baseDir = Paths.get(screenshotDir).toAbsolutePath();
            java.nio.file.Files.createDirectories(baseDir);
            
            String filename = "linkedin_search_" + System.currentTimeMillis() + ".png";
            java.nio.file.Path screenshotPath = baseDir.resolve(filename);
            
            // Capture full page screenshot
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotPath)
                .setFullPage(true));
            
            // Convert to base64 for result
            byte[] screenshotBytes = java.nio.file.Files.readAllBytes(screenshotPath);
            String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
            
            // Store in result
            result.addScreenshot(screenshotPath.toString(), base64Screenshot);
            log.info("📸 Screenshot captured: {}", screenshotPath);
            
            return screenshotPath.toString();
            
        } catch (Exception e) {
            log.error("❌ Error capturing screenshot", e);
            return null;
        }
    }
    
    /**
     * Helper method to get current task ID from context
     */
    private String getCurrentTaskId() {
        // Get from TaskContext if available, or generate a default
        try {
            return io.wingie.service.TaskContext.getCurrentTaskId();
        } catch (Exception e) {
            log.debug("No task context available, using default ID");
            return "linkedin-screenshot-" + System.currentTimeMillis();
        }
    }
}