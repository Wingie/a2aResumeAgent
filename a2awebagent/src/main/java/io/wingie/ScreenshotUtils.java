package io.wingie;

import io.wingie.playwright.PlaywrightScreenshotUtils;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for screenshot capture using Playwright
 * Provides static methods for screenshot functionality with fallback handling
 */
@Slf4j
public class ScreenshotUtils {
    
    /**
     * Capture screenshot using Playwright Page (new preferred method)
     */
    public static byte[] captureScreenshotWithFallbacks(Page page, String context) {
        log.info("Delegating to PlaywrightScreenshotUtils");
        return PlaywrightScreenshotUtils.captureScreenshotWithFallbacks(page, context);
    }
    
    /**
     * Capture and save screenshot using Playwright Page
     */
    public static String captureAndSaveScreenshot(Page page, String context) {
        log.info("Delegating to PlaywrightScreenshotUtils for file save");
        return PlaywrightScreenshotUtils.captureAndSaveScreenshot(page, context);
    }
    
    /**
     * Capture viewport screenshot using Playwright Page
     */
    public static byte[] captureViewportScreenshot(Page page, String context) {
        log.info("Delegating to PlaywrightScreenshotUtils for viewport capture");
        return PlaywrightScreenshotUtils.captureViewportScreenshot(page, context);
    }
    
    /**
     * Legacy method - no longer supported
     * @deprecated Use captureScreenshotWithFallbacks(Page, String) instead
     */
    @Deprecated
    public static byte[] captureScreenshotWithFallbacks(Object driver, String context) {
        log.warn("Legacy WebDriver screenshot method is no longer supported - use Playwright Page instead");
        return null; // Return null for legacy calls
    }
    
    /**
     * Check if page is ready for screenshot using Playwright
     */
    public static boolean isPageReadyForScreenshot(Page page) {
        return PlaywrightScreenshotUtils.isPageReadyForScreenshot(page);
    }
    
    /**
     * Get page dimensions using Playwright
     */
    public static String getPageDimensions(Page page) {
        return PlaywrightScreenshotUtils.getPageDimensions(page);
    }
}