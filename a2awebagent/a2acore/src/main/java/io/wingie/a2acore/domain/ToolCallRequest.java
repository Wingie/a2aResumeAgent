package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a request to call a specific MCP tool.
 * 
 * Contains the tool name and arguments for execution.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallRequest {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("arguments")
    private Map<String, Object> arguments;
    
    // Constructors
    public ToolCallRequest() {}
    
    public ToolCallRequest(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
    
    // Utility methods
    public Object getArgument(String key) {
        return arguments != null ? arguments.get(key) : null;
    }
    
    public String getArgumentAsString(String key) {
        Object value = getArgument(key);
        return value != null ? value.toString() : null;
    }
    
    public Integer getArgumentAsInteger(String key) {
        Object value = getArgument(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Boolean getArgumentAsBoolean(String key) {
        Object value = getArgument(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "ToolCallRequest{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}