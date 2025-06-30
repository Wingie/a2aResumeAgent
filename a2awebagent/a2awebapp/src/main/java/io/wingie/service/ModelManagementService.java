package io.wingie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * Service to manage AI model selection and switching
 * Provides runtime model switching capabilities for the admin dashboard
 */
@Service
@Slf4j
public class ModelManagementService {

    @Autowired
    private ToolDescriptionCacheService cacheService;

    @Value("${task.processor.provider:openrouter}")
    private String currentProvider;

    @Value("${task.processor.modelName:google/gemma-3n-e4b-it:free}")
    private String currentModelName;

    private Properties tools4aiProperties;
    private final String TOOLS4AI_PROPERTIES_PATH = "tools4ai.properties";

    // Predefined model configurations from tools4ai.properties
    private final Map<String, ModelConfig> availableModels = new LinkedHashMap<>();

    public ModelManagementService() {
        initializeAvailableModels();
        loadCurrentProperties();
    }

    /**
     * Initialize available models from the evaluation configuration
     */
    private void initializeAvailableModels() {
        // OpenRouter models (free tier)
        availableModels.put("gemma-3b-free", new ModelConfig(
            "gemma-3b-free", 
            "google/gemma-3n-e4b-it:free", 
            "openrouter", 
            "Google Gemma 3B - Free tier model",
            "Free",
            true
        ));
        
        availableModels.put("deepseek-r1-free", new ModelConfig(
            "deepseek-r1-free", 
            "deepseek/deepseek-r1-0528:free", 
            "openrouter", 
            "DeepSeek R1 - Advanced reasoning model (Free)",
            "Free",
            true
        ));

        // OpenAI models
        availableModels.put("gpt-4o-mini", new ModelConfig(
            "gpt-4o-mini", 
            "gpt-4o-mini", 
            "openai", 
            "OpenAI GPT-4 Optimized Mini - Fast and efficient",
            "Paid",
            false
        ));

        // Anthropic models
        availableModels.put("claude-3-haiku", new ModelConfig(
            "claude-3-haiku", 
            "claude-3-haiku-20240307", 
            "anthropic", 
            "Anthropic Claude 3 Haiku - Fast and lightweight",
            "Paid",
            false
        ));

        // Mistral models
        availableModels.put("mistral-large", new ModelConfig(
            "mistral-large", 
            "mistral-large-latest", 
            "mistral", 
            "Mistral Large - Advanced reasoning",
            "Paid",
            false
        ));

        // Google models
        availableModels.put("gemini-2-flash", new ModelConfig(
            "gemini-2-flash", 
            "gemini-2.0-flash-001", 
            "google", 
            "Google Gemini 2.0 Flash - Fast multimodal model",
            "Paid",
            false
        ));
    }

    /**
     * Load current properties file
     */
    private void loadCurrentProperties() {
        tools4aiProperties = new Properties();
        try {
            Resource resource = new ClassPathResource(TOOLS4AI_PROPERTIES_PATH);
            try (InputStream is = resource.getInputStream()) {
                tools4aiProperties.load(is);
                log.debug("Loaded tools4ai.properties with {} properties", tools4aiProperties.size());
            }
        } catch (IOException e) {
            log.warn("Could not load tools4ai.properties file: {}", e.getMessage());
            tools4aiProperties = new Properties(); // Use empty properties as fallback
        }
    }

    /**
     * Get all available models
     */
    public List<ModelConfig> getAvailableModels() {
        return new ArrayList<>(availableModels.values());
    }

    /**
     * Get current model configuration
     */
    public ModelConfig getCurrentModel() {
        // Find current model by matching modelName
        return availableModels.values().stream()
            .filter(model -> model.getModelName().equals(currentModelName))
            .findFirst()
            .orElse(new ModelConfig("current", currentModelName, currentProvider, "Current Model", "Unknown", false));
    }

    /**
     * Switch to a new model (runtime only - requires restart for persistence)
     */
    public ModelSwitchResult switchModel(String modelKey) {
        log.info("Attempting to switch to model: {}", modelKey);
        
        ModelConfig newModel = availableModels.get(modelKey);
        if (newModel == null) {
            return new ModelSwitchResult(false, "Model not found: " + modelKey, null, null);
        }

        try {
            // Update runtime values (these will be used until next restart)
            String oldModel = currentModelName;
            String oldProvider = currentProvider;
            
            this.currentModelName = newModel.getModelName();
            this.currentProvider = newModel.getProvider();
            
            log.info("Successfully switched from {}:{} to {}:{}", 
                oldProvider, oldModel, currentProvider, currentModelName);
            
            return new ModelSwitchResult(true, 
                "Model switched successfully (runtime only - restart required for persistence)", 
                oldModel, newModel.getModelName());
            
        } catch (Exception e) {
            log.error("Failed to switch model to {}: {}", modelKey, e.getMessage());
            return new ModelSwitchResult(false, "Failed to switch model: " + e.getMessage(), null, null);
        }
    }


    /**
     * Clear cache for old model (optional cleanup)
     */
    public void clearOldModelCache(String oldModelName) {
        if (oldModelName != null && !oldModelName.equals(currentModelName)) {
            try {
                cacheService.clearCacheByModel(oldModelName);
                log.info("Cleared cache for old model: {}", oldModelName);
            } catch (Exception e) {
                log.warn("Failed to clear cache for old model {}: {}", oldModelName, e.getMessage());
            }
        }
    }

    /**
     * Get model statistics and usage information
     */
    public ModelStatistics getModelStatistics() {
        ModelStatistics stats = new ModelStatistics();
        stats.setCurrentModel(getCurrentModel());
        stats.setTotalAvailableModels(availableModels.size());
        stats.setFreeModelsCount((int) availableModels.values().stream().filter(ModelConfig::isFree).count());
        stats.setPaidModelsCount((int) availableModels.values().stream().filter(m -> !m.isFree()).count());
        
        // Get cache statistics for current model
        try {
            List<Object[]> providerStats = cacheService.getProviderStatistics();
            for (Object[] stat : providerStats) {
                String provider = (String) stat[0];
                if (provider.equals(currentModelName)) {
                    stats.setCachedDescriptionsCount(((Long) stat[1]).intValue());
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Could not get cache statistics: {}", e.getMessage());
        }
        
        return stats;
    }

    /**
     * Validate if a model is properly configured
     */
    public ModelValidationResult validateModel(String modelKey) {
        ModelConfig model = availableModels.get(modelKey);
        if (model == null) {
            return new ModelValidationResult(false, "Model not found", new ArrayList<>());
        }

        List<String> issues = new ArrayList<>();
        
        // Check API key availability based on provider
        switch (model.getProvider()) {
            case "openai":
                if (isPropertyEmpty("openAiKey")) {
                    issues.add("OpenAI API key not configured");
                }
                break;
            case "anthropic":
                if (isPropertyEmpty("claudeKey")) {
                    issues.add("Claude API key not configured");
                }
                break;
            case "mistral":
                if (isPropertyEmpty("mistralKey")) {
                    issues.add("Mistral API key not configured");
                }
                break;
            case "openrouter":
                if (isPropertyEmpty("openrouterKey") && isPropertyEmpty("openAiKey")) {
                    issues.add("OpenRouter API key not configured");
                }
                break;
        }

        return new ModelValidationResult(issues.isEmpty(), 
            issues.isEmpty() ? "Model is properly configured" : "Configuration issues found", 
            issues);
    }

    private boolean isPropertyEmpty(String key) {
        String value = tools4aiProperties.getProperty(key);
        return value == null || value.trim().isEmpty() || value.equals("YOUR_API_KEY_HERE");
    }

    // Inner classes for data transfer
    public static class ModelConfig {
        private String key;
        private String modelName;
        private String provider;
        private String description;
        private String tier;
        private boolean free;

        public ModelConfig(String key, String modelName, String provider, String description, String tier, boolean free) {
            this.key = key;
            this.modelName = modelName;
            this.provider = provider;
            this.description = description;
            this.tier = tier;
            this.free = free;
        }

        // Getters
        public String getKey() { return key; }
        public String getModelName() { return modelName; }
        public String getProvider() { return provider; }
        public String getDescription() { return description; }
        public String getTier() { return tier; }
        public boolean isFree() { return free; }
    }

    public static class ModelSwitchResult {
        private boolean success;
        private String message;
        private String oldModel;
        private String newModel;

        public ModelSwitchResult(boolean success, String message, String oldModel, String newModel) {
            this.success = success;
            this.message = message;
            this.oldModel = oldModel;
            this.newModel = newModel;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getOldModel() { return oldModel; }
        public String getNewModel() { return newModel; }
    }

    public static class ModelStatistics {
        private ModelConfig currentModel;
        private int totalAvailableModels;
        private int freeModelsCount;
        private int paidModelsCount;
        private int cachedDescriptionsCount;

        // Getters and setters
        public ModelConfig getCurrentModel() { return currentModel; }
        public void setCurrentModel(ModelConfig currentModel) { this.currentModel = currentModel; }
        public int getTotalAvailableModels() { return totalAvailableModels; }
        public void setTotalAvailableModels(int totalAvailableModels) { this.totalAvailableModels = totalAvailableModels; }
        public int getFreeModelsCount() { return freeModelsCount; }
        public void setFreeModelsCount(int freeModelsCount) { this.freeModelsCount = freeModelsCount; }
        public int getPaidModelsCount() { return paidModelsCount; }
        public void setPaidModelsCount(int paidModelsCount) { this.paidModelsCount = paidModelsCount; }
        public int getCachedDescriptionsCount() { return cachedDescriptionsCount; }
        public void setCachedDescriptionsCount(int cachedDescriptionsCount) { this.cachedDescriptionsCount = cachedDescriptionsCount; }
    }

    public static class ModelValidationResult {
        private boolean valid;
        private String message;
        private List<String> issues;

        public ModelValidationResult(boolean valid, String message, List<String> issues) {
            this.valid = valid;
            this.message = message;
            this.issues = issues;
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getIssues() { return issues; }
    }
}