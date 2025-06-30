package io.wingie.a2acore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP tool action that can be called by AI agents.
 * 
 * This annotation replaces the external tools4ai @Action annotation with
 * a fast, lightweight implementation that eliminates startup AI calls.
 * 
 * <p>Usage example:
 * <pre>
 * &#64;Action(description = "Searches the web for information")
 * public String searchWeb(String query) {
 *     // Implementation
 * }
 * </pre>
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
    
    /**
     * Human-readable description of what this action does.
     * This will be used as the tool description in MCP protocol.
     * 
     * @return the action description
     */
    String description() default "";
    
    /**
     * Optional custom name for the tool. If empty, the method name will be used.
     * 
     * @return the tool name
     */
    String name() default "";
    
    /**
     * Optional array of example usage patterns for this action.
     * These can be used for documentation and testing.
     * 
     * @return array of usage examples
     */
    String[] examples() default {};
    
    /**
     * Whether this action is enabled. Disabled actions won't be registered as tools.
     * 
     * @return true if enabled, false otherwise
     */
    boolean enabled() default true;
    
    /**
     * Optional timeout in milliseconds for this action execution.
     * If 0, uses the default timeout from configuration.
     * 
     * @return timeout in milliseconds
     */
    long timeoutMs() default 0;
}