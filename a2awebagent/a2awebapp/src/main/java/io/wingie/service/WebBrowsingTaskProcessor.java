package io.wingie.service;

import io.wingie.entity.TaskExecution;
import io.wingie.playwright.PlaywrightWebBrowsingAction;
import io.wingie.repository.TaskExecutionRepository;
import io.wingie.a2acore.domain.ExecutionParameters;
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
    
    @Autowired
    private ScreenshotService screenshotService;
    
    @Autowired
    private TaskExecutionRepository taskExecutionRepository;
    
    @Autowired
    private StepControlService stepControlService;
    
    @Value("${app.storage.screenshots:/app/screenshots}")
    private String screenshotPath;

    public String processTravelSearch(TaskExecution task) throws IOException {
        return processTravelSearchWithParams(task, null);
    }
    
    public String processTravelSearchWithParams(TaskExecution task, ExecutionParameters params) throws IOException {
        String taskId = task.getTaskId();
        String query = task.getOriginalQuery();
        
        // Use default parameters if none provided
        if (params == null) {
            params = ExecutionParameters.auto(10); // Default for travel search
        }
        
        log.info("Processing travel search for task {} with params {}: {}", taskId, params, query);
        
        // Check if WebBrowsingAction is available
        if (webBrowsingAction == null) {
            log.error("WebBrowsingAction not available for task {}", taskId);
            progressService.updateProgress(task, 100, "Web automation service unavailable");
            return generateServiceUnavailableResponse(query);
        }
        
        try {
            // Initialize step control
            stepControlService.initializeStepControl(taskId, params);
            
            // Step 1: Initialize search
            progressService.updateProgress(task, 10, "Initializing travel search...");
            
            // Enhanced query for travel search
            String enhancedQuery = buildTravelSearchQueryWithParams(query, params);
            
            // Step 2: Navigate to travel sites
            progressService.updateProgress(task, 20, "Navigating to travel booking sites...");
            
            // Step 3: Perform search with step control
            progressService.updateProgress(task, 40, "Searching for flights and accommodations...");
            
            // Use step-controlled execution
            String searchResults = executeWithStepControl(task, enhancedQuery, params, (progress, message) -> {
                progressService.updateProgress(task, 40 + (int)(progress * 0.5), message);
            });
            
            // Step 4: Process results
            progressService.updateProgress(task, 90, "Processing and formatting results...");
            
            // Get execution summary
            StepControlService.ExecutionSummary summary = stepControlService.completeStepControl(taskId);
            
            // Format results for travel search
            String formattedResults = formatTravelResultsWithSummary(searchResults, query, params, summary);
            
            progressService.updateProgress(task, 100, "Travel search completed successfully");
            
            log.info("Travel search completed for task {} with summary: {}", taskId, summary);
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
            
            String textResults = webBrowsingAction.browseWebAndReturnText(query, null);
            
            // Check for cancellation after text extraction
            if (isTaskCancelled(task)) {
                throw new TaskExecutorService.TaskCancelledException("Task was cancelled during text extraction");
            }
            
            callback.updateProgress(0.7, "Capturing screenshots...");
            
            // Capture screenshots
            io.wingie.a2acore.domain.ImageContent screenshotImageContent = webBrowsingAction.browseWebAndReturnImage(query);
            String screenshotResult = "Screenshot captured";
            
            // Handle ImageContent for async task processing - FIX SCREENSHOT INTEGRATION GAP
            if (screenshotImageContent != null && screenshotImageContent.getData() != null && !screenshotImageContent.getData().isEmpty()) {
                try {
                    // ImageContent.getData() already returns base64-encoded data
                    String base64Data = screenshotImageContent.getData();
                    
                    // Save screenshot and get accessible URL
                    String screenshotUrl = screenshotService.saveScreenshotAndGetUrl(base64Data, "playwright");
                    
                    if (screenshotUrl != null) {
                        // Add screenshot directly to TaskExecution to avoid circular dependency
                        task.getScreenshots().add(screenshotUrl);
                        task.setUpdated(LocalDateTime.now());
                        taskExecutionRepository.save(task);
                        
                        screenshotResult = String.format("Screenshot captured and saved - URL: %s, MIME type: %s, Base64 size: %d chars", 
                            screenshotUrl,
                            screenshotImageContent.getMimeType(), 
                            base64Data.length());
                        
                        log.info("📸 Screenshot successfully integrated for task {}: {}", task.getTaskId(), screenshotUrl);
                    } else {
                        log.warn("⚠️ Failed to save screenshot for task {}", task.getTaskId());
                        screenshotResult = "Screenshot capture failed during save operation";
                    }
                } catch (Exception e) {
                    log.error("❌ Error processing screenshot for task {}: {}", task.getTaskId(), e.getMessage());
                    screenshotResult = "Screenshot processing error: " + e.getMessage();
                }
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
        return buildTravelSearchQueryWithParams(originalQuery, ExecutionParameters.auto(10));
    }
    
    private String buildTravelSearchQueryWithParams(String originalQuery, ExecutionParameters params) {
        // Enhance the query for better travel search results
        String enhanced = originalQuery;
        
        if (!enhanced.toLowerCase().contains("flight") && !enhanced.toLowerCase().contains("hotel")) {
            enhanced = "Find flights and hotels for: " + enhanced;
        }
        
        if (!enhanced.toLowerCase().contains("booking.com") && !enhanced.toLowerCase().contains("expedia")) {
            enhanced += ". Search on booking.com and expedia.com for best options.";
        }
        
        // Add step control instructions based on execution mode
        if (params.getExecutionMode() == ExecutionParameters.ExecutionMode.ONE_SHOT) {
            enhanced += " Take one screenshot of the main search results.";
        } else {
            enhanced += " Take screenshots of search results and pricing across multiple steps.";
            enhanced += String.format(" Maximum %d automation steps allowed.", params.getMaxSteps());
        }
        
        if (params.getAllowEarlyCompletion()) {
            enhanced += " Complete early if good results are found.";
        }
        
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
    
    private String formatTravelResultsWithSummary(String rawResults, String originalQuery, ExecutionParameters params, StepControlService.ExecutionSummary summary) {
        return String.format("""
            # 🧳 Travel Search Results (User-Controlled Execution)
            
            ## Search Query
            %s
            
            ## Execution Parameters
            - **Mode**: %s
            - **Max Steps**: %d
            - **Early Completion**: %s
            - **Step Screenshots**: %s
            
            ## Results Summary
            %s
            
            ## Execution Summary
            - **Steps Completed**: %d/%d (%s)
            - **Efficiency**: %s
            - **Early Completion**: %s
            - **Search Completed**: %s
            
            ## Screenshots
            Screenshots have been captured and saved for visual reference.
            
            ---
            *Generated by a2aTravelAgent user-controlled automation system*
            """, 
            originalQuery, 
            params.getExecutionMode(),
            params.getMaxSteps(),
            params.getAllowEarlyCompletion() ? "Enabled" : "Disabled",
            params.getCaptureStepScreenshots() ? "Enabled" : "Disabled",
            rawResults,
            summary.getStepsCompleted(),
            summary.getMaxSteps(),
            summary.isEarlyCompletion() ? "early completion" : "full execution",
            summary.getEfficiencyFormatted(),
            summary.isEarlyCompletion() ? "Yes" : "No",
            LocalDateTime.now());
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

    /**
     * Execute with step control and progress tracking.
     */
    private String executeWithStepControl(TaskExecution task, String query, ExecutionParameters params, ProgressCallback callback) throws IOException {
        String taskId = task.getTaskId();
        
        log.info("Starting step-controlled execution for task {} with params: {}", taskId, params);
        
        // Check for cancellation before starting
        if (isTaskCancelled(task)) {
            throw new TaskExecutorService.TaskCancelledException("Task was cancelled");
        }
        
        callback.updateProgress(0.1, "Starting step-controlled web automation...");
        
        try {
            // Convert execution parameters to JSON for the Playwright action
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String paramsJson = mapper.writeValueAsString(params);
            
            callback.updateProgress(0.2, "Executing web interactions with step control...");
            
            // Use the enhanced Playwright action with parameters
            String textResults = webBrowsingAction.browseWebAndReturnTextWithParams(query, paramsJson);
            
            // Check for cancellation after text extraction
            if (isTaskCancelled(task)) {
                throw new TaskExecutorService.TaskCancelledException("Task was cancelled during text extraction");
            }
            
            callback.updateProgress(0.7, "Capturing screenshots with step tracking...");
            
            // Capture screenshots with step control
            io.wingie.a2acore.domain.ImageContent screenshotImageContent = webBrowsingAction.browseWebAndReturnImage(query);
            String screenshotResult = "Screenshot captured with step control";
            
            // Handle ImageContent for step-controlled execution
            if (screenshotImageContent != null && screenshotImageContent.getData() != null && !screenshotImageContent.getData().isEmpty()) {
                try {
                    String base64Data = screenshotImageContent.getData();
                    String screenshotUrl = screenshotService.saveScreenshotAndGetUrl(base64Data, "step-controlled");
                    
                    if (screenshotUrl != null) {
                        task.getScreenshots().add(screenshotUrl);
                        task.setUpdated(LocalDateTime.now());
                        taskExecutionRepository.save(task);
                        
                        screenshotResult = String.format("Step-controlled screenshot captured - URL: %s, Mode: %s, Max Steps: %d", 
                            screenshotUrl, params.getExecutionMode(), params.getMaxSteps());
                        
                        log.info("📸 Step-controlled screenshot integrated for task {}: {}", taskId, screenshotUrl);
                    } else {
                        screenshotResult = "Step-controlled screenshot capture failed during save operation";
                    }
                } catch (Exception e) {
                    log.error("❌ Error processing step-controlled screenshot for task {}: {}", taskId, e.getMessage());
                    screenshotResult = "Step-controlled screenshot processing error: " + e.getMessage();
                }
            }
            
            callback.updateProgress(0.9, "Processing extracted data with execution summary...");
            
            // Combine results with step control information
            String combinedResults = combineTextAndScreenshotResults(textResults, screenshotResult);
            
            callback.updateProgress(1.0, "Step-controlled web automation completed");
            
            return combinedResults;
            
        } catch (Exception e) {
            log.error("Error during step-controlled web browsing execution for task {}", taskId, e);
            throw new IOException("Step-controlled web browsing execution failed: " + e.getMessage(), e);
        }
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