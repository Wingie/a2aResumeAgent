package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents text content in MCP responses.
 * 
 * Used for returning textual data from tool executions.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextContent extends Content {
    
    @JsonProperty("text")
    private String text;
    
    // Constructors
    public TextContent() {
        super("text");
    }
    
    public TextContent(String text) {
        super("text");
        this.text = text;
    }
    
    // Static factory method
    public static TextContent of(String text) {
        return new TextContent(text);
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "TextContent{" +
                "text='" + text + '\'' +
                '}';
    }
}