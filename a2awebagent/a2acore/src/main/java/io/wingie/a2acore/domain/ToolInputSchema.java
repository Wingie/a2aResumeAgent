package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents the input schema for an MCP tool.
 * 
 * Defines the structure and types of parameters that a tool accepts,
 * following JSON Schema specification for MCP protocol compliance.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolInputSchema {
    
    @JsonProperty("type")
    private String type = "object";
    
    @JsonProperty("properties")
    private Map<String, ToolPropertySchema> properties;
    
    @JsonProperty("required")
    private List<String> required;
    
    @JsonProperty("additionalProperties")
    private Boolean additionalProperties = false;
    
    @JsonProperty("description")
    private String description;
    
    // Constructors
    public ToolInputSchema() {}
    
    public ToolInputSchema(Map<String, ToolPropertySchema> properties, List<String> required) {
        this.properties = properties;
        this.required = required;
    }
    
    // Builder pattern
    public static ToolInputSchemaBuilder builder() {
        return new ToolInputSchemaBuilder();
    }
    
    public static class ToolInputSchemaBuilder {
        private String type = "object";
        private Map<String, ToolPropertySchema> properties;
        private List<String> required;
        private Boolean additionalProperties = false;
        private String description;
        
        public ToolInputSchemaBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public ToolInputSchemaBuilder properties(Map<String, ToolPropertySchema> properties) {
            this.properties = properties;
            return this;
        }
        
        public ToolInputSchemaBuilder required(List<String> required) {
            this.required = required;
            return this;
        }
        
        public ToolInputSchemaBuilder additionalProperties(Boolean additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }
        
        public ToolInputSchemaBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public ToolInputSchema build() {
            ToolInputSchema schema = new ToolInputSchema(properties, required);
            schema.setType(type);
            schema.setAdditionalProperties(additionalProperties);
            schema.setDescription(description);
            return schema;
        }
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, ToolPropertySchema> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, ToolPropertySchema> properties) {
        this.properties = properties;
    }
    
    public List<String> getRequired() {
        return required;
    }
    
    public void setRequired(List<String> required) {
        this.required = required;
    }
    
    public Boolean getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setAdditionalProperties(Boolean additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "ToolInputSchema{" +
                "type='" + type + '\'' +
                ", properties=" + properties +
                ", required=" + required +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}