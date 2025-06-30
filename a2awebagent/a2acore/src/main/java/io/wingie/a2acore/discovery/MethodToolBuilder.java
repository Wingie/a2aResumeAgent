package io.wingie.a2acore.discovery;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.domain.Tool;
import io.wingie.a2acore.domain.ToolAnnotations;
import io.wingie.a2acore.domain.ToolInputSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds Tool objects from @Action annotated methods using STATIC descriptions.
 * 
 * NO AI PROCESSING - uses hardcoded descriptions for instant startup performance.
 * Target: <1ms per tool conversion.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class MethodToolBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(MethodToolBuilder.class);
    
    private final SchemaGenerator schemaGenerator;
    
    // STATIC TOOL DESCRIPTIONS - No AI needed!
    private static final Map<String, String> STATIC_DESCRIPTIONS = new HashMap<>();
    
    static {
        // Web browsing tools
        STATIC_DESCRIPTIONS.put("browseWebAndReturnText", 
            "Automates web browsing tasks using Playwright and returns text content from the web pages");
        STATIC_DESCRIPTIONS.put("browseWebAndReturnImage", 
            "Automates web browsing tasks using Playwright and returns screenshot images of web pages");
        STATIC_DESCRIPTIONS.put("takeCurrentPageScreenshot", 
            "Captures a screenshot of the current web page and returns it as base64 encoded image");
        
        // Portfolio and expertise tools
        STATIC_DESCRIPTIONS.put("getWingstonsProjectsExpertiseResume", 
            "Provides detailed information about Wingston's technical projects, skills, and professional expertise");
        
        // Food safety tools
        STATIC_DESCRIPTIONS.put("askTasteBeforeYouWaste", 
            "Analyzes food safety questions and provides recommendations about food freshness and safety");
        STATIC_DESCRIPTIONS.put("getTasteBeforeYouWasteScreenshot", 
            "Captures screenshots of the Taste Before You Waste food safety application interface");
        
        // LinkedIn tools
        STATIC_DESCRIPTIONS.put("searchLinkedInProfile", 
            "Searches for LinkedIn profiles and extracts professional information and connections");
        
        // Demo/test tools
        STATIC_DESCRIPTIONS.put("searchHelloWorld", 
            "A simple demonstration tool that performs basic search operations for testing purposes");
        
        // Add more static descriptions as needed for other tools
        STATIC_DESCRIPTIONS.put("searchWeb", 
            "Performs web searches using search engines and returns relevant results");
        STATIC_DESCRIPTIONS.put("extractText", 
            "Extracts and processes text content from various sources including web pages and documents");
        STATIC_DESCRIPTIONS.put("analyzeImage", 
            "Analyzes images and provides descriptions or extracts information from visual content");
    }
    
    public MethodToolBuilder(SchemaGenerator schemaGenerator) {
        this.schemaGenerator = schemaGenerator;
    }
    
    /**
     * Builds a Tool from an @Action annotated method using static descriptions.
     * Ultra-fast conversion without AI processing.
     */
    public Tool buildTool(Method method, Object bean, Action actionAnnotation, Agent agentAnnotation) {
        long startTime = System.nanoTime();
        
        try {
            // Get tool name (from annotation or method name)
            String toolName = getToolName(method, actionAnnotation);
            
            // Get STATIC description (no AI!)
            String description = getStaticDescription(toolName, actionAnnotation);
            
            // Generate input schema (fast reflection-based)
            ToolInputSchema inputSchema = schemaGenerator.generateSchema(method);
            
            // Create tool annotations
            ToolAnnotations annotations = createToolAnnotations(actionAnnotation, agentAnnotation);
            
            // Build the tool
            Tool tool = Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .annotations(annotations)
                .build();
            
            long durationNanos = System.nanoTime() - startTime;
            log.debug("Built tool '{}' in {:.2f}ms", toolName, durationNanos / 1_000_000.0);
            
            return tool;
            
        } catch (Exception e) {
            log.error("Failed to build tool for method {}", method.getName(), e);
            throw new RuntimeException("Tool building failed for method: " + method.getName(), e);
        }
    }
    
    /**
     * Gets the tool name from annotation or method name.
     */
    private String getToolName(Method method, Action actionAnnotation) {
        String annotationName = actionAnnotation.name().trim();
        return !annotationName.isEmpty() ? annotationName : method.getName();
    }
    
    /**
     * Gets STATIC description - no AI processing!
     */
    private String getStaticDescription(String toolName, Action actionAnnotation) {
        // First try annotation description
        String annotationDescription = actionAnnotation.description().trim();
        if (!annotationDescription.isEmpty()) {
            return annotationDescription;
        }
        
        // Then try static map
        String staticDescription = STATIC_DESCRIPTIONS.get(toolName);
        if (staticDescription != null) {
            return staticDescription;
        }
        
        // Fallback to generic description
        return "Tool for " + toolName.replaceAll("([A-Z])", " $1").toLowerCase().trim();
    }
    
    /**
     * Creates tool annotations from Agent and Action metadata.
     */
    private ToolAnnotations createToolAnnotations(Action actionAnnotation, Agent agentAnnotation) {
        ToolAnnotations.ToolAnnotationsBuilder builder = ToolAnnotations.builder();
        
        // Agent-level annotations
        if (agentAnnotation != null) {
            builder.groupName(agentAnnotation.groupName())
                   .groupDescription(agentAnnotation.groupDescription())
                   .version(agentAnnotation.version())
                   .priority(agentAnnotation.priority());
        }
        
        // Action-level annotations
        if (actionAnnotation.examples().length > 0) {
            builder.examples(java.util.Arrays.asList(actionAnnotation.examples()));
        }
        
        if (actionAnnotation.timeoutMs() > 0) {
            builder.timeoutMs(actionAnnotation.timeoutMs());
        }
        
        builder.enabled(actionAnnotation.enabled());
        
        return builder.build();
    }
    
    /**
     * Adds a static description for a tool name.
     * Allows dynamic registration of descriptions if needed.
     */
    public static void addStaticDescription(String toolName, String description) {
        STATIC_DESCRIPTIONS.put(toolName, description);
        log.info("Added static description for tool: {}", toolName);
    }
    
    /**
     * Gets all registered static descriptions.
     */
    public static Map<String, String> getStaticDescriptions() {
        return new HashMap<>(STATIC_DESCRIPTIONS);
    }
    
    /**
     * Checks if a static description exists for a tool.
     */
    public static boolean hasStaticDescription(String toolName) {
        return STATIC_DESCRIPTIONS.containsKey(toolName);
    }
}