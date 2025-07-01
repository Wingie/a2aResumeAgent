package io.wingie.a2acore.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wingie.a2acore.domain.*;
import io.wingie.a2acore.execution.ToolExecutor;
import io.wingie.a2acore.execution.ToolExecutionException;
import io.wingie.a2acore.integration.ToolExecutionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles JSON-RPC 2.0 request processing for MCP protocol.
 * 
 * Provides standardized request/response handling with proper error
 * formatting and protocol compliance for MCP tools.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class JsonRpcHandler {
    
    private static final Logger log = LoggerFactory.getLogger(JsonRpcHandler.class);
    
    private final ToolExecutor toolExecutor;
    private final ToolExecutionAdapter toolExecutionAdapter;
    private final ObjectMapper objectMapper;
    
    public JsonRpcHandler(ToolExecutor toolExecutor, ToolExecutionAdapter toolExecutionAdapter) {
        this.toolExecutor = toolExecutor;
        this.toolExecutionAdapter = toolExecutionAdapter;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Handles a tools/list request.
     * 
     * @param request The JSON-RPC request
     * @return JSON-RPC response with list of available tools
     */
    public JsonRpcResponse handleListTools(JsonRpcRequest request) {
        try {
            log.debug("Processing tools/list request");
            
            ListToolsResult result = new ListToolsResult(toolExecutor.getAllTools());
            
            return JsonRpcResponse.success(request.getId(), result);
            
        } catch (Exception e) {
            log.error("Failed to list tools", e);
            return JsonRpcResponse.error(request.getId(), JsonRpcError.internalError(e.getMessage()));
        }
    }
    
    /**
     * Handles a tools/call request.
     * 
     * @param request The JSON-RPC request
     * @return JSON-RPC response with tool execution result
     */
    public JsonRpcResponse handleCallTool(JsonRpcRequest request) {
        try {
            // Extract tool call parameters
            ToolCallRequest toolCall = extractToolCallRequest(request);
            
            if (toolCall == null) {
                return JsonRpcResponse.invalidParams(request.getId(), "Missing or invalid tool call parameters");
            }
            
            log.debug("Processing tools/call request for tool: {}", toolCall.getName());
            
            // Validate tool exists
            if (!toolExecutionAdapter.hasToolImpl(toolCall.getName())) {
                return JsonRpcResponse.error(request.getId(), 
                    JsonRpcError.toolNotFound(toolCall.getName()));
            }
            
            // Execute the tool with real-time tracking and SSE broadcasting
            Object result = toolExecutionAdapter.executeWithIntegration(toolCall);
            
            return JsonRpcResponse.success(request.getId(), result);
            
        } catch (ToolExecutionException e) {
            log.error("Tool execution failed", e);
            return createToolExecutionErrorResponse(request.getId(), e);
        } catch (Exception e) {
            log.error("Unexpected error in tool call", e);
            return JsonRpcResponse.error(request.getId(), JsonRpcError.internalError(e.getMessage()));
        }
    }
    
    /**
     * Handles unknown methods.
     * 
     * @param request The JSON-RPC request
     * @return JSON-RPC error response
     */
    public JsonRpcResponse handleUnknownMethod(JsonRpcRequest request) {
        log.warn("Unknown method requested: {}", request.getMethod());
        return JsonRpcResponse.methodNotFound(request.getId(), request.getMethod());
    }
    
    /**
     * Handles malformed requests.
     * 
     * @param requestId The request ID if available
     * @param error The parsing error
     * @return JSON-RPC error response
     */
    public JsonRpcResponse handleMalformedRequest(Object requestId, String error) {
        log.error("Malformed request: {}", error);
        return JsonRpcResponse.invalidRequest(requestId);
    }
    
    /**
     * Extracts ToolCallRequest from JSON-RPC request parameters.
     */
    private ToolCallRequest extractToolCallRequest(JsonRpcRequest request) {
        try {
            Object params = request.getParams();
            if (params == null) {
                return null;
            }
            
            // Handle different parameter formats
            if (params instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) params;
                
                String toolName = (String) paramMap.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) paramMap.get("arguments");
                
                if (toolName == null) {
                    return null;
                }
                
                return new ToolCallRequest(toolName, arguments);
            }
            
            // Try to convert using ObjectMapper
            return objectMapper.convertValue(params, ToolCallRequest.class);
            
        } catch (Exception e) {
            log.error("Failed to extract tool call request", e);
            return null;
        }
    }
    
    /**
     * Creates appropriate error response for tool execution failures.
     */
    private JsonRpcResponse createToolExecutionErrorResponse(Object requestId, ToolExecutionException e) {
        switch (e.getErrorType()) {
            case TOOL_NOT_FOUND:
                return JsonRpcResponse.error(requestId, JsonRpcError.toolNotFound(e.getToolName()));
            
            case TIMEOUT:
                return JsonRpcResponse.error(requestId, JsonRpcError.toolTimeoutError(
                    e.getToolName(), extractTimeoutFromMessage(e.getMessage())));
            
            case PARAMETER_MAPPING_ERROR:
                return JsonRpcResponse.error(requestId, JsonRpcError.parameterValidationError(e.getMessage()));
            
            case METHOD_INVOCATION_ERROR:
            case SERIALIZATION_ERROR:
            case VALIDATION_ERROR:
                return JsonRpcResponse.error(requestId, JsonRpcError.toolExecutionError(e.getMessage()));
            
            default:
                return JsonRpcResponse.error(requestId, JsonRpcError.internalError(e.getMessage()));
        }
    }
    
    /**
     * Extracts timeout value from error message.
     */
    private long extractTimeoutFromMessage(String message) {
        try {
            // Look for pattern like "after 30000ms"
            int afterIndex = message.indexOf("after ");
            if (afterIndex > 0) {
                int msIndex = message.indexOf("ms", afterIndex);
                if (msIndex > 0) {
                    String timeoutStr = message.substring(afterIndex + 6, msIndex);
                    return Long.parseLong(timeoutStr);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    /**
     * Validates JSON-RPC request format.
     */
    public boolean isValidJsonRpcRequest(JsonRpcRequest request) {
        if (request == null) {
            return false;
        }
        
        // Check JSON-RPC version
        if (!"2.0".equals(request.getJsonrpc())) {
            return false;
        }
        
        // Check method is present
        if (request.getMethod() == null || request.getMethod().trim().isEmpty()) {
            return false;
        }
        
        // For requests (not notifications), ID must be present
        if (request.getId() == null && !request.isNotification()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets handler statistics.
     */
    public HandlerStatistics getStatistics() {
        return new HandlerStatistics();
    }
    
    /**
     * Statistics about JSON-RPC request handling.
     */
    public static class HandlerStatistics {
        private long totalRequests = 0;
        private long successfulRequests = 0;
        private long failedRequests = 0;
        private long averageResponseTimeMs = 0;
        
        // Getters would be here for monitoring
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getAverageResponseTimeMs() { return averageResponseTimeMs; }
        
        @Override
        public String toString() {
            return String.format("HandlerStatistics{total=%d, success=%d, failed=%d, avgTime=%dms}", 
                totalRequests, successfulRequests, failedRequests, averageResponseTimeMs);
        }
    }
}