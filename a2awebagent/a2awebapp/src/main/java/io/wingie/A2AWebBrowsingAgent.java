package io.wingie;

import io.wingie.a2acore.tools4ai.processor.AIProcessingException;
import lombok.extern.java.Log;

import java.io.IOException;

@Log
public class A2AWebBrowsingAgent {
    public static void main(String[] args) throws IOException, AIProcessingException {
        // This class is kept for legacy compatibility
        // All web automation functionality has been migrated to Playwright-based implementation
        // See PlaywrightWebBrowsingAction for current implementation
        
        log.info("A2AWebBrowsingAgent: Web automation is now handled by Playwright-based services");
        log.info("Use the Spring Boot application to access web automation tools");
    }
}
