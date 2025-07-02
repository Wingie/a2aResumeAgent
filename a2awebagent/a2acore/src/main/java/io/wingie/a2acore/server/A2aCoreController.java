package io.wingie.a2acore.server;

import io.wingie.a2acore.config.A2aCoreProperties;
import io.wingie.a2acore.discovery.StaticToolRegistry;
import io.wingie.a2acore.discovery.ToolDiscoveryResult;
import io.wingie.a2acore.discovery.ToolDiscoveryService;
import io.wingie.a2acore.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.List;
import java.util.Map;

/**
 * Main MCP controller providing unified tool discovery and execution endpoints.
 * 
 * Replaces the dual controller architecture with a single, fast controller
 * that eliminates startup AI calls and provides <5 second initialization.
 * 
 * This is the core REST API for a2acore MCP framework.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class A2aCoreController {
    
    private static final Logger log = LoggerFactory.getLogger(A2aCoreController.class);
    
    private final ToolDiscoveryService discoveryService;
    private final StaticToolRegistry toolRegistry;
    private final JsonRpcHandler jsonRpcHandler;
    private final A2aCoreProperties properties;
    
    private volatile boolean initialized = false;
    private long initializationTimeMs = 0;
    
    public A2aCoreController(ToolDiscoveryService discoveryService,
                            StaticToolRegistry toolRegistry,
                            JsonRpcHandler jsonRpcHandler,
                            A2aCoreProperties properties) {
        this.discoveryService = discoveryService;
        this.toolRegistry = toolRegistry;
        this.jsonRpcHandler = jsonRpcHandler;
        this.properties = properties;
    }
    
    /**
     * Fast initialization - TARGET: <5 seconds total, <100ms discovery.
     * Uses ApplicationReadyEvent to avoid circular dependency issues.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("A2ACore framework starting initialization...");
            
            // Step 1: Unified discovery of tools and beans (target: <100ms)
            ToolDiscoveryResult discoveryResult = discoveryService.discoverToolsAndBeans();
            
            // Step 2: Validate discovery results
            if (!discoveryResult.isConsistent()) {
                throw new RuntimeException("Tool discovery failed consistency validation: " + discoveryResult.getSummary());
            }
            
            // Step 3: Register tools with their corresponding beans (target: <10ms)
            toolRegistry.registerTools(discoveryResult.getTools(), discoveryResult.getToolBeans());
            
            initializationTimeMs = System.currentTimeMillis() - startTime;
            initialized = true;
            
            if (properties.getLogging().isLogToolDiscovery()) {
                log.info("A2ACore initialized {} tools with {} beans in {}ms", 
                    discoveryResult.getToolCount(), discoveryResult.getBeanCount(), initializationTimeMs);
                
                if (properties.getLogging().isLogPerformanceMetrics()) {
                    logInitializationMetrics(discoveryResult);
                }
            }
            
            // Performance validation
            if (initializationTimeMs > properties.getDiscovery().getMaxInitializationTimeMs()) {
                log.warn("Initialization took {}ms, exceeding target of {}ms", 
                    initializationTimeMs, properties.getDiscovery().getMaxInitializationTimeMs());
            } else {
                log.info("✅ Fast initialization achieved: {}ms (target: {}ms)", 
                    initializationTimeMs, properties.getDiscovery().getMaxInitializationTimeMs());
            }
            
        } catch (Exception e) {
            log.error("A2ACore initialization failed", e);
            throw new RuntimeException("A2ACore framework initialization failed", e);
        }
    }
    
    /**
     * GET /v1/tools - List all available tools (MCP protocol).
     */
    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListToolsResult> listTools() {
        if (!initialized) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ListToolsResult(List.of()));
        }
        
        try {
            JsonRpcRequest request = new JsonRpcRequest("tools/list", null, "list-tools");
            JsonRpcResponse response = jsonRpcHandler.handleListTools(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok((ListToolsResult) response.getResult());
            } else {
                log.error("Failed to list tools: {}", response.getError());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ListToolsResult(List.of()));
            }
            
        } catch (Exception e) {
            log.error("Error listing tools", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ListToolsResult(List.of()));
        }
    }
    
    /**
     * POST /v1/tools/call - Execute a tool (MCP protocol).
     * 
     * @deprecated Use the main POST /v1 endpoint with JSON-RPC method "tools/call" instead.
     * This endpoint is maintained for backward compatibility but may be removed in future versions.
     * 
     * Recommended usage:
     * POST /v1 with {"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "...", "arguments": {...}}, "id": 1}
     */
    @PostMapping(value = "/tools/call", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<JsonRpcResponse> callTool(@RequestBody ToolCallRequest toolCallRequest) {
        if (!initialized) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(JsonRpcResponse.error("call-tool", "A2ACore not initialized"));
        }
        
        try {
            // Create JSON-RPC request
            JsonRpcRequest jsonRpcRequest = new JsonRpcRequest("tools/call", toolCallRequest, "call-tool");
            
            // Handle the request
            JsonRpcResponse response = jsonRpcHandler.handleCallTool(jsonRpcRequest);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                // Return error with appropriate HTTP status
                HttpStatus httpStatus = mapJsonRpcErrorToHttpStatus(response.getError());
                return ResponseEntity.status(httpStatus).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error calling tool: {}", toolCallRequest.getName(), e);
            JsonRpcResponse errorResponse = JsonRpcResponse.error("call-tool", 
                JsonRpcError.internalError(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * POST /v1 - Generic JSON-RPC endpoint for full MCP protocol support.
     */
    @PostMapping(value = "", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonRpcResponse> handleJsonRpc(@RequestBody JsonRpcRequest request) {
        if (!initialized) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(JsonRpcResponse.error(request.getId(), "A2ACore not initialized"));
        }
        
        try {
            // Validate request format
            if (!jsonRpcHandler.isValidJsonRpcRequest(request)) {
                return ResponseEntity.badRequest()
                    .body(jsonRpcHandler.handleMalformedRequest(request.getId(), "Invalid JSON-RPC request"));
            }
            
            // Route based on method
            JsonRpcResponse response;
            switch (request.getMethod()) {
                case "tools/list":
                    response = jsonRpcHandler.handleListTools(request);
                    break;
                case "tools/call":
                    response = jsonRpcHandler.handleCallTool(request);
                    break;
                default:
                    response = jsonRpcHandler.handleUnknownMethod(request);
                    break;
            }
            
            // Map to appropriate HTTP status
            HttpStatus httpStatus = response.isSuccess() ? HttpStatus.OK : 
                mapJsonRpcErrorToHttpStatus(response.getError());
            
            return ResponseEntity.status(httpStatus).body(response);
            
        } catch (Exception e) {
            log.error("Error handling JSON-RPC request", e);
            JsonRpcResponse errorResponse = JsonRpcResponse.error(request.getId(), 
                JsonRpcError.internalError(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * GET /v1/health - Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", initialized ? "UP" : "DOWN",
            "initialized", initialized,
            "initializationTimeMs", initializationTimeMs,
            "toolCount", initialized ? toolRegistry.getStatistics().getToolCount() : 0,
            "framework", "a2acore",
            "version", "1.0.0"
        );
        
        HttpStatus status = initialized ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
    
    /**
     * GET /v1/metrics - Performance and usage metrics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        if (!initialized) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Not initialized"));
        }
        
        Map<String, Object> metrics = Map.of(
            "initialization", Map.of(
                "timeMs", initializationTimeMs,
                "targetMs", properties.getDiscovery().getMaxInitializationTimeMs(),
                "performanceAchieved", initializationTimeMs <= properties.getDiscovery().getMaxInitializationTimeMs()
            ),
            "registry", toolRegistry.getStatistics(),
            "discovery", discoveryService.getStatistics(),
            "handler", jsonRpcHandler.getStatistics(),
            "configuration", Map.of(
                "scanPackages", properties.getDiscovery().getScanPackages(),
                "defaultTimeoutMs", properties.getExecution().getDefaultTimeoutMs(),
                "cacheEnabled", properties.getCache().isEnabled()
            )
        );
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Maps JSON-RPC error codes to HTTP status codes.
     */
    private HttpStatus mapJsonRpcErrorToHttpStatus(JsonRpcError error) {
        if (error == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        switch (error.getCode()) {
            case JsonRpcError.PARSE_ERROR:
                return HttpStatus.BAD_REQUEST;
            case JsonRpcError.INVALID_REQUEST:
                return HttpStatus.BAD_REQUEST;
            case JsonRpcError.METHOD_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case JsonRpcError.INVALID_PARAMS:
                return HttpStatus.BAD_REQUEST;
            case JsonRpcError.TOOL_NOT_FOUND_ERROR:
                return HttpStatus.NOT_FOUND;
            case JsonRpcError.TOOL_TIMEOUT_ERROR:
                return HttpStatus.REQUEST_TIMEOUT;
            case JsonRpcError.PARAMETER_VALIDATION_ERROR:
                return HttpStatus.BAD_REQUEST;
            case JsonRpcError.TOOL_EXECUTION_ERROR:
            case JsonRpcError.INTERNAL_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * Logs detailed initialization metrics.
     */
    private void logInitializationMetrics(ToolDiscoveryResult discoveryResult) {
        log.info("A2ACore Initialization Metrics:");
        log.info("  📊 Total time: {}ms", initializationTimeMs);
        log.info("  🔧 Tools discovered: {}", discoveryResult.getToolCount());
        log.info("  🗂️ Beans mapped: {}", discoveryResult.getBeanCount());
        log.info("  🔗 Tool/Bean consistency: {}", discoveryResult.isConsistent() ? "✅ VALID" : "❌ INVALID");
        log.info("  ⚡ Avg time per tool: {:.2f}ms", 
            discoveryResult.getToolCount() > 0 ? (double) initializationTimeMs / discoveryResult.getToolCount() : 0);
        log.info("  🎯 Performance target: {}ms", properties.getDiscovery().getMaxInitializationTimeMs());
        log.info("  ✅ Target achieved: {}", initializationTimeMs <= properties.getDiscovery().getMaxInitializationTimeMs());
        
        // Log discovered tools
        if (properties.getLogging().isLogToolDiscovery()) {
            log.info("Discovered tools with beans:");
            discoveryResult.getTools().forEach(tool -> 
                log.info("  - {} ({}) → bean: {}", 
                    tool.getName(), 
                    tool.getDescription(),
                    discoveryResult.getToolBeans().get(tool.getName()) != null ? "✅" : "❌"));
        }
    }
    
    /**
     * Gets controller statistics.
     */
    public ControllerStatistics getStatistics() {
        return new ControllerStatistics(initialized, initializationTimeMs, 
            initialized ? toolRegistry.getStatistics().getToolCount() : 0);
    }
    
    /**
     * Statistics about the controller.
     */
    public static class ControllerStatistics {
        private final boolean initialized;
        private final long initializationTimeMs;
        private final int toolCount;
        
        public ControllerStatistics(boolean initialized, long initializationTimeMs, int toolCount) {
            this.initialized = initialized;
            this.initializationTimeMs = initializationTimeMs;
            this.toolCount = toolCount;
        }
        
        public boolean isInitialized() { return initialized; }
        public long getInitializationTimeMs() { return initializationTimeMs; }
        public int getToolCount() { return toolCount; }
        
        @Override
        public String toString() {
            return String.format("ControllerStatistics{initialized=%s, initTime=%dms, tools=%d}", 
                initialized, initializationTimeMs, toolCount);
        }
    }
}