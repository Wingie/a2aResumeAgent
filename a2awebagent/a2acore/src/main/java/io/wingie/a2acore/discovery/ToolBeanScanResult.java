package io.wingie.a2acore.discovery;

import io.wingie.a2acore.domain.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of scanning a single Spring bean for @Action tools.
 * 
 * Contains both the Tool metadata and the bean instance mappings
 * discovered from a single Spring bean class.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public class ToolBeanScanResult {
    
    private final List<Tool> tools;
    private final Map<String, Object> toolBeans;
    
    /**
     * Creates an empty scan result.
     */
    public ToolBeanScanResult() {
        this.tools = new ArrayList<>();
        this.toolBeans = new HashMap<>();
    }
    
    /**
     * Creates a scan result with initial data.
     * 
     * @param tools List of discovered tools
     * @param toolBeans Map of tool name to bean instance
     */
    public ToolBeanScanResult(List<Tool> tools, Map<String, Object> toolBeans) {
        this.tools = new ArrayList<>(tools);
        this.toolBeans = new HashMap<>(toolBeans);
    }
    
    /**
     * Adds a tool and its corresponding bean to the result.
     * 
     * @param tool Tool metadata
     * @param bean Spring bean instance containing the @Action method
     */
    public void addTool(Tool tool, Object bean) {
        tools.add(tool);
        toolBeans.put(tool.getName(), bean);
    }
    
    /**
     * Adds multiple tools from another scan result.
     * 
     * @param other Another scan result to merge
     */
    public void addAll(ToolBeanScanResult other) {
        tools.addAll(other.tools);
        toolBeans.putAll(other.toolBeans);
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
     * Checks if any tools were discovered.
     * 
     * @return true if tools were found
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }
    
    /**
     * Gets the number of discovered tools.
     * 
     * @return Tool count
     */
    public int getToolCount() {
        return tools.size();
    }
    
    @Override
    public String toString() {
        return String.format("ToolBeanScanResult{tools=%d, beans=%d}", 
            tools.size(), toolBeans.size());
    }
}