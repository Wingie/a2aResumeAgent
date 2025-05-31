package io.vishalmysore;

import com.microsoft.playwright.*;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import lombok.extern.java.Log;

@Log
public class PWScreenShotAndTextCallback implements PlaywrightCallback {
    private final CustomScriptResult customResult;
    private final AIProcessor processor;
    private String context;
    private StringBuffer allSteps;

    public PWScreenShotAndTextCallback(String context, StringBuffer allSteps,CustomScriptResult customResult, AIProcessor processor) {
        this.customResult = customResult;
        this.processor = processor;
        this.context = context;
        this.allSteps = allSteps;

    }

    @Override
    public boolean beforeWebAction(String lineToBeProcessed, Browser browser, BrowserContext context) {
        log.info("Total pages in context: "+ context.pages().size());

        log.info("Processing line (before): " + lineToBeProcessed);
        try {
            if (context.pages().isEmpty()) {
                return true; // No pages to process may be first line or no action needed
            }
            Page page = context.pages().get(0);
            String html = page.content();
            customResult.addBeforeHtml(html);

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            customResult.addScreenshot(screenshot);


        } catch (Exception e) {
            log.warning("Before action failed: " + e.getMessage());
        }
        return true;
    }

    @Override
    public void afterWebAction(String lineProcessed, Browser browser, BrowserContext context) {
        log.info("Processed line (after): " + lineProcessed);
        log.info("Total pages in context: "+ context.pages().size());
        try {
            Page page = context.pages().get(0);
            String html = page.content();
            customResult.addAfterHtml(html);

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            customResult.addScreenshot(screenshot);


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
            String prompt = " you are an automated playwright web script correction assistant. " +
                    "Your task is to correct the following line of code: " + line +
                    ". The error message is: " + errorMessage +
                    ". Please provide a corrected version of the line. Overall context is: " + this.context +" the steps were broken down to: " + allSteps.toString();
            return processor.query(prompt);
        } catch (AIProcessingException e) {
            throw new RuntimeException("Failed to query AI for correction", e);
        }
    }
}
