package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents image content in MCP responses.
 * 
 * Used for returning images from tool executions, typically as base64-encoded data.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageContent extends Content {
    
    @JsonProperty("data")
    private String data;
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    // Constructors
    public ImageContent() {
        super("image");
    }
    
    public ImageContent(String data, String mimeType) {
        super("image");
        this.data = data;
        this.mimeType = mimeType;
    }
    
    // Static factory methods
    public static ImageContent png(String base64Data) {
        return new ImageContent(base64Data, "image/png");
    }
    
    public static ImageContent jpeg(String base64Data) {
        return new ImageContent(base64Data, "image/jpeg");
    }
    
    public static ImageContent of(String base64Data, String mimeType) {
        return new ImageContent(base64Data, mimeType);
    }
    
    // Getters and Setters
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    @Override
    public String toString() {
        return "ImageContent{" +
                "data='" + (data != null ? data.substring(0, Math.min(50, data.length())) + "..." : null) + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}