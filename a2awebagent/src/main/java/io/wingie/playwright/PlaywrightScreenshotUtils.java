package io.wingie.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Playwright-based screenshot utility class
 * Replaces the Selenium-based ScreenshotUtils with simplified Playwright functionality
 */
@Slf4j
public class PlaywrightScreenshotUtils {
    
    private static final int DEFAULT_WAIT_TIMEOUT = 15000; // 15 seconds
    private static final int SCREENSHOT_WAIT_MS = 2000; // 2 seconds for page stability
    
    /**
     * Capture screenshot with enhanced waiting and error handling
     */
    public static byte[] captureScreenshotWithFallbacks(Page page, String context) {
        log.info("=== Starting Playwright Screenshot Capture ===");
        log.info("Context: {}", context);
        
        try {
            // Wait for page to be in a stable state
            waitForPageStability(page);
            
            // Take full page screenshot
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setType(ScreenshotType.PNG));
            
            log.info("Screenshot captured successfully - size: {} bytes", screenshot.length);
            return screenshot;
            
        } catch (Exception e) {
            log.error("Primary screenshot capture failed, trying fallback methods", e);
            return captureWithFallbacks(page, context);
        }
    }

    /**
     * Capture screenshot and save to file
     */
    public static String captureAndSaveScreenshot(Page page, String context) {
        try {
            // Wait for page stability
            waitForPageStability(page);
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = "playwright_" + timestamp + ".png";
            Path screenshotPath = Paths.get("/app/screenshots", filename);
            
            // Ensure screenshots directory exists
            Files.createDirectories(screenshotPath.getParent());
            
            // Take screenshot and save to file
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true)
                    .setType(ScreenshotType.PNG));
            
            log.info("Screenshot saved to: {} (Context: {})", screenshotPath, context);
            return screenshotPath.toString();
            
        } catch (Exception e) {
            log.error("Failed to capture and save screenshot: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Capture viewport-only screenshot (faster)
     */
    public static byte[] captureViewportScreenshot(Page page, String context) {
        log.info("Capturing viewport screenshot - Context: {}", context);
        
        try {
            waitForPageStability(page);
            
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(false)
                    .setType(ScreenshotType.PNG));
            
            log.info("Viewport screenshot captured - size: {} bytes", screenshot.length);
            return screenshot;
            
        } catch (Exception e) {
            log.error("Viewport screenshot capture failed", e);
            return null;
        }
    }

    /**
     * Capture screenshot of a specific element
     */
    public static byte[] captureElementScreenshot(Page page, String selector, String context) {
        log.info("Capturing element screenshot for selector: {} - Context: {}", selector, context);
        
        try {
            waitForPageStability(page);
            
            // Wait for element to be visible
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(DEFAULT_WAIT_TIMEOUT));
            
            // Take screenshot of the specific element
            byte[] screenshot = page.locator(selector).screenshot(new com.microsoft.playwright.Locator.ScreenshotOptions()
                    .setType(ScreenshotType.PNG));
            
            log.info("Element screenshot captured - size: {} bytes", screenshot.length);
            return screenshot;
            
        } catch (Exception e) {
            log.error("Element screenshot capture failed for selector: {}", selector, e);
            return null;
        }
    }

    /**
     * Wait for page to be in a stable state before taking screenshot
     */
    private static void waitForPageStability(Page page) {
        try {
            // Wait for network to be idle
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Additional wait for dynamic content
            Thread.sleep(SCREENSHOT_WAIT_MS);
            
            log.debug("Page stability check completed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Page stability wait interrupted");
        } catch (Exception e) {
            log.warn("Page stability check failed: {}", e.getMessage());
        }
    }

    /**
     * Fallback screenshot methods when primary capture fails
     */
    private static byte[] captureWithFallbacks(Page page, String context) {
        log.info("Attempting fallback screenshot methods");
        
        // Fallback 1: Try viewport screenshot instead of full page
        try {
            log.info("Fallback 1: Attempting viewport screenshot");
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(false)
                    .setType(ScreenshotType.PNG));
            
            log.info("Fallback viewport screenshot successful");
            return screenshot;
            
        } catch (Exception e) {
            log.warn("Fallback 1 failed", e);
        }

        // Fallback 2: Try with minimal options
        try {
            log.info("Fallback 2: Attempting minimal screenshot");
            byte[] screenshot = page.screenshot();
            
            log.info("Fallback minimal screenshot successful");
            return screenshot;
            
        } catch (Exception e) {
            log.warn("Fallback 2 failed", e);
        }

        // Fallback 3: Try after longer wait
        try {
            log.info("Fallback 3: Attempting screenshot after extended wait");
            Thread.sleep(5000); // 5 second wait
            
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setType(ScreenshotType.PNG));
            
            log.info("Fallback extended wait screenshot successful");
            return screenshot;
            
        } catch (Exception e) {
            log.error("All fallback methods failed", e);
        }

        log.error("=== ALL SCREENSHOT METHODS FAILED ===");
        return null;
    }

    /**
     * Check if page is ready for screenshot
     */
    public static boolean isPageReadyForScreenshot(Page page) {
        try {
            // Check if page is not closed and document is ready
            page.evaluate("document.readyState");
            return true;
        } catch (Exception e) {
            log.warn("Page readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get page dimensions for screenshot planning
     */
    public static String getPageDimensions(Page page) {
        try {
            Object dimensions = page.evaluate("() => ({ width: document.body.scrollWidth, height: document.body.scrollHeight })");
            return dimensions.toString();
        } catch (Exception e) {
            log.warn("Failed to get page dimensions: {}", e.getMessage());
            return "unknown";
        }
    }
}