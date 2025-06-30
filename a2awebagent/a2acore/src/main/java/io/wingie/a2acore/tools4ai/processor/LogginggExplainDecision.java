package io.wingie.a2acore.tools4ai.processor;

import io.wingie.a2acore.tools4ai.detect.ExplainDecision;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogginggExplainDecision implements ExplainDecision {
    @Override
    public String explain(String promptText, String methodName, String reason) {
       log.debug("promptText {} , reason {} ",promptText, reason);
       return reason;
    }
}
