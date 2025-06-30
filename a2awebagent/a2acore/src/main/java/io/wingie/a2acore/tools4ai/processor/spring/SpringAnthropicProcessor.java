package io.wingie.a2acore.tools4ai.processor.spring;

import io.wingie.a2acore.tools4ai.predict.PredictionLoader;
import io.wingie.a2acore.tools4ai.processor.AnthropicActionProcessor;
import org.springframework.context.ApplicationContext;

public class SpringAnthropicProcessor extends AnthropicActionProcessor {
    public SpringAnthropicProcessor(ApplicationContext context)  {
        PredictionLoader.getInstance(context);

    }
}
