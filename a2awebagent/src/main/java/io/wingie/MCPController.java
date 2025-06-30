package io.wingie;

import com.t4a.api.AIAction;
import com.t4a.detect.ActionCallback;
import com.t4a.detect.HumanInLoop;
import com.t4a.detect.ExplainDecision;
import com.t4a.predict.PredictionLoader;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.AIProcessingException;
import io.github.vishalmysore.mcp.server.MCPToolsController;
import io.github.vishalmysore.mcp.domain.*;
import io.wingie.service.ToolDescriptionCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/v1")
@Lazy
public class MCPController extends MCPToolsController {

    private static final Logger logger = Logger.getLogger(MCPController.class.getName());
    
    @Autowired
    private ToolDescriptionCacheService cacheService;

    public MCPController() {
        super();
        setServerName("WingstonTravelAgent");
        setVersion("1.0.0");
        setProtocolVersion("2024-11-05");
    }
    
    /**
     * Override to provide caching AIProcessor instead of the default one
     * This intercepts tool description generation and caches responses in PostgreSQL
     */
    @Override
    public AIProcessor getBaseProcessor() {
        try {
            // Get the real processor that would normally be used
            AIProcessor realProcessor = PredictionLoader.getInstance().createOrGetAIProcessor();
            
            // Get current model name for cache key generation
            String currentModel = getCurrentModelName();
            
            // Create a simple caching wrapper using anonymous class
            return new AIProcessor() {
                @Override
                public String query(String prompt) throws AIProcessingException {
                    if (isToolDescriptionQuery(prompt)) {
                        return handleCachedToolDescriptionQuery(prompt, realProcessor, currentModel);
                    }
                    return realProcessor.query(prompt);
                }
                
                // Delegate all other methods to the real processor
                @Override
                public Object processSingleAction(String input, AIAction action, HumanInLoop humanInLoop, ExplainDecision explainDecision, ActionCallback actionCallback) throws AIProcessingException {
                    return realProcessor.processSingleAction(input, action, humanInLoop, explainDecision, actionCallback);
                }
                
                @Override
                public Object processSingleAction(String input, ActionCallback actionCallback) throws AIProcessingException {
                    return realProcessor.processSingleAction(input, actionCallback);
                }
                
                @Override
                public Object processSingleAction(String input) throws AIProcessingException {
                    return realProcessor.processSingleAction(input);
                }
                
                @Override
                public Object processSingleAction(String input, HumanInLoop humanInLoop, ExplainDecision explainDecision) throws AIProcessingException {
                    return realProcessor.processSingleAction(input, humanInLoop, explainDecision);
                }
                
                @Override
                public Object processSingleAction(String input, AIAction action, HumanInLoop humanInLoop, ExplainDecision explainDecision) throws AIProcessingException {
                    return realProcessor.processSingleAction(input, action, humanInLoop, explainDecision);
                }
            };
            
        } catch (Exception e) {
            logger.warning("Failed to create caching processor, using default: " + e.getMessage());
            return super.getBaseProcessor();
        }
    }
    
    /**
     * Check if this is a tool description generation query
     */
    private boolean isToolDescriptionQuery(String prompt) {
        return prompt != null && 
               prompt.contains("json string") && 
               prompt.contains("parameters section") &&
               prompt.contains("return the required fields");
    }
    
    /**
     * Handle tool description queries with caching
     */
    private String handleCachedToolDescriptionQuery(String prompt, AIProcessor realProcessor, String currentModel) throws AIProcessingException {
        long startTime = System.currentTimeMillis();
        
        // Generate cache key from model + prompt
        String cacheKey = generateCacheKey(currentModel, prompt);
        
        // Try cache first
        String cachedResponse = cacheService.getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            long cacheTime = System.currentTimeMillis() - startTime;
            logger.info("Cache HIT for tool description (" + cacheTime + "ms) - model: " + currentModel);
            return cachedResponse;
        }
        
        // Cache miss - generate with real AI processor
        logger.info("Cache MISS for tool description - generating with AI (model: " + currentModel + ")");
        String aiResponse = realProcessor.query(prompt);
        
        // Cache the response for future use
        long generationTime = System.currentTimeMillis() - startTime;
        cacheService.cacheResponse(cacheKey, aiResponse, generationTime);
        
        logger.info("Generated and cached tool description (" + generationTime + "ms) - model: " + currentModel);
        return aiResponse;
    }
    
    /**
     * Generate cache key for consistent lookups
     */
    private String generateCacheKey(String modelName, String prompt) {
        try {
            String input = modelName + ":" + prompt;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // Fallback to simple hash if MD5 not available
            return String.valueOf((modelName + ":" + prompt).hashCode());
        }
    }
    
    /**
     * Get current model name for caching purposes
     */
    private String getCurrentModelName() {
        try {
            Map<Object, Object> properties = PredictionLoader.getInstance().getTools4AIProperties();
            
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
            
            return provider != null ? provider : "default";
        } catch (Exception e) {
            logger.warning("Failed to get model name: " + e.getMessage());
            return "default";
        }
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, List<Tool>>> listTools() {
        logger.info("MCP listTools endpoint called");
        return super.listTools();
    }

    @PostMapping("/tools/call")
    public ResponseEntity<JSONRPCResponse> callTool(@RequestBody ToolCallRequest request) {
        logger.info("MCP callTool endpoint called with tool: " + request.getName());
        return super.callTool(request);
    }

    @GetMapping("/resources")
    public ResponseEntity<ListResourcesResult> listResources() {
        logger.info("MCP listResources endpoint called");
        return ResponseEntity.ok(getResourcesResult());
    }

    @GetMapping("/prompts")
    public ResponseEntity<ListPromptsResult> listPrompts() {
        logger.info("MCP listPrompts endpoint called");
        return ResponseEntity.ok(getPromptsResult());
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        logger.info("MCP config endpoint called");
        return super.getServerConfig();
    }
}