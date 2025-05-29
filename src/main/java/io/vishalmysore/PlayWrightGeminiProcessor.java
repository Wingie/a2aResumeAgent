package io.vishalmysore;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.t4a.JsonUtils;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.GeminiV2ActionProcessor;
import com.t4a.transform.PromptTransformer;


public class PlayWrightGeminiProcessor extends GeminiV2ActionProcessor implements PlaywrightProcessor {

    @Override
    public boolean trueFalseQuery(String question) throws AIProcessingException {
        return false;
    }

    @Override
    public Browser getBrowser() {
        return null;
    }

    @Override
    public JsonUtils getUtils() {
        return null;
    }

    @Override
    public PromptTransformer getTransformer() {
        return null;
    }

    @Override
    public AIProcessor getActionProcessor() {
        return null;
    }

    @Override
    public BrowserContext getContext() {
        return null;
    }
}
