package io.wingie.a2acore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or parameter for AI prompt integration.
 * 
 * This annotation replaces the external tools4ai @Prompt annotation with
 * a fast, lightweight implementation for AI prompt processing.
 * 
 * <p>Usage example:
 * <pre>
 * &#64;Prompt(describe = "Is this URL safe to navigate?")
 * private boolean isSafe;
 * </pre>
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Prompt {
    
    /**
     * Human-readable description for this field in AI prompts.
     * This will be used to describe the field's purpose to AI models.
     * 
     * @return the field description
     */
    String describe() default "";
    
    /**
     * Optional date format specification for date/time fields.
     * Used when this field represents a date or time value.
     * 
     * @return the date format pattern
     */
    String dateFormat() default "";
    
    /**
     * Whether this field should be ignored during prompt processing.
     * When true, the field won't be included in AI prompts.
     * 
     * @return true if the field should be ignored, false otherwise
     */
    boolean ignore() default false;
    
    /**
     * Optional validation rules for this field.
     * Can specify constraints or validation patterns.
     * 
     * @return validation pattern or rules
     */
    String validation() default "";
    
    /**
     * Whether this field is required in the prompt.
     * 
     * @return true if required, false otherwise
     */
    boolean required() default false;
}