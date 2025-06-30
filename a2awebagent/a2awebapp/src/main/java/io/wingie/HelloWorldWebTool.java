package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * TEMPLATE: Example web automation tool showing proper WebBrowsingAction usage.
 * Copy this pattern for any new tools that need web browsing capabilities.
 */
@Service
@Slf4j
@Agent(name = "example", description = "Example web automation tools")
public class HelloWorldWebTool {

    @Autowired
    @Lazy
    private io.wingie.playwright.PlaywrightWebBrowsingAction webBrowsingAction;

    /**
     * Example action that searches for "Hello World" and takes a screenshot.
     * This demonstrates proper error handling and fallback behavior.
     */
    @Action(description = "Search for Hello World and demonstrate web automation", name = "searchHelloWorld")
    public String searchHelloWorld(@Parameter(description = "The search term to look for") String searchTerm) {
        log.info("Starting Hello World search for: {}", searchTerm);
        
        try {
            // Check if WebBrowsingAction is available
            if (webBrowsingAction == null) {
                log.warn("WebBrowsingAction not available, returning fallback response");
                return generateFallbackResponse(searchTerm);
            }
            
            // Perform web automation
            String textResult = webBrowsingAction.browseWebAndReturnText(
                String.format("Go to Google.com and search for '%s', then extract the first 3 results", searchTerm)
            );
            
            // Take a screenshot
            String screenshotResult = webBrowsingAction.browseWebAndReturnImage(
                String.format("Take a screenshot of the search results for '%s'", searchTerm)
            );
            
            return formatSuccessResponse(searchTerm, textResult, screenshotResult);
            
        } catch (Exception e) {
            log.error("Error during web search for '{}': {}", searchTerm, e.getMessage(), e);
            return generateErrorResponse(searchTerm, e.getMessage());
        }
    }

    /**
     * Generates a fallback response when WebBrowsingAction is not available.
     */
    private String generateFallbackResponse(String searchTerm) {
        return String.format("""
            # Hello World Search - Service Temporarily Unavailable
            
            ## Search Query: "%s"
            
            The web automation service is temporarily unavailable. This might be due to:
            - Chrome/Chromium not being installed or accessible
            - Browser automation initialization issues
            - Container environment restrictions
            
            ### What You Can Do:
            1. Try again in a few moments
            2. Contact support if the issue persists
            3. Use alternative search methods
            
            ### Technical Details:
            - Service: WebBrowsingAction
            - Status: Not initialized
            - Fallback: Text-only response
            
            ---
            *This is an automated fallback response*
            """, searchTerm);
    }

    /**
     * Formats a successful response with search results.
     */
    private String formatSuccessResponse(String searchTerm, String textResult, String screenshotResult) {
        return String.format("""
            # Hello World Search Results
            
            ## Search Query: "%s"
            
            ### Text Results:
            %s
            
            ### Visual Results:
            %s
            
            ### Search Completed Successfully
            - Timestamp: %s
            - Service: WebBrowsingAction
            - Status: Success
            
            ---
            *Powered by automated web search*
            """, searchTerm, textResult, screenshotResult, java.time.LocalDateTime.now());
    }

    /**
     * Generates an error response with helpful information.
     */
    private String generateErrorResponse(String searchTerm, String errorMessage) {
        return String.format("""
            # Hello World Search - Error Occurred
            
            ## Search Query: "%s"
            
            ### Error Details:
            %s
            
            ### Troubleshooting:
            1. Check if Chrome/Chromium is installed
            2. Verify Playwright browser automation is properly configured
            3. Check application logs for more details
            
            ### Alternative Actions:
            - Try a simpler search query
            - Wait a moment and retry
            - Contact technical support
            
            ---
            *Error occurred at: %s*
            """, searchTerm, errorMessage, java.time.LocalDateTime.now());
    }
}