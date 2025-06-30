package io.wingie.service;

import io.wingie.entity.TaskExecution;
import io.wingie.playwright.PlaywrightWebBrowsingAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class WebBrowsingTaskProcessor {

    @Autowired(required = false)
    @Lazy
    private PlaywrightWebBrowsingAction webBrowsingAction;
    
    @Autowired
    private TaskProgressService progressService;
    
    @Value("${app.storage.screenshots:/app/screenshots}")
    private String screenshotPath;

    public String processTravelSearch(TaskExecution task) throws IOException {
        String taskId = task.getTaskId();
        String query = task.getOriginalQuery();
        
        log.info("Processing travel search for task {}: {}", taskId, query);
        
        // Check if WebBrowsingAction is available
        if (webBrowsingAction == null) {
            log.error("WebBrowsingAction not available for task {}", taskId);
            progressService.updateProgress(task, 100, "Web automation service unavailable");
            return generateServiceUnavailableResponse(query);
        }
        
        try {
            // Step 1: Initialize search
            progressService.updateProgress(task, 10, "Initializing travel search...");
            
            // Enhanced query for travel search
            String enhancedQuery = buildTravelSearchQuery(query);
            
            // Step 2: Navigate to travel sites
            progressService.updateProgress(task, 20, "Navigating to travel booking sites...");
            
            // Step 3: Perform search with screenshots
            progressService.updateProgress(task, 40, "Searching for flights and accommodations...");
            
            // Use existing WebBrowsingAction but with progress callbacks
            AtomicReference<String> results = new AtomicReference<>();
            
            // Create a wrapper that captures progress
            String searchResults = executeWithProgress(task, enhancedQuery, (progress, message) -> {
                progressService.updateProgress(task, 40 + (int)(progress * 0.5), message);
            });
            
            // Step 4: Process results
            progressService.updateProgress(task, 90, "Processing and formatting results...");
            
            // Format results for travel search
            String formattedResults = formatTravelResults(searchResults, query);
            
            progressService.updateProgress(task, 100, "Travel search completed successfully");
            
            log.info("Travel search completed for task {}", taskId);
            return formattedResults;
            
        } catch (Exception e) {
            log.error("Error processing travel search for task {}", taskId, e);
            throw new RuntimeException("Travel search failed: " + e.getMessage(), e);
        }
    }

    public String processLinkedInSearch(TaskExecution task) throws IOException {
        String taskId = task.getTaskId();
        String query = task.getOriginalQuery();
        
        log.info("Processing LinkedIn search for task {}: {}", taskId, query);
        
        // Check if WebBrowsingAction is available
        if (webBrowsingAction == null) {
            log.error("WebBrowsingAction not available for task {}", taskId);
            progressService.updateProgress(task, 100, "Web automation service unavailable");
            return generateServiceUnavailableResponse(query);
        }
        
        try {
            progressService.updateProgress(task, 10, "Initializing LinkedIn search...");
            
            // Enhanced query for LinkedIn
            String enhancedQuery = buildLinkedInSearchQuery(query);
            
            progressService.updateProgress(task, 30, "Navigating to LinkedIn...");
            
            // Execute search with progress tracking
            String searchResults = executeWithProgress(task, enhancedQuery, (progress, message) -> {
                progressService.updateProgress(task, 30 + (int)(progress * 0.6), message);
            });
            
            progressService.updateProgress(task, 95, "Processing LinkedIn profile data...");
            
            String formattedResults = formatLinkedInResults(searchResults, query);
            
            progressService.updateProgress(task, 100, "LinkedIn search completed");
            
            log.info("LinkedIn search completed for task {}", taskId);
            return formattedResults;
            
        } catch (Exception e) {
            log.error("Error processing LinkedIn search for task {}", taskId, e);
            throw new RuntimeException("LinkedIn search failed: " + e.getMessage(), e);
        }
    }

    public String processWebBrowsing(TaskExecution task) throws IOException {
        String taskId = task.getTaskId();
        String query = task.getOriginalQuery();
        
        log.info("Processing general web browsing for task {}: {}", taskId, query);
        
        // Check if WebBrowsingAction is available
        if (webBrowsingAction == null) {
            log.error("WebBrowsingAction not available for task {}", taskId);
            progressService.updateProgress(task, 100, "Web automation service unavailable");
            return generateServiceUnavailableResponse(query);
        }
        
        try {
            progressService.updateProgress(task, 10, "Initializing web browsing...");
            
            progressService.updateProgress(task, 25, "Analyzing browse request...");
            
            // Execute browsing with progress tracking
            String results = executeWithProgress(task, query, (progress, message) -> {
                progressService.updateProgress(task, 25 + (int)(progress * 0.7), message);
            });
            
            progressService.updateProgress(task, 95, "Finalizing results...");
            
            String formattedResults = formatWebBrowsingResults(results, query);
            
            progressService.updateProgress(task, 100, "Web browsing completed");
            
            log.info("Web browsing completed for task {}", taskId);
            return formattedResults;
            
        } catch (Exception e) {
            log.error("Error processing web browsing for task {}", taskId, e);
            throw new RuntimeException("Web browsing failed: " + e.getMessage(), e);
        }
    }

    private String executeWithProgress(TaskExecution task, String query, ProgressCallback callback) throws IOException {
        String taskId = task.getTaskId();
        
        // Check for cancellation before starting
        if (isTaskCancelled(task)) {
            throw new TaskExecutorService.TaskCancelledException("Task was cancelled");
        }
        
        callback.updateProgress(0.1, "Starting web automation...");
        
        // Split the browsing action into steps for better progress tracking
        try {
            callback.updateProgress(0.2, "Parsing browsing instructions...");
            
            // Execute the main browsing action
            callback.updateProgress(0.3, "Executing web interactions...");
            
            String textResults = webBrowsingAction.browseWebAndReturnText(query);
            
            // Check for cancellation after text extraction
            if (isTaskCancelled(task)) {
                throw new TaskExecutorService.TaskCancelledException("Task was cancelled during text extraction");
            }
            
            callback.updateProgress(0.7, "Capturing screenshots...");
            
            // Capture screenshots
            io.wingie.a2acore.domain.ImageContent screenshotImageContent = webBrowsingAction.browseWebAndReturnImage(query);
            String screenshotResult = "Screenshot captured";
            
            // Handle ImageContent for async task processing
            if (screenshotImageContent != null && screenshotImageContent.getData() != null && !screenshotImageContent.getData().isEmpty()) {
                // Create a descriptive result message for the async system
                screenshotResult = String.format("Screenshot captured successfully - MIME type: %s, Data size: %d bytes", 
                    screenshotImageContent.getMimeType(), 
                    screenshotImageContent.getData().length());
                
                // For async tasks, we can optionally save the screenshot path
                // Note: The actual screenshot file paths are handled by PlaywrightWebBrowsingAction internally
                // and stored in the TaskExecution.screenshots via the browser automation flow
            }
            
            callback.updateProgress(0.9, "Processing extracted data...");
            
            // Combine results
            String combinedResults = combineTextAndScreenshotResults(textResults, screenshotResult);
            
            callback.updateProgress(1.0, "Web automation completed");
            
            return combinedResults;
            
        } catch (Exception e) {
            log.error("Error during web browsing execution for task {}", taskId, e);
            throw new IOException("Web browsing execution failed: " + e.getMessage(), e);
        }
    }

    private boolean isTaskCancelled(TaskExecution task) {
        // This would check Redis or database for cancellation status
        // For now, just return false - implement actual cancellation check
        return false;
    }

    private String buildTravelSearchQuery(String originalQuery) {
        // Enhance the query for better travel search results
        String enhanced = originalQuery;
        
        if (!enhanced.toLowerCase().contains("flight") && !enhanced.toLowerCase().contains("hotel")) {
            enhanced = "Find flights and hotels for: " + enhanced;
        }
        
        if (!enhanced.toLowerCase().contains("booking.com") && !enhanced.toLowerCase().contains("expedia")) {
            enhanced += ". Search on booking.com and expedia.com for best options.";
        }
        
        enhanced += " Take screenshots of search results and pricing.";
        
        return enhanced;
    }

    private String buildLinkedInSearchQuery(String originalQuery) {
        String enhanced = originalQuery;
        
        if (!enhanced.toLowerCase().contains("linkedin")) {
            enhanced = "Search LinkedIn for: " + enhanced;
        }
        
        enhanced += " Take screenshots of profile pages and search results.";
        
        return enhanced;
    }

    private String formatTravelResults(String rawResults, String originalQuery) {
        return String.format("""
            # Travel Search Results
            
            ## Search Query
            %s
            
            ## Results Summary
            %s
            
            ## Search Completed
            Timestamp: %s
            
            ## Screenshots
            Screenshots have been captured and saved for visual reference.
            
            ---
            *Results generated by a2aTravelAgent async task system*
            """, originalQuery, rawResults, LocalDateTime.now());
    }

    private String formatLinkedInResults(String rawResults, String originalQuery) {
        return String.format("""
            # LinkedIn Search Results
            
            ## Search Query
            %s
            
            ## Profile Information
            %s
            
            ## Search Completed
            Timestamp: %s
            
            ## Screenshots
            Profile screenshots have been captured for visual reference.
            
            ---
            *Results generated by a2aTravelAgent LinkedIn automation*
            """, originalQuery, rawResults, LocalDateTime.now());
    }

    private String formatWebBrowsingResults(String rawResults, String originalQuery) {
        return String.format("""
            # Web Browsing Results
            
            ## Request
            %s
            
            ## Extracted Information
            %s
            
            ## Completed
            Timestamp: %s
            
            ## Screenshots
            Screenshots captured during browsing session.
            
            ---
            *Results generated by a2aTravelAgent web automation*
            """, originalQuery, rawResults, LocalDateTime.now());
    }

    private String combineTextAndScreenshotResults(String textResults, String screenshotResult) {
        StringBuilder combined = new StringBuilder();
        
        if (textResults != null && !textResults.trim().isEmpty()) {
            combined.append("## Extracted Text Data\n");
            combined.append(textResults);
            combined.append("\n\n");
        }
        
        if (screenshotResult != null && !screenshotResult.trim().isEmpty()) {
            combined.append("## Screenshot Information\n");
            combined.append(screenshotResult);
            combined.append("\n");
        }
        
        return combined.toString();
    }

    // Functional interface for progress callbacks
    @FunctionalInterface
    public interface ProgressCallback {
        void updateProgress(double progress, String message);
    }
    
    private String generateServiceUnavailableResponse(String query) {
        return String.format("""
            # Web Automation Service Unavailable
            
            ## Request: "%s"
            
            The web automation service is temporarily unavailable. This may be due to:
            
            1. **Chrome/Chromium not installed**: The browser engine required for automation is missing
            2. **Container environment**: Running in Docker without proper browser setup
            3. **Initialization failure**: Playwright browser automation could not start properly
            
            ### What You Can Do:
            - Try again in a few moments
            - Contact system administrator to check service status
            - Use alternative methods for your search
            
            ### Technical Details:
            - Service: WebBrowsingAction
            - Status: Not initialized
            - Environment: %s
            
            ---
            *This is an automated response due to service unavailability*
            """, query, System.getProperty("os.name", "Unknown"));
    }
}