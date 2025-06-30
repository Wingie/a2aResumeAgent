package io.wingie.a2acore.execution;

import io.wingie.a2acore.annotation.Action;

import java.lang.reflect.Method;

/**
 * Represents a cached method for tool execution.
 * 
 * Contains all metadata needed for fast method invocation including
 * the method reference, bean instance, and annotation metadata.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public class ToolMethod {
    
    private final String toolName;
    private final Method method;
    private final Object bean;
    private final Action actionAnnotation;
    
    public ToolMethod(String toolName, Method method, Object bean, Action actionAnnotation) {
        this.toolName = toolName;
        this.method = method;
        this.bean = bean;
        this.actionAnnotation = actionAnnotation;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public Object getBean() {
        return bean;
    }
    
    public Action getActionAnnotation() {
        return actionAnnotation;
    }
    
    /**
     * Gets the parameter types for this method.
     */
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }
    
    /**
     * Gets the parameter count for this method.
     */
    public int getParameterCount() {
        return method.getParameterCount();
    }
    
    /**
     * Gets the method parameters with reflection metadata.
     */
    public java.lang.reflect.Parameter[] getParameters() {
        return method.getParameters();
    }
    
    /**
     * Gets the timeout for this method from annotation.
     */
    public long getTimeoutMs() {
        return actionAnnotation.timeoutMs();
    }
    
    /**
     * Checks if this method is enabled.
     */
    public boolean isEnabled() {
        return actionAnnotation.enabled();
    }
    
    @Override
    public String toString() {
        return "ToolMethod{" +
                "toolName='" + toolName + '\'' +
                ", method=" + method.getName() +
                ", bean=" + bean.getClass().getSimpleName() +
                '}';
    }
}