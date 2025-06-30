package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 error object.
 * 
 * Follows the JSON-RPC specification for error responses.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcError {
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // Application-specific error codes (should be in range -32000 to -32099)
    public static final int TOOL_EXECUTION_ERROR = -32000;
    public static final int TOOL_TIMEOUT_ERROR = -32001;
    public static final int TOOL_NOT_FOUND_ERROR = -32002;
    public static final int PARAMETER_VALIDATION_ERROR = -32003;
    
    @JsonProperty("code")
    private int code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    // Constructors
    public JsonRpcError() {}
    
    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public JsonRpcError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // Static factory methods for common errors
    public static JsonRpcError parseError() {
        return new JsonRpcError(PARSE_ERROR, "Parse error");
    }
    
    public static JsonRpcError invalidRequest() {
        return new JsonRpcError(INVALID_REQUEST, "Invalid Request");
    }
    
    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method);
    }
    
    public static JsonRpcError invalidParams(String message) {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params: " + message);
    }
    
    public static JsonRpcError internalError(String message) {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error: " + message);
    }
    
    public static JsonRpcError toolExecutionError(String message) {
        return new JsonRpcError(TOOL_EXECUTION_ERROR, "Tool execution error: " + message);
    }
    
    public static JsonRpcError toolTimeoutError(String toolName, long timeoutMs) {
        return new JsonRpcError(TOOL_TIMEOUT_ERROR, 
            "Tool '" + toolName + "' timed out after " + timeoutMs + "ms");
    }
    
    public static JsonRpcError toolNotFound(String toolName) {
        return new JsonRpcError(TOOL_NOT_FOUND_ERROR, "Tool not found: " + toolName);
    }
    
    public static JsonRpcError parameterValidationError(String message) {
        return new JsonRpcError(PARAMETER_VALIDATION_ERROR, 
            "Parameter validation error: " + message);
    }
    
    // Getters and Setters
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "JsonRpcError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}