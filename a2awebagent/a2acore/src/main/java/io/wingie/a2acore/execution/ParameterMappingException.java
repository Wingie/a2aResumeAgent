package io.wingie.a2acore.execution;

/**
 * Exception thrown when parameter mapping fails.
 * 
 * Provides detailed information about parameter conversion errors
 * including validation failures and type conversion issues.
 * 
 * @author a2acore
 * @since 1.0.0
 */
public class ParameterMappingException extends RuntimeException {
    
    private final String parameterName;
    private final String expectedType;
    private final Object providedValue;
    
    public ParameterMappingException(String message) {
        super(message);
        this.parameterName = null;
        this.expectedType = null;
        this.providedValue = null;
    }
    
    public ParameterMappingException(String message, Throwable cause) {
        super(message, cause);
        this.parameterName = null;
        this.expectedType = null;
        this.providedValue = null;
    }
    
    public ParameterMappingException(String parameterName, String expectedType, 
                                   Object providedValue, String message) {
        super(message);
        this.parameterName = parameterName;
        this.expectedType = expectedType;
        this.providedValue = providedValue;
    }
    
    public ParameterMappingException(String parameterName, String expectedType, 
                                   Object providedValue, String message, Throwable cause) {
        super(message, cause);
        this.parameterName = parameterName;
        this.expectedType = expectedType;
        this.providedValue = providedValue;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public String getExpectedType() {
        return expectedType;
    }
    
    public Object getProvidedValue() {
        return providedValue;
    }
    
    /**
     * Creates a type conversion exception.
     */
    public static ParameterMappingException typeConversion(String parameterName, 
                                                          String expectedType, 
                                                          Object providedValue, 
                                                          Throwable cause) {
        String message = String.format(
            "Cannot convert parameter '%s' from %s to %s. Provided value: %s", 
            parameterName, 
            providedValue != null ? providedValue.getClass().getSimpleName() : "null",
            expectedType,
            providedValue);
        
        return new ParameterMappingException(parameterName, expectedType, providedValue, message, cause);
    }
    
    /**
     * Creates a validation exception.
     */
    public static ParameterMappingException validation(String parameterName, 
                                                      Object providedValue, 
                                                      String validationMessage) {
        String message = String.format(
            "Validation failed for parameter '%s' with value '%s': %s", 
            parameterName, providedValue, validationMessage);
        
        return new ParameterMappingException(parameterName, null, providedValue, message);
    }
    
    /**
     * Creates a required parameter missing exception.
     */
    public static ParameterMappingException requiredParameterMissing(String parameterName) {
        String message = String.format("Required parameter '%s' is missing", parameterName);
        return new ParameterMappingException(parameterName, null, null, message);
    }
    
    @Override
    public String toString() {
        return String.format("ParameterMappingException{parameterName='%s', expectedType='%s', providedValue=%s, message='%s'}", 
            parameterName, expectedType, providedValue, getMessage());
    }
}