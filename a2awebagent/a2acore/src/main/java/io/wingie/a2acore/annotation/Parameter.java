package io.wingie.a2acore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for method parameters in MCP tool actions.
 * 
 * This annotation allows fine-grained control over parameter descriptions
 * and validation for better integration with AI agents.
 * 
 * <p>Usage example:
 * <pre>
 * &#64;Action(description = "Searches for information")
 * public String search(
 *     &#64;Parameter(description = "Search query", required = true) String query,
 *     &#64;Parameter(description = "Maximum results", defaultValue = "10") int maxResults
 * ) {
 *     // Implementation
 * }
 * </pre>
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    
    /**
     * Human-readable description of this parameter.
     * Helps AI agents understand what value to provide.
     * 
     * @return parameter description
     */
    String description() default "";
    
    /**
     * Whether this parameter is required.
     * Required parameters must be provided by the caller.
     * 
     * @return true if required, false otherwise
     */
    boolean required() default true;
    
    /**
     * Default value for this parameter as a string.
     * Will be converted to the appropriate type if the parameter is not provided.
     * 
     * @return default value
     */
    String defaultValue() default "";
    
    /**
     * Optional validation pattern (regex) for string parameters.
     * 
     * @return validation regex pattern
     */
    String pattern() default "";
    
    /**
     * Optional minimum value for numeric parameters.
     * 
     * @return minimum value
     */
    double min() default Double.NEGATIVE_INFINITY;
    
    /**
     * Optional maximum value for numeric parameters.
     * 
     * @return maximum value
     */
    double max() default Double.POSITIVE_INFINITY;
    
    /**
     * Optional array of valid values for enum-like parameters.
     * 
     * @return array of valid values
     */
    String[] values() default {};
    
    /**
     * Optional example value for documentation and testing.
     * 
     * @return example value
     */
    String example() default "";
}