package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents image content in MCP responses using URL references instead of base64 data.
 * 
 * This class is used for returning images from tool executions without sending
 * large base64 strings to the model. Instead, it provides HTTP URLs that both
 * Claude Desktop and frontend applications can use to display images.
 * 
 * Benefits:
 * - Reduces JSON payload size significantly
 * - Faster model response processing
 * - Better network efficiency
 * - Same display functionality for end users
 * 
 * @author a2acore
 * @since 1.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageContentUrl extends Content {
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("mimeType")
    private String mimeType;
    
    @JsonProperty("alt")
    private String alt;
    
    // Constructors
    public ImageContentUrl() {
        super("image");
    }
    
    public ImageContentUrl(String url, String mimeType) {
        super("image");
        this.url = url;
        this.mimeType = mimeType;
    }
    
    public ImageContentUrl(String url, String mimeType, String alt) {
        super("image");
        this.url = url;
        this.mimeType = mimeType;
        this.alt = alt;
    }
    
    // Static factory methods
    public static ImageContentUrl png(String url) {
        return new ImageContentUrl(url, "image/png");
    }
    
    public static ImageContentUrl jpeg(String url) {
        return new ImageContentUrl(url, "image/jpeg");
    }
    
    public static ImageContentUrl of(String url, String mimeType) {
        return new ImageContentUrl(url, mimeType);
    }
    
    public static ImageContentUrl withAlt(String url, String mimeType, String alt) {
        return new ImageContentUrl(url, mimeType, alt);
    }
    
    // Getters and Setters
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getAlt() {
        return alt;
    }
    
    public void setAlt(String alt) {
        this.alt = alt;
    }
    
    @Override
    public String toString() {
        return "ImageContentUrl{" +
                "url='" + url + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", alt='" + alt + '\'' +
                '}';
    }
}