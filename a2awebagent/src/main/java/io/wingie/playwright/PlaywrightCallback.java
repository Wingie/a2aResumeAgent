package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;

/**
 * Playwright callback interface for web automation events
 * Replaces the Selenium-based callback system
 */
public interface PlaywrightCallback {
    
    /**
     * Called before executing a web action
     * @param lineToBeProcessed The action line that will be processed
     * @param browser The Playwright browser instance
     * @param context The browser context
     * @return true to continue processing, false to skip this action
     */
    boolean beforeWebAction(String lineToBeProcessed, Browser browser, BrowserContext context);
    
    /**
     * Called after executing a web action
     * @param lineProcessed The action line that was processed
     * @param browser The Playwright browser instance
     * @param context The browser context
     */
    void afterWebAction(String lineProcessed, Browser browser, BrowserContext context);

    /**
     * Handle errors during action processing
     * @param line The action line that caused the error
     * @param errorMessage The error message
     * @param browser The Playwright browser instance
     * @param context The browser context
     * @param retryCount The number of retries attempted
     * @return Corrected line or null to abort
     */
    String handleError(String line, String errorMessage, Browser browser, BrowserContext context, int retryCount);
}