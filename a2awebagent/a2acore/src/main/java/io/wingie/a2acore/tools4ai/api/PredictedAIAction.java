package io.wingie.a2acore.tools4ai.api;

/**
 * Just a marker interface to denote that the implementing classes will be predicted by default.
 * They will be added to the prediction list
 */
public interface PredictedAIAction extends AIAction{
    @Override
    public default String getActionParameters() {
        return "";
    }
}
