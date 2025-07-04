package io.wingie.a2acore.tools4ai.processor.spring;

import io.wingie.a2acore.tools4ai.predict.PredictionLoader;
import io.wingie.a2acore.tools4ai.processor.OpenAiActionProcessor;
import org.springframework.context.ApplicationContext;
/**
 * This will ensure that the action classes are loaded from Spring Applicaiton Context rather than
 * creating the new one , the advantage of that is we can maintain spring dependency injection for all the beans
 * Uses OpenAI for processing
 */
public class SpringOpenAIProcessor extends OpenAiActionProcessor {
    public SpringOpenAIProcessor(ApplicationContext context) {
        PredictionLoader.getInstance(context);
    }
}
