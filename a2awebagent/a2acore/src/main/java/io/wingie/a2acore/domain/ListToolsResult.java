package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the result of listing available MCP tools.
 * 
 * Used as the response for the "tools/list" MCP method.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListToolsResult {
    
    @JsonProperty("tools")
    private List<Tool> tools;
    
    @JsonProperty("nextCursor")
    private String nextCursor;
    
    // Constructors
    public ListToolsResult() {}
    
    public ListToolsResult(List<Tool> tools) {
        this.tools = tools;
    }
    
    public ListToolsResult(List<Tool> tools, String nextCursor) {
        this.tools = tools;
        this.nextCursor = nextCursor;
    }
    
    // Getters and Setters
    public List<Tool> getTools() {
        return tools;
    }
    
    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }
    
    public String getNextCursor() {
        return nextCursor;
    }
    
    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
    
    // Utility methods
    public int getToolCount() {
        return tools != null ? tools.size() : 0;
    }
    
    public boolean hasNextPage() {
        return nextCursor != null && !nextCursor.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "ListToolsResult{" +
                "tools=" + (tools != null ? tools.size() + " tools" : "null") +
                ", nextCursor='" + nextCursor + '\'' +
                '}';
    }
}