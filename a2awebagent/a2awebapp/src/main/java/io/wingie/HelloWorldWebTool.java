package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ImageContent;
import io.wingie.service.ScreenshotService;
import io.wingie.service.MoodTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Autowired
    private ScreenshotService screenshotService;

    @Autowired
    private MoodTemplateMapper moodTemplateMapper;

    /**
     * Generates a meme using the memegen API that Claude can use to communicate with users.
     * Takes meme template, top text, and bottom text to create custom memes.
     * 
     * Available verified meme templates (use exact API names):
     * 
     * **Popular Choice Templates:**
     * - drake: Drake pointing/rejecting format - preferences (top=reject, bottom=prefer)
     * - db: Distracted boyfriend - temptation/choices
     * - woman-cat: Woman yelling at cat - arguments/confusion
     * - gb: Galaxy brain (expanding brain) - progression of ideas from simple to complex
     * - gru: Gru's plan - multi-step plans that go wrong
     * - pooh: Tuxedo Winnie the Pooh - classy vs basic choices
     * 
     * **Reaction Templates:**
     * - fine: This is fine dog - denial/acceptance of problems
     * - fry: Futurama Fry "Not sure if" - suspicion/uncertainty
     * - pigeon: Is this a pigeon - misunderstanding/confusion
     * - stonks: Stonks man - financial gains/investments
     * - buzz: X, X everywhere - pointing out something everywhere
     * - kermit: But that's none of my business - sarcastic observations
     * - rollsafe: Roll Safe - being clever/smart
     * 
     * **Character-based Templates:**
     * - spongebob: Mocking SpongeBob - repeating someone mockingly
     * - patrick: Push it somewhere else Patrick - relocating problems
     * - doge: Doge - much wow, very excite format
     * - success: Success kid - celebrating small victories
     * - yuno: Y U NO guy - frustration with actions
     * - fwp: First world problems - trivial complaints
     * - aag: Ancient aliens guy - conspiracy theories/wild explanations
     * - blb: Bad luck Brian - when things go wrong
     * - oag: Overly attached girlfriend - clingy/obsessive behavior
     * 
     * **Advice/Statement Templates:**
     * - cmm: Change my mind - controversial statements
     * - mordor: One does not simply - expressing difficulty
     * - philosoraptor: Thinking raptor - philosophical questions
     * - bear: Confession bear - admitting something awkward
     */
    @Action(description = "Generate memes for communication using memegen API with comprehensive template selection", name = "generateMeme")
    public String generateMeme(
        @Parameter(description = "EXACT API NAME ONLY - Use these exact strings: 'drake', 'db', 'woman-cat', 'gb', 'gru', 'pooh', 'fine', 'fry', 'pigeon', 'stonks', 'buzz', 'kermit', 'rollsafe', 'spongebob', 'patrick', 'doge', 'success', 'yuno', 'fwp', 'aag', 'blb', 'oag', 'cmm', 'mordor', 'philosoraptor', 'bear'. Do NOT use descriptive names - use exact strings only.") String template,
        @Parameter(description = "Top text for the meme") String topText,
        @Parameter(description = "Bottom text for the meme (optional, can be empty)") String bottomText) {
        
        log.info("Generating meme with template: {}, top: {}, bottom: {}", template, topText, bottomText);
        log.info("🔍 DEBUG: Received template from Claude: '{}'", template);
        log.info("🔍 DEBUG: Template length: {}, contains spaces: {}", template.length(), template.contains(" "));
        
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
            ImageContent screenshotImage = webBrowsingAction.browseWebAndReturnImage(
                String.format("Navigate to %s and take a high-quality screenshot of the meme", memeUrl)
            );
            
            // Save screenshot to static directory and get URL
            String screenshotUrl = null;
            if (screenshotImage != null && screenshotImage.getData() != null) {
                screenshotUrl = screenshotService.saveMemeScreenshot(screenshotImage.getData());
            }
            
            return formatMemeResponse(template, topText, bottomText, memeUrl, screenshotUrl);
            
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
        log.info("🔍 DEBUG: buildMemegenUrl called with template: '{}'", template);
        
        // Clean and encode text for URL
        String encodedTop = encodeTextForUrl(topText);
        String encodedBottom = encodeTextForUrl(bottomText);
        
        log.info("🔍 DEBUG: Encoded texts - top: '{}', bottom: '{}'", encodedTop, encodedBottom);
        
        // Clean template name
        String cleanTemplate = template.toLowerCase().trim();
        log.info("🔍 DEBUG: Cleaned template name: '{}'", cleanTemplate);
        
        // Build URL based on whether we have bottom text
        String finalUrl;
        if (bottomText == null || bottomText.trim().isEmpty()) {
            finalUrl = String.format("https://api.memegen.link/images/%s/%s.png", 
                cleanTemplate, encodedTop);
        } else {
            finalUrl = String.format("https://api.memegen.link/images/%s/%s/%s.png", 
                cleanTemplate, encodedTop, encodedBottom);
        }
        
        log.info("🔍 DEBUG: Final memegen URL: '{}'", finalUrl);
        return finalUrl;
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
            - `db` - Distracted boyfriend meme  
            - `woman-cat` - Woman yelling at cat
            - `fine` - This is fine dog
            - `gb` - Galaxy brain (expanding brain)
            - `cmm` - Change my mind sign
            - `gru` - Gru's plan
            - `pigeon` - Is this a pigeon?
            
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
    private String formatMemeResponse(String template, String topText, String bottomText, String memeUrl, String screenshotUrl) {
        if (screenshotUrl != null) {
            return String.format("""
                ![Generated Meme](%s)
                
                **Template**: %s (%s)
                **Created**: %s
                
                *Render this image inline using the markdown above*
                """, screenshotUrl, template, getTemplateDescription(template), java.time.LocalDateTime.now());
        } else {
            return String.format("""
                ⚠️ **Meme Generation Failed**
                
                **Template**: %s (%s)
                **Top Text**: "%s" 
                **Bottom Text**: "%s"
                
                Screenshot capture failed. The meme was generated but couldn't be captured.
                """, template, getTemplateDescription(template), topText, bottomText);
        }
    }

    /**
     * Gets a description for a meme template.
     */
    private String getTemplateDescription(String template) {
        log.info("🔍 DEBUG: getTemplateDescription called with template: '{}'", template);
        String description = switch (template.toLowerCase()) {
            case "drake" -> "Drake pointing/rejecting format";
            case "db" -> "Distracted boyfriend";
            case "woman-cat" -> "Woman yelling at cat";
            case "gb" -> "Galaxy brain (expanding brain)";
            case "gru" -> "Gru's plan";
            case "pooh" -> "Tuxedo Winnie the Pooh";
            case "fine" -> "This is fine dog";
            case "fry" -> "Futurama Fry 'Not sure if'";
            case "pigeon" -> "Is this a pigeon";
            case "stonks" -> "Stonks man";
            case "buzz" -> "X, X everywhere";
            case "kermit" -> "But that's none of my business";
            case "rollsafe" -> "Roll Safe";
            case "spongebob" -> "Mocking SpongeBob";
            case "patrick" -> "Push it somewhere else Patrick";
            case "doge" -> "Doge";
            case "success" -> "Success kid";
            case "yuno" -> "Y U NO guy";
            case "fwp" -> "First world problems";
            case "aag" -> "Ancient aliens guy";
            case "blb" -> "Bad luck Brian";
            case "oag" -> "Overly attached girlfriend";
            case "cmm" -> "Change my mind";
            case "mordor" -> "One does not simply";
            case "philosoraptor" -> "Thinking raptor";
            case "bear" -> "Confession bear";
            default -> "Custom meme template";
        };
        log.info("🔍 DEBUG: Template description for '{}': '{}'", template, description);
        return description;
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
            - `db` - Distracted boyfriend
            - `woman-cat` - Woman yelling at cat
            - `fine` - This is fine dog
            - `gb` - Galaxy brain (expanding brain)
            - `cmm` - Change my mind
            - `gru` - Gru's plan
            - `pigeon` - Is this a pigeon
            
            ---
            *❗ Error occurred at: %s*
            """, template, topText, bottomText, memeUrl, errorMessage, memeUrl, java.time.LocalDateTime.now());
    }

    /**
     * Generates a meme using mood-based template selection.
     * Claude can specify a mood/emotion and the system will automatically choose an appropriate template.
     */
    @Action(description = "Generate memes using mood-based template selection - let Claude express emotions through appropriate meme templates", name = "generateMoodMeme")
    public String generateMoodMeme(
        @Parameter(description = "Mood or emotion for template selection. Examples: 'happy', 'frustrated', 'sarcastic', 'confused', 'successful', 'comparing', 'planning', 'accepting'. The system will choose an appropriate template based on this mood.") String mood,
        @Parameter(description = "Top text for the meme") String topText,
        @Parameter(description = "Bottom text for the meme (optional, can be empty)") String bottomText) {
        
        log.info("Generating mood-based meme with mood: {}, top: {}, bottom: {}", mood, topText, bottomText);
        
        try {
            // Get template suggestions for the mood
            List<String> templateSuggestions = moodTemplateMapper.getTemplatesForMood(mood);
            String selectedTemplate = templateSuggestions.isEmpty() ? "drake" : templateSuggestions.get(0);
            
            log.info("🎭 Mood '{}' mapped to template suggestions: {}, selected: '{}'", 
                    mood, templateSuggestions, selectedTemplate);
            
            // Check if WebBrowsingAction is available
            if (webBrowsingAction == null) {
                log.warn("WebBrowsingAction not available, returning fallback response");
                return generateMoodFallbackResponse(mood, selectedTemplate, topText, bottomText, templateSuggestions);
            }
            
            // Build memegen URL
            String memeUrl = buildMemegenUrl(selectedTemplate, topText, bottomText);
            log.info("Generated memegen URL for mood '{}': {}", mood, memeUrl);
            
            // Take a screenshot of the meme
            ImageContent screenshotImage = webBrowsingAction.browseWebAndReturnImage(
                String.format("Navigate to %s and take a high-quality screenshot of the meme", memeUrl)
            );
            
            // Save screenshot to static directory and get URL
            String screenshotUrl = null;
            if (screenshotImage != null && screenshotImage.getData() != null) {
                screenshotUrl = screenshotService.saveMemeScreenshot(screenshotImage.getData());
            }
            
            return formatMoodMemeResponse(mood, selectedTemplate, topText, bottomText, memeUrl, screenshotUrl, templateSuggestions);
            
        } catch (Exception e) {
            log.error("Error generating mood-based meme with mood '{}': {}", mood, e.getMessage(), e);
            return generateMoodErrorResponse(mood, topText, bottomText, e.getMessage());
        }
    }

    /**
     * Gets available mood categories and template mappings for reference.
     */
    @Action(description = "Get a guide showing available mood categories for meme generation and their corresponding templates", name = "getMoodGuide")
    public String getMoodGuide() {
        try {
            String guide = moodTemplateMapper.getMoodGuide();
            log.info("Provided mood guide with {} available mood categories", moodTemplateMapper.getAvailableMoods().size());
            return guide;
        } catch (Exception e) {
            log.error("Error getting mood guide: {}", e.getMessage(), e);
            return "**Mood Guide Temporarily Unavailable**\\n\\nPlease try again later or use the regular generateMeme method with specific template names.";
        }
    }

    /**
     * Formats a successful mood-based meme generation response.
     */
    private String formatMoodMemeResponse(String mood, String selectedTemplate, String topText, String bottomText, 
                                         String memeUrl, String screenshotUrl, List<String> templateSuggestions) {
        if (screenshotUrl != null) {
            return String.format("""
                ![Generated Meme](%s)
                
                **Mood**: %s → **Template**: %s (%s)
                **Alternative Templates**: %s
                **Created**: %s
                
                *Render this image inline using the markdown above*
                """, screenshotUrl, mood, selectedTemplate, getTemplateDescription(selectedTemplate), 
                templateSuggestions.size() > 1 ? templateSuggestions.subList(1, Math.min(4, templateSuggestions.size())) : "none",
                java.time.LocalDateTime.now());
        } else {
            return String.format("""
                ⚠️ **Mood-Based Meme Generation Failed**
                
                **Mood**: %s → **Template**: %s (%s)
                **Top Text**: "%s" 
                **Bottom Text**: "%s"
                **Alternative Templates**: %s
                
                Screenshot capture failed. The meme was generated but couldn't be captured.
                """, mood, selectedTemplate, getTemplateDescription(selectedTemplate), topText, bottomText,
                templateSuggestions.size() > 1 ? templateSuggestions.subList(1, Math.min(4, templateSuggestions.size())) : "none");
        }
    }

    /**
     * Generates a fallback response for mood-based memes when WebBrowsingAction is not available.
     */
    private String generateMoodFallbackResponse(String mood, String selectedTemplate, String topText, String bottomText, List<String> templateSuggestions) {
        String memeUrl = buildMemegenUrl(selectedTemplate, topText, bottomText);
        return String.format("""
            # 🎭 Mood-Based Meme Generator - Service Temporarily Unavailable
            
            ## Requested Mood Meme: 
            - **Mood**: %s
            - **Selected Template**: %s (%s)
            - **Top Text**: %s
            - **Bottom Text**: %s
            - **Would Generate URL**: %s
            
            ### 🎨 Mood Mapping Results:
            **Template Suggestions for '%s'**: %s
            
            The web automation service is temporarily unavailable. This might be due to:
            - Chrome/Chromium not being installed or accessible
            - Browser automation initialization issues
            - Container environment restrictions
            
            ### What You Can Do:
            1. Try again in a few moments
            2. Visit the URL directly in your browser: %s
            3. Use a different mood or try the regular generateMeme method
            
            ### 🎭 Available Mood Categories:
            - **Positive**: happy, successful, confident → success, stonks, pooh
            - **Sarcastic**: sarcastic, mocking, dismissive → kermit, spongebob, rollsafe  
            - **Confused**: confused, uncertain, suspicious → fry, pigeon, philosoraptor
            - **Frustrated**: frustrated, annoyed, angry → woman-cat, yuno, blb
            - **Comparing**: comparing, choosing, preferring → drake, pooh, db
            
            ---
            *This is an automated fallback response*
            """, mood, selectedTemplate, getTemplateDescription(selectedTemplate), topText, bottomText, memeUrl, 
            mood, templateSuggestions, memeUrl);
    }

    /**
     * Generates an error response for mood-based meme generation.
     */
    private String generateMoodErrorResponse(String mood, String topText, String bottomText, String errorMessage) {
        return String.format("""
            # 🚫 Mood-Based Meme Generation - Error Occurred
            
            ## Requested Mood Meme:
            - **Mood**: %s
            - **Top Text**: "%s"
            - **Bottom Text**: "%s"
            
            ### ❌ Error Details:
            %s
            
            ### 🔧 Troubleshooting:
            1. **Check Mood**: Try a different mood (e.g., 'happy', 'frustrated', 'sarcastic')
            2. **Text Length**: Very long text might cause issues
            3. **Service Status**: The mood mapping service might be temporarily unavailable
            4. **Alternative**: Use the regular generateMeme method with a specific template
            
            ### 🎭 Popular Moods to Try:
            - `happy` or `successful` → success, stonks templates
            - `frustrated` or `annoyed` → woman-cat, yuno templates
            - `sarcastic` or `mocking` → kermit, spongebob templates
            - `confused` or `uncertain` → fry, pigeon templates
            - `comparing` or `choosing` → drake, pooh templates
            
            ### 🔄 Alternative Actions:
            - Try the getMoodGuide action for complete mood categories
            - Use generateMeme with a specific template like 'drake' or 'fine'
            - Wait a moment and retry with a simpler mood description
            
            ---
            *❗ Error occurred at: %s*
            """, mood, topText, bottomText, errorMessage, java.time.LocalDateTime.now());
    }
}