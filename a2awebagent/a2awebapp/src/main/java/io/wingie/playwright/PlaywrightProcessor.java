package io.wingie.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import io.wingie.a2acore.tools4ai.JsonUtils;
import io.wingie.a2acore.tools4ai.processor.AIProcessingException;

/**
 * Playwright processor interface for web automation
 * Based on the a2aPlaywrightReference implementation
 */
public interface PlaywrightProcessor {

    // Legacy methods commented out - no longer used with a2acore framework
    // The actual web automation is handled by PlaywrightWebBrowsingAction service
    // These methods were dependent on PromptTransformer which is no longer available
    
    /*
    public default void processWebAction(String prompt) throws AIProcessingException {
        // This method is no longer used - web automation is handled by 
        // PlaywrightWebBrowsingAction service with a2acore annotations
        throw new AIProcessingException("processWebAction is deprecated - use PlaywrightWebBrowsingAction service");
    }

    public default String getStringFromPrompt(String prompt, String key) throws AIProcessingException {
        // This method is no longer used - direct JSON processing is handled
        // by a2acore framework
        throw new AIProcessingException("getStringFromPrompt is deprecated - use a2acore JsonUtils");
    }
    */

    boolean trueFalseQuery(String question) throws AIProcessingException;

    Browser getBrowser();

    JsonUtils getUtils();

    BrowserContext getContext();
}