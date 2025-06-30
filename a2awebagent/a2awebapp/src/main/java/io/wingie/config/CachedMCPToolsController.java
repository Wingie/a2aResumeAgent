package io.wingie.config;

import com.t4a.JsonUtils;
import com.t4a.api.AIAction;
import com.t4a.api.GenericJavaMethodAction;
import com.t4a.api.GroupInfo;
import com.t4a.predict.PredictionLoader;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.AIProcessingException;
import io.github.vishalmysore.mcp.domain.*;
import io.github.vishalmysore.mcp.server.MCPToolsController;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Enhanced MCPToolsController that integrates with PostgreSQL caching
 * to avoid regenerating tool descriptions on every startup.
 * 
 * This implementation overrides the convertGroupActionsToTools method
 * to use cached descriptions when available, falling back to AI generation
 * only when necessary.
 */
@Component
@Primary
@Slf4j
public class CachedMCPToolsController extends MCPToolsController {
    
    @Autowired
    private ToolDescriptionCacheBridge cacheBridge;
    
    private final JsonUtils utils = new JsonUtils();
    
    /**
     * Get the current AI model name for caching purposes
     */
    private String getCurrentModelName() {
        try {
            Map<Object, Object> properties = PredictionLoader.getInstance().getTools4AIProperties();
            
            // Try different model name properties based on provider
            String provider = (String) properties.get("agent.provider");
            if ("openrouter".equals(provider) || "openai".equals(provider)) {
                String modelName = (String) properties.get("openAiModelName");
                if (modelName != null && !modelName.trim().isEmpty()) {
                    return modelName;
                }
            }
            
            if ("gemini".equals(provider)) {
                String modelName = (String) properties.get("gemini.modelName");
                if (modelName != null && !modelName.trim().isEmpty()) {
                    return modelName;
                }
            }
            
            if ("anthropic".equals(provider)) {
                String modelName = (String) properties.get("anthropic.modelName");
                if (modelName != null && !modelName.trim().isEmpty()) {
                    return modelName;
                }
            }
            
            // Fallback to provider name
            return provider != null ? provider : "default";
        } catch (Exception e) {
            log.warn("Failed to get model name for caching: {}", e.getMessage());
            return "default";
        }
    }
    
    /**
     * Generate tool description with caching support
     */
    private String generateToolDescription(String actionName, String jsonStr, AIProcessor processor) {
        String currentModel = getCurrentModelName();
        
        // Try to get cached description first
        String cachedDescription = cacheBridge.getCachedDescription(currentModel, actionName);
        if (cachedDescription != null) {
            log.info("Using cached description for tool: {} (model: {})", actionName, currentModel);
            return cachedDescription;
        }
        
        // Cache miss - generate with AI
        try {
            log.info("Generating description for tool: {} (model: {})", actionName, currentModel);
            String aiResponse = processor.query("I am giving you a json string check the parameters section and return the required fields including subfields as simple json, do not include any other commentary, control or special characters " + jsonStr);
            String cleanResponse = utils.extractJson(aiResponse);
            
            // Cache the generated description
            cacheBridge.cacheDescription(currentModel, actionName, cleanResponse);
            log.info("Cached description for tool: {} (model: {}) - {} chars", 
                    actionName, currentModel, cleanResponse.length());
            
            return cleanResponse;
        } catch (AIProcessingException e) {
            log.error("Failed to generate description for tool: {} (model: {}): {}", 
                    actionName, currentModel, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void init() {
        log.info("Initializing CachedMCPToolsController with PostgreSQL caching support");
        
        // Get the AI processor using the parent's method
        AIProcessor baseProcessor;
        try {
            baseProcessor = PredictionLoader.getInstance().createOrGetAIProcessor();
        } catch (Exception e) {
            log.warn("Failed to create processor from PredictionLoader, using fallback: {}", e.getMessage());
            baseProcessor = createOpenRouterProcessor();
        }
        
        // If we still don't have a processor or it's GeminiV2, use OpenRouter
        if (baseProcessor == null || baseProcessor.getClass().getSimpleName().contains("GeminiV2")) {
            log.info("Using OpenRouter processor instead of Gemini");
            baseProcessor = createOpenRouterProcessor();
        }
        
        // Set the base processor using reflection (since the field is private)
        try {
            java.lang.reflect.Field field = MCPToolsController.class.getDeclaredField("baseProcessor");
            field.setAccessible(true);
            field.set(this, baseProcessor);
        } catch (Exception e) {
            log.error("Failed to set base processor: {}", e.getMessage());
        }
        
        // Continue with the rest of the initialization
        super.init();
    }
    
    private AIProcessor createOpenRouterProcessor() {
        try {
            // Check if OpenRouter is configured
            Map<Object, Object> properties = PredictionLoader.getInstance().getTools4AIProperties();
            String provider = (String) properties.get("agent.provider");
            
            if ("openrouter".equals(provider)) {
                // Use OpenAI processor with OpenRouter configuration
                return (AIProcessor) Class.forName("com.t4a.processor.OpenAiActionProcessor").getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            log.warn("Failed to configure OpenRouter processor: {}", e.getMessage());
        }
        
        // Final fallback to OpenAI processor (will use OpenRouter if configured in properties)
        try {
            return (AIProcessor) Class.forName("com.t4a.processor.OpenAiActionProcessor").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to create OpenAI processor: {}", e.getMessage());
            return null;
        }
    }
}