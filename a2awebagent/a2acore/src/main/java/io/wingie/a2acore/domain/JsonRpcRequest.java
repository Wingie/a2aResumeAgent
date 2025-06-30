package io.wingie.a2acore.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 request.
 * 
 * Used for handling MCP protocol communication following the JSON-RPC specification.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcRequest {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Object params;
    
    @JsonProperty("id")
    private Object id;
    
    // Constructors
    public JsonRpcRequest() {}
    
    public JsonRpcRequest(String method, Object params, Object id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }
    
    // Static factory methods
    public static JsonRpcRequest request(String method, Object params, Object id) {
        return new JsonRpcRequest(method, params, id);
    }
    
    public static JsonRpcRequest notification(String method, Object params) {
        return new JsonRpcRequest(method, params, null);
    }
    
    // Getters and Setters
    public String getJsonrpc() {
        return jsonrpc;
    }
    
    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public Object getParams() {
        return params;
    }
    
    public void setParams(Object params) {
        this.params = params;
    }
    
    public Object getId() {
        return id;
    }
    
    public void setId(Object id) {
        this.id = id;
    }
    
    // Utility methods
    public boolean isNotification() {
        return id == null;
    }
    
    public boolean isRequest() {
        return id != null;
    }
    
    @Override
    public String toString() {
        return "JsonRpcRequest{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", id=" + id +
                '}';
    }
}