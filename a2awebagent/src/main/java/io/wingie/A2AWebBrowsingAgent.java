package io.wingie;

import com.t4a.processor.AIProcessingException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.vishalmysore.a2a.client.LocalA2ATaskClient;
import io.github.vishalmysore.a2a.domain.FileContent;
import io.github.vishalmysore.a2a.domain.FilePart;
import io.github.vishalmysore.a2a.domain.Task;
import lombok.extern.java.Log;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
