package io.wingie.a2acore.integration;

import io.wingie.a2acore.domain.ToolCallRequest;
import io.wingie.a2acore.execution.ToolExecutor;
import io.wingie.a2acore.execution.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Adapter that bridges a2acore tool execution with a2awebapp TaskExecution tracking.
 * 
 * This component uses ApplicationContext to access a2awebapp services without
 * creating a hard dependency, enabling real-time SSE broadcasting and task tracking
 * for MCP tool executions.
 */
@Component
public class ToolExecutionAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(ToolExecutionAdapter.class);
    
    private final ToolExecutor toolExecutor;
    private final ApplicationContext applicationContext;
    
    // Lazy-loaded reference to TaskExecutionIntegrationService
    private volatile Object taskIntegrationService;
    private volatile Method executeWithTrackingMethod;
    
    public ToolExecutionAdapter(ToolExecutor toolExecutor, ApplicationContext applicationContext) {
        this.toolExecutor = toolExecutor;
        this.applicationContext = applicationContext;
    }
    
    /**
     * Executes a tool with TaskExecution tracking and real-time SSE broadcasting.
     * 
     * Falls back to standard execution if TaskExecutionIntegrationService is not available.
     */
    public Object executeWithIntegration(ToolCallRequest request) throws ToolExecutionException {
        try {
            // Try to use TaskExecutionIntegrationService if available
            Object integrationService = getTaskIntegrationService();
            if (integrationService != null) {
                return executeWithTracking(request, integrationService);
            }
            
            // Fallback to standard execution
            log.debug("TaskExecutionIntegrationService not available, using standard execution");
            return toolExecutor.execute(request);
            
        } catch (Exception e) {
            log.error("Error in integrated tool execution for {}", request.getName(), e);
            
            // Always fallback to standard execution to maintain MCP functionality
            if (e instanceof ToolExecutionException) {
                throw e;
            } else {
                return toolExecutor.execute(request);
            }
        }
    }
    
    /**
     * Executes tool with tracking using reflection to avoid hard dependency.
     */
    private Object executeWithTracking(ToolCallRequest request, Object integrationService) throws ToolExecutionException {
        try {
            Method method = getExecuteWithTrackingMethod(integrationService);
            
            // Create supplier for actual tool execution
            Supplier<Object> toolExecution = () -> {
                try {
                    return toolExecutor.execute(request);
                } catch (ToolExecutionException e) {
                    throw new RuntimeException(e);
                }
            };
            
            // Convert arguments to JSON string for tracking
            String arguments = request.getArguments() != null ? request.getArguments().toString() : "";
            
            // Call executeWithTracking method
            return method.invoke(integrationService, request.getName(), arguments, toolExecution);
            
        } catch (Exception e) {
            log.error("Failed to execute tool with tracking: {}", e.getMessage());
            
            // Extract and re-throw ToolExecutionException if it's the root cause
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof ToolExecutionException) {
                    throw (ToolExecutionException) cause;
                }
                cause = cause.getCause();
            }
            
            // If not a ToolExecutionException, wrap it
            throw new ToolExecutionException("Tool execution with tracking failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lazy-loads TaskExecutionIntegrationService from ApplicationContext.
     */
    private Object getTaskIntegrationService() {
        if (taskIntegrationService == null) {
            try {
                // Use class name to avoid hard dependency
                Class<?> serviceClass = Class.forName("io.wingie.service.TaskExecutionIntegrationService");
                taskIntegrationService = applicationContext.getBean(serviceClass);
                log.info("✅ TaskExecutionIntegrationService found - enabling real-time tracking");
            } catch (Exception e) {
                log.debug("TaskExecutionIntegrationService not available: {}", e.getMessage());
                // Set to a marker object to avoid repeated attempts
                taskIntegrationService = new Object();
            }
        }
        
        // Return null if it's our marker object
        return taskIntegrationService.getClass().getSimpleName().equals("Object") ? null : taskIntegrationService;
    }
    
    /**
     * Gets the executeWithTracking method using reflection.
     */
    private Method getExecuteWithTrackingMethod(Object integrationService) throws NoSuchMethodException {
        if (executeWithTrackingMethod == null) {
            executeWithTrackingMethod = integrationService.getClass().getMethod(
                "executeWithTracking", 
                String.class,      // toolName
                String.class,      // arguments  
                Supplier.class     // toolExecution
            );
        }
        return executeWithTrackingMethod;
    }
    
    /**
     * Checks if the standard ToolExecutor has the tool implementation.
     */
    public boolean hasToolImpl(String toolName) {
        return toolExecutor.hasToolImpl(toolName);
    }
}