package io.wingie;

import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.scripts.SeleniumCallback;
import lombok.extern.java.Log;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.logging.Logger;

public class A2ASeleniumCallBack implements SeleniumCallback {

    private static final Logger log = Logger.getLogger(A2ASeleniumCallBack.class.getName());
    private static final int DEFAULT_WAIT_TIMEOUT = 10; // seconds
    private static final int SCREENSHOT_WAIT_MS = 2000; // 2 seconds for rendering

    CustomScriptResult customResult;
    AIProcessor processor;

    public A2ASeleniumCallBack(CustomScriptResult customResult, AIProcessor processor) {
        this.customResult = customResult;
        this.processor = processor;
    }

    @Override
    public boolean beforeWebAction(String lineToBeProessed,WebDriver driver) {
        if(lineToBeProessed.contains("browser"))
            return false;
        log.info("Processing line: " + lineToBeProessed);
        try {
            String html = driver.getPageSource();
            customResult.addBeforeHtml(html);
            
            // Take screenshot with enhanced waiting and validation
            byte[] screenshot = takeEnhancedScreenshot(driver, "BEFORE action: " + lineToBeProessed);
            if (screenshot != null) {
                customResult.addScreenshot(screenshot);
            }
        } catch (WebDriverException e) {
            log.warning("Error in beforeWebAction: " + e.getMessage());
        }
        return true;
    }

    @Override
    public void afterWebAction(String lineProcessed,WebDriver driver) {
        log.info("Processed line: " + lineProcessed);
        try {
            String html = driver.getPageSource();
            customResult.addAfterHtml(html);
            
            // Take screenshot with enhanced waiting and validation
            byte[] screenshot = takeEnhancedScreenshot(driver, "AFTER action: " + lineProcessed);
            if (screenshot != null) {
                customResult.addScreenshot(screenshot);
            }
        } catch (WebDriverException e) {
            log.warning("Error in afterWebAction: " + e.getMessage());
        }
    }

    /**
     * Enhanced screenshot capture with explicit waits and page validation
     */
    private byte[] takeEnhancedScreenshot(WebDriver driver, String context) {
        try {
            log.info("Taking enhanced screenshot for context: " + context);
            
            // Use the enhanced ScreenshotUtils with multiple fallback strategies
            byte[] screenshot = ScreenshotUtils.captureScreenshotWithFallbacks(driver, context);
            
            if (screenshot != null) {
                log.info("Screenshot captured successfully using ScreenshotUtils, size: " + screenshot.length + " bytes");
                return screenshot;
            } else {
                log.warning("ScreenshotUtils failed, attempting legacy method");
                return takeLegacyScreenshot(driver, context);
            }
            
        } catch (Exception e) {
            log.severe("Error in takeEnhancedScreenshot: " + e.getMessage());
            e.printStackTrace();
            return takeLegacyScreenshot(driver, context);
        }
    }
    
    /**
     * Legacy screenshot method as final fallback
     */
    private byte[] takeLegacyScreenshot(WebDriver driver, String context) {
        try {
            log.info("Using legacy screenshot method for context: " + context);
            
            // Step 1: Wait for page to be in ready state
            waitForPageLoad(driver);
            
            // Step 2: Execute JavaScript to ensure rendering is complete
            ensurePageRendering(driver);
            
            // Step 3: Additional wait for rendering to complete
            Thread.sleep(SCREENSHOT_WAIT_MS);
            
            // Step 4: Validate page content before screenshot
            if (isPageContentValid(driver)) {
                log.info("Page content validated, taking screenshot");
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                log.info("Legacy screenshot captured successfully, size: " + screenshot.length + " bytes");
                return screenshot;
            } else {
                log.warning("Page content validation failed, screenshot may be blank");
                // Still attempt to take screenshot for debugging
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                log.warning("Legacy screenshot taken despite validation failure, size: " + screenshot.length + " bytes");
                return screenshot;
            }
            
        } catch (Exception e) {
            log.severe("Error taking legacy screenshot: " + e.getMessage());
            e.printStackTrace();
            
            // Final fallback: try basic screenshot
            try {
                log.info("Attempting final basic screenshot");
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                log.info("Final fallback screenshot captured, size: " + screenshot.length + " bytes");
                return screenshot;
            } catch (Exception fallbackEx) {
                log.severe("All screenshot methods failed: " + fallbackEx.getMessage());
                return null;
            }
        }
    }

    /**
     * Wait for page to reach ready state
     */
    private void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_TIMEOUT));
            
            // Wait for document ready state
            wait.until(webDriver -> {
                String readyState = ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").toString();
                log.fine("Document ready state: " + readyState);
                return "complete".equals(readyState);
            });
            
            log.info("Page load completed (document.readyState = complete)");
            
        } catch (Exception e) {
            log.warning("Error waiting for page load: " + e.getMessage());
        }
    }

    /**
     * Execute JavaScript to ensure rendering is complete
     */
    private void ensurePageRendering(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Force a repaint
            js.executeScript("window.scrollTo(0, 0);");
            
            // Trigger a style recalculation
            js.executeScript("document.body.style.zoom = '1.0';");
            
            // Force layout recalculation
            js.executeScript("document.body.offsetHeight;");
            
            // Wait for any pending animations/transitions
            js.executeScript(
                "return new Promise(resolve => {" +
                "  if (window.requestAnimationFrame) {" +
                "    requestAnimationFrame(() => requestAnimationFrame(resolve));" +
                "  } else {" +
                "    setTimeout(resolve, 50);" +
                "  }" +
                "});"
            );
            
            log.info("Page rendering JavaScript executed successfully");
            
        } catch (Exception e) {
            log.warning("Error executing rendering JavaScript: " + e.getMessage());
        }
    }

    /**
     * Validate that page has meaningful content
     */
    private boolean isPageContentValid(WebDriver driver) {
        try {
            // Check if page has a body element
            WebElement body = driver.findElement(By.tagName("body"));
            if (body == null) {
                log.warning("No body element found");
                return false;
            }
            
            // Check page dimensions
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long documentHeight = (Long) js.executeScript("return document.body.scrollHeight;");
            Long windowHeight = (Long) js.executeScript("return window.innerHeight;");
            Long windowWidth = (Long) js.executeScript("return window.innerWidth;");
            
            log.info("Page dimensions - documentHeight: " + documentHeight + 
                    ", windowHeight: " + windowHeight + ", windowWidth: " + windowWidth);
            
            // Validate dimensions are reasonable
            if (windowWidth == null || windowWidth < 100 || windowHeight == null || windowHeight < 100) {
                log.warning("Invalid window dimensions: " + windowWidth + "x" + windowHeight);
                return false;
            }
            
            // Check if page has some visible content
            String bodyText = body.getText();
            if (bodyText == null || bodyText.trim().isEmpty()) {
                log.warning("Body element has no text content");
                // Don't fail validation just for this, as some pages might be image-heavy
            }
            
            // Get page title for debugging
            String title = driver.getTitle();
            log.info("Page title: " + (title != null ? title : "No title"));
            
            // Get current URL for debugging
            String currentUrl = driver.getCurrentUrl();
            log.info("Current URL: " + currentUrl);
            
            return true;
            
        } catch (Exception e) {
            log.warning("Error validating page content: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String handleError(String line, String errorMessage, WebDriver driver, int numberOfRetries) {
        // Log the error message you can take any action here like reprocessing the line
        log.severe("Error processing line: " + line + " Error: " + errorMessage);
        String newline = null;
        if(numberOfRetries > 3) {
            log.severe("Max retries reached for line: " + line);
            return null; // or handle as needed
        }
        try {
            newline = processor.query(" this line " + line + " failed with error " + errorMessage + " please provide new line to process");
        } catch (AIProcessingException e) {
            throw new RuntimeException(e);
        }
        return newline;
    }
}