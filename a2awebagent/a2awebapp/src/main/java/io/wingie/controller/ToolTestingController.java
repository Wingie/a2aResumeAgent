package io.wingie.controller;

import io.wingie.a2acore.discovery.StaticToolRegistry;
import io.wingie.a2acore.domain.*;
import io.wingie.a2acore.server.JsonRpcHandler;
import io.wingie.dto.ToolTestResult;
import io.wingie.dto.ToolTestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the interactive tool testing interface.
 * Provides a user-friendly web interface to test all available tools
 * with parameter input forms and result display.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/tools-test")
public class ToolTestingController {

    private final StaticToolRegistry toolRegistry;
    private final JsonRpcHandler jsonRpcHandler;

    /**
     * Main tool testing interface page
     */
    @GetMapping
    public String toolTestingInterface(Model model) {
        log.debug("Loading tool testing interface");
        
        try {
            // Get all available tools
            List<Tool> allTools = toolRegistry.getAllTools();
            
            // Group tools by category based on their agent annotations
            Map<String, List<Tool>> toolsByCategory = groupToolsByCategory(allTools);
            
            // Add statistics
            Map<String, Object> stats = Map.of(
                "totalTools", allTools.size(),
                "categories", toolsByCategory.size(),
                "toolsWithParameters", allTools.stream()
                    .mapToLong(tool -> tool.getInputSchema() != null && 
                        tool.getInputSchema().getProperties() != null ? 
                        tool.getInputSchema().getProperties().size() : 0)
                    .sum()
            );
            
            model.addAttribute("toolsByCategory", toolsByCategory);
            model.addAttribute("stats", stats);
            model.addAttribute("pageTitle", "Interactive Tool Testing Interface");
            
            return "tool-testing";
            
        } catch (Exception e) {
            log.error("Error loading tool testing interface", e);
            model.addAttribute("error", "Failed to load tools: " + e.getMessage());
            return "error";
        }
    }

    /**
     * API endpoint to get tool details
     */
    @GetMapping("/api/tool/{toolName}")
    @ResponseBody
    public ResponseEntity<Tool> getToolDetails(@PathVariable String toolName) {
        try {
            Optional<Tool> tool = toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst();
            
            if (tool.isPresent()) {
                return ResponseEntity.ok(tool.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting tool details for: {}", toolName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API endpoint to test a tool
     */
    @PostMapping("/api/test")
    @ResponseBody
    public ResponseEntity<ToolTestResult> testTool(@RequestBody ToolTestRequest testRequest) {
        log.info("Testing tool: {} with parameters: {}", testRequest.getToolName(), testRequest.getParameters());
        
        long startTime = System.currentTimeMillis();
        ToolTestResult result = new ToolTestResult();
        result.setToolName(testRequest.getToolName());
        result.setParameters(testRequest.getParameters());
        result.setStartTime(LocalDateTime.now());
        
        try {
            // Validate tool exists
            Optional<Tool> toolOpt = toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals(testRequest.getToolName()))
                .findFirst();
            
            if (toolOpt.isEmpty()) {
                result.setSuccess(false);
                result.setError("Tool not found: " + testRequest.getToolName());
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return ResponseEntity.badRequest().body(result);
            }
            
            Tool tool = toolOpt.get();
            result.setToolDescription(tool.getDescription());
            
            // Create tool call request
            ToolCallRequest toolCallRequest = new ToolCallRequest();
            toolCallRequest.setName(testRequest.getToolName());
            toolCallRequest.setArguments(testRequest.getParameters());
            
            // Create JSON-RPC request
            JsonRpcRequest jsonRpcRequest = new JsonRpcRequest("tools/call", toolCallRequest, "test-" + UUID.randomUUID());
            
            // Execute the tool
            JsonRpcResponse response = jsonRpcHandler.handleCallTool(jsonRpcRequest);
            
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setEndTime(LocalDateTime.now());
            
            if (response.isSuccess()) {
                result.setSuccess(true);
                result.setResult(response.getResult());
                log.info("Tool test successful for: {} in {}ms", testRequest.getToolName(), result.getExecutionTimeMs());
            } else {
                result.setSuccess(false);
                result.setError(response.getError() != null ? response.getError().getMessage() : "Unknown error");
                log.warn("Tool test failed for: {} - {}", testRequest.getToolName(), result.getError());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("Exception during tool execution: " + e.getMessage());
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setEndTime(LocalDateTime.now());
            
            log.error("Error testing tool: {}", testRequest.getToolName(), e);
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * API endpoint to get all tools summary
     */
    @GetMapping("/api/tools")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllTools() {
        try {
            List<Tool> allTools = toolRegistry.getAllTools();
            
            Map<String, Object> response = Map.of(
                "tools", allTools,
                "count", allTools.size(),
                "categories", groupToolsByCategory(allTools),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting all tools", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        try {
            int toolCount = toolRegistry.getAllTools().size();
            
            Map<String, Object> health = Map.of(
                "status", toolCount > 0 ? "UP" : "DOWN",
                "toolCount", toolCount,
                "registryInitialized", toolRegistry.getStatistics() != null,
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Error checking health", e);
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.internalServerError().body(health);
        }
    }

    /**
     * Helper method to group tools by category/agent
     */
    private Map<String, List<Tool>> groupToolsByCategory(List<Tool> tools) {
        // Simple categorization based on tool names and descriptions
        Map<String, List<Tool>> categories = new LinkedHashMap<>();
        
        for (Tool tool : tools) {
            String category = categorizeToolByName(tool.getName(), tool.getDescription());
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(tool);
        }
        
        return categories;
    }

    /**
     * Categorize tool based on name and description
     */
    private String categorizeToolByName(String toolName, String description) {
        String lowerName = toolName.toLowerCase();
        String lowerDesc = description != null ? description.toLowerCase() : "";
        
        if (lowerName.contains("taste") || lowerName.contains("food") || lowerDesc.contains("food")) {
            return "Food Safety & Waste Prevention";
        }
        if (lowerName.contains("linkedin") || lowerDesc.contains("linkedin") || lowerDesc.contains("professional")) {
            return "Professional Networking";
        }
        if (lowerName.contains("wingston") || lowerName.contains("resume") || lowerDesc.contains("expertise")) {
            return "Portfolio & Resume";
        }
        if (lowerName.contains("browse") || lowerName.contains("web") || lowerDesc.contains("web") || lowerDesc.contains("browse")) {
            return "Web Automation";
        }
        if (lowerName.contains("hello") || lowerName.contains("search") || lowerDesc.contains("search")) {
            return "Search & Demo";
        }
        if (lowerName.contains("screenshot") || lowerDesc.contains("screenshot") || lowerDesc.contains("image")) {
            return "Screenshot & Visual";
        }
        
        return "General Tools";
    }
}