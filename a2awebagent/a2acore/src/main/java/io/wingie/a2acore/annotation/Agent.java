package io.wingie.a2acore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an MCP agent that groups related tool actions.
 * 
 * This annotation replaces the external tools4ai @Agent annotation with
 * a fast, lightweight implementation for organizing related actions.
 * 
 * <p>Usage example:
 * <pre>
 * &#64;Agent(groupName = "web browsing", groupDescription = "Web automation tools")
 * &#64;Service
 * public class WebBrowsingAgent {
 *     // @Action methods here
 * }
 * </pre>
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Agent {
    
    /**
     * Human-readable name for this group of actions.
     * Used for organizing tools in MCP client interfaces.
     * 
     * @return the group name
     */
    String name() default "";
    
    /**
     * Human-readable description of what this agent does.
     * Describes the category of actions provided by this agent.
     * 
     * @return the group description
     */
    String description() default "";
    
    /**
     * @deprecated Use name() instead
     */
    @Deprecated
    String groupName() default "";
    
    /**
     * @deprecated Use description() instead
     */
    @Deprecated
    String groupDescription() default "";
    
    /**
     * Optional version information for this agent.
     * 
     * @return version string
     */
    String version() default "";
    
    /**
     * Whether this agent is enabled. Disabled agents won't be scanned for actions.
     * 
     * @return true if enabled, false otherwise
     */
    boolean enabled() default true;
    
    /**
     * Optional priority for this agent. Higher values are processed first.
     * 
     * @return priority value
     */
    int priority() default 0;
}