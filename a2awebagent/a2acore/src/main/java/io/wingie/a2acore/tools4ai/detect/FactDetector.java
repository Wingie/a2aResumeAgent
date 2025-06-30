package io.wingie.a2acore.tools4ai.detect;

import io.wingie.a2acore.tools4ai.api.ActionType;
import io.wingie.a2acore.tools4ai.api.DetectorAction;
import io.wingie.a2acore.tools4ai.api.GuardRailException;

public class FactDetector implements DetectorAction {
    @Override
    public ActionType getActionType() {
        return ActionType.FACT;
    }

    @Override
    public String getDescription() {
        return "Fact Check in response";
    }

    @Override
    public DetectValueRes execute(DetectValues dd)  throws GuardRailException {
        return null;
    }
}
