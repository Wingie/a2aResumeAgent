package io.wingie.a2acore.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wingie.a2acore.annotation.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps JSON arguments to Java method parameters.
 * 
 * Handles type conversion, validation, and default values for
 * @Action method parameters using Jackson for JSON processing.
 * 
 * @author a2acore
 * @since 1.0.0
 */
@Component
public class ParameterMapper {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterMapper.class);
    
    private final ObjectMapper objectMapper;
    
    public ParameterMapper() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Maps JSON arguments to method parameters.
     * 
     * @param arguments JSON arguments from the tool call
     * @param toolMethod Method metadata
     * @return Array of mapped arguments ready for method invocation
     * @throws ParameterMappingException if mapping fails
     */
    public Object[] mapArguments(Map<String, Object> arguments, ToolMethod toolMethod) 
            throws ParameterMappingException {
        
        try {
            Class<?>[] parameterTypes = toolMethod.getParameterTypes();
            java.lang.reflect.Parameter[] parameters = toolMethod.getParameters();
            
            if (parameterTypes.length == 0) {
                return new Object[0];
            }
            
            Object[] args = new Object[parameterTypes.length];
            
            // Handle the common case of single String parameter
            if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
                args[0] = mapSingleStringParameter(arguments, parameters[0]);
                return args;
            }
            
            // Handle multiple parameters
            for (int i = 0; i < parameterTypes.length; i++) {
                java.lang.reflect.Parameter param = parameters[i];
                Class<?> paramType = parameterTypes[i];
                
                String paramName = getParameterName(param, i);
                Object value = arguments.get(paramName);
                
                args[i] = mapSingleParameter(value, paramType, param);
            }
            
            return args;
            
        } catch (Exception e) {
            log.error("Failed to map parameters for tool {}", toolMethod.getToolName(), e);
            throw new ParameterMappingException("Parameter mapping failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Maps a single String parameter using the standard pattern.
     */
    private String mapSingleStringParameter(Map<String, Object> arguments, 
                                           java.lang.reflect.Parameter parameter) {
        // Try standard parameter name first
        Object value = arguments.get("provideAllValuesInPlainEnglish");
        
        if (value == null) {
            // Try actual parameter name
            String paramName = getParameterName(parameter, 0);
            value = arguments.get(paramName);
        }
        
        if (value == null) {
            // Check for default value
            Parameter paramAnnotation = parameter.getAnnotation(Parameter.class);
            if (paramAnnotation != null && !paramAnnotation.defaultValue().isEmpty()) {
                return paramAnnotation.defaultValue();
            }
            
            // Check if required
            if (paramAnnotation == null || paramAnnotation.required()) {
                throw new ParameterMappingException("Required parameter not provided");
            }
            
            return null;
        }
        
        return value.toString();
    }
    
    /**
     * Maps a single parameter to the target type.
     */
    private Object mapSingleParameter(Object value, Class<?> targetType, 
                                     java.lang.reflect.Parameter parameter) 
            throws ParameterMappingException {
        
        Parameter paramAnnotation = parameter.getAnnotation(Parameter.class);
        
        // Handle null values
        if (value == null) {
            // Check for default value
            if (paramAnnotation != null && !paramAnnotation.defaultValue().isEmpty()) {
                value = paramAnnotation.defaultValue();
            } else {
                // Check if required
                if (paramAnnotation == null || paramAnnotation.required()) {
                    throw new ParameterMappingException(
                        "Required parameter '" + parameter.getName() + "' not provided");
                }
                return null;
            }
        }
        
        // Convert to target type
        try {
            return convertValue(value, targetType, paramAnnotation);
        } catch (Exception e) {
            throw new ParameterMappingException(
                "Failed to convert parameter '" + parameter.getName() + 
                "' to type " + targetType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a value to the target type with validation.
     */
    private Object convertValue(Object value, Class<?> targetType, Parameter paramAnnotation) {
        // String conversion
        if (targetType == String.class) {
            String stringValue = value.toString();
            
            // Validate pattern if specified
            if (paramAnnotation != null && !paramAnnotation.pattern().isEmpty()) {
                if (!stringValue.matches(paramAnnotation.pattern())) {
                    throw new ParameterMappingException(
                        "Value '" + stringValue + "' does not match pattern: " + paramAnnotation.pattern());
                }
            }
            
            return stringValue;
        }
        
        // Numeric conversions
        if (targetType == int.class || targetType == Integer.class) {
            Integer intValue = convertToInteger(value);
            validateNumericRange(intValue, paramAnnotation);
            return intValue;
        }
        
        if (targetType == long.class || targetType == Long.class) {
            Long longValue = convertToLong(value);
            validateNumericRange(longValue, paramAnnotation);
            return longValue;
        }
        
        if (targetType == double.class || targetType == Double.class) {
            Double doubleValue = convertToDouble(value);
            validateNumericRange(doubleValue, paramAnnotation);
            return doubleValue;
        }
        
        if (targetType == boolean.class || targetType == Boolean.class) {
            return convertToBoolean(value);
        }
        
        // Complex types - use Jackson
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new ParameterMappingException(
                "Failed to convert value to " + targetType.getSimpleName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Converts value to Integer.
     */
    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }
    
    /**
     * Converts value to Long.
     */
    private Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return Long.parseLong(value.toString());
        }
    }
    
    /**
     * Converts value to Double.
     */
    private Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return Double.parseDouble(value.toString());
        }
    }
    
    /**
     * Converts value to Boolean.
     */
    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }
    
    /**
     * Validates numeric values against parameter constraints.
     */
    private void validateNumericRange(Number value, Parameter paramAnnotation) {
        if (paramAnnotation == null) {
            return;
        }
        
        double doubleValue = value.doubleValue();
        
        if (paramAnnotation.min() != Double.NEGATIVE_INFINITY && doubleValue < paramAnnotation.min()) {
            throw new ParameterMappingException(
                "Value " + value + " is below minimum " + paramAnnotation.min());
        }
        
        if (paramAnnotation.max() != Double.POSITIVE_INFINITY && doubleValue > paramAnnotation.max()) {
            throw new ParameterMappingException(
                "Value " + value + " is above maximum " + paramAnnotation.max());
        }
    }
    
    /**
     * Gets parameter name, handling reflection limitations.
     */
    private String getParameterName(java.lang.reflect.Parameter param, int index) {
        if (param.isNamePresent()) {
            return param.getName();
        }
        return "param" + index;
    }
}