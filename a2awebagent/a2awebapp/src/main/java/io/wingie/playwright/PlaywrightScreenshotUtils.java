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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

/**
 * Playwright-based screenshot utility class
 * Provides enhanced web automation screenshot capabilities using Microsoft Playwright
 */
@Slf4j
public class PlaywrightScreenshotUtils {
    
    private static final int DEFAULT_WAIT_TIMEOUT = 30000; // 30 seconds
    private static final int SCREENSHOT_WAIT_MS = 3000; // 3 seconds for page stability
    private static final int MAX_RETRIES = 3;
    
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
            
            // Validate screenshot content
            ScreenshotValidationResult validation = validateScreenshotContent(screenshot);
            if (!validation.isValid) {
                log.warn("Screenshot validation failed: {} - Retrying with fallback", validation.reason);
                return captureWithFallbacks(page, context);
            }
            
            log.info("Screenshot validation passed: {}", validation.quality);
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
            log.debug("Starting page stability check...");
            
            // Wait for DOM content loaded first
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            log.debug("DOM content loaded");
            
            // Wait for network to be idle (no network activity for 500ms)
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log.debug("Network idle state reached");
            
            // Wait for any JavaScript frameworks to initialize
            page.waitForFunction("() => document.readyState === 'complete'");
            log.debug("Document ready state complete");
            
            // Additional wait for dynamic content and animations
            Thread.sleep(SCREENSHOT_WAIT_MS);
            
            // Check for common loading indicators and wait for them to disappear
            try {
                page.waitForSelector("[class*='loading'], [class*='spinner'], .loader", 
                    new Page.WaitForSelectorOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED).setTimeout(5000));
                log.debug("Loading indicators disappeared");
            } catch (Exception e) {
                log.debug("No loading indicators found or timeout reached");
            }
            
            log.debug("Page stability check completed successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Page stability wait interrupted");
        } catch (Exception e) {
            log.warn("Page stability check failed: {} - proceeding with screenshot", e.getMessage());
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

    /**
     * Validates screenshot content to detect blank, white, or low-quality images
     */
    private static ScreenshotValidationResult validateScreenshotContent(byte[] screenshotBytes) {
        if (screenshotBytes == null || screenshotBytes.length == 0) {
            return new ScreenshotValidationResult(false, "Empty screenshot data", 0.0);
        }
        
        if (screenshotBytes.length < 1000) { // Less than 1KB is probably empty
            return new ScreenshotValidationResult(false, "Screenshot too small", 0.0);
        }
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
            if (image == null) {
                return new ScreenshotValidationResult(false, "Invalid image format", 0.0);
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            if (width < 100 || height < 100) {
                return new ScreenshotValidationResult(false, "Image dimensions too small", 0.0);
            }
            
            // Sample pixels to detect blank/white images
            double whitePixelRatio = calculateWhitePixelRatio(image);
            double colorVariance = calculateColorVariance(image);
            
            if (whitePixelRatio > 0.95) {
                return new ScreenshotValidationResult(false, "Image is mostly white/blank", whitePixelRatio);
            }
            
            if (colorVariance < 10.0) {
                return new ScreenshotValidationResult(false, "Image lacks content variance", colorVariance);
            }
            
            // Calculate quality score
            double qualityScore = Math.min(1.0, (1.0 - whitePixelRatio) * (colorVariance / 100.0));
            
            return new ScreenshotValidationResult(true, "Valid screenshot", qualityScore);
            
        } catch (Exception e) {
            log.warn("Screenshot validation failed", e);
            return new ScreenshotValidationResult(false, "Validation error: " + e.getMessage(), 0.0);
        }
    }

    /**
     * Calculate the ratio of white/near-white pixels in the image
     */
    private static double calculateWhitePixelRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = 0;
        int whitePixels = 0;
        
        // Sample every 10th pixel for performance
        for (int x = 0; x < width; x += 10) {
            for (int y = 0; y < height; y += 10) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                // Consider pixel white if all RGB values are > 240
                if (red > 240 && green > 240 && blue > 240) {
                    whitePixels++;
                }
                totalPixels++;
            }
        }
        
        return totalPixels > 0 ? (double) whitePixels / totalPixels : 1.0;
    }

    /**
     * Calculate color variance to detect content richness
     */
    private static double calculateColorVariance(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalRed = 0, totalGreen = 0, totalBlue = 0;
        int pixelCount = 0;
        
        // Sample every 20th pixel for performance
        for (int x = 0; x < width; x += 20) {
            for (int y = 0; y < height; y += 20) {
                int rgb = image.getRGB(x, y);
                totalRed += (rgb >> 16) & 0xFF;
                totalGreen += (rgb >> 8) & 0xFF;
                totalBlue += rgb & 0xFF;
                pixelCount++;
            }
        }
        
        if (pixelCount == 0) return 0.0;
        
        double avgRed = (double) totalRed / pixelCount;
        double avgGreen = (double) totalGreen / pixelCount;
        double avgBlue = (double) totalBlue / pixelCount;
        
        double variance = 0;
        pixelCount = 0;
        
        // Calculate variance
        for (int x = 0; x < width; x += 20) {
            for (int y = 0; y < height; y += 20) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                variance += Math.pow(red - avgRed, 2) + Math.pow(green - avgGreen, 2) + Math.pow(blue - avgBlue, 2);
                pixelCount++;
            }
        }
        
        return pixelCount > 0 ? Math.sqrt(variance / pixelCount) : 0.0;
    }

    /**
     * Result of screenshot content validation
     */
    private static class ScreenshotValidationResult {
        final boolean isValid;
        final String reason;
        final double quality;
        
        ScreenshotValidationResult(boolean isValid, String reason, double quality) {
            this.isValid = isValid;
            this.reason = reason;
            this.quality = quality;
        }
    }
}