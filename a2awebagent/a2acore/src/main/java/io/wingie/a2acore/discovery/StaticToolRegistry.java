package io.wingie.a2acore.discovery;

import io.wingie.a2acore.domain.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fast static tool registry for instant tool lookup.
 * 
 * Eliminates the need for AI-based tool registration by maintaining
 * an in-memory registry of all discovered tools.
 * 
 * Target: <1ms tool lookup time.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class StaticToolRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(StaticToolRegistry.class);
    
    // Thread-safe registry for high-performance concurrent access
    private final Map<String, Tool> toolRegistry = new ConcurrentHashMap<>();
    private final Map<String, Object> beanRegistry = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    
    /**
     * Registers all discovered tools for fast lookup.
     * Called once during startup - no AI processing.
     * 
     * Includes comprehensive validation to ensure tool/bean consistency.
     */
    public void registerTools(List<Tool> tools, Map<String, Object> toolBeans) {
        long startTime = System.currentTimeMillis();
        
        // Validate input parameters
        if (tools == null) {
            throw new IllegalArgumentException("Tools list cannot be null");
        }
        if (toolBeans == null) {
            throw new IllegalArgumentException("Tool beans map cannot be null");
        }
        
        // Critical validation: ensure tools and beans are consistent
        validateToolBeanConsistency(tools, toolBeans);
        
        // Clear existing registrations
        toolRegistry.clear();
        beanRegistry.clear();
        
        // Register each tool
        for (Tool tool : tools) {
            toolRegistry.put(tool.getName(), tool);
            log.debug("Registered tool: {}", tool.getName());
        }
        
        // Register associated beans for method invocation
        beanRegistry.putAll(toolBeans);
        
        initialized = true;
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Registered {} tools with {} beans in {}ms", tools.size(), toolBeans.size(), duration);
        
        // Performance validation
        if (duration > 10) {
            log.warn("Tool registration took {}ms - consider optimization", duration);
        }
        
        // Final validation
        if (getStatistics().getToolCount() != getStatistics().getBeanCount()) {
            log.error("CRITICAL: Registry inconsistency after registration - tools={}, beans={}", 
                getStatistics().getToolCount(), getStatistics().getBeanCount());
        }
    }
    
    /**
     * Validates that tools and beans are consistent and complete.
     */
    private void validateToolBeanConsistency(List<Tool> tools, Map<String, Object> toolBeans) {
        // Check for empty tools with non-empty beans or vice versa
        if (tools.isEmpty() && !toolBeans.isEmpty()) {
            throw new IllegalArgumentException("Tools list is empty but tool beans provided");
        }
        if (!tools.isEmpty() && toolBeans.isEmpty()) {
            log.error("CRITICAL: {} tools discovered but no bean instances provided", tools.size());
            throw new IllegalArgumentException(
                "Tools discovered but no bean instances provided - this causes 'No bean found' execution errors");
        }
        
        // Extract tool names for comparison
        Set<String> toolNames = tools.stream()
            .map(Tool::getName)
            .collect(Collectors.toSet());
        Set<String> beanToolNames = toolBeans.keySet();
        
        // Check for exact match
        if (!toolNames.equals(beanToolNames)) {
            log.error("CRITICAL: Tool/Bean mapping inconsistency");
            log.error("  Tool names: {}", toolNames);
            log.error("  Bean tool names: {}", beanToolNames);
            
            // Identify missing beans
            Set<String> missingBeans = toolNames.stream()
                .filter(name -> !beanToolNames.contains(name))
                .collect(Collectors.toSet());
            if (!missingBeans.isEmpty()) {
                log.error("  Missing beans for tools: {}", missingBeans);
            }
            
            // Identify orphaned beans
            Set<String> orphanedBeans = beanToolNames.stream()
                .filter(name -> !toolNames.contains(name))
                .collect(Collectors.toSet());
            if (!orphanedBeans.isEmpty()) {
                log.error("  Orphaned beans without tools: {}", orphanedBeans);
            }
            
            throw new IllegalArgumentException("Tool names and bean tool names don't match");
        }
        
        // Validate bean instances are not null
        for (Map.Entry<String, Object> entry : toolBeans.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Null bean instance for tool: " + entry.getKey());
            }
        }
        
        log.debug("✅ Tool/Bean consistency validation passed: {} tools with matching beans", tools.size());
    }
    
    /**
     * Fast tool lookup by name.
     */
    public Tool getTool(String toolName) {
        if (!initialized) {
            throw new IllegalStateException("Tool registry not initialized");
        }
        
        return toolRegistry.get(toolName);
    }
    
    /**
     * Gets all registered tools.
     */
    public List<Tool> getAllTools() {
        if (!initialized) {
            throw new IllegalStateException("Tool registry not initialized");
        }
        
        return List.copyOf(toolRegistry.values());
    }
    
    /**
     * Gets the bean associated with a tool for method invocation.
     */
    public Object getToolBean(String toolName) {
        return beanRegistry.get(toolName);
    }
    
    /**
     * Checks if a tool is registered.
     */
    public boolean hasToolImpl(String toolName) {
        return initialized && toolRegistry.containsKey(toolName);
    }
    
    /**
     * Gets registry statistics.
     */
    public RegistryStatistics getStatistics() {
        return new RegistryStatistics(
            toolRegistry.size(),
            beanRegistry.size(),
            initialized
        );
    }
    
    /**
     * Statistics about the tool registry.
     */
    public static class RegistryStatistics {
        private final int toolCount;
        private final int beanCount;
        private final boolean initialized;
        
        public RegistryStatistics(int toolCount, int beanCount, boolean initialized) {
            this.toolCount = toolCount;
            this.beanCount = beanCount;
            this.initialized = initialized;
        }
        
        public int getToolCount() { return toolCount; }
        public int getBeanCount() { return beanCount; }
        public boolean isInitialized() { return initialized; }
        
        @Override
        public String toString() {
            return String.format("RegistryStatistics{tools=%d, beans=%d, initialized=%s}", 
                toolCount, beanCount, initialized);
        }
    }
}