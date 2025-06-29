package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple logging implementation of PlaywrightCallback
 * Used for basic script processing without complex result handling
 */
@Slf4j
public class LoggingPlaywrightCallback implements PlaywrightCallback {

    @Override
    public boolean beforeWebAction(String lineToBeProcessed, Browser browser, BrowserContext context) {
        log.info("Before action: {}", lineToBeProcessed);
        return true;
    }

    @Override
    public void afterWebAction(String lineProcessed, Browser browser, BrowserContext context) {
        log.info("After action: {}", lineProcessed);
    }

    @Override
    public String handleError(String line, String errorMessage, Browser browser, BrowserContext context, int retryCount) {
        log.error("Error processing line '{}': {} (retry: {})", line, errorMessage, retryCount);
        
        // Simple retry logic - don't retry more than 3 times
        if (retryCount > 3) {
            log.error("Max retries reached for line: {}", line);
            return null;
        }
        
        // For logging callback, just return null (no retry)
        return null;
    }
}