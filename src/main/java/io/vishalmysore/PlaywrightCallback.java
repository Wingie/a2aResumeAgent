package io.vishalmysore;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;


public interface PlaywrightCallback {
    boolean beforeWebAction(String lineToBeProcessed, Browser driver, BrowserContext context);
    void afterWebAction(String lineProcessed,Browser driver,BrowserContext context);

    String handleError(String line, String errorMessage, Browser driver, BrowserContext context,int retryCount);
}
