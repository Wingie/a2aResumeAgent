package io.wingie.a2acore.tools4ai.api;

import io.wingie.a2acore.tools4ai.detect.DetectValues;
import io.wingie.a2acore.tools4ai.detect.DetectValueRes;
import io.wingie.a2acore.tools4ai.processor.AIProcessingException;

/**
 * Base class for AI Hallucination and Bias detection
 */
public interface DetectorAction extends AIAction{
    @Override
    default String getActionName() {
        return "execute";
    }

    public DetectValueRes execute(DetectValues dd) throws GuardRailException, AIProcessingException;
}
