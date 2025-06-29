package io.wingie;

import io.wingie.utils.SafeWebDriverWrapper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Utility class for enhanced screenshot capture with various fallback strategies
 */
public class ScreenshotUtils {
    
    private static final Logger log = Logger.getLogger(ScreenshotUtils.class.getName());
    private static final int DEFAULT_WAIT_TIMEOUT = 15; // increased timeout
    private static final int LONG_WAIT_MS = 3000; // 3 seconds for complex pages
    
    /**
     * Capture screenshot with multiple fallback strategies
     */
    public static byte[] captureScreenshotWithFallbacks(WebDriver driver, String context) {
        log.info("=== Starting Enhanced Screenshot Capture ===");
        log.info("Context: " + context);
        
        // Wrap driver in SafeWebDriverWrapper to handle proxy casting issues
        SafeWebDriverWrapper safeDriver = SafeWebDriverWrapper.wrap(driver);
        log.info("Driver capabilities - JavaScript: " + safeDriver.supportsJavaScript() + 
                ", Screenshots: " + safeDriver.supportsScreenshots());
        
        // Strategy 1: Standard enhanced screenshot
        byte[] screenshot = captureEnhancedScreenshot(safeDriver);
        if (isScreenshotValid(screenshot)) {
            log.info("Standard enhanced screenshot successful");
            return screenshot;
        }
        
        // Strategy 2: Force refresh and retry
        log.warning("Standard screenshot failed, trying refresh strategy");
        screenshot = captureWithRefresh(safeDriver);
        if (isScreenshotValid(screenshot)) {
            log.info("Refresh strategy screenshot successful");
            return screenshot;
        }
        
        // Strategy 3: Viewport manipulation
        log.warning("Refresh strategy failed, trying viewport manipulation");
        screenshot = captureWithViewportManipulation(safeDriver);
        if (isScreenshotValid(screenshot)) {
            log.info("Viewport manipulation screenshot successful");
            return screenshot;
        }
        
        // Strategy 4: Basic fallback
        log.warning("All enhanced strategies failed, using basic fallback");
        screenshot = captureBasicScreenshot(safeDriver);
        log.warning("Fallback screenshot captured, size: " + (screenshot != null ? screenshot.length : 0) + " bytes");
        
        return screenshot;
    }
    
    /**
     * Enhanced screenshot with comprehensive waiting and validation
     */
    private static byte[] captureEnhancedScreenshot(SafeWebDriverWrapper driver) {
        try {
            // Wait for document ready
            waitForDocumentReady(driver);
            
            // Wait for network idle (if possible)
            waitForNetworkIdle(driver);
            
            // Execute rendering optimizations
            optimizeForScreenshot(driver);
            
            // Additional wait for rendering
            Thread.sleep(LONG_WAIT_MS);
            
            // Capture screenshot using safe wrapper
            return driver.getScreenshotAs(OutputType.BYTES);
            
        } catch (Exception e) {
            log.warning("Enhanced screenshot failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Screenshot with page refresh
     */
    private static byte[] captureWithRefresh(SafeWebDriverWrapper driver) {
        try {
            String currentUrl = driver.getCurrentUrl();
            log.info("Refreshing page: " + currentUrl);
            
            driver.navigate().refresh();
            
            // Wait longer after refresh
            waitForDocumentReady(driver);
            Thread.sleep(LONG_WAIT_MS);
            
            // Try to scroll to trigger rendering (if JavaScript is supported)
            if (driver.supportsJavaScript()) {
                driver.executeScript("window.scrollTo(0, 100); window.scrollTo(0, 0);");
                Thread.sleep(1000);
            }
            
            return driver.getScreenshotAs(OutputType.BYTES);
            
        } catch (Exception e) {
            log.warning("Refresh screenshot failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Screenshot with viewport manipulation
     */
    private static byte[] captureWithViewportManipulation(SafeWebDriverWrapper driver) {
        try {
            if (!driver.supportsJavaScript()) {
                log.warning("JavaScript not supported, skipping viewport manipulation");
                return driver.getScreenshotAs(OutputType.BYTES);
            }
            
            // Get original dimensions
            Long originalWidth = (Long) driver.executeScript("return window.innerWidth;");
            Long originalHeight = (Long) driver.executeScript("return window.innerHeight;");
            
            log.info("Original viewport: " + originalWidth + "x" + originalHeight);
            
            // Resize viewport to force re-rendering
            driver.manage().window().setSize(new Dimension(1920, 1080));
            Thread.sleep(1000);
            
            // Force layout recalculation
            driver.executeScript(
                "document.body.style.transform = 'scale(1.01)';" +
                "document.body.offsetHeight;" +
                "document.body.style.transform = 'scale(1)';" +
                "document.body.offsetHeight;"
            );
            
            // Multiple render frames
            for (int i = 0; i < 3; i++) {
                driver.executeScript("requestAnimationFrame(() => {});");
                Thread.sleep(100);
            }
            
            Thread.sleep(2000);
            
            return driver.getScreenshotAs(OutputType.BYTES);
            
        } catch (Exception e) {
            log.warning("Viewport manipulation screenshot failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Basic screenshot as final fallback
     */
    private static byte[] captureBasicScreenshot(SafeWebDriverWrapper driver) {
        try {
            return driver.getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.severe("Even basic screenshot failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Wait for document to be ready
     */
    private static void waitForDocumentReady(SafeWebDriverWrapper driver) {
        try {
            if (!driver.supportsJavaScript()) {
                log.fine("JavaScript not supported, skipping document ready check");
                Thread.sleep(2000); // Basic fallback wait
                return;
            }
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_TIMEOUT));
            
            wait.until(webDriver -> {
                SafeWebDriverWrapper safeWrapper = SafeWebDriverWrapper.wrap(webDriver);
                String readyState = safeWrapper.executeScript("return document.readyState").toString();
                return "complete".equals(readyState);
            });
            
            log.fine("Document ready state: complete");
            
        } catch (Exception e) {
            log.warning("Error waiting for document ready: " + e.getMessage());
        }
    }
    
    /**
     * Wait for network to be idle (basic implementation)
     */
    private static void waitForNetworkIdle(SafeWebDriverWrapper driver) {
        try {
            // Wait for any pending requests to complete
            Thread.sleep(2000);
            
            if (!driver.supportsJavaScript()) {
                log.fine("JavaScript not supported, using basic wait for network idle");
                Thread.sleep(3000);
                return;
            }
            
            // Wait for images to load
            Boolean imagesLoaded = (Boolean) driver.executeScript(
                "return Array.from(document.images).every(img => img.complete);"
            );
            
            if (!imagesLoaded) {
                log.info("Waiting for images to load...");
                Thread.sleep(3000);
            }
            
        } catch (Exception e) {
            log.fine("Network idle check failed: " + e.getMessage());
        }
    }
    
    /**
     * Execute JavaScript optimizations for screenshot
     */
    private static void optimizeForScreenshot(SafeWebDriverWrapper driver) {
        try {
            if (!driver.supportsJavaScript()) {
                log.fine("JavaScript not supported, skipping screenshot optimizations");
                return;
            }
            
            // Disable animations for consistent screenshots
            driver.executeScript(
                "var style = document.createElement('style');" +
                "style.textContent = '*, *::before, *::after { " +
                "  animation-duration: 0.01ms !important; " +
                "  animation-delay: 0.01ms !important; " +
                "  transition-duration: 0.01ms !important; " +
                "  transition-delay: 0.01ms !important; " +
                "}';" +
                "document.head.appendChild(style);"
            );
            
            // Force repaint
            driver.executeScript("document.body.style.transform = 'translateZ(0)';");
            driver.executeScript("document.body.offsetHeight;");
            driver.executeScript("document.body.style.transform = '';");
            
            // Ensure scroll position is at top
            driver.executeScript("window.scrollTo(0, 0);");
            
            log.fine("Screenshot optimizations applied");
            
        } catch (Exception e) {
            log.warning("Error applying screenshot optimizations: " + e.getMessage());
        }
    }
    
    /**
     * Validate if screenshot is meaningful (not blank/white)
     */
    private static boolean isScreenshotValid(byte[] screenshot) {
        if (screenshot == null || screenshot.length < 1000) {
            log.warning("Screenshot validation failed: null or too small (" + 
                       (screenshot != null ? screenshot.length : 0) + " bytes)");
            return false;
        }
        
        // Basic size check - screenshots should be at least a few KB for meaningful content
        if (screenshot.length < 10000) {
            log.warning("Screenshot suspiciously small: " + screenshot.length + " bytes");
            return false;
        }
        
        log.info("Screenshot validation passed: " + screenshot.length + " bytes");
        return true;
    }
}