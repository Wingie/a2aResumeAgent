package io.wingie.a2acore.execution;

/**
 * Exception thrown when tool execution fails.
 * 
 * Provides structured error information for tool execution failures
 * including timeout, parameter validation, and method invocation errors.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public class ToolExecutionException extends Exception {
    
    private final String toolName;
    private final ErrorType errorType;
    
    public enum ErrorType {
        TOOL_NOT_FOUND,
        PARAMETER_MAPPING_ERROR,
        TIMEOUT,
        METHOD_INVOCATION_ERROR,
        SERIALIZATION_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
    
    public ToolExecutionException(String message) {
        super(message);
        this.toolName = null;
        this.errorType = ErrorType.UNKNOWN_ERROR;
    }
    
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.toolName = null;
        this.errorType = ErrorType.UNKNOWN_ERROR;
    }
    
    public ToolExecutionException(String toolName, ErrorType errorType, String message) {
        super(message);
        this.toolName = toolName;
        this.errorType = errorType;
    }
    
    public ToolExecutionException(String toolName, ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
        this.errorType = errorType;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Creates a tool not found exception.
     */
    public static ToolExecutionException toolNotFound(String toolName) {
        return new ToolExecutionException(toolName, ErrorType.TOOL_NOT_FOUND, 
            "Tool not found: " + toolName);
    }
    
    /**
     * Creates a timeout exception.
     */
    public static ToolExecutionException timeout(String toolName, long timeoutMs) {
        return new ToolExecutionException(toolName, ErrorType.TIMEOUT, 
            String.format("Tool '%s' timed out after %dms", toolName, timeoutMs));
    }
    
    /**
     * Creates a parameter mapping exception.
     */
    public static ToolExecutionException parameterMapping(String toolName, String message) {
        return new ToolExecutionException(toolName, ErrorType.PARAMETER_MAPPING_ERROR, 
            "Parameter mapping failed for tool '" + toolName + "': " + message);
    }
    
    /**
     * Creates a method invocation exception.
     */
    public static ToolExecutionException methodInvocation(String toolName, String message, Throwable cause) {
        return new ToolExecutionException(toolName, ErrorType.METHOD_INVOCATION_ERROR, 
            "Method invocation failed for tool '" + toolName + "': " + message, cause);
    }
    
    @Override
    public String toString() {
        return String.format("ToolExecutionException{toolName='%s', errorType=%s, message='%s'}", 
            toolName, errorType, getMessage());
    }
}