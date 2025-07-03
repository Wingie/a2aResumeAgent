package io.wingie.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * JPA converter for handling Map to JSONB column mapping.
 * 
 * This converter properly handles the conversion between Java Map objects
 * and PostgreSQL JSONB columns, ensuring proper JSON formatting and
 * database compatibility.
 */
@Converter
@Slf4j
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Map to JSON: {}", e.getMessage());
            return "{}"; // Return empty JSON object on error
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(dbData, Map.class);
        } catch (IOException e) {
            log.error("Failed to deserialize JSON to Map: {}", e.getMessage());
            return Map.of("error", "Failed to parse JSON data");
        }
    }
}