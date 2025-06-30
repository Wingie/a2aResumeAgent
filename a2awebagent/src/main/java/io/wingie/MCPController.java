package io.wingie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4a.JsonUtils;
import com.t4a.api.AIAction;
import com.t4a.api.GenericJavaMethodAction;
import com.t4a.api.GroupInfo;
import com.t4a.predict.PredictionLoader;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.AIProcessingException;
import com.t4a.transform.PromptTransformer;
import io.github.vishalmysore.mcp.server.MCPToolsController;
import io.github.vishalmysore.mcp.domain.*;
import io.wingie.entity.ToolDescription;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/v1")
@Lazy
@Slf4j
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
     * Override init to use incremental caching with error resilience
     */
    @Override
    public void init() {
        log.info("🚀 Starting cached tool description generation...");
        
        try {
            // Initialize components
            AIProcessor baseProcessor = PredictionLoader.getInstance().createOrGetAIProcessor();
            PromptTransformer promptTransformer = PredictionLoader.getInstance().createOrGetPromptTransformer();
            
            // Get current model for cache keys
            String currentModel = getCurrentModelName();
            log.info("📝 Using model for caching: {}", currentModel);
            
            // Get action groups and convert to tools with caching
            Map<GroupInfo, String> groupActions = PredictionLoader.getInstance().getActionGroupList().getGroupActions();
            List<Tool> tools = convertGroupActionsToToolsWithCaching(groupActions, currentModel, baseProcessor, promptTransformer);

            // Store results
            ListToolsResult newToolsResult = new ListToolsResult();
            newToolsResult.setTools(tools);
            storeListToolsResult(newToolsResult);
            storeListReources(new ArrayList<>());
            storeListPrompts(new ArrayList<>());
            setProperties();
            
            log.info("✅ Tool description generation completed successfully. Generated {} tools", tools.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize MCPController", e);
            // Initialize with empty results to prevent complete failure
            initializeEmptyResults();
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
    
    /**
     * Convert group actions to tools with PostgreSQL caching and error resilience
     */
    private List<Tool> convertGroupActionsToToolsWithCaching(Map<GroupInfo, String> groupActions, 
                                                            String currentModel, 
                                                            AIProcessor baseProcessor, 
                                                            PromptTransformer promptTransformer) {
        List<Tool> tools = new ArrayList<>();
        Map<String, AIAction> predictions = PredictionLoader.getInstance().getPredictions();
        JsonUtils utils = new JsonUtils();

        int totalTools = 0;
        int cachedTools = 0;
        int generatedTools = 0;
        int failedTools = 0;

        for (Map.Entry<GroupInfo, String> entry : groupActions.entrySet()) {
            String[] actionNames = entry.getValue().split(",");

            for (String actionName : actionNames) {
                totalTools++;
                actionName = actionName.trim();
                
                try {
                    AIAction action = predictions.get(actionName);
                    if (!(action instanceof GenericJavaMethodAction)) {
                        continue;
                    }

                    GenericJavaMethodAction methodAction = (GenericJavaMethodAction) action;
                    Method method = methodAction.getActionMethod();

                    if (!isMethodAllowed(method)) {
                        continue;
                    }

                    log.info("🔧 Processing tool: {} ({}/{})", actionName, totalTools, predictions.size());

                    // Check cache first
                    Optional<ToolDescription> cachedDescription = cacheService.getCachedDescription(currentModel, actionName);
                    
                    Tool tool;
                    if (cachedDescription.isPresent()) {
                        // Use cached description
                        tool = createToolFromCache(action, cachedDescription.get(), utils);
                        cachedTools++;
                        log.info("✅ Used cached description for: {}", actionName);
                    } else {
                        // Generate new description
                        tool = generateToolDescription(action, methodAction, currentModel, baseProcessor, promptTransformer, utils);
                        if (tool != null) {
                            generatedTools++;
                            log.info("🆕 Generated new description for: {}", actionName);
                        } else {
                            failedTools++;
                            log.warn("⚠️ Failed to generate description for: {} - using default", actionName);
                            tool = createDefaultTool(action, methodAction);
                        }
                    }

                    if (tool != null) {
                        addResource(method);
                        tools.add(tool);
                        addPrompt(tool);
                    }

                } catch (Exception e) {
                    failedTools++;
                    log.error("❌ Failed processing tool {}: {} - continuing with next tool", actionName, e.getMessage());
                    // Continue processing other tools instead of crashing
                }
            }
        }

        log.info("📊 Tool generation summary: Total={}, Cached={}, Generated={}, Failed={}", 
            totalTools, cachedTools, generatedTools, failedTools);
        
        return tools;
    }
    
    /**
     * Generate tool description with AI and cache the result immediately
     */
    private Tool generateToolDescription(AIAction action, GenericJavaMethodAction methodAction, 
                                       String currentModel, AIProcessor baseProcessor, 
                                       PromptTransformer promptTransformer, JsonUtils utils) {
        long startTime = System.currentTimeMillis();
        
        try {
            Tool tool = new Tool();
            tool.setName(action.getActionName());
            tool.setDescription(action.getDescription());

            // Generate parameters schema with AI
            String jsonStr = methodAction.getActionParameters();
            String aiResponse = baseProcessor.query("I am giving you a json string check the parameters section and return the required fields including subfields as simple json, do not include any other commentary, control or special characters " + jsonStr);
            aiResponse = utils.extractJson(aiResponse);

            // Create tool parameters and schema
            String customParam = "provideAllValuesInPlainEnglish";
            
            // ToolParameters (for Claude/LLM)
            ToolParameters parameters = new ToolParameters();
            ToolParameter toolParam = new ToolParameter();
            toolParam.setType("string");
            toolParam.setDescription(aiResponse);
            parameters.getProperties().put(customParam, toolParam);
            parameters.getRequired().add(customParam);

            // InputSchema (for MCP)
            ToolInputSchema inputSchema = new ToolInputSchema();
            Map<String, ToolPropertySchema> schemaProperties = new HashMap<>();
            ToolPropertySchema schema = new ToolPropertySchema();
            schema.setType("string");
            schema.setDescription(aiResponse);
            schema.setAdditionalProperties(new HashMap<>());
            schemaProperties.put(customParam, schema);
            inputSchema.setProperties(schemaProperties);
            inputSchema.setRequired(Arrays.asList(customParam));
            
            tool.setInputSchema(inputSchema);

            // Generate tool annotations
            ToolAnnotations toolAnnotations = null;
            try {
                toolAnnotations = (ToolAnnotations) promptTransformer.transformIntoPojo(
                    "this is the tool name " + action.getActionName() + " and here are the parameters it takes " + jsonStr + 
                    " I want you to give name name value pair describing what this tool does so that people can identify this tool better", 
                    ToolAnnotations.class
                );
            } catch (AIProcessingException e) {
                log.warn("Failed to generate tool annotations for {}: {}", action.getActionName(), e.getMessage());
            }
            
            tool.setAnnotations(toolAnnotations);

            // Cache the successful result immediately
            long generationTime = System.currentTimeMillis() - startTime;
            cacheToolDescription(currentModel, action.getActionName(), aiResponse, jsonStr, toolAnnotations, generationTime);

            return tool;

        } catch (Exception e) {
            log.error("Failed generating tool {}: {}", action.getActionName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Create tool from cached description
     */
    private Tool createToolFromCache(AIAction action, ToolDescription cached, JsonUtils utils) {
        try {
            Tool tool = new Tool();
            tool.setName(action.getActionName());
            tool.setDescription(action.getDescription());

            // Parse cached parameters info
            String parametersInfo = cached.getDescription();
            String customParam = "provideAllValuesInPlainEnglish";
            
            // Create tool parameters
            ToolParameters parameters = new ToolParameters();
            ToolParameter toolParam = new ToolParameter();
            toolParam.setType("string");
            toolParam.setDescription(parametersInfo);
            parameters.getProperties().put(customParam, toolParam);
            parameters.getRequired().add(customParam);

            // Create input schema
            ToolInputSchema inputSchema = new ToolInputSchema();
            Map<String, ToolPropertySchema> schemaProperties = new HashMap<>();
            ToolPropertySchema schema = new ToolPropertySchema();
            schema.setType("string");
            schema.setDescription(parametersInfo);
            schema.setAdditionalProperties(new HashMap<>());
            schemaProperties.put(customParam, schema);
            inputSchema.setProperties(schemaProperties);
            inputSchema.setRequired(Arrays.asList(customParam));
            
            tool.setInputSchema(inputSchema);

            // Parse cached tool properties for annotations
            String toolProperties = cached.getToolProperties();
            if (toolProperties != null && !toolProperties.trim().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    ToolAnnotations annotations = mapper.readValue(toolProperties, ToolAnnotations.class);
                    tool.setAnnotations(annotations);
                } catch (Exception e) {
                    log.warn("Failed to parse cached annotations for {}: {}", action.getActionName(), e.getMessage());
                }
            }

            return tool;

        } catch (Exception e) {
            log.error("Failed to create tool from cache for {}: {}", action.getActionName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a basic default tool when generation fails
     */
    private Tool createDefaultTool(AIAction action, GenericJavaMethodAction methodAction) {
        Tool tool = new Tool();
        tool.setName(action.getActionName());
        tool.setDescription(action.getDescription() != null ? action.getDescription() : "Tool for " + action.getActionName());

        String customParam = "provideAllValuesInPlainEnglish";
        String defaultDescription = "Please provide instructions for this tool";
        
        // Create basic parameters
        ToolParameters parameters = new ToolParameters();
        ToolParameter toolParam = new ToolParameter();
        toolParam.setType("string");
        toolParam.setDescription(defaultDescription);
        parameters.getProperties().put(customParam, toolParam);
        parameters.getRequired().add(customParam);

        // Create basic input schema
        ToolInputSchema inputSchema = new ToolInputSchema();
        Map<String, ToolPropertySchema> schemaProperties = new HashMap<>();
        ToolPropertySchema schema = new ToolPropertySchema();
        schema.setType("string");
        schema.setDescription(defaultDescription);
        schema.setAdditionalProperties(new HashMap<>());
        schemaProperties.put(customParam, schema);
        inputSchema.setProperties(schemaProperties);
        inputSchema.setRequired(Arrays.asList(customParam));

        tool.setInputSchema(inputSchema);

        return tool;
    }
    
    /**
     * Cache tool description to PostgreSQL immediately
     */
    private void cacheToolDescription(String currentModel, String toolName, String description, 
                                    String parametersInfo, ToolAnnotations annotations, long generationTime) {
        try {
            String toolProperties = "";
            if (annotations != null) {
                ObjectMapper mapper = new ObjectMapper();
                toolProperties = mapper.writeValueAsString(annotations);
            }
            cacheService.cacheDescription(currentModel, toolName, description, parametersInfo, toolProperties, generationTime);
        } catch (Exception e) {
            log.warn("Failed to cache description for {}: {}", toolName, e.getMessage());
        }
    }
    
    /**
     * Get current model name for cache keys
     */
    private String getCurrentModelName() {
        try {
            // Get the actual model name being used by the AI processor
            AIProcessor processor = PredictionLoader.getInstance().createOrGetAIProcessor();
            String modelName = processor.getClass().getSimpleName();
            
            // Try to get the actual model name from tools4ai properties
            Map<Object, Object> properties = PredictionLoader.getInstance().getTools4AIProperties();
            String provider = (String) properties.get("agent.provider");
            
            if ("openrouter".equals(provider) || "openai".equals(provider)) {
                String openAiModelName = (String) properties.get("openAiModelName");
                if (openAiModelName != null && !openAiModelName.trim().isEmpty()) {
                    log.debug("Using OpenRouter model for cache key: {}", openAiModelName);
                    return openAiModelName;
                }
            } else if ("gemini".equals(provider)) {
                String geminiModelName = (String) properties.get("gemini.modelName");
                if (geminiModelName != null && !geminiModelName.trim().isEmpty()) {
                    log.debug("Using Gemini model for cache key: {}", geminiModelName);
                    return geminiModelName;
                }
            } else if ("anthropic".equals(provider) || "claude".equals(provider)) {
                String claudeModelName = (String) properties.get("anthropic.modelName");
                if (claudeModelName != null && !claudeModelName.trim().isEmpty()) {
                    log.debug("Using Claude model for cache key: {}", claudeModelName);
                    return claudeModelName;
                }
            }
            
            // Fallback to cache service default
            log.debug("Using fallback model for cache key: {}", cacheService.getCurrentProviderModel());
            return cacheService.getCurrentProviderModel();
            
        } catch (Exception e) {
            log.warn("Failed to get current model name, using fallback: {}", e.getMessage());
            return cacheService.getCurrentProviderModel();
        }
    }
    
    /**
     * Initialize empty results when initialization fails
     */
    private void initializeEmptyResults() {
        ListToolsResult emptyResult = new ListToolsResult();
        emptyResult.setTools(new ArrayList<>());
        storeListToolsResult(emptyResult);
        storeListReources(new ArrayList<>());
        storeListPrompts(new ArrayList<>());
    }
}