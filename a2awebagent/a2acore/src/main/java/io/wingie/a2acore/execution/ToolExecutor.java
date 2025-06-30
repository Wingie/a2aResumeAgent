package io.wingie.a2acore.execution;

import io.wingie.a2acore.config.A2aCoreProperties;
import io.wingie.a2acore.domain.Tool;
import io.wingie.a2acore.domain.ToolCallRequest;
import io.wingie.a2acore.discovery.StaticToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Core tool execution engine with method invocation and parameter mapping.
 * 
 * Handles the actual execution of @Action annotated methods with proper
 * parameter conversion, timeout handling, and result serialization.
 * 
 * Target performance: <100ms execution overhead.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Service
public class ToolExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);
    
    private final StaticToolRegistry toolRegistry;
    private final ParameterMapper parameterMapper;
    private final ResultSerializer resultSerializer;
    private final A2aCoreProperties properties;
    
    // Cache for method metadata to avoid repeated reflection
    private final Map<String, ToolMethod> methodCache = new ConcurrentHashMap<>();
    
    public ToolExecutor(StaticToolRegistry toolRegistry,
                       ParameterMapper parameterMapper, 
                       ResultSerializer resultSerializer,
                       A2aCoreProperties properties) {
        this.toolRegistry = toolRegistry;
        this.parameterMapper = parameterMapper;
        this.resultSerializer = resultSerializer;
        this.properties = properties;
    }
    
    /**
     * Executes a tool call synchronously.
     * 
     * @param request The tool call request
     * @return The execution result
     * @throws ToolExecutionException if execution fails
     */
    public Object execute(ToolCallRequest request) throws ToolExecutionException {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new ToolExecutionException("Tool name cannot be empty");
            }
            
            // Get tool metadata
            Tool tool = toolRegistry.getTool(request.getName());
            if (tool == null) {
                throw new ToolExecutionException("Tool not found: " + request.getName());
            }
            
            // Get method metadata (cached for performance)
            ToolMethod toolMethod = getOrCreateToolMethod(tool, request.getName());
            
            // Map parameters
            Object[] args = parameterMapper.mapArguments(request.getArguments(), toolMethod);
            
            // Execute with timeout
            Object result = executeWithTimeout(toolMethod, args, getTimeoutMs(tool));
            
            // Serialize result
            Object serializedResult = resultSerializer.serialize(result);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (properties.getLogging().isLogToolExecution()) {
                log.info("Executed tool '{}' in {}ms", request.getName(), duration);
            }
            
            // Performance warning
            if (duration > 5000) { // 5 seconds
                log.warn("Tool '{}' took {}ms to execute - consider optimization", 
                    request.getName(), duration);
            }
            
            return serializedResult;
            
        } catch (ToolExecutionException e) {
            log.error("Tool execution failed for '{}': {}", request.getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error executing tool '{}'", request.getName(), e);
            throw new ToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes a tool call asynchronously.
     * 
     * @param request The tool call request
     * @return CompletableFuture with the execution result
     */
    public CompletableFuture<Object> executeAsync(ToolCallRequest request) {
        if (!properties.getExecution().isEnableAsync()) {
            // If async is disabled, execute synchronously but wrap in CompletableFuture
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execute(request);
                } catch (ToolExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(request);
            } catch (ToolExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Gets all registered tools from the registry.
     */
    public List<Tool> getAllTools() {
        return toolRegistry.getAllTools();
    }
    
    /**
     * Checks if a tool is available for execution.
     */
    public boolean hasToolImpl(String toolName) {
        return toolRegistry.hasToolImpl(toolName);
    }
    
    /**
     * Executes a method with timeout protection.
     */
    private Object executeWithTimeout(ToolMethod toolMethod, Object[] args, long timeoutMs) 
            throws ToolExecutionException {
        
        if (timeoutMs <= 0) {
            // No timeout - execute directly
            return invokeMethod(toolMethod, args);
        }
        
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return invokeMethod(toolMethod, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new ToolExecutionException(
                String.format("Tool '%s' timed out after %dms", toolMethod.getToolName(), timeoutMs));
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof ToolExecutionException) {
                throw (ToolExecutionException) cause;
            }
            throw new ToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Invokes the actual method.
     */
    private Object invokeMethod(ToolMethod toolMethod, Object[] args) throws ToolExecutionException {
        try {
            Method method = toolMethod.getMethod();
            Object bean = toolMethod.getBean();
            
            // Make method accessible if needed
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            
            return method.invoke(bean, args);
            
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw new ToolExecutionException("Tool execution failed: " + cause.getMessage(), cause);
            }
            throw new ToolExecutionException("Tool execution failed: " + cause.getMessage(), cause);
        } catch (Exception e) {
            throw new ToolExecutionException("Method invocation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets or creates a ToolMethod object for the given tool.
     */
    private ToolMethod getOrCreateToolMethod(Tool tool, String toolName) throws ToolExecutionException {
        return methodCache.computeIfAbsent(toolName, name -> {
            try {
                Object bean = toolRegistry.getToolBean(toolName);
                if (bean == null) {
                    throw new RuntimeException("No bean found for tool: " + toolName);
                }
                
                // Find the method with @Action annotation matching this tool name
                Class<?> clazz = bean.getClass();
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(io.wingie.a2acore.annotation.Action.class)) {
                        io.wingie.a2acore.annotation.Action actionAnnotation = 
                            method.getAnnotation(io.wingie.a2acore.annotation.Action.class);
                        
                        String methodToolName = actionAnnotation.name().isEmpty() ? 
                            method.getName() : actionAnnotation.name();
                        
                        if (methodToolName.equals(toolName)) {
                            return new ToolMethod(toolName, method, bean, actionAnnotation);
                        }
                    }
                }
                
                throw new RuntimeException("No @Action method found for tool: " + toolName);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ToolMethod for " + toolName, e);
            }
        });
    }
    
    /**
     * Gets the timeout for a tool execution.
     */
    private long getTimeoutMs(Tool tool) {
        // Check tool-specific timeout
        if (tool.getAnnotations() != null && tool.getAnnotations().getTimeoutMs() != null) {
            return tool.getAnnotations().getTimeoutMs();
        }
        
        // Use default timeout
        return properties.getExecution().getDefaultTimeoutMs();
    }
    
    /**
     * Gets execution statistics.
     */
    public ExecutionStatistics getStatistics() {
        return new ExecutionStatistics(
            methodCache.size(),
            toolRegistry.getStatistics().getToolCount()
        );
    }
    
    /**
     * Statistics about tool execution.
     */
    public static class ExecutionStatistics {
        private final int cachedMethods;
        private final int availableTools;
        
        public ExecutionStatistics(int cachedMethods, int availableTools) {
            this.cachedMethods = cachedMethods;
            this.availableTools = availableTools;
        }
        
        public int getCachedMethods() { return cachedMethods; }
        public int getAvailableTools() { return availableTools; }
        
        @Override
        public String toString() {
            return String.format("ExecutionStatistics{cachedMethods=%d, availableTools=%d}", 
                cachedMethods, availableTools);
        }
    }
}