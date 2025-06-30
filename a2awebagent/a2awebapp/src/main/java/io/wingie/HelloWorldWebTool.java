package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Meme Generator Tool using memegen API for creating memes that Claude can use to communicate with users.
 * Generates memes by visiting the memegen website and taking screenshots via browser automation.
 */
@Service
@Slf4j
@Agent(name = "meme", description = "Generate memes for communication using memegen API")
public class HelloWorldWebTool {

    @Autowired
    @Lazy
    private io.wingie.playwright.PlaywrightWebBrowsingAction webBrowsingAction;

    /**
     * Generates a meme using the memegen API that Claude can use to communicate with users.
     * Takes meme template, top text, and bottom text to create custom memes.
     */
    @Action(description = "Generate memes for communication using memegen API - specify template, top text, and bottom text", name = "generateMeme")
    public String generateMeme(
        @Parameter(description = "Meme template (e.g., 'drake', 'distracted-boyfriend', 'woman-yelling-at-cat', 'this-is-fine', 'expanding-brain', 'change-my-mind', 'two-buttons', 'is-this-a-pigeon')") String template,
        @Parameter(description = "Top text for the meme") String topText,
        @Parameter(description = "Bottom text for the meme (optional, can be empty)") String bottomText) {
        
        log.info("Generating meme with template: {}, top: {}, bottom: {}", template, topText, bottomText);
        
        try {
            // Check if WebBrowsingAction is available
            if (webBrowsingAction == null) {
                log.warn("WebBrowsingAction not available, returning fallback response");
                return generateFallbackResponse(template, topText, bottomText);
            }
            
            // Build memegen URL
            String memeUrl = buildMemegenUrl(template, topText, bottomText);
            log.info("Generated memegen URL: {}", memeUrl);
            
            // Take a screenshot of the meme
            String screenshotResult = webBrowsingAction.browseWebAndReturnImage(
                String.format("Navigate to %s and take a high-quality screenshot of the meme", memeUrl)
            ).getData(); // Extract base64 data from ImageContent
            
            return formatMemeResponse(template, topText, bottomText, memeUrl, screenshotResult);
            
        } catch (Exception e) {
            log.error("Error generating meme with template '{}': {}", template, e.getMessage(), e);
            return generateErrorResponse(template, topText, bottomText, e.getMessage());
        }
    }

    /**
     * Builds a memegen URL from template and text parameters.
     * Format: https://api.memegen.link/images/{template}/{top_text}/{bottom_text}.png
     */
    private String buildMemegenUrl(String template, String topText, String bottomText) {
        // Clean and encode text for URL
        String encodedTop = encodeTextForUrl(topText);
        String encodedBottom = encodeTextForUrl(bottomText);
        
        // Build URL based on whether we have bottom text
        if (bottomText == null || bottomText.trim().isEmpty()) {
            return String.format("https://api.memegen.link/images/%s/%s.png", 
                template.toLowerCase().trim(), encodedTop);
        } else {
            return String.format("https://api.memegen.link/images/%s/%s/%s.png", 
                template.toLowerCase().trim(), encodedTop, encodedBottom);
        }
    }
    
    /**
     * Encodes text for use in memegen URLs - replaces spaces with underscores and handles special characters.
     */
    private String encodeTextForUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "_";
        }
        
        return text.trim()
            .replace(" ", "_")
            .replace("?", "~q")
            .replace("%", "~p")
            .replace("#", "~h")
            .replace("/", "~s")
            .replace("\"", "''")
            .replace("&", "~a")
            .replaceAll("[^a-zA-Z0-9_~']", "");
    }

    /**
     * Generates a fallback response when WebBrowsingAction is not available.
     */
    private String generateFallbackResponse(String template, String topText, String bottomText) {
        String memeUrl = buildMemegenUrl(template, topText, bottomText);
        return String.format("""
            # Meme Generator - Service Temporarily Unavailable
            
            ## Requested Meme: 
            - **Template**: %s
            - **Top Text**: %s
            - **Bottom Text**: %s
            - **Would Generate URL**: %s
            
            The web automation service is temporarily unavailable. This might be due to:
            - Chrome/Chromium not being installed or accessible
            - Browser automation initialization issues
            - Container environment restrictions
            
            ### What You Can Do:
            1. Try again in a few moments
            2. Visit the URL directly in your browser: %s
            3. Contact support if the issue persists
            
            ### Popular Meme Templates:
            - `drake` - Drake pointing meme
            - `distracted-boyfriend` - Distracted boyfriend meme  
            - `woman-yelling-at-cat` - Woman yelling at cat
            - `this-is-fine` - This is fine dog
            - `expanding-brain` - Expanding brain levels
            - `change-my-mind` - Change my mind sign
            - `two-buttons` - Two buttons choice
            - `is-this-a-pigeon` - Is this a pigeon?
            
            ### Technical Details:
            - Service: WebBrowsingAction
            - Status: Not initialized
            - Fallback: Text-only response with URL
            
            ---
            *This is an automated fallback response*
            """, template, topText, bottomText, memeUrl, memeUrl);
    }

    /**
     * Formats a successful meme generation response.
     */
    private String formatMemeResponse(String template, String topText, String bottomText, String memeUrl, String screenshotResult) {
        return String.format("""
            # 🎭 Meme Generated Successfully!
            
            ## Meme Details:
            - **Template**: %s
            - **Top Text**: "%s"
            - **Bottom Text**: "%s"
            - **Source URL**: %s
            
            ### 📸 Generated Meme:
            %s
            
            ### ✅ Generation Completed Successfully
            - Timestamp: %s
            - Service: WebBrowsingAction + Memegen API
            - Status: Success
            - Format: PNG screenshot via browser automation
            
            ### 💡 Tips for Better Memes:
            - Keep text short and punchy
            - Popular templates work best
            - Try different templates for variety
            
            ---
            *🤖 Claude Code Meme Generator - Powered by memegen.link*
            """, template, topText, bottomText, memeUrl, screenshotResult, java.time.LocalDateTime.now());
    }

    /**
     * Generates an error response with helpful information.
     */
    private String generateErrorResponse(String template, String topText, String bottomText, String errorMessage) {
        String memeUrl = buildMemegenUrl(template, topText, bottomText);
        return String.format("""
            # 🚫 Meme Generation - Error Occurred
            
            ## Requested Meme:
            - **Template**: %s
            - **Top Text**: "%s"
            - **Bottom Text**: "%s"
            - **Attempted URL**: %s
            
            ### ❌ Error Details:
            %s
            
            ### 🔧 Troubleshooting:
            1. **Check Template Name**: Ensure the template exists (try 'drake', 'distracted-boyfriend', etc.)
            2. **Text Length**: Very long text might cause issues
            3. **Special Characters**: Some characters might not be supported
            4. **Browser Issues**: Check if Chrome/Chromium is installed and working
            5. **Network**: Verify internet connection to memegen.link
            
            ### 🔄 Alternative Actions:
            - Try a different meme template
            - Shorten the text
            - Remove special characters
            - Wait a moment and retry
            - Visit the URL directly: %s
            
            ### 📋 Popular Working Templates:
            - `drake` - Drake pointing/rejecting
            - `distracted-boyfriend` - Distracted boyfriend looking back
            - `woman-yelling-at-cat` - Woman yelling at confused cat
            - `this-is-fine` - Dog sitting in burning room
            - `expanding-brain` - Brain expanding through levels
            - `change-my-mind` - Steven Crowder sign
            - `two-buttons` - Sweating guy choosing between buttons
            - `is-this-a-pigeon` - Butterfly identification confusion
            
            ---
            *❗ Error occurred at: %s*
            """, template, topText, bottomText, memeUrl, errorMessage, memeUrl, java.time.LocalDateTime.now());
    }
}