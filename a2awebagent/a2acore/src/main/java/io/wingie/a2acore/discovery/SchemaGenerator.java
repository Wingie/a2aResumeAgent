package io.wingie.a2acore.discovery;

import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ToolInputSchema;
import io.wingie.a2acore.domain.ToolPropertySchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Generates JSON schemas from method parameters using pure reflection.
 * 
 * NO AI PROCESSING - uses reflection and type analysis for instant schema generation.
 * Target: <1ms per method schema generation.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class SchemaGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaGenerator.class);
    
    /**
     * Generates a ToolInputSchema from a method's parameters.
     * Uses the standard "provideAllValuesInPlainEnglish" pattern for simplicity.
     */
    public ToolInputSchema generateSchema(Method method) {
        long startTime = System.nanoTime();
        
        try {
            Map<String, ToolPropertySchema> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            
            // Check if method has parameters
            Class<?>[] parameterTypes = method.getParameterTypes();
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            
            if (parameterTypes.length == 0) {
                // No parameters - create minimal schema
                return createMinimalSchema();
            }
            
            // For methods with parameters, analyze each one
            for (int i = 0; i < parameters.length; i++) {
                java.lang.reflect.Parameter param = parameters[i];
                Class<?> paramType = parameterTypes[i];
                
                String paramName = getParameterName(param, i);
                ToolPropertySchema propertySchema = createPropertySchema(param, paramType);
                
                properties.put(paramName, propertySchema);
                
                // Check if required
                Parameter paramAnnotation = param.getAnnotation(Parameter.class);
                if (paramAnnotation == null || paramAnnotation.required()) {
                    required.add(paramName);
                }
            }
            
            // If we only have one String parameter, use the simplified pattern
            if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                return createSimplifiedStringSchema(parameters[0]);
            }
            
            ToolInputSchema schema = ToolInputSchema.builder()
                .type("object")
                .properties(properties)
                .required(required)
                .additionalProperties(false)
                .description("Parameters for " + method.getName())
                .build();
            
            long durationNanos = System.nanoTime() - startTime;
            log.debug("Generated schema for {} in {:.3f}ms", 
                method.getName(), durationNanos / 1_000_000.0);
            
            return schema;
            
        } catch (Exception e) {
            log.error("Failed to generate schema for method {}", method.getName(), e);
            // Return fallback schema
            return createMinimalSchema();
        }
    }
    
    /**
     * Creates the standard simplified schema for single String parameter methods.
     * This matches the existing pattern used in a2awebagent tools.
     */
    private ToolInputSchema createSimplifiedStringSchema(java.lang.reflect.Parameter parameter) {
        Map<String, ToolPropertySchema> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        // Use the standard parameter name
        String paramName = "provideAllValuesInPlainEnglish";
        
        // Get description from annotation or use default
        String description = "Provide instructions for this tool in plain English";
        Parameter paramAnnotation = parameter.getAnnotation(Parameter.class);
        if (paramAnnotation != null && !paramAnnotation.description().isEmpty()) {
            description = paramAnnotation.description();
        }
        
        ToolPropertySchema propertySchema = ToolPropertySchema.builder()
            .type("string")
            .description(description)
            .example("Perform the requested action")
            .build();
        
        properties.put(paramName, propertySchema);
        required.add(paramName);
        
        return ToolInputSchema.builder()
            .type("object")
            .properties(properties)
            .required(required)
            .additionalProperties(false)
            .build();
    }
    
    /**
     * Creates a minimal schema for methods with no parameters.
     */
    private ToolInputSchema createMinimalSchema() {
        Map<String, ToolPropertySchema> properties = new HashMap<>();
        
        // Even no-parameter methods can have optional instructions
        properties.put("provideAllValuesInPlainEnglish", 
            ToolPropertySchema.builder()
                .type("string")
                .description("Optional instructions for this tool")
                .build());
        
        return ToolInputSchema.builder()
            .type("object")
            .properties(properties)
            .required(new ArrayList<>()) // No required parameters
            .additionalProperties(false)
            .build();
    }
    
    /**
     * Creates a property schema for a single parameter.
     */
    private ToolPropertySchema createPropertySchema(java.lang.reflect.Parameter param, Class<?> paramType) {
        ToolPropertySchema.ToolPropertySchemaBuilder builder = ToolPropertySchema.builder();
        
        // Set basic type
        String jsonType = mapJavaTypeToJsonType(paramType);
        builder.type(jsonType);
        
        // Get metadata from @Parameter annotation
        Parameter paramAnnotation = param.getAnnotation(Parameter.class);
        if (paramAnnotation != null) {
            // Description
            if (!paramAnnotation.description().isEmpty()) {
                builder.description(paramAnnotation.description());
            }
            
            // Default value
            if (!paramAnnotation.defaultValue().isEmpty()) {
                builder.defaultValue(convertDefaultValue(paramAnnotation.defaultValue(), paramType));
            }
            
            // Validation constraints
            if (!paramAnnotation.pattern().isEmpty()) {
                builder.pattern(paramAnnotation.pattern());
            }
            
            if (paramAnnotation.min() != Double.NEGATIVE_INFINITY) {
                builder.minimum(paramAnnotation.min());
            }
            
            if (paramAnnotation.max() != Double.POSITIVE_INFINITY) {
                builder.maximum(paramAnnotation.max());
            }
            
            // Enum values
            if (paramAnnotation.values().length > 0) {
                builder.enumValues(Arrays.asList((Object[]) paramAnnotation.values()));
            }
            
            // Example
            if (!paramAnnotation.example().isEmpty()) {
                builder.example(paramAnnotation.example());
            }
        }
        
        // Set default description if none provided
        if (paramAnnotation == null || paramAnnotation.description().isEmpty()) {
            builder.description("Parameter of type " + paramType.getSimpleName());
        }
        
        return builder.build();
    }
    
    /**
     * Maps Java types to JSON Schema types.
     */
    private String mapJavaTypeToJsonType(Class<?> javaType) {
        if (javaType == String.class || javaType == char.class || javaType == Character.class) {
            return "string";
        } else if (javaType == int.class || javaType == Integer.class ||
                   javaType == long.class || javaType == Long.class ||
                   javaType == short.class || javaType == Short.class ||
                   javaType == byte.class || javaType == Byte.class) {
            return "integer";
        } else if (javaType == float.class || javaType == Float.class ||
                   javaType == double.class || javaType == Double.class) {
            return "number";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        } else if (javaType.isArray() || Collection.class.isAssignableFrom(javaType)) {
            return "array";
        } else if (Map.class.isAssignableFrom(javaType) || javaType == Object.class) {
            return "object";
        } else {
            // For custom objects, default to string (they'll be JSON serialized)
            return "string";
        }
    }
    
    /**
     * Converts string default value to appropriate type.
     */
    private Object convertDefaultValue(String defaultValue, Class<?> targetType) {
        try {
            if (targetType == String.class) {
                return defaultValue;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(defaultValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(defaultValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(defaultValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(defaultValue);
            } else {
                return defaultValue; // Fallback to string
            }
        } catch (Exception e) {
            log.warn("Failed to convert default value '{}' to type {}", defaultValue, targetType.getSimpleName());
            return defaultValue;
        }
    }
    
    /**
     * Gets parameter name, handling cases where reflection parameter names aren't available.
     */
    private String getParameterName(java.lang.reflect.Parameter param, int index) {
        // Try to get the actual parameter name
        if (param.isNamePresent()) {
            return param.getName();
        }
        
        // Fallback to positional name
        return "param" + index;
    }
}