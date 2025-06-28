package io.wingie;

import io.github.vishalmysore.mcp.server.MCPToolsController;
import io.github.vishalmysore.mcp.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/v1")
public class MCPController extends MCPToolsController {

    private static final Logger logger = Logger.getLogger(MCPController.class.getName());

    public MCPController() {
        super();
        setServerName("WingstonTravelAgent");
        setVersion("1.0.0");
        setProtocolVersion("2024-11-05");
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, List<Tool>>> listTools() {
        logger.info("MCP listTools endpoint called");
        return super.listTools();
    }

    @PostMapping("/tools/call")
    public ResponseEntity<JSONRPCResponse> callTool(@RequestBody ToolCallRequest request) {
        logger.info("MCP callTool endpoint called with tool: " + request.getName());
        return super.callTool(request);
    }

    @GetMapping("/resources")
    public ResponseEntity<ListResourcesResult> listResources() {
        logger.info("MCP listResources endpoint called");
        return ResponseEntity.ok(getResourcesResult());
    }

    @GetMapping("/prompts")
    public ResponseEntity<ListPromptsResult> listPrompts() {
        logger.info("MCP listPrompts endpoint called");
        return ResponseEntity.ok(getPromptsResult());
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        logger.info("MCP config endpoint called");
        return super.getServerConfig();
    }
}