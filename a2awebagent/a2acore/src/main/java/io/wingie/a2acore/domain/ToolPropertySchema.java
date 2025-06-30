package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a property schema within a tool's input schema.
 * 
 * Defines the type, description, and validation rules for individual
 * parameters in MCP tool definitions.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolPropertySchema {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("default")
    private Object defaultValue;
    
    @JsonProperty("enum")
    private List<Object> enumValues;
    
    @JsonProperty("pattern")
    private String pattern;
    
    @JsonProperty("minimum")
    private Double minimum;
    
    @JsonProperty("maximum")
    private Double maximum;
    
    @JsonProperty("minLength")
    private Integer minLength;
    
    @JsonProperty("maxLength")
    private Integer maxLength;
    
    @JsonProperty("example")
    private Object example;
    
    // Constructors
    public ToolPropertySchema() {}
    
    public ToolPropertySchema(String type, String description) {
        this.type = type;
        this.description = description;
    }
    
    // Builder pattern
    public static ToolPropertySchemaBuilder builder() {
        return new ToolPropertySchemaBuilder();
    }
    
    public static class ToolPropertySchemaBuilder {
        private String type;
        private String description;
        private Object defaultValue;
        private List<Object> enumValues;
        private String pattern;
        private Double minimum;
        private Double maximum;
        private Integer minLength;
        private Integer maxLength;
        private Object example;
        
        public ToolPropertySchemaBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public ToolPropertySchemaBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public ToolPropertySchemaBuilder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        
        public ToolPropertySchemaBuilder enumValues(List<Object> enumValues) {
            this.enumValues = enumValues;
            return this;
        }
        
        public ToolPropertySchemaBuilder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }
        
        public ToolPropertySchemaBuilder minimum(Double minimum) {
            this.minimum = minimum;
            return this;
        }
        
        public ToolPropertySchemaBuilder maximum(Double maximum) {
            this.maximum = maximum;
            return this;
        }
        
        public ToolPropertySchemaBuilder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }
        
        public ToolPropertySchemaBuilder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }
        
        public ToolPropertySchemaBuilder example(Object example) {
            this.example = example;
            return this;
        }
        
        public ToolPropertySchema build() {
            ToolPropertySchema schema = new ToolPropertySchema(type, description);
            schema.setDefaultValue(defaultValue);
            schema.setEnumValues(enumValues);
            schema.setPattern(pattern);
            schema.setMinimum(minimum);
            schema.setMaximum(maximum);
            schema.setMinLength(minLength);
            schema.setMaxLength(maxLength);
            schema.setExample(example);
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public List<Object> getEnumValues() {
        return enumValues;
    }
    
    public void setEnumValues(List<Object> enumValues) {
        this.enumValues = enumValues;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public Double getMinimum() {
        return minimum;
    }
    
    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }
    
    public Double getMaximum() {
        return maximum;
    }
    
    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }
    
    public Integer getMinLength() {
        return minLength;
    }
    
    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }
    
    public Integer getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    
    public Object getExample() {
        return example;
    }
    
    public void setExample(Object example) {
        this.example = example;
    }
    
    @Override
    public String toString() {
        return "ToolPropertySchema{" +
                "type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }
}