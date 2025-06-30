package io.wingie.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result DTO for tool testing operations.
 * Contains the execution results, timing information, and any errors.
 */
@Data
public class ToolTestResult {
    
    /**
     * Name of the tool that was tested
     */
    private String toolName;
    
    /**
     * Description of the tool
     */
    private String toolDescription;
    
    /**
     * Parameters that were passed to the tool
     */
    private Map<String, Object> parameters;
    
    /**
     * Whether the tool execution was successful
     */
    private boolean success;
    
    /**
     * The result returned by the tool (if successful)
     */
    private Object result;
    
    /**
     * Error message (if execution failed)
     */
    private String error;
    
    /**
     * When the test started
     */
    private LocalDateTime startTime;
    
    /**
     * When the test ended
     */
    private LocalDateTime endTime;
    
    /**
     * Execution time in milliseconds
     */
    private long executionTimeMs;
    
    /**
     * Additional metadata about the execution
     */
    private Map<String, Object> metadata;
    
    /**
     * Whether the result contains sensitive data that should be filtered
     */
    private boolean containsSensitiveData = false;
    
    /**
     * Category of the tool that was tested
     */
    private String toolCategory;
    
    /**
     * Size of the result (characters for text, bytes for binary)
     */
    private Long resultSize;
}