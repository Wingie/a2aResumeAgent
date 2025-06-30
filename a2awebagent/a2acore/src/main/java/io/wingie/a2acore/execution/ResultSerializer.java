package io.wingie.a2acore.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wingie.a2acore.domain.Content;
import io.wingie.a2acore.domain.ImageContent;
import io.wingie.a2acore.domain.TextContent;
import io.wingie.a2acore.domain.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Serializes tool execution results to MCP-compatible format.
 * 
 * Handles conversion of various Java return types to appropriate
 * MCP content types including text, images, and structured data.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class ResultSerializer {
    
    private static final Logger log = LoggerFactory.getLogger(ResultSerializer.class);
    
    private final ObjectMapper objectMapper;
    
    public ResultSerializer() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Serializes a tool execution result to MCP-compatible ToolCallResult format.
     * 
     * @param result The raw result from method execution
     * @return ToolCallResult wrapped for Claude Desktop compatibility
     */
    public ToolCallResult serialize(Object result) {
        if (result == null) {
            return ToolCallResult.success(TextContent.of("Tool executed successfully with no output"));
        }
        
        try {
            // Handle MCP Content types directly
            if (result instanceof Content) {
                return ToolCallResult.success((Content) result);
            }
            
            // Handle basic types
            if (result instanceof String) {
                return ToolCallResult.success(serializeString((String) result));
            }
            
            if (result instanceof Number || result instanceof Boolean) {
                return ToolCallResult.success(TextContent.of(result.toString()));
            }
            
            // Handle collections and maps
            if (result instanceof List || result instanceof Map) {
                return ToolCallResult.success(serializeStructuredData(result));
            }
            
            // Handle byte arrays (potential images)
            if (result instanceof byte[]) {
                return ToolCallResult.success(serializeByteArray((byte[]) result));
            }
            
            // Handle custom objects
            return ToolCallResult.success(serializeObject(result));
            
        } catch (Exception e) {
            log.error("Failed to serialize result of type {}", 
                result.getClass().getSimpleName(), e);
            return ToolCallResult.error("Error serializing result: " + e.getMessage());
        }
    }
    
    /**
     * Serializes string results, detecting base64 images.
     */
    private Content serializeString(String result) {
        // Check if it looks like a base64 encoded image
        if (isBase64Image(result)) {
            String mimeType = detectImageMimeType(result);
            return ImageContent.of(result, mimeType);
        }
        
        // Regular text content
        return TextContent.of(result);
    }
    
    /**
     * Serializes structured data (Lists, Maps) to JSON text.
     */
    private Content serializeStructuredData(Object result) {
        try {
            String jsonString = objectMapper.writeValueAsString(result);
            
            // For small JSON, return as text. For large JSON, might want to truncate
            if (jsonString.length() > 10000) {
                log.warn("Large JSON result ({} chars) - consider pagination", jsonString.length());
            }
            
            return TextContent.of(jsonString);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize structured data to JSON", e);
            return TextContent.of("Error: Unable to serialize structured data");
        }
    }
    
    /**
     * Serializes byte arrays, typically as base64 images.
     */
    private Content serializeByteArray(byte[] bytes) {
        try {
            // Convert to base64
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            
            // Assume PNG if we can't detect type
            return ImageContent.png(base64);
            
        } catch (Exception e) {
            log.error("Failed to serialize byte array", e);
            return TextContent.of("Error: Unable to serialize binary data");
        }
    }
    
    /**
     * Serializes custom objects using Jackson.
     */
    private Content serializeObject(Object result) {
        try {
            // First try to convert to a simple string representation
            String stringRepresentation = result.toString();
            
            // If toString() gives useful info, use it
            if (isUsefulToString(stringRepresentation, result.getClass())) {
                return TextContent.of(stringRepresentation);
            }
            
            // Otherwise, serialize to JSON
            String jsonString = objectMapper.writeValueAsString(result);
            return TextContent.of(jsonString);
            
        } catch (Exception e) {
            log.error("Failed to serialize object of type {}", result.getClass().getSimpleName(), e);
            return TextContent.of("Result: " + result.getClass().getSimpleName() + " object");
        }
    }
    
    /**
     * Checks if a string looks like a base64 encoded image.
     */
    private boolean isBase64Image(String data) {
        if (data == null || data.length() < 100) {
            return false;
        }
        
        // Remove data URL prefix if present
        String base64Data = data;
        if (data.startsWith("data:image/")) {
            int commaIndex = data.indexOf(',');
            if (commaIndex > 0) {
                base64Data = data.substring(commaIndex + 1);
            }
        }
        
        // Check if it's valid base64 and has reasonable length for an image
        try {
            java.util.Base64.getDecoder().decode(base64Data);
            return base64Data.length() > 1000; // Arbitrary threshold for image size
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Detects MIME type from base64 image data.
     */
    private String detectImageMimeType(String data) {
        if (data.startsWith("data:image/")) {
            int semicolonIndex = data.indexOf(';');
            if (semicolonIndex > 0) {
                return data.substring(5, semicolonIndex); // Extract "image/png" etc.
            }
        }
        
        // Try to detect from base64 content
        try {
            String base64Data = data;
            if (data.contains(",")) {
                base64Data = data.substring(data.indexOf(',') + 1);
            }
            
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data.substring(0, Math.min(100, base64Data.length())));
            
            // Check PNG magic bytes
            if (decoded.length > 8 && 
                decoded[0] == (byte) 0x89 && decoded[1] == 'P' && 
                decoded[2] == 'N' && decoded[3] == 'G') {
                return "image/png";
            }
            
            // Check JPEG magic bytes
            if (decoded.length > 2 && 
                decoded[0] == (byte) 0xFF && decoded[1] == (byte) 0xD8) {
                return "image/jpeg";
            }
            
        } catch (Exception e) {
            log.debug("Failed to detect image type", e);
        }
        
        // Default to PNG
        return "image/png";
    }
    
    /**
     * Checks if toString() provides useful information.
     */
    private boolean isUsefulToString(String stringRep, Class<?> clazz) {
        // Avoid generic toString() like "com.example.Class@1a2b3c4d"
        String className = clazz.getSimpleName();
        return !stringRep.contains("@") || 
               !stringRep.startsWith(className + "@") &&
               !stringRep.startsWith(clazz.getName() + "@");
    }
}