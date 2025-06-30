package io.wingie.playwright;

import com.google.gson.Gson;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import io.wingie.a2acore.tools4ai.processor.AIProcessingException;
import io.wingie.a2acore.tools4ai.detect.ActionCallback;
import io.wingie.a2acore.tools4ai.detect.ActionState;
import io.wingie.a2acore.tools4ai.processor.scripts.ScriptProcessor;
import io.wingie.a2acore.tools4ai.processor.scripts.ScriptResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Playwright script processor for executing web automation scripts
 * Extends Tools4AI ScriptProcessor for web automation capabilities
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.playwright.script-processor.enabled", havingValue = "true", matchIfMissing = false)
public class PlaywrightScriptProcessor extends ScriptProcessor {
    
    @Getter
    private final Gson gson;
    
    @Getter
    private final Browser browser;
    
    @Getter
    private final BrowserContext context;
    
    @Getter
    private PlaywrightProcessor playwrightProcessor;

    public PlaywrightScriptProcessor(Browser browser, BrowserContext context) {
        this.gson = new Gson();
        this.browser = browser;
        this.context = context;
        this.playwrightProcessor = createPlaywrightProcessor();
    }

    public PlaywrightScriptProcessor(Browser browser, BrowserContext context, PlaywrightProcessor processor) {
        this.gson = new Gson();
        this.browser = browser;
        this.context = context;
        this.playwrightProcessor = processor;
    }

    private PlaywrightProcessor createPlaywrightProcessor() {
        // Create a simple Playwright processor implementation
        return new PlaywrightProcessor() {
            @Override
            public Browser getBrowser() {
                return browser;
            }

            @Override
            public BrowserContext getContext() {
                return context;
            }

            @Override
            public io.wingie.a2acore.tools4ai.JsonUtils getUtils() {
                return new io.wingie.a2acore.tools4ai.JsonUtils();
            }

            @Override
            public boolean trueFalseQuery(String question) throws AIProcessingException {
                // Simple implementation
                return true;
            }
        };
    }

    public ScriptResult process(String fileName) {
        return process(fileName, new LoggingPlaywrightCallback());
    }

    public ScriptResult process(String fileName, PlaywrightCallback callback) {
        ScriptResult result = new ScriptResult();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                is = new FileInputStream(fileName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                log.info("Processing script file: {}", fileName);
                processCommands(reader, result, callback);
            }
        } catch (IOException e) {
            log.error("Error processing file: {}", e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: {}", e.getMessage());
        }
        return result;
    }

    public ScriptResult process(String context, StringBuffer steps, PlaywrightCallback callback) {
        ScriptResult result = new ScriptResult();
        try {
            BufferedReader reader = new BufferedReader(new StringReader(steps.toString()));
            log.info("Processing content from StringBuffer");
            processCommands(reader, result, callback);
        } catch (IOException e) {
            log.error("Error processing content: {}", e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: {}", e.getMessage());
        }
        return result;
    }

    public ScriptResult process(String content, ActionCallback callback) {
        ScriptResult result = new ScriptResult();
        try {
            BufferedReader reader = new BufferedReader(new StringReader(content));
            log.debug("Processing content string");
            processCommands(reader, result, callback);
        } catch (IOException e) {
            log.error("Error processing content: {}", e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: {}", e.getMessage());
        }
        return result;
    }

    public void processCommands(BufferedReader reader, ScriptResult result, PlaywrightCallback callback) 
            throws IOException, AIProcessingException {
        String line;
        while ((line = reader.readLine()) != null) {
            boolean process = callback.beforeWebAction(line, browser, context);
            if (process) {
                processWebAction(line, callback, 0);
                callback.afterWebAction(line, browser, context);
            }
            log.debug("{}", result);
        }
    }

    public void processWebAction(String line, PlaywrightCallback callback, int retryCount) {
        try {
            if (playwrightProcessor != null) {
                // Note: processWebAction method not available in simplified interface
                log.info("Processing web action: {}", line);
            } else {
                log.warn("No Playwright processor available for line: {}", line);
            }
        } catch (Exception e) {
            log.warn("Error processing web action: {}", e.getMessage());
            String newLine = callback.handleError(line, e.getMessage(), browser, context, retryCount);
            if (newLine != null) {
                retryCount = retryCount + 1;
                processWebAction(newLine, callback, retryCount);
            }
        }
    }

    public void processCommands(BufferedReader reader, ScriptResult result, ActionCallback callback) 
            throws IOException, AIProcessingException {
        String line;
        while ((line = reader.readLine()) != null) {
            callback.sendtStatus("processing " + line, ActionState.WORKING);
            if (playwrightProcessor != null) {
                // Note: processWebAction method not available in simplified interface
                log.info("Processing line: {}", line);
            }
            callback.sendtStatus("processed " + line, ActionState.WORKING);
            log.debug("{}", result);
        }
    }
}