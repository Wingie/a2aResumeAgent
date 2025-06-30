package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an MCP tool definition.
 * 
 * This is the core data structure that describes a tool available 
 * for AI agents to call via the MCP protocol.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private ToolInputSchema inputSchema;
    
    @JsonProperty("annotations")
    private ToolAnnotations annotations;
    
    // Constructors
    public Tool() {}
    
    public Tool(String name, String description, ToolInputSchema inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }
    
    // Builder pattern for easier construction
    public static ToolBuilder builder() {
        return new ToolBuilder();
    }
    
    public static class ToolBuilder {
        private String name;
        private String description;
        private ToolInputSchema inputSchema;
        private ToolAnnotations annotations;
        
        public ToolBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public ToolBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public ToolBuilder inputSchema(ToolInputSchema inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }
        
        public ToolBuilder annotations(ToolAnnotations annotations) {
            this.annotations = annotations;
            return this;
        }
        
        public Tool build() {
            Tool tool = new Tool(name, description, inputSchema);
            tool.setAnnotations(annotations);
            return tool;
        }
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ToolInputSchema getInputSchema() {
        return inputSchema;
    }
    
    public void setInputSchema(ToolInputSchema inputSchema) {
        this.inputSchema = inputSchema;
    }
    
    public ToolAnnotations getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(ToolAnnotations annotations) {
        this.annotations = annotations;
    }
    
    // Utility methods
    @Override
    public String toString() {
        return "Tool{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema=" + inputSchema +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tool tool = (Tool) o;
        return name != null ? name.equals(tool.name) : tool.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}