package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ExecutionParameters;
import io.wingie.playwright.PlaywrightWebBrowsingAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * User-controlled web browsing tool providing granular control over automation execution.
 * 
 * Designed for scenarios where users need precise control over:
 * - Step limits and execution bounds
 * - Early completion behavior
 * - Screenshot capturing preferences
 * - Execution modes (one-shot, multi-step, auto)
 */
@Service
@Slf4j
@Agent(name = "user controlled browsing", description = "User-controlled web automation with execution parameters")
public class UserControlledBrowsingTool {

    @Autowired
    private PlaywrightWebBrowsingAction playwrightAction;

    /**
     * Travel search with user-controlled parameters.
     * Example: "Travel search on booking.com with 10 steps max"
     */
    @Action(
        description = "Search for travel options with controlled execution parameters", 
        name = "travelSearchWithControl"
    )
    public String travelSearchWithControl(
            @Parameter(description = "Travel search query (e.g., 'flights from NYC to Paris in December')") String searchQuery,
            @Parameter(description = "Maximum number of automation steps (default: 10)") Integer maxSteps,
            @Parameter(description = "Execution mode: ONE_SHOT, MULTI_STEP, or AUTO (default: AUTO)") String executionMode,
            @Parameter(description = "Allow early completion if objectives are met (default: true)") Boolean allowEarlyCompletion) {

        log.info("Travel search with control: query='{}', maxSteps={}, mode={}", searchQuery, maxSteps, executionMode);

        try {
            // Build execution parameters
            ExecutionParameters.ExecutionMode mode = parseExecutionMode(executionMode, ExecutionParameters.ExecutionMode.AUTO);
            ExecutionParameters params = ExecutionParameters.builder()
                .maxSteps(maxSteps != null ? maxSteps : 10)
                .executionMode(mode)
                .allowEarlyCompletion(allowEarlyCompletion != null ? allowEarlyCompletion : true)
                .captureStepScreenshots(true)
                .build();

            // Enhanced travel query
            String enhancedQuery = buildTravelQuery(searchQuery);
            
            // Convert parameters to JSON for the underlying action
            String paramsJson = convertToJson(params);
            
            String results = playwrightAction.browseWebAndReturnTextWithParams(enhancedQuery, paramsJson);
            
            return formatTravelResults(searchQuery, results, params);
            
        } catch (Exception e) {
            log.error("Travel search with control failed", e);
            return String.format("❌ Travel search failed: %s", e.getMessage());
        }
    }

    /**
     * LinkedIn search with controlled execution.
     * Example: "LinkedIn search with 5 steps max"
     */
    @Action(
        description = "Search LinkedIn profiles with controlled execution parameters", 
        name = "linkedInSearchWithControl"
    )
    public String linkedInSearchWithControl(
            @Parameter(description = "LinkedIn search query (e.g., 'software engineers at Google')") String searchQuery,
            @Parameter(description = "Maximum number of automation steps (default: 5)") Integer maxSteps,
            @Parameter(description = "Execution mode: ONE_SHOT, MULTI_STEP, or AUTO (default: MULTI_STEP)") String executionMode) {

        log.info("LinkedIn search with control: query='{}', maxSteps={}, mode={}", searchQuery, maxSteps, executionMode);

        try {
            ExecutionParameters.ExecutionMode mode = parseExecutionMode(executionMode, ExecutionParameters.ExecutionMode.MULTI_STEP);
            ExecutionParameters params = ExecutionParameters.builder()
                .maxSteps(maxSteps != null ? maxSteps : 5)
                .executionMode(mode)
                .allowEarlyCompletion(true)
                .captureStepScreenshots(true)
                .build();

            String enhancedQuery = buildLinkedInQuery(searchQuery);
            String paramsJson = convertToJson(params);
            
            String results = playwrightAction.browseWebAndReturnTextWithParams(enhancedQuery, paramsJson);
            
            return formatLinkedInResults(searchQuery, results, params);
            
        } catch (Exception e) {
            log.error("LinkedIn search with control failed", e);
            return String.format("❌ LinkedIn search failed: %s", e.getMessage());
        }
    }

    /**
     * One-shot web action for quick tasks.
     * Example: "One-shot LinkedIn profile check"
     */
    @Action(
        description = "Perform a single web action and return immediately", 
        name = "oneShotWebAction"
    )
    public String oneShotWebAction(
            @Parameter(description = "Single web action to perform (e.g., 'check my LinkedIn profile')") String actionDescription) {

        log.info("One-shot web action: {}", actionDescription);

        try {
            ExecutionParameters params = ExecutionParameters.oneShot();
            String paramsJson = convertToJson(params);
            
            String results = playwrightAction.browseWebAndReturnTextWithParams(actionDescription, paramsJson);
            
            return formatOneShotResults(actionDescription, results);
            
        } catch (Exception e) {
            log.error("One-shot web action failed", e);
            return String.format("❌ One-shot action failed: %s", e.getMessage());
        }
    }

    /**
     * Advanced web automation with full parameter control.
     */
    @Action(
        description = "Advanced web automation with complete parameter control", 
        name = "advancedWebAutomation"
    )
    public String advancedWebAutomation(
            @Parameter(description = "Detailed automation instructions") String instructions,
            @Parameter(description = "Complete execution parameters as JSON") String executionParametersJson) {

        log.info("Advanced web automation: instructions='{}', params='{}'", instructions, executionParametersJson);

        try {
            String results = playwrightAction.browseWebAndReturnTextWithParams(instructions, executionParametersJson);
            return formatAdvancedResults(instructions, results, executionParametersJson);
            
        } catch (Exception e) {
            log.error("Advanced web automation failed", e);
            return String.format("❌ Advanced automation failed: %s", e.getMessage());
        }
    }

    // Helper methods

    private ExecutionParameters.ExecutionMode parseExecutionMode(String mode, ExecutionParameters.ExecutionMode defaultMode) {
        if (mode == null || mode.trim().isEmpty()) {
            return defaultMode;
        }
        
        try {
            return ExecutionParameters.ExecutionMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution mode '{}', using default: {}", mode, defaultMode);
            return defaultMode;
        }
    }

    private String buildTravelQuery(String originalQuery) {
        StringBuilder enhanced = new StringBuilder();
        enhanced.append("Travel search: ").append(originalQuery);
        
        if (!originalQuery.toLowerCase().contains("booking.com") && 
            !originalQuery.toLowerCase().contains("expedia")) {
            enhanced.append("\nNavigate to booking.com or expedia.com");
        }
        
        enhanced.append("\nSearch for available options and capture pricing information");
        enhanced.append("\nTake screenshots of search results for reference");
        
        return enhanced.toString();
    }

    private String buildLinkedInQuery(String originalQuery) {
        StringBuilder enhanced = new StringBuilder();
        
        if (!originalQuery.toLowerCase().contains("linkedin")) {
            enhanced.append("Navigate to LinkedIn and search for: ");
        }
        enhanced.append(originalQuery);
        enhanced.append("\nCapture profile information and connection details");
        enhanced.append("\nTake screenshots of relevant profiles");
        
        return enhanced.toString();
    }

    private String convertToJson(ExecutionParameters params) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(params);
        } catch (Exception e) {
            log.error("Failed to convert parameters to JSON", e);
            return "{}";
        }
    }

    private String formatTravelResults(String query, String results, ExecutionParameters params) {
        return String.format("""
            # 🧳 Travel Search Results (User-Controlled)
            
            ## Search Parameters
            - **Query**: %s
            - **Max Steps**: %d
            - **Execution Mode**: %s
            - **Early Completion**: %s
            
            ## Results
            %s
            
            ## Execution Summary
            - Mode: %s execution with %d step limit
            - Screenshots: %s
            - Completed: %s
            
            ---
            *Generated by a2aTravelAgent user-controlled automation*
            """, 
            query, 
            params.getMaxSteps(),
            params.getExecutionMode(),
            params.getAllowEarlyCompletion() ? "Enabled" : "Disabled",
            results,
            params.getExecutionMode().name().toLowerCase(),
            params.getMaxSteps(),
            params.getCaptureStepScreenshots() ? "Captured" : "Disabled",
            java.time.LocalDateTime.now());
    }

    private String formatLinkedInResults(String query, String results, ExecutionParameters params) {
        return String.format("""
            # 💼 LinkedIn Search Results (User-Controlled)
            
            ## Search Parameters
            - **Query**: %s
            - **Max Steps**: %d
            - **Execution Mode**: %s
            
            ## Profile Data
            %s
            
            ## Execution Summary
            - Controlled execution with %d step limit
            - Mode: %s
            - Completed: %s
            
            ---
            *Generated by a2aTravelAgent LinkedIn automation*
            """, 
            query, 
            params.getMaxSteps(),
            params.getExecutionMode(),
            results,
            params.getMaxSteps(),
            params.getExecutionMode().name().toLowerCase(),
            java.time.LocalDateTime.now());
    }

    private String formatOneShotResults(String action, String results) {
        return String.format("""
            # ⚡ One-Shot Web Action Results
            
            ## Action Performed
            %s
            
            ## Results
            %s
            
            ## Execution Details
            - Mode: Single action execution
            - Screenshots: Captured if applicable
            - Completed: %s
            
            ---
            *Generated by a2aTravelAgent one-shot automation*
            """, 
            action, 
            results,
            java.time.LocalDateTime.now());
    }

    private String formatAdvancedResults(String instructions, String results, String paramsJson) {
        return String.format("""
            # 🔧 Advanced Web Automation Results
            
            ## Instructions
            %s
            
            ## Execution Parameters
            ```json
            %s
            ```
            
            ## Results
            %s
            
            ## Completed
            %s
            
            ---
            *Generated by a2aTravelAgent advanced automation*
            """, 
            instructions, 
            paramsJson, 
            results,
            java.time.LocalDateTime.now());
    }
}