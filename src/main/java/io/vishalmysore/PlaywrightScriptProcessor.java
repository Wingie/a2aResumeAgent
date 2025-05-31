package io.vishalmysore;

import com.google.gson.Gson;
import com.t4a.detect.ActionCallback;
import com.t4a.detect.ActionState;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.scripts.*;


import lombok.Getter;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class PlaywrightScriptProcessor extends ScriptProcessor {
    @Getter
    private Gson gson;
    @Getter
    private PlaywrightProcessor playwrightProcessor;

    public PlaywrightScriptProcessor() {
        gson = new Gson();
        playwrightProcessor = new PlaywrightOpenAIProcessor();
    }
    public PlaywrightScriptProcessor(Gson gson) {
        this.gson = gson;
    }

    public PlaywrightScriptProcessor(Gson gson, PlaywrightProcessor playwrightProcessor) {
        this.gson = gson;
        this.playwrightProcessor = playwrightProcessor;
    }

    public PlaywrightScriptProcessor(PlaywrightProcessor playwrightProcessor) {
        gson = new Gson();
        this.playwrightProcessor = playwrightProcessor;
    }

    public ScriptResult process(String fileName) {
        return process(fileName,new LoggingPlaywrightCallback());
    }
    public ScriptResult process(String fileName, PlaywrightCallback callback) {
        ScriptResult result = new ScriptResult();
        try {
            // Try classpath first
            InputStream is = PlaywrightScriptProcessor.class.getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                // If not found in classpath, try filesystem
                is = new FileInputStream(fileName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                log.info("Processing script file: " + fileName);
                processCommands(reader, result, callback);
            }
        } catch (IOException e) {
            log.error("Error processing file: " + e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: " + e.getMessage());
        }
        return result;
    }

    public ScriptResult process(String context,StringBuffer steps, PlaywrightCallback callback) {
        ScriptResult result = new ScriptResult();
        try {
            BufferedReader reader = new BufferedReader(new StringReader(steps.toString()));
            log.info("Processing content from StringBuffer");
            processCommands(reader, result, callback);
        } catch (IOException e) {
            log.error("Error processing content: " + e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: " + e.getMessage());
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
            log.error("Error processing content: " + e.getMessage());
        } catch (AIProcessingException e) {
            log.error("AI Processing error: " + e.getMessage());
        }
        return result;
    }

    public void processCommands( BufferedReader reader, ScriptResult result, PlaywrightCallback callback) throws IOException, AIProcessingException {

        String line;


        while ((line = reader.readLine()) != null) {
            boolean process= callback.beforeWebAction(line,getPlaywrightProcessor().getBrowser(),getPlaywrightProcessor().getContext());
            if(process) {
                processWebAction(line,callback, 0);
                callback.afterWebAction(line,getPlaywrightProcessor().getBrowser(),getPlaywrightProcessor().getContext());
            }
            log.debug("{}",result);
        }
    }


    public void processWebAction(String line,PlaywrightCallback callback, int retryCount) {
        try {
            getPlaywrightProcessor().processWebAction(line);
        } catch (Exception e) {
            log.warn(e.getMessage());
            String newLine = callback.handleError(line, e.getMessage(), getPlaywrightProcessor().getBrowser(), getPlaywrightProcessor().getContext(), retryCount);
            if(newLine !=null) {
                retryCount = retryCount+1;
                processWebAction(newLine, callback, retryCount);
            }
        }
    }

    public void processCommands( BufferedReader reader, ScriptResult result, ActionCallback callback) throws IOException, AIProcessingException {
        PlaywrightProcessor processor = getPlaywrightProcessor();
        String line;


        while ((line = reader.readLine()) != null) {
            callback.sendtStatus("processing "+line, ActionState.WORKING);
            processor.processWebAction(line);
            callback.sendtStatus("processed "+line, ActionState.WORKING);
            log.debug("{}",result);
        }
    }
}
