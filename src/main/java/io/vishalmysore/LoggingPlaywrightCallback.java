package io.vishalmysore;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;

public class LoggingPlaywrightCallback implements PlaywrightCallback{
    @Override
    public boolean beforeWebAction(String lineToBeProcessed, Browser driver, BrowserContext context) {
        return false;
    }

    @Override
    public void afterWebAction(String lineProcessed, Browser driver,BrowserContext context) {

    }

    @Override
    public String handleError(String line, String errorMessage, Browser driver,BrowserContext context, int retryCount) {
        return "";
    }
}
