package io.wingie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * Service for mapping emotional moods to appropriate meme templates.
 * Uses the memegen-templates.json file to create intelligent mood-based template suggestions.
 */
@Service
@Slf4j
public class MoodTemplateMapper {

    private List<MemeTemplate> templates;
    private Map<String, List<String>> moodToTemplates;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        loadTemplatesFromJson();
        createMoodMappings();
        log.info("MoodTemplateMapper initialized with {} templates and {} mood categories", 
                templates.size(), moodToTemplates.size());
    }

    /**
     * Gets the best template suggestions for a given mood/emotion.
     */
    public List<String> getTemplatesForMood(String mood) {
        if (mood == null || mood.trim().isEmpty()) {
            return getPopularTemplates();
        }
        
        String normalizedMood = mood.toLowerCase().trim();
        
        // Direct mood mapping
        List<String> directMatch = moodToTemplates.get(normalizedMood);
        if (directMatch != null && !directMatch.isEmpty()) {
            log.debug("Found direct mood mapping for '{}': {}", mood, directMatch);
            return new ArrayList<>(directMatch);
        }
        
        // Fuzzy matching for similar moods
        List<String> fuzzyMatch = findFuzzyMoodMatch(normalizedMood);
        if (!fuzzyMatch.isEmpty()) {
            log.debug("Found fuzzy mood mapping for '{}': {}", mood, fuzzyMatch);
            return fuzzyMatch;
        }
        
        log.debug("No mood mapping found for '{}', returning popular templates", mood);
        return getPopularTemplates();
    }

    /**
     * Gets a single random template for a given mood.
     */
    public String getRandomTemplateForMood(String mood) {
        List<String> templates = getTemplatesForMood(mood);
        if (templates.isEmpty()) {
            return "drake"; // Safe fallback
        }
        return templates.get(new Random().nextInt(templates.size()));
    }

    /**
     * Gets all available moods/emotions.
     */
    public Set<String> getAvailableMoods() {
        return moodToTemplates.keySet();
    }

    /**
     * Gets popular/default templates when no mood is specified.
     */
    public List<String> getPopularTemplates() {
        return Arrays.asList("drake", "db", "woman-cat", "fine", "fry", "pigeon", "success");
    }

    /**
     * Gets a description of what mood categories are available.
     */
    public String getMoodGuide() {
        return """
            Available mood categories for meme generation:
            
            **Positive Emotions:**
            - happy, excited, successful, proud, confident → success, stonks, pooh
            
            **Negative Emotions:**  
            - sad, frustrated, unlucky, annoyed → blb, fwp, woman-cat
            
            **Sarcastic/Ironic:**
            - sarcastic, ironic, mocking, dismissive → kermit, spongebob, rollsafe
            
            **Confused/Uncertain:**
            - confused, uncertain, suspicious, questioning → fry, pigeon, philosoraptor
            
            **Preferences/Choices:**
            - comparing, choosing, preferring, upgrading → drake, pooh, db
            
            **Plans/Progression:**
            - planning, evolving, step-by-step, progression → gru, gb
            
            **Acceptance/Denial:**
            - accepting, denying, coping, resigned → fine, bear
            
            **Clever/Smart:**
            - clever, smart, thinking, philosophical → rollsafe, philosoraptor
            """;
    }

    private void loadTemplatesFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("memegen-templates.json");
            templates = objectMapper.readValue(resource.getInputStream(), 
                    new TypeReference<List<MemeTemplate>>() {});
            log.debug("Loaded {} meme templates from JSON", templates.size());
        } catch (IOException e) {
            log.error("Failed to load memegen templates from JSON", e);
            templates = Collections.emptyList();
        }
    }

    private void createMoodMappings() {
        moodToTemplates = new HashMap<>();
        
        // Positive emotions
        addMoodMapping(Arrays.asList("happy", "excited", "successful", "proud", "confident", "winning", "achieving"), 
                      Arrays.asList("success", "stonks", "pooh"));
        
        // Negative emotions
        addMoodMapping(Arrays.asList("sad", "frustrated", "unlucky", "annoyed", "angry", "upset", "disappointed"), 
                      Arrays.asList("blb", "fwp", "woman-cat", "yuno"));
        
        // Sarcastic/Ironic
        addMoodMapping(Arrays.asList("sarcastic", "ironic", "mocking", "dismissive", "petty", "sassy"), 
                      Arrays.asList("kermit", "spongebob", "rollsafe"));
        
        // Confused/Uncertain  
        addMoodMapping(Arrays.asList("confused", "uncertain", "suspicious", "questioning", "doubtful", "skeptical"), 
                      Arrays.asList("fry", "pigeon", "philosoraptor"));
        
        // Preferences/Choices
        addMoodMapping(Arrays.asList("comparing", "choosing", "preferring", "upgrading", "deciding", "tempted"), 
                      Arrays.asList("drake", "pooh", "db"));
        
        // Plans/Progression
        addMoodMapping(Arrays.asList("planning", "evolving", "progression", "step-by-step", "developing", "growing"), 
                      Arrays.asList("gru", "gb"));
        
        // Acceptance/Denial
        addMoodMapping(Arrays.asList("accepting", "denying", "coping", "resigned", "fine", "whatever", "dealing"), 
                      Arrays.asList("fine", "bear"));
        
        // Clever/Smart
        addMoodMapping(Arrays.asList("clever", "smart", "thinking", "philosophical", "wise", "insightful"), 
                      Arrays.asList("rollsafe", "philosoraptor"));
        
        // Difficulty/Challenge
        addMoodMapping(Arrays.asList("difficult", "challenging", "hard", "impossible", "tough"), 
                      Arrays.asList("mordor"));
        
        // Everywhere/Abundance
        addMoodMapping(Arrays.asList("everywhere", "abundant", "all-around", "pervasive"), 
                      Arrays.asList("buzz"));
        
        // Aliens/Conspiracy
        addMoodMapping(Arrays.asList("aliens", "conspiracy", "mysterious", "unexplained"), 
                      Arrays.asList("aag"));
        
        // Obsessive/Clingy
        addMoodMapping(Arrays.asList("obsessive", "clingy", "attached", "possessive"), 
                      Arrays.asList("oag"));
        
        // Much wow/Excitement (Doge style)
        addMoodMapping(Arrays.asList("wow", "such", "much", "very", "doge-style"), 
                      Arrays.asList("doge"));
    }

    private void addMoodMapping(List<String> moods, List<String> templates) {
        for (String mood : moods) {
            moodToTemplates.put(mood.toLowerCase(), new ArrayList<>(templates));
        }
    }

    private List<String> findFuzzyMoodMatch(String mood) {
        // Check if the mood contains any of our mapped keywords
        for (Map.Entry<String, List<String>> entry : moodToTemplates.entrySet()) {
            String mappedMood = entry.getKey();
            if (mood.contains(mappedMood) || mappedMood.contains(mood)) {
                return new ArrayList<>(entry.getValue());
            }
        }
        
        // Check for emotional keywords in the mood description
        if (mood.contains("good") || mood.contains("great") || mood.contains("positive")) {
            return moodToTemplates.get("happy");
        }
        if (mood.contains("bad") || mood.contains("negative") || mood.contains("down")) {
            return moodToTemplates.get("sad");
        }
        if (mood.contains("unsure") || mood.contains("maybe") || mood.contains("hmm")) {
            return moodToTemplates.get("confused");
        }
        
        return Collections.emptyList();
    }

    /**
     * Inner class representing a meme template from the JSON file.
     */
    public static class MemeTemplate {
        public String id;
        public String name;
        public int lines;
        public String blank;
        public Map<String, Object> example;
        public String source;
        public List<String> keywords;
        
        // Getters for JSON deserialization
        public String getId() { return id; }
        public String getName() { return name; }
        public int getLines() { return lines; }
        public String getBlank() { return blank; }
        public Map<String, Object> getExample() { return example; }
        public String getSource() { return source; }
        public List<String> getKeywords() { return keywords; }
    }
}