package io.wingie.service;

import io.wingie.entity.AgentDecisionStep;
import io.wingie.entity.LLMCallLog;
import io.wingie.entity.ToolDescription;
import io.wingie.repository.AgentDecisionStepRepository;
import io.wingie.repository.LLMCallLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AOP-based LLM call tracker for the agentic-harness system.
 * 
 * Automatically intercepts all LLM API calls and logs them to PostgreSQL with:
 * - Cache integration (links to ToolDescription system)
 * - Token usage and cost tracking
 * - Performance metrics
 * - Error handling and retry patterns
 * - Agent workflow correlation
 * 
 * This provides complete observability for agentic loops and cost optimization.
 */
@Aspect
@Component
@Slf4j
public class LLMCallTracker {
    
    @Autowired
    private LLMCallLogRepository llmCallLogRepository;
    
    @Autowired
    private AgentDecisionStepRepository agentDecisionStepRepository;
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @Value("${llm.tracking.enabled:true}")
    private boolean trackingEnabled;
    
    @Value("${llm.tracking.log-payloads:false}")
    private boolean logPayloads;
    
    // Token counting patterns for different providers
    private static final Pattern OPENAI_TOKEN_PATTERN = Pattern.compile("\"total_tokens\"\\s*:\\s*(\\d+)");
    private static final Pattern INPUT_TOKEN_PATTERN = Pattern.compile("\"prompt_tokens\"\\s*:\\s*(\\d+)");
    private static final Pattern OUTPUT_TOKEN_PATTERN = Pattern.compile("\"completion_tokens\"\\s*:\\s*(\\d+)");
    
    // Cost estimates per 1K tokens (USD) - can be moved to configuration
    private static final BigDecimal OPENAI_GPT4_INPUT_COST = new BigDecimal("0.0030");
    private static final BigDecimal OPENAI_GPT4_OUTPUT_COST = new BigDecimal("0.0060");
    private static final BigDecimal OPENAI_GPT35_INPUT_COST = new BigDecimal("0.0015");
    private static final BigDecimal OPENAI_GPT35_OUTPUT_COST = new BigDecimal("0.0020");
    private static final BigDecimal GEMINI_INPUT_COST = new BigDecimal("0.0010");
    private static final BigDecimal GEMINI_OUTPUT_COST = new BigDecimal("0.0020");
    private static final BigDecimal CLAUDE_INPUT_COST = new BigDecimal("0.0025");
    private static final BigDecimal CLAUDE_OUTPUT_COST = new BigDecimal("0.0125");
    
    /**
     * Intercept all LLM processor calls and track them
     */
    @Around("execution(* io.wingie.a2acore.tools4ai.processor.*Processor.query(..))")
    public Object trackLLMCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!trackingEnabled) {
            return joinPoint.proceed();
        }
        
        String provider = extractProviderFromClassName(joinPoint.getTarget().getClass().getSimpleName());
        String modelName = extractModelName(joinPoint.getArgs());
        String prompt = extractPrompt(joinPoint.getArgs());
        
        // Generate cache key for this call
        String cacheKey = generateCacheKey(provider, modelName, prompt);
        
        // Check if this would be a cache hit
        boolean cacheHit = checkCacheHit(cacheKey);
        
        LLMCallLog callLog = LLMCallLog.builder()
                .callId(UUID.randomUUID().toString())
                .cacheKey(cacheKey)
                .cacheHit(cacheHit)
                .provider(provider)
                .modelName(modelName)
                .toolName(extractToolName(prompt))
                .taskExecutionId(TaskContext.getCurrentTaskId())
                .sessionId(TaskContext.getCurrentSessionId())
                .userId(TaskContext.getCurrentUserId())
                .requestPayload(logPayloads ? prompt : null)
                .createdAt(LocalDateTime.now())
                .build();
        
        if (cacheHit) {
            // This is a cache hit - no actual LLM call will be made
            callLog.markCacheHit();
            logCacheHit(callLog);
            // Continue with the original call (will return cached result)
            Object result = joinPoint.proceed();
            return result;
        }
        
        // This is a cache miss - track the actual LLM call
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("🤖 Starting LLM call: {} - {} (cache key: {})", provider, modelName, cacheKey);
            
            Object result = joinPoint.proceed();
            
            // Call completed successfully
            long responseTime = System.currentTimeMillis() - startTime;
            callLog.setResponseTimeMs(responseTime);
            callLog.markCompleted();
            
            // Extract token usage and cost from response
            if (result != null) {
                extractTokenUsageAndCost(callLog, result.toString());
                if (logPayloads) {
                    callLog.setResponsePayload(result.toString());
                }
            }
            
            log.info("✅ LLM call completed: {} - {} in {}ms, {} tokens, ${}",
                    provider, modelName, responseTime, callLog.getTotalTokens(), callLog.getEstimatedCostUsd());
            
            // Save log asynchronously to avoid blocking
            saveLogAsync(callLog);
            
            // Create agent decision step if we have task context
            if (TaskContext.hasActiveTask()) {
                createAgentDecisionStepAsync(callLog, prompt, result);
            }
            
            return result;
            
        } catch (Exception e) {
            // Call failed
            long responseTime = System.currentTimeMillis() - startTime;
            callLog.setResponseTimeMs(responseTime);
            callLog.markFailed(e.getClass().getSimpleName(), e.getMessage());
            
            log.error("❌ LLM call failed: {} - {} after {}ms: {}",
                    provider, modelName, responseTime, e.getMessage());
            
            // Save error log asynchronously
            saveLogAsync(callLog);
            
            throw e; // Re-throw the exception
        }
    }
    
    /**
     * Check if this call would result in a cache hit
     */
    private boolean checkCacheHit(String cacheKey) {
        try {
            // Check if we have a cached response for this key
            String cached = cacheService.getCachedResponse(cacheKey);
            return cached != null;
        } catch (Exception e) {
            log.debug("Error checking cache for key {}: {}", cacheKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * Log cache hit event
     */
    private void logCacheHit(LLMCallLog callLog) {
        log.info("💾 Cache HIT: {} - {} (key: {})",
                callLog.getProvider(), callLog.getModelName(), callLog.getCacheKey());
        saveLogAsync(callLog);
    }
    
    /**
     * Extract provider name from processor class name
     */
    private String extractProviderFromClassName(String className) {
        if (className.contains("OpenAi")) return "openai";
        if (className.contains("Gemini")) return "gemini";
        if (className.contains("Anthropic")) return "anthropic";
        if (className.contains("Claude")) return "anthropic";
        return "unknown";
    }
    
    /**
     * Extract model name from method arguments or processor class
     */
    private String extractModelName(Object[] args) {
        // For now, return model based on provider - this could be enhanced
        // to extract actual model names from arguments if they contain model info
        String provider = extractProviderFromClassName(
            Thread.currentThread().getStackTrace()[3].getClassName()
        );
        
        return switch (provider) {
            case "openai" -> "gpt-4o-mini";
            case "gemini" -> "gemini-2.0-flash";
            case "anthropic" -> "claude-3-haiku";
            default -> "unknown-model";
        };
    }
    
    /**
     * Extract prompt text from method arguments
     */
    private String extractPrompt(Object[] args) {
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "";
    }
    
    /**
     * Extract tool name from prompt or current execution context
     */
    private String extractToolName(String prompt) {
        // Try to extract from stack trace to find the calling @Action method
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                // Look for tool classes and methods
                if (className.contains("Tool") || className.contains("Action")) {
                    return methodName;
                }
            }
        } catch (Exception e) {
            // Fallback to prompt analysis
        }
        
        // Fallback to prompt-based heuristics
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("meme") || lowerPrompt.contains("generate")) return "generateMeme";
        if (lowerPrompt.contains("linkedin") || lowerPrompt.contains("profile")) return "searchLinkedInProfile";
        if (lowerPrompt.contains("browse") || lowerPrompt.contains("web")) return "browseWebAndReturnText";
        if (lowerPrompt.contains("screenshot") || lowerPrompt.contains("image")) return "browseWebAndReturnImage";
        if (lowerPrompt.contains("taste") || lowerPrompt.contains("waste")) return "askTasteBeforeYouWaste";
        if (lowerPrompt.contains("wingston") || lowerPrompt.contains("resume")) return "getWingstonsProjectsExpertiseResume";
        
        return "ai-query";
    }
    
    /**
     * Generate cache key for LLM call
     */
    private String generateCacheKey(String provider, String modelName, String prompt) {
        // Create a hash-based cache key
        int promptHash = prompt.hashCode();
        return String.format("%s:%s:%08x", provider, modelName, promptHash);
    }
    
    
    /**
     * Extract token usage and calculate costs from LLM response
     */
    private void extractTokenUsageAndCost(LLMCallLog callLog, String response) {
        try {
            // Extract token counts from response JSON
            Matcher inputMatcher = INPUT_TOKEN_PATTERN.matcher(response);
            Matcher outputMatcher = OUTPUT_TOKEN_PATTERN.matcher(response);
            
            if (inputMatcher.find()) {
                callLog.setInputTokens(Integer.parseInt(inputMatcher.group(1)));
            }
            
            if (outputMatcher.find()) {
                callLog.setOutputTokens(Integer.parseInt(outputMatcher.group(1)));
            }
            
            // Calculate estimated cost
            if (callLog.getInputTokens() != null && callLog.getOutputTokens() != null) {
                BigDecimal cost = calculateCost(callLog.getProvider(), callLog.getModelName(),
                        callLog.getInputTokens(), callLog.getOutputTokens());
                callLog.setEstimatedCostUsd(cost);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting token usage from response: {}", e.getMessage());
        }
    }
    
    /**
     * Calculate estimated cost based on provider, model, and token usage
     */
    private BigDecimal calculateCost(String provider, String modelName, int inputTokens, int outputTokens) {
        BigDecimal inputCostPer1K;
        BigDecimal outputCostPer1K;
        
        switch (provider.toLowerCase()) {
            case "openai":
                if (modelName.contains("gpt-4")) {
                    inputCostPer1K = OPENAI_GPT4_INPUT_COST;
                    outputCostPer1K = OPENAI_GPT4_OUTPUT_COST;
                } else {
                    inputCostPer1K = OPENAI_GPT35_INPUT_COST;
                    outputCostPer1K = OPENAI_GPT35_OUTPUT_COST;
                }
                break;
            case "gemini":
                inputCostPer1K = GEMINI_INPUT_COST;
                outputCostPer1K = GEMINI_OUTPUT_COST;
                break;
            case "anthropic":
                inputCostPer1K = CLAUDE_INPUT_COST;
                outputCostPer1K = CLAUDE_OUTPUT_COST;
                break;
            default:
                inputCostPer1K = new BigDecimal("0.001"); // Default fallback
                outputCostPer1K = new BigDecimal("0.002");
        }
        
        BigDecimal inputCost = inputCostPer1K.multiply(new BigDecimal(inputTokens)).divide(new BigDecimal(1000));
        BigDecimal outputCost = outputCostPer1K.multiply(new BigDecimal(outputTokens)).divide(new BigDecimal(1000));
        
        return inputCost.add(outputCost);
    }
    
    /**
     * Save LLM call log asynchronously to avoid blocking main thread
     */
    @Async
    public void saveLogAsync(LLMCallLog callLog) {
        try {
            llmCallLogRepository.save(callLog);
            log.debug("💾 Saved LLM call log: {} ({})", callLog.getCallId(), callLog.getCacheKey());
        } catch (Exception e) {
            log.error("❌ Failed to save LLM call log: {}", e.getMessage());
        }
    }
    
    /**
     * Track cache hit for existing tool description usage
     */
    public void trackCacheHit(String toolName, String providerModel) {
        if (!trackingEnabled) {
            return;
        }
        
        LLMCallLog callLog = LLMCallLog.builder()
                .callId(UUID.randomUUID().toString())
                .cacheKey(providerModel + ":" + toolName)
                .cacheHit(true)
                .provider(extractProviderFromModel(providerModel))
                .modelName(providerModel)
                .toolName(toolName)
                .taskExecutionId(TaskContext.getCurrentTaskId())
                .sessionId(TaskContext.getCurrentSessionId())
                .userId(TaskContext.getCurrentUserId())
                .build();
        
        callLog.markCacheHit();
        saveLogAsync(callLog);
    }
    
    /**
     * Extract provider name from provider model string
     */
    private String extractProviderFromModel(String providerModel) {
        if (providerModel.contains("gpt") || providerModel.contains("openai")) return "openai";
        if (providerModel.contains("gemini")) return "gemini";
        if (providerModel.contains("claude")) return "anthropic";
        return "unknown";
    }
    
    /**
     * Create an AgentDecisionStep entry for this LLM call
     */
    @Async
    public void createAgentDecisionStepAsync(LLMCallLog llmCallLog, String prompt, Object result) {
        if (!trackingEnabled || !TaskContext.hasActiveTask()) {
            return;
        }
        
        try {
            String taskId = TaskContext.getCurrentTaskId();
            
            // Get the next step number for this task
            Integer stepNumber = getNextStepNumber(taskId);
            
            // Extract reasoning from the prompt and result
            String reasoning = extractReasoning(prompt, result);
            String toolSelected = llmCallLog.getToolName();
            
            // Create the agent decision step
            AgentDecisionStep decisionStep = AgentDecisionStep.builder()
                .taskExecutionId(taskId)
                .stepNumber(stepNumber)
                .toolSelected(toolSelected)
                .reasoningText(reasoning)
                .llmCallId(llmCallLog.getCallId())
                .tokensUsed(llmCallLog.getTotalTokens())
                .stepCostUsd(llmCallLog.getEstimatedCostUsd())
                .build();
            
            // Set confidence score based on response quality (simplified heuristic)
            decisionStep.setConfidenceScore(calculateConfidenceScore(result));
            
            // Set execution context
            decisionStep.setExecutionContextMap(Map.of(
                "provider", llmCallLog.getProvider(),
                "model", llmCallLog.getModelName(),
                "cacheHit", llmCallLog.getCacheHit(),
                "responseTime", llmCallLog.getResponseTimeMs() != null ? llmCallLog.getResponseTimeMs().toString() : "0"
            ));
            
            // Mark as started and completed (since LLM call is complete)
            decisionStep.markStarted();
            decisionStep.markCompleted(llmCallLog.isSuccessful());
            
            agentDecisionStepRepository.save(decisionStep);
            
            log.debug("🧠 Created agent decision step: {} for task {} (step {})", 
                decisionStep.getStepId(), taskId, stepNumber);
            
        } catch (Exception e) {
            log.error("❌ Failed to create agent decision step: {}", e.getMessage());
        }
    }
    
    /**
     * Get the next step number for a task
     */
    private Integer getNextStepNumber(String taskId) {
        try {
            return agentDecisionStepRepository.findMaxStepNumberByTaskId(taskId)
                .map(max -> max + 1)
                .orElse(1);
        } catch (Exception e) {
            log.warn("Failed to get next step number for task {}, using 1", taskId);
            return 1;
        }
    }
    
    /**
     * Extract reasoning from prompt and result
     */
    private String extractReasoning(String prompt, Object result) {
        // Simplified reasoning extraction - could be enhanced with NLP
        String reasoning = "AI decision based on prompt analysis";
        
        if (prompt.contains("meme")) {
            reasoning = "Decided to generate a meme based on user request";
        } else if (prompt.contains("linkedin")) {
            reasoning = "Decided to search LinkedIn profile based on query";
        } else if (prompt.contains("browse") || prompt.contains("web")) {
            reasoning = "Decided to browse web content based on user needs";
        } else if (prompt.contains("screenshot")) {
            reasoning = "Decided to capture screenshot for visual verification";
        }
        
        // Add result quality assessment
        if (result != null) {
            String resultStr = result.toString();
            if (resultStr.length() > 100) {
                reasoning += " - Comprehensive response generated";
            } else if (resultStr.contains("error") || resultStr.contains("failed")) {
                reasoning += " - Encountered issues during processing";
            } else {
                reasoning += " - Successful response generated";
            }
        }
        
        return reasoning;
    }
    
    /**
     * Calculate confidence score based on result quality
     */
    private java.math.BigDecimal calculateConfidenceScore(Object result) {
        if (result == null) {
            return new java.math.BigDecimal("0.3");
        }
        
        String resultStr = result.toString();
        
        // Higher confidence for longer, more detailed responses
        if (resultStr.length() > 500) {
            return new java.math.BigDecimal("0.9");
        } else if (resultStr.length() > 200) {
            return new java.math.BigDecimal("0.8");
        } else if (resultStr.length() > 50) {
            return new java.math.BigDecimal("0.7");
        } else {
            return new java.math.BigDecimal("0.5");
        }
    }
}