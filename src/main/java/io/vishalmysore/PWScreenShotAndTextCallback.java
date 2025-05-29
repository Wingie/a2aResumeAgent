package io.vishalmysore;

import com.microsoft.playwright.*;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import lombok.extern.java.Log;

@Log
public class PWScreenShotAndTextCallback implements PlaywrightCallback {
    private final CustomScriptResult customResult;
    private final AIProcessor processor;

    public PWScreenShotAndTextCallback(CustomScriptResult customResult, AIProcessor processor) {
        this.customResult = customResult;
        this.processor = processor;

    }

    @Override
    public boolean beforeWebAction(String lineToBeProcessed, Browser browser, BrowserContext context) {


        log.info("Processing line (before): " + lineToBeProcessed);
        try {
            Page page = context.newPage();
            page.navigate("about:blank"); // Optional placeholder
            String html = page.content();
            customResult.addBeforeHtml(html);

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            customResult.addScreenshot(screenshot);

            page.close();
        } catch (Exception e) {
            log.warning("Before action failed: " + e.getMessage());
        }
        return true;
    }

    @Override
    public void afterWebAction(String lineProcessed, Browser browser, BrowserContext context) {
        log.info("Processed line (after): " + lineProcessed);
        try {
            Page page = context.newPage();
            page.navigate("about:blank"); // Optional placeholder
            String html = page.content();
            customResult.addAfterHtml(html);

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            customResult.addScreenshot(screenshot);

            page.close();
        } catch (Exception e) {
            log.warning("After action failed: " + e.getMessage());
        }
    }

    @Override
    public String handleError(String line, String errorMessage, Browser browser, BrowserContext context, int numberOfRetries) {
        log.severe("Error processing line: " + line + " | Error: " + errorMessage);
        if (numberOfRetries > 3) {
            log.severe("Max retries reached for line: " + line);
            return null;
        }
        try {
            return processor.query("This line failed: " + line + " with error: " + errorMessage + ". Please provide a revised instruction.");
        } catch (AIProcessingException e) {
            throw new RuntimeException("Failed to query AI for correction", e);
        }
    }
}
