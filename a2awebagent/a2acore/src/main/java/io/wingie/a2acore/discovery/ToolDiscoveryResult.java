package io.wingie.a2acore.discovery;

import io.wingie.a2acore.domain.Tool;

import java.util.List;
import java.util.Map;

/**
 * Result of unified tool and bean discovery operation.
 * 
 * Contains both Tool metadata and the corresponding Spring bean instances
 * needed for tool execution. This ensures atomic discovery and prevents
 * tool/bean mapping inconsistencies.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public class ToolDiscoveryResult {
    
    private final List<Tool> tools;
    private final Map<String, Object> toolBeans;
    
    /**
     * Creates a new discovery result.
     * 
     * @param tools List of discovered Tool metadata
     * @param toolBeans Map of tool name to Spring bean instance
     */
    public ToolDiscoveryResult(List<Tool> tools, Map<String, Object> toolBeans) {
        this.tools = tools;
        this.toolBeans = toolBeans;
    }
    
    /**
     * Gets the discovered tools.
     * 
     * @return List of Tool metadata
     */
    public List<Tool> getTools() {
        return tools;
    }
    
    /**
     * Gets the tool-to-bean mapping.
     * 
     * @return Map of tool name to Spring bean instance
     */
    public Map<String, Object> getToolBeans() {
        return toolBeans;
    }
    
    /**
     * Gets the number of discovered tools.
     * 
     * @return Tool count
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * Gets the number of mapped beans.
     * 
     * @return Bean count
     */
    public int getBeanCount() {
        return toolBeans.size();
    }
    
    /**
     * Validates that tools and beans are consistent.
     * 
     * @return true if tool count equals bean count and all tools have corresponding beans
     */
    public boolean isConsistent() {
        if (tools.size() != toolBeans.size()) {
            return false;
        }
        
        // Check that every tool has a corresponding bean
        for (Tool tool : tools) {
            if (!toolBeans.containsKey(tool.getName())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets a summary of the discovery result for logging.
     * 
     * @return Summary string
     */
    public String getSummary() {
        return String.format("ToolDiscoveryResult{tools=%d, beans=%d, consistent=%s}", 
            getToolCount(), getBeanCount(), isConsistent());
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}