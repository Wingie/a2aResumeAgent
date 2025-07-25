package io.wingie.a2acore.tools4ai.processor;

import io.wingie.a2acore.tools4ai.api.AIAction;
import io.wingie.a2acore.tools4ai.detect.ActionCallback;
import io.wingie.a2acore.tools4ai.detect.ExplainDecision;
import io.wingie.a2acore.tools4ai.detect.HumanInLoop;

public class LocalAIActionProcessor implements AIProcessor {
    @Override
    public Object processSingleAction(String promptText, HumanInLoop humanVerification, ExplainDecision explain) {
        return null;
    }

    @Override
    public String query(String promptText) throws AIProcessingException {
        return null;
    }

    @Override
    public Object processSingleAction(String prompt, AIAction action, HumanInLoop humanVerification, ExplainDecision explain, ActionCallback callback) throws AIProcessingException {
        return null;
    }

    @Override
    public Object processSingleAction(String promptText, AIAction action, HumanInLoop humanVerification, ExplainDecision explain) throws AIProcessingException {
        return null;
    }
    public Object processSingleAction(String promptText)  throws AIProcessingException {
        return processSingleAction(promptText, null, new LoggingHumanDecision(), new LogginggExplainDecision());
    }

    @Override
    public Object processSingleAction(String promptText, ActionCallback callback) throws AIProcessingException {
        return null;
    }
}
