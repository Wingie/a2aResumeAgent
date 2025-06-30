package io.wingie.dto;

import lombok.Data;
import java.util.Map;

/**
 * Request DTO for tool testing operations.
 * Contains the tool name and parameters to be used for testing.
 */
@Data
public class ToolTestRequest {
    
    /**
     * Name of the tool to test
     */
    private String toolName;
    
    /**
     * Parameters to pass to the tool
     */
    private Map<String, Object> parameters;
    
    /**
     * Optional timeout for the test (in milliseconds)
     */
    private Long timeoutMs;
    
    /**
     * Whether to capture detailed execution information
     */
    private boolean captureDetails = true;
    
    /**
     * Optional user context for the test
     */
    private String userContext;
}