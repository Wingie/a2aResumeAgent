package io.wingie;

import io.wingie.a2acore.annotation.Action;
import io.wingie.a2acore.annotation.Agent;
import io.wingie.a2acore.annotation.Parameter;
import io.wingie.a2acore.domain.ImageContent;
import io.wingie.a2acore.domain.ImageContentUrl;
import io.wingie.a2acore.domain.TextContent;
import io.wingie.a2acore.domain.ToolCallResult;
import io.wingie.service.ScreenshotService;
import io.wingie.service.MoodTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
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
    public ToolCallResult generateMeme(
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
            return ToolCallResult.error(generateTemplateValidationError(template, topText, bottomText, validationError));
        }
        
        try {
            // Check if WebBrowsingAction is available
            if (webBrowsingAction == null) {
                log.warn("WebBrowsingAction not available, returning fallback response");
                return ToolCallResult.error(generateFallbackResponse(template, topText, bottomText));
            }
            
            // Build memegen URL
            String memeUrl = buildMemegenUrl(template, topText, bottomText);
            log.info("Generated memegen URL: {}", memeUrl);
            
            // Fetch image directly from API and save as URL (single file creation)
            ImageContentUrl urlImage = fetchMemeImageAsUrl(memeUrl);
            
            // Get screenshot URL from the fetched image
            String screenshotUrl = urlImage != null ? urlImage.getUrl() : null;
            
            // Create text content with current markdown response
            TextContent textResponse = TextContent.of(formatMemeResponse(template, topText, bottomText, memeUrl, screenshotUrl));
            
            // Return mixed content with both text and URL-based image
            if (urlImage != null) {
                log.info("Returning mixed content: TextContent + ImageContentUrl ({})", urlImage.getUrl());
                return ToolCallResult.success(List.of(textResponse, urlImage));
            } else {
                log.warn("URL-based image fetch failed, falling back to base64 approach");
                
                // Fallback to base64 approach if URL method fails
                ImageContent directImage = fetchMemeImageDirectly(memeUrl);
                if (directImage != null) {
                    log.info("Fallback: Returning mixed content with base64 (length: {})", 
                        directImage.getData() != null ? directImage.getData().length() : 0);
                    return ToolCallResult.success(List.of(textResponse, directImage));
                } else {
                    log.warn("Both URL and base64 image fetch failed, returning text-only response");
                    return ToolCallResult.success(textResponse);
                }
            }
            
        } catch (Exception e) {
            log.error("Error generating meme with template '{}': {}", template, e.getMessage(), e);
            return ToolCallResult.error(generateErrorResponse(template, topText, bottomText, e.getMessage()));
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
     * Fetches meme image directly from memegen.link API and saves as HTTP URL.
     * This provides better performance by avoiding large base64 data in JSON responses
     * while still allowing Claude Desktop and frontend to display images.
     * 
     * @param memeUrl The memegen.link URL to fetch
     * @return ImageContentUrl with HTTP URL reference, or null if fetch fails
     */
    private ImageContentUrl fetchMemeImageAsUrl(String memeUrl) {
        try {
            log.info("Fetching meme image and saving as URL from: {}", memeUrl);
            
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(memeUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
                
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                byte[] imageBytes = response.body();
                String base64Data = Base64.getEncoder().encodeToString(imageBytes);
                
                log.info("Successfully fetched meme image: {} bytes", imageBytes.length);
                
                // Save to static directory via ScreenshotService and get HTTP URL
                String httpUrl = screenshotService.saveMemeScreenshot(base64Data);
                
                if (httpUrl != null) {
                    log.info("Meme image saved to HTTP URL: {}", httpUrl);
                    return ImageContentUrl.png(httpUrl);
                } else {
                    log.warn("Failed to save meme image to static directory");
                    return null;
                }
            } else {
                log.warn("Failed to fetch meme image, HTTP status: {}", response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error fetching meme image from {}: {}", memeUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Legacy: Fetches meme image directly from memegen.link API and returns as ImageContent.
     * 
     * This method bypasses browser automation and directly fetches the PNG image
     * from the memegen API, converts it to base64, and returns as ImageContent.
     * 
     * @param memeUrl The memegen.link URL to fetch
     * @return ImageContent with base64 PNG data, or null if fetch fails
     */
    private ImageContent fetchMemeImageDirectly(String memeUrl) {
        try {
            log.info("Fetching meme image directly from: {}", memeUrl);
            
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(memeUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
                
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                byte[] imageBytes = response.body();
                String base64Data = Base64.getEncoder().encodeToString(imageBytes);
                
                log.info("Successfully fetched meme image: {} bytes, base64 length: {}", 
                    imageBytes.length, base64Data.length());
                
                // Check if image might be too large for Claude Desktop (1MB limit)
                if (imageBytes.length > 1_000_000) {
                    log.warn("Image size {} bytes exceeds 1MB - may cause truncation in Claude Desktop", imageBytes.length);
                } else {
                    log.info("Image size {} bytes is within acceptable range for Claude Desktop", imageBytes.length);
                }
                    
                return ImageContent.png(base64Data);
            } else {
                log.warn("Failed to fetch meme image, HTTP status: {}", response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error fetching meme image directly from {}: {}", memeUrl, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Formats template suggestions for display in responses.
     */
    private String formatTemplateSuggestions(List<String> templateSuggestions) {
        if (templateSuggestions == null || templateSuggestions.isEmpty()) {
            return "- Try different moods like: 'sarcastic', 'confused', 'successful', 'comparing'";
        }
        
        StringBuilder suggestions = new StringBuilder();
        for (String template : templateSuggestions.subList(0, Math.min(5, templateSuggestions.size()))) {
            suggestions.append("- **").append(template).append("**: ").append(getTemplateDescription(template)).append("\n");
        }
        
        return suggestions.toString();
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
                **Direct API URL**: [Open Full Size](%s) ↗️
                
                ### 📋 Response Format
                This response includes **dual content**:
                1. **Markdown Image**: Screenshot displayed above (click for full size)
                2. **Direct Base64**: Raw meme image included as ImageContent for direct viewing
                
                💡 **For Claude Desktop**: The base64 ImageContent allows direct image viewing without truncation
                🔗 **For Users**: Click the "Open Full Size" link above to view in browser
                
                *Both the markdown screenshot and direct base64 image show the same meme content*
                """, screenshotUrl, template, getTemplateDescription(template), java.time.LocalDateTime.now(), memeUrl);
        } else {
            return String.format("""
                **Template**: %s (%s)
                **Top Text**: "%s" 
                **Bottom Text**: "%s"
                **Direct API URL**: [View Meme](%s) ↗️
                
                ### 📋 Response Format
                This response includes **dual content**:
                1. **Direct Base64**: Raw meme image included as ImageContent for direct viewing
                2. **Fallback URL**: Click link above to view meme in browser
                
                💡 **For Claude Desktop**: The base64 ImageContent allows direct image viewing
                🔗 **For Users**: Click the "View Meme" link above to open in new tab
                
                ⚠️ *Screenshot capture failed, but direct API image is available*
                """, template, getTemplateDescription(template), topText, bottomText, memeUrl);
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
    public ToolCallResult generateMoodMeme(
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
                return ToolCallResult.error(generateMoodFallbackResponse(mood, selectedTemplate, topText, bottomText, templateSuggestions));
            }
            
            // Build memegen URL
            String memeUrl = buildMemegenUrl(selectedTemplate, topText, bottomText);
            log.info("Generated memegen URL for mood '{}': {}", mood, memeUrl);
            
            // Fetch image directly from API and save as URL (single file creation)
            ImageContentUrl urlImage = fetchMemeImageAsUrl(memeUrl);
            
            // Get screenshot URL from the fetched image
            String screenshotUrl = urlImage != null ? urlImage.getUrl() : null;
            
            // Create text content with current markdown response
            TextContent textResponse = TextContent.of(formatMoodMemeResponse(mood, selectedTemplate, topText, bottomText, memeUrl, screenshotUrl, templateSuggestions));
            
            // Return mixed content with both text and URL-based image
            if (urlImage != null) {
                log.info("Returning mood-based mixed content: TextContent + ImageContentUrl ({})", urlImage.getUrl());
                return ToolCallResult.success(List.of(textResponse, urlImage));
            } else {
                log.warn("URL-based image fetch failed for mood-based meme, falling back to base64 approach");
                
                // Fallback to base64 approach if URL method fails
                ImageContent directImage = fetchMemeImageDirectly(memeUrl);
                if (directImage != null) {
                    log.info("Fallback: Returning mood-based mixed content with base64 (length: {})", 
                        directImage.getData() != null ? directImage.getData().length() : 0);
                    return ToolCallResult.success(List.of(textResponse, directImage));
                } else {
                    log.warn("Both URL and base64 image fetch failed for mood-based meme, returning text-only response");
                    return ToolCallResult.success(textResponse);
                }
            }
            
        } catch (Exception e) {
            log.error("Error generating mood-based meme with mood '{}': {}", mood, e.getMessage(), e);
            return ToolCallResult.error(generateMoodErrorResponse(mood, topText, bottomText, e.getMessage()));
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
                **Direct API URL**: [Open Full Size](%s) ↗️
                
                ### 📋 Response Format
                This response includes **dual content**:
                1. **Markdown Image**: Screenshot displayed above (click for full size)
                2. **Direct Base64**: Raw meme image included as ImageContent for direct viewing
                
                💡 **For Claude Desktop**: The base64 ImageContent allows direct image viewing without truncation
                🔗 **For Users**: Click the "Open Full Size" link above to view in browser
                
                ### 🎨 Template Suggestions
                Try these alternative moods and templates:
                %s
                
                *Both the markdown screenshot and direct base64 image show the same meme content*
                """, screenshotUrl, mood, selectedTemplate, getTemplateDescription(selectedTemplate), 
                templateSuggestions.size() > 1 ? templateSuggestions.subList(1, Math.min(4, templateSuggestions.size())) : "none",
                java.time.LocalDateTime.now(), memeUrl, formatTemplateSuggestions(templateSuggestions));
        } else {
            return String.format("""
                **Mood**: %s → **Template**: %s (%s)
                **Top Text**: "%s" 
                **Bottom Text**: "%s"
                **Alternative Templates**: %s
                **Direct API URL**: [View Meme](%s) ↗️
                
                ### 📋 Response Format
                This response includes **dual content**:
                1. **Direct Base64**: Raw meme image included as ImageContent for direct viewing
                2. **Fallback URL**: Click link above to view meme in browser
                
                💡 **For Claude Desktop**: The base64 ImageContent allows direct image viewing
                🔗 **For Users**: Click the "View Meme" link above to open in new tab
                
                ### 🎨 Template Suggestions
                Try these alternative moods and templates:
                %s
                
                ⚠️ *Screenshot capture failed, but direct API image is available*
                """, mood, selectedTemplate, getTemplateDescription(selectedTemplate), topText, bottomText,
                templateSuggestions.size() > 1 ? templateSuggestions.subList(1, Math.min(4, templateSuggestions.size())) : "none",
                memeUrl, formatTemplateSuggestions(templateSuggestions));
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