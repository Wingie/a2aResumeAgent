package io.wingie.a2acore.tools4ai.detect;

import io.wingie.a2acore.tools4ai.api.ActionType;
import io.wingie.a2acore.tools4ai.api.DetectorAction;
import io.wingie.a2acore.tools4ai.api.GuardRailException;

/**
 * Detect Bias in response using Zero-shot classification
 * To detect bias, look for unequal treatment in outputs, analyze the data source,
 * and challenge the AI's assumptions with follow-up questions.
 */
public class BiasDetector implements DetectorAction {
    @Override
    public ActionType getActionType() {
        return ActionType.BIAS;
    }

    @Override
    public String getDescription() {
        return "Detect Bias in response";
    }

    @Override
    public DetectValueRes execute(DetectValues dd) throws GuardRailException {
        return null;
    }
}
