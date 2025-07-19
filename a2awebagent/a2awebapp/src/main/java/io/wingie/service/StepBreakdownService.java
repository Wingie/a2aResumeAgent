package io.wingie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for intelligently breaking down natural language instructions into executable browser steps
 */
@Service
@Slf4j
public class StepBreakdownService {

    // Pattern matching for common action verbs and targets
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(?:navigate to|go to|visit|open)\\s+([\\w\\.-]+\\.[a-z]{2,}(?:/\\S*)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEARCH_PATTERN = Pattern.compile("(?i)search for\\s+['\"]?([^'\"\\n]+)['\"]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_PATTERN = Pattern.compile("(?i)click(?:\\s+on)?\\s+['\"]?([^'\"\\n]+)['\"]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILL_PATTERN = Pattern.compile("(?i)(?:fill|enter|type)\\s+['\"]?([^'\"\\n]+)['\"]?\\s+(?:in|into)\\s+['\"]?([^'\"\\n]+)['\"]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)select\\s+['\"]?([^'\"\\n]+)['\"]?", Pattern.CASE_INSENSITIVE);
    
    // Common workflow patterns
    private static final Pattern BOOKING_PATTERN = Pattern.compile("(?i)book\\s+(?:a\\s+)?(flight|hotel|car|ticket)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOPPING_PATTERN = Pattern.compile("(?i)(?:buy|purchase|order)\\s+([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(?i)(?:compare|find\\s+(?:the\\s+)?(?:best|cheapest))\\s+([^\\n]+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Break down natural language instructions into executable browser steps
     */
    public List<String> breakdownInstructions(String instructions, int maxSteps) {
        log.debug("Breaking down instructions: {}", instructions);
        
        // If instructions already contain explicit steps (newlines), use enhanced parsing
        if (instructions.contains("\n")) {
            return enhanceExplicitSteps(instructions, maxSteps);
        }
        
        // Single instruction - use intelligent breakdown
        return intelligentBreakdown(instructions, maxSteps);
    }
    
    /**
     * Enhance explicitly defined steps with better parsing
     */
    private List<String> enhanceExplicitSteps(String instructions, int maxSteps) {
        List<String> steps = new ArrayList<>();
        String[] rawSteps = instructions.split("\\n");
        
        for (String rawStep : rawSteps) {
            rawStep = rawStep.trim();
            if (rawStep.isEmpty()) continue;
            
            // Enhance each step with more context
            String enhancedStep = enhanceStep(rawStep);
            steps.add(enhancedStep);
            
            if (steps.size() >= maxSteps) break;
        }
        
        return steps;
    }
    
    /**
     * Intelligently break down single complex instruction into steps
     */
    private List<String> intelligentBreakdown(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        String lowerInstruction = instruction.toLowerCase();
        
        // Detect workflow type and generate appropriate steps
        if (isBookingWorkflow(lowerInstruction)) {
            steps.addAll(generateBookingSteps(instruction, maxSteps));
        } else if (isShoppingWorkflow(lowerInstruction)) {
            steps.addAll(generateShoppingSteps(instruction, maxSteps));
        } else if (isSearchWorkflow(lowerInstruction)) {
            steps.addAll(generateSearchSteps(instruction, maxSteps));
        } else if (isComparisonWorkflow(lowerInstruction)) {
            steps.addAll(generateComparisonSteps(instruction, maxSteps));
        } else {
            // Default: try to parse as individual actions
            steps.addAll(generateGenericSteps(instruction, maxSteps));
        }
        
        // Ensure we don't exceed maxSteps
        if (steps.size() > maxSteps) {
            steps = steps.subList(0, maxSteps);
        }
        
        log.info("Generated {} steps from instruction: {}", steps.size(), instruction);
        return steps;
    }
    
    private boolean isBookingWorkflow(String instruction) {
        return BOOKING_PATTERN.matcher(instruction).find() ||
               instruction.contains("reserve") ||
               instruction.contains("book");
    }
    
    private boolean isShoppingWorkflow(String instruction) {
        return SHOPPING_PATTERN.matcher(instruction).find() ||
               instruction.contains("add to cart") ||
               instruction.contains("checkout");
    }
    
    private boolean isSearchWorkflow(String instruction) {
        return instruction.contains("search") ||
               instruction.contains("find") ||
               instruction.contains("look for");
    }
    
    private boolean isComparisonWorkflow(String instruction) {
        return COMPARISON_PATTERN.matcher(instruction).find() ||
               instruction.contains("compare") ||
               instruction.contains("best") ||
               instruction.contains("cheapest");
    }
    
    private List<String> generateBookingSteps(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        
        // Extract location/details from instruction
        String details = extractBookingDetails(instruction);
        String site = determineBestBookingSite(instruction);
        
        steps.add("Navigate to " + site);
        steps.add("Wait for page to load and accept cookies if prompted");
        steps.add("Look for search form and fill in details: " + details);
        steps.add("Click search button and wait for results");
        
        if (maxSteps > 4) {
            steps.add("Analyze search results and look for best options");
            steps.add("Click on preferred option to view details");
        }
        
        if (maxSteps > 6) {
            steps.add("Review booking details and pricing");
            steps.add("Take screenshot of final selection for reference");
        }
        
        return steps;
    }
    
    private List<String> generateShoppingSteps(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        
        String product = extractProductDetails(instruction);
        String site = determineBestShoppingSite(instruction);
        
        steps.add("Navigate to " + site);
        steps.add("Use search function to look for: " + product);
        steps.add("Browse search results and compare options");
        
        if (maxSteps > 3) {
            steps.add("Click on promising product to view details");
            steps.add("Check product reviews and specifications");
        }
        
        if (maxSteps > 5) {
            steps.add("Note pricing and availability information");
            steps.add("Take screenshot of product details");
        }
        
        return steps;
    }
    
    private List<String> generateSearchSteps(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        
        String searchTerm = extractSearchTerm(instruction);
        String site = determineSearchSite(instruction);
        
        steps.add("Navigate to " + site);
        steps.add("Locate search box and enter: " + searchTerm);
        steps.add("Submit search and wait for results");
        
        if (maxSteps > 3) {
            steps.add("Review search results and identify relevant links");
            steps.add("Click on most relevant result");
        }
        
        if (maxSteps > 5) {
            steps.add("Read through content and extract key information");
            steps.add("Take screenshot of important information");
        }
        
        return steps;
    }
    
    private List<String> generateComparisonSteps(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        
        String compareItem = extractComparisonItem(instruction);
        
        steps.add("Navigate to comparison or review site");
        steps.add("Search for comparison information about: " + compareItem);
        steps.add("Review multiple sources and options");
        
        if (maxSteps > 3) {
            steps.add("Create side-by-side comparison of top options");
            steps.add("Note key differences and advantages");
        }
        
        if (maxSteps > 5) {
            steps.add("Check pricing from different sources");
            steps.add("Take screenshots of comparison results");
        }
        
        return steps;
    }
    
    private List<String> generateGenericSteps(String instruction, int maxSteps) {
        List<String> steps = new ArrayList<>();
        
        // Try to extract URL
        Matcher urlMatcher = URL_PATTERN.matcher(instruction);
        if (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            steps.add("Navigate to " + url);
            steps.add("Wait for page to load completely");
        } else {
            // Default to Google search
            steps.add("Navigate to google.com");
            steps.add("Search for information related to: " + instruction);
        }
        
        // Add action-specific steps
        if (CLICK_PATTERN.matcher(instruction).find()) {
            steps.add("Look for clickable element and click it");
        }
        
        if (FILL_PATTERN.matcher(instruction).find()) {
            steps.add("Fill out form fields as requested");
        }
        
        // Always end with screenshot
        steps.add("Take screenshot of final result");
        
        return steps;
    }
    
    private String enhanceStep(String rawStep) {
        // Add context and waiting instructions to basic steps
        if (rawStep.toLowerCase().startsWith("navigate") || rawStep.toLowerCase().startsWith("go to")) {
            return rawStep + " and wait for page to load";
        }
        
        if (rawStep.toLowerCase().contains("click")) {
            return rawStep + " and wait for any page changes";
        }
        
        if (rawStep.toLowerCase().contains("search")) {
            return rawStep + " and wait for search results";
        }
        
        if (rawStep.toLowerCase().contains("fill") || rawStep.toLowerCase().contains("enter")) {
            return rawStep + " and verify input was accepted";
        }
        
        return rawStep;
    }
    
    // Helper methods for extraction
    private String extractBookingDetails(String instruction) {
        // Extract travel details like "NYC to Paris"
        Pattern routePattern = Pattern.compile("(?i)(?:from\\s+)?([a-zA-Z\\s]+)\\s+to\\s+([a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = routePattern.matcher(instruction);
        if (matcher.find()) {
            return matcher.group(1).trim() + " to " + matcher.group(2).trim();
        }
        
        // Extract dates
        Pattern datePattern = Pattern.compile("(?i)(?:on\\s+|in\\s+)?([a-zA-Z]+\\s+\\d{1,2}(?:st|nd|rd|th)?|\\d{1,2}/\\d{1,2})", Pattern.CASE_INSENSITIVE);
        matcher = datePattern.matcher(instruction);
        if (matcher.find()) {
            return "date: " + matcher.group(1);
        }
        
        return "booking request";
    }
    
    private String extractProductDetails(String instruction) {
        Pattern productPattern = Pattern.compile("(?i)(?:buy|purchase|order)\\s+(?:a\\s+)?([^\\n\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = productPattern.matcher(instruction);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "product search";
    }
    
    private String extractSearchTerm(String instruction) {
        Matcher matcher = SEARCH_PATTERN.matcher(instruction);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Fallback: use the whole instruction
        return instruction.replaceAll("(?i)search\\s+(?:for\\s+)?", "").trim();
    }
    
    private String extractComparisonItem(String instruction) {
        Matcher matcher = COMPARISON_PATTERN.matcher(instruction);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "comparison item";
    }
    
    private String determineBestBookingSite(String instruction) {
        String lower = instruction.toLowerCase();
        if (lower.contains("flight")) return "https://www.booking.com";
        if (lower.contains("hotel")) return "https://www.booking.com";
        if (lower.contains("car")) return "https://www.rentalcars.com";
        return "https://www.expedia.com";
    }
    
    private String determineBestShoppingSite(String instruction) {
        return "https://www.amazon.com";
    }
    
    private String determineSearchSite(String instruction) {
        String lower = instruction.toLowerCase();
        if (lower.contains("linkedin")) return "https://www.linkedin.com";
        if (lower.contains("wikipedia")) return "https://www.wikipedia.org";
        return "https://www.google.com";
    }
}