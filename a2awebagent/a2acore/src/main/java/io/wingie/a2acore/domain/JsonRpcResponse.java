package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 response.
 * 
 * Used for sending responses back to MCP clients following the JSON-RPC specification.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private JsonRpcError error;
    
    @JsonProperty("id")
    private Object id;
    
    // Constructors
    public JsonRpcResponse() {}
    
    public JsonRpcResponse(Object id) {
        this.id = id;
    }
    
    // Static factory methods for success responses
    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse(id);
        response.setResult(result);
        return response;
    }
    
    // Static factory methods for error responses
    public static JsonRpcResponse error(Object id, String message) {
        return error(id, JsonRpcError.INTERNAL_ERROR, message);
    }
    
    public static JsonRpcResponse error(Object id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse(id);
        response.setError(new JsonRpcError(code, message));
        return response;
    }
    
    public static JsonRpcResponse error(Object id, JsonRpcError error) {
        JsonRpcResponse response = new JsonRpcResponse(id);
        response.setError(error);
        return response;
    }
    
    // Predefined error responses
    public static JsonRpcResponse parseError(Object id) {
        return error(id, JsonRpcError.PARSE_ERROR, "Parse error");
    }
    
    public static JsonRpcResponse invalidRequest(Object id) {
        return error(id, JsonRpcError.INVALID_REQUEST, "Invalid Request");
    }
    
    public static JsonRpcResponse methodNotFound(Object id, String method) {
        return error(id, JsonRpcError.METHOD_NOT_FOUND, "Method not found: " + method);
    }
    
    public static JsonRpcResponse invalidParams(Object id, String message) {
        return error(id, JsonRpcError.INVALID_PARAMS, "Invalid params: " + message);
    }
    
    // Getters and Setters
    public String getJsonrpc() {
        return jsonrpc;
    }
    
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public JsonRpcError getError() {
        return error;
    }
    
    public void setError(JsonRpcError error) {
        this.error = error;
    }
    
    public Object getId() {
        return id;
    }
    
    public void setId(Object id) {
        this.id = id;
    }
    
    // Utility methods
    public boolean isSuccess() {
        return error == null;
    }
    
    public boolean isError() {
        return error != null;
    }
    
    @Override
    public String toString() {
        return "JsonRpcResponse{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", result=" + result +
                ", error=" + error +
                ", id=" + id +
                '}';
    }
}