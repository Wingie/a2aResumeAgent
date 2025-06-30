package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the result of a tool call in MCP-compatible format.
 * 
 * Wraps tool execution results in the format expected by Claude Desktop
 * and other MCP clients that require content to be in an array structure.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallResult {
    
    @JsonProperty("content")
    private List<Content> content;
    
    @JsonProperty("isError")
    private Boolean isError = false;
    
    // Constructors
    public ToolCallResult() {}
    
    public ToolCallResult(List<Content> content) {
        this.content = content;
        this.isError = false;
    }
    
    public ToolCallResult(List<Content> content, Boolean isError) {
        this.content = content;
        this.isError = isError;
    }
    
    // Static factory methods for success responses
    public static ToolCallResult success(Content content) {
        ToolCallResult result = new ToolCallResult();
        result.content = List.of(content);
        result.isError = false;
        return result;
    }
    
    public static ToolCallResult success(List<Content> content) {
        ToolCallResult result = new ToolCallResult();
        result.content = content;
        result.isError = false;
        return result;
    }
    
    // Static factory methods for error responses
    public static ToolCallResult error(String errorMessage) {
        ToolCallResult result = new ToolCallResult();
        result.content = List.of(TextContent.of("Error: " + errorMessage));
        result.isError = true;
        return result;
    }
    
    public static ToolCallResult error(Content errorContent) {
        ToolCallResult result = new ToolCallResult();
        result.content = List.of(errorContent);
        result.isError = true;
        return result;
    }
    
    // Getters and Setters
    public List<Content> getContent() {
        return content;
    }
    
    public void setContent(List<Content> content) {
        this.content = content;
    }
    
    public Boolean getIsError() {
        return isError;
    }
    
    public void setIsError(Boolean isError) {
        this.isError = isError;
    }
    
    // Utility methods
    public boolean isSuccess() {
        return !Boolean.TRUE.equals(isError);
    }
    
    public boolean hasError() {
        return Boolean.TRUE.equals(isError);
    }
    
    /**
     * Adds additional content to the result.
     * 
     * @param additionalContent Content to add
     */
    public void addContent(Content additionalContent) {
        if (this.content == null) {
            this.content = List.of(additionalContent);
        } else {
            this.content = new java.util.ArrayList<>(this.content);
            this.content.add(additionalContent);
        }
    }
    
    @Override
    public String toString() {
        return "ToolCallResult{" +
                "content=" + content +
                ", isError=" + isError +
                '}';
    }
}