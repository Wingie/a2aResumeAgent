package io.wingie.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.regex.Pattern;

/**
 * Logback filter to mask sensitive API keys in log messages
 * Prevents accidental exposure of API keys in logs
 */
public class SensitiveDataLoggingFilter extends Filter<ILoggingEvent> {
    
    // Patterns to detect and mask API keys
    private static final Pattern OPENAI_KEY_PATTERN = Pattern.compile("(openAiKey: )(sk-[a-zA-Z0-9-]{20,})");
    private static final Pattern CLAUDE_KEY_PATTERN = Pattern.compile("(claudeKey: )(sk-[a-zA-Z0-9-]{20,})");
    private static final Pattern MISTRAL_KEY_PATTERN = Pattern.compile("(mistralKey: )([a-zA-Z0-9]{20,})");
    private static final Pattern SERPER_KEY_PATTERN = Pattern.compile("(serperKey: )([a-zA-Z0-9]{20,})");
    
    @Override
    public FilterReply decide(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        
        if (message != null && containsSensitiveData(message)) {
            // Create a new event with masked message
            String maskedMessage = maskSensitiveData(message);
            
            // We can't modify the original event easily, so we'll return DENY
            // and log the masked version through a separate appender
            // For now, we'll just deny the original log message
            return FilterReply.DENY;
        }
        
        return FilterReply.NEUTRAL;
    }
    
    private boolean containsSensitiveData(String message) {
        return message.contains("openAiKey: sk-") || 
               message.contains("claudeKey: sk-") ||
               message.contains("mistralKey: ") ||
               message.contains("serperKey: ");
    }
    
    private String maskSensitiveData(String message) {
        String masked = message;
        
        // Mask OpenAI keys
        masked = OPENAI_KEY_PATTERN.matcher(masked)
            .replaceAll("$1sk-***MASKED***");
            
        // Mask Claude keys  
        masked = CLAUDE_KEY_PATTERN.matcher(masked)
            .replaceAll("$1sk-***MASKED***");
            
        // Mask Mistral keys
        masked = MISTRAL_KEY_PATTERN.matcher(masked)
            .replaceAll("$1***MASKED***");
            
        // Mask Serper keys
        masked = SERPER_KEY_PATTERN.matcher(masked)
            .replaceAll("$1***MASKED***");
            
        return masked;
    }
}