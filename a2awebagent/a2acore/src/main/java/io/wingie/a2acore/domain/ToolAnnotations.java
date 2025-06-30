package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents additional annotations for MCP tools.
 * 
 * Contains metadata that enhances tool descriptions and behavior
 * beyond the basic MCP protocol requirements.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolAnnotations {
    
    @JsonProperty("groupName")
    private String groupName;
    
    @JsonProperty("groupDescription")
    private String groupDescription;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("examples")
    private List<String> examples;
    
    @JsonProperty("timeoutMs")
    private Long timeoutMs;
    
    @JsonProperty("enabled")
    private Boolean enabled = true;
    
    @JsonProperty("priority")
    private Integer priority = 0;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public ToolAnnotations() {}
    
    public ToolAnnotations(String groupName, String groupDescription) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
    }
    
    // Builder pattern
    public static ToolAnnotationsBuilder builder() {
        return new ToolAnnotationsBuilder();
    }
    
    public static class ToolAnnotationsBuilder {
        private String groupName;
        private String groupDescription;
        private String version;
        private List<String> examples;
        private Long timeoutMs;
        private Boolean enabled = true;
        private Integer priority = 0;
        private List<String> tags;
        private Map<String, Object> metadata;
        
        public ToolAnnotationsBuilder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }
        
        public ToolAnnotationsBuilder groupDescription(String groupDescription) {
            this.groupDescription = groupDescription;
            return this;
        }
        
        public ToolAnnotationsBuilder version(String version) {
            this.version = version;
            return this;
        }
        
        public ToolAnnotationsBuilder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }
        
        public ToolAnnotationsBuilder timeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public ToolAnnotationsBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public ToolAnnotationsBuilder priority(Integer priority) {
            this.priority = priority;
            return this;
        }
        
        public ToolAnnotationsBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public ToolAnnotationsBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public ToolAnnotations build() {
            ToolAnnotations annotations = new ToolAnnotations(groupName, groupDescription);
            annotations.setVersion(version);
            annotations.setExamples(examples);
            annotations.setTimeoutMs(timeoutMs);
            annotations.setEnabled(enabled);
            annotations.setPriority(priority);
            annotations.setTags(tags);
            annotations.setMetadata(metadata);
            return annotations;
        }
    }
    
    // Getters and Setters
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupDescription() {
        return groupDescription;
    }
    
    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<String> getExamples() {
        return examples;
    }
    
    public void setExamples(List<String> examples) {
        this.examples = examples;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "ToolAnnotations{" +
                "groupName='" + groupName + '\'' +
                ", groupDescription='" + groupDescription + '\'' +
                ", version='" + version + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}