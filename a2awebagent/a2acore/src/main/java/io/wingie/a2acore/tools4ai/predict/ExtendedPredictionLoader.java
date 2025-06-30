package io.wingie.a2acore.tools4ai.predict;

import io.wingie.a2acore.tools4ai.action.ExtendedPredictedAction;

import java.util.Map;

public interface ExtendedPredictionLoader {
    public Map<String, ExtendedPredictedAction>  getExtendedActions() throws LoaderException;
}
