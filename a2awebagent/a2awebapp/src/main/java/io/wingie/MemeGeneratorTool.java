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
import java.util.Set;

/**
 * Professional Meme Generator Tool using memegen.link API for creating memes that AI agents can use to communicate with users.
 * 
 * This tool provides comprehensive meme generation capabilities with:
 * - 26+ verified meme templates with proper API names
 * - Intelligent mood-based template selection
 * - Robust special character encoding for URLs
 * - Full ImageContent MCP protocol support
 * - Automatic screenshot capture and serving
 * - Graceful fallback handling
 * 
 * Key Features:
 * - Template-based generation: Use exact API names like 'drake', 'db', 'woman-cat'
 * - Mood-based generation: Specify emotions like 'happy', 'sarcastic', 'confused'
 * - Special character handling: Automatic URL encoding for memegen.link compatibility
 * - Image delivery: Base64 encoded screenshots via MCP ImageContent protocol
 * 
 * Usage Examples:
 * {@code
 * // Direct template usage
 * generateMeme("drake", "Writing bugs", "Writing features");
 * 
 * // Mood-based selection
 * generateMoodMeme("sarcastic", "Code reviews", "Just approve it");
 * 
 * // Special characters handled automatically
 * generateMeme("fry", "Not sure if feature?", "Or bug...");
 * }
 * 
 * @see MoodTemplateMapper for mood-to-template mappings
 * @see ScreenshotService for image handling
 * @author a2awebagent
 * @version 2.0
 */
@Service
@Slf4j
@Agent(name = "memeGenerator", description = "Professional meme generator with template and mood-based selection, special character encoding, and MCP ImageContent support")
public class MemeGeneratorTool {

    @Autowired
    @Lazy
    private io.wingie.playwright.PlaywrightWebBrowsingAction webBrowsingAction;

    @Autowired
    private ScreenshotService screenshotService;

    @Autowired
    private MoodTemplateMapper moodTemplateMapper;
    
    /**
     * Set of all valid template names for validation.
     * These are the exact API names that memegen.link accepts.
     */
    private static final Set<String> VALID_TEMPLATES = Set.of(
        "10-guy", "aag", "ams", "bd", "bear", "blb", "buzz", "cmm", "db", "doge",
        "drake", "fine", "fry", "fwp", "gb", "gru", "kermit", "mordor", "oag", 
        "patrick", "philosoraptor", "pigeon", "pooh", "rollsafe", "spongebob", 
        "stonks", "success", "woman-cat", "yuno"
    );

    /**
     * Generates a meme using the memegen.link API with automatic special character encoding.
     * 
     * CRITICAL: This tool automatically handles special characters in text according to memegen.link URL encoding rules.
     * Agents do NOT need to pre-encode text - the tool handles all encoding automatically.
     * 
     * Special Character Encoding (handled automatically):
     * - Spaces → underscores (_)
     * - Question marks (?) → ~q
     * - Ampersands (&) → ~a  
     * - Percentages (%) → ~p
     * - Hashtags (#) → ~h
     * - Forward slashes (/) → ~s
     * - Double quotes (") → two single quotes ('')
     * - Other special chars → removed for URL safety
     * 
     * Reserved Multi-Character Patterns (for advanced usage):
     * - Double underscores (__) → single underscore (_)
     * - Double dashes (--) → single dash (-)
     * - Tilde + N (~n) → newline character
     * - Other tilde patterns: ~q, ~a, ~p, ~h, ~s, ~b, ~l, ~g (reserved)
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
     * 
     * Usage Examples:
     * generateMeme("drake", "Using var in JS", "Using const in JS");
     * generateMeme("fry", "Not sure if feature?", "Or bug...");  // Special chars handled automatically
     * generateMeme("woman-cat", "You said 5 minutes!", "That was 3 hours ago!");
     */
    @Action(description = "Generate memes with automatic special character encoding and comprehensive template selection", name = "generateMeme")
    public String generateMeme(
        @Parameter(description = "EXACT API NAME ONLY - Use these exact strings: 'drake', 'db', 'woman-cat', 'gb', 'gru', 'pooh', 'fine', 'fry', 'pigeon', 'stonks', 'buzz', 'kermit', 'rollsafe', 'spongebob', 'patrick', 'doge', 'success', 'yuno', 'fwp', 'aag', 'blb', 'oag', 'cmm', 'mordor', 'philosoraptor', 'bear'. Do NOT use descriptive names - use exact API strings only.") String template,
        @Parameter(description = "Top text for the meme. Special characters (?, &, %, #, /, etc.) will be automatically encoded for URL compatibility. Use natural text - no pre-encoding needed.") String topText,
        @Parameter(description = "Bottom text for the meme (optional, can be empty). Special characters will be automatically encoded. Use natural text - no pre-encoding needed.") String bottomText) {
        
        log.info("Generating meme with template: {}, top: {}, bottom: {}", template, topText, bottomText);
        log.info("🔍 DEBUG: Received template from agent: '{}'", template);
        log.info("🔍 DEBUG: Template length: {}, contains spaces: {}", template.length(), template.contains(" "));
        
        // Validate template name first
        String validationError = validateTemplate(template);
        if (validationError != null) {
            log.warn("Template validation failed for '{}': {}", template, validationError);
            return generateTemplateValidationError(template, topText, bottomText, validationError);
        }
        
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
     * Encodes text for use in memegen URLs with comprehensive special character handling.
     * 
     * This method implements the full memegen.link URL encoding specification:
     * - Basic characters: spaces → underscores
     * - Reserved URL chars: ?, &, %, #, / → tilde patterns
     * - Quotes: double quotes → two single quotes
     * - Multi-char patterns: __, -- → single char
     * - Newlines: ~n pattern support
     * - Safety: removes unsafe characters for URL compatibility
     * 
     * @param text The text to encode for URL usage
     * @return URL-safe encoded text suitable for memegen.link API
     */
    private String encodeTextForUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "_";
        }
        
        String encoded = text.trim()
            // Handle newlines first (before other processing)
            .replace("\n", "~n")
            .replace("\r\n", "~n")
            .replace("\r", "~n")
            
            // Basic space replacement
            .replace(" ", "_")
            
            // Reserved URL characters → tilde patterns
            .replace("?", "~q")
            .replace("&", "~a")
            .replace("%", "~p")
            .replace("#", "~h")
            .replace("/", "~s")
            .replace("\\", "~b")
            .replace("<", "~l")
            .replace(">", "~g")
            
            // Quote handling
            .replace("\"", "''")
            
            // Multi-character pattern normalization
            .replace("__", "_")   // Double underscore → single
            .replace("--", "-")   // Double dash → single
            
            // Remove remaining unsafe characters (keep alphanumeric, _, ~, ', -)
            .replaceAll("[^a-zA-Z0-9_~'-]", "");
            
        // Ensure we don't return empty string
        return encoded.isEmpty() ? "_" : encoded;
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
     * Generates a meme using intelligent mood-based template selection with automatic special character encoding.
     * 
     * This method allows agents to express emotions naturally without needing to know specific template names.
     * The system intelligently maps moods to appropriate meme templates based on emotional context.
     * 
     * CRITICAL: Like generateMeme, this tool automatically handles all special character encoding.
     * Agents should use natural text - no pre-encoding needed.
     * 
     * Available Mood Categories:
     * 
     * **Positive Emotions:**
     * - happy, excited, successful, proud, confident, winning → success, stonks, pooh templates
     * 
     * **Negative Emotions:**
     * - sad, frustrated, unlucky, annoyed, angry, upset → blb, fwp, woman-cat, yuno templates
     * 
     * **Sarcastic/Ironic:**
     * - sarcastic, ironic, mocking, dismissive, petty, sassy → kermit, spongebob, rollsafe templates
     * 
     * **Confused/Uncertain:**
     * - confused, uncertain, suspicious, questioning, doubtful, skeptical → fry, pigeon, philosoraptor templates
     * 
     * **Preferences/Choices:**
     * - comparing, choosing, preferring, upgrading, deciding, tempted → drake, pooh, db templates
     * 
     * **Plans/Progression:**
     * - planning, evolving, progression, step-by-step, developing, growing → gru, gb templates
     * 
     * **Acceptance/Denial:**
     * - accepting, denying, coping, resigned, fine, whatever, dealing → fine, bear templates
     * 
     * **Clever/Smart:**
     * - clever, smart, thinking, philosophical, wise, insightful → rollsafe, philosoraptor templates
     * 
     * **Special Moods:**
     * - difficult, challenging, hard, impossible → mordor template
     * - everywhere, abundant, all-around, pervasive → buzz template
     * - aliens, conspiracy, mysterious, unexplained → aag template
     * - obsessive, clingy, attached, possessive → oag template
     * - wow, such, much, very, doge-style → doge template
     * 
     * Usage Examples:
     * generateMoodMeme("sarcastic", "Code reviews", "Just approve it");
     * generateMoodMeme("frustrated", "When the build fails", "Again...");
     * generateMoodMeme("successful", "Code works first try!", "");
     */
    @Action(description = "Generate memes using intelligent mood-based template selection with automatic special character encoding - express emotions naturally", name = "generateMoodMeme")
    public String generateMoodMeme(
        @Parameter(description = "Mood or emotion for intelligent template selection. Use natural language: 'happy', 'frustrated', 'sarcastic', 'confused', 'successful', 'comparing', 'planning', 'accepting', 'clever', 'difficult', 'everywhere', 'obsessive', 'wow', etc. The system maps moods to optimal templates automatically.") String mood,
        @Parameter(description = "Top text for the meme. Special characters (?, &, %, #, /, etc.) will be automatically encoded for URL compatibility. Use natural text - no pre-encoding needed.") String topText,
        @Parameter(description = "Bottom text for the meme (optional, can be empty). Special characters will be automatically encoded. Use natural text - no pre-encoding needed.") String bottomText) {
        
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
     * Validates that the template name is supported by memegen.link API.
     * 
     * @param template The template name to validate
     * @return null if valid, error message if invalid
     */
    private String validateTemplate(String template) {
        if (template == null || template.trim().isEmpty()) {
            return "Template name cannot be null or empty";
        }
        
        String cleanTemplate = template.toLowerCase().trim();
        
        if (!VALID_TEMPLATES.contains(cleanTemplate)) {
            return String.format("Template '%s' is not supported. Valid templates: %s", 
                    template, getPopularTemplatesSuggestion());
        }
        
        return null; // Valid template
    }
    
    /**
     * Gets a suggestion of popular templates for error messages.
     */
    private String getPopularTemplatesSuggestion() {
        return "drake, db, woman-cat, fine, fry, pigeon, success, gb, gru, pooh, kermit, rollsafe";
    }
    
    /**
     * Generates an error response for template validation failures.
     */
    private String generateTemplateValidationError(String template, String topText, String bottomText, String validationError) {
        return String.format("""
            # 🙅 Template Validation Error
            
            ## Invalid Template Request:
            - **Template**: '%s'
            - **Top Text**: "%s"
            - **Bottom Text**: "%s"
            
            ### ⚠️ Validation Error:
            %s
            
            ### 📝 Popular Templates to Try:
            - **drake** - Drake pointing/rejecting format
            - **db** - Distracted boyfriend
            - **woman-cat** - Woman yelling at cat
            - **fine** - This is fine dog
            - **fry** - Futurama Fry "Not sure if"
            - **pigeon** - Is this a pigeon
            - **success** - Success kid
            - **gb** - Galaxy brain (expanding brain)
            - **gru** - Gru's plan
            - **pooh** - Tuxedo Winnie the Pooh
            - **kermit** - But that's none of my business
            - **rollsafe** - Roll Safe
            
            ### 💡 Alternative Options:
            1. **Use getMoodGuide()** to see mood-based selection
            2. **Try generateMoodMeme()** with emotions like 'happy', 'sarcastic', 'confused'
            3. **Use exact template names** from the list above
            
            ### 🔗 Template Reference:
            All templates use exact memegen.link API names. Use the exact strings shown above.
            
            ---
            *Template validation failed at: %s*
            """, template, topText, bottomText, validationError, java.time.LocalDateTime.now());
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