package io.wingie.a2acore.tools4ai.action;

import io.wingie.a2acore.tools4ai.annotations.Action;
import io.wingie.a2acore.tools4ai.api.JavaMethodAction;

/**
 * This Action can be used when you want the AI to get back with answer and if you do not have any
 * specific action to be performed. For example
 * "Hey can you provide weather for tornto and get back with places to see?"
 *
 * In this case the first action is WeatherAction with method called getTemperature and the second action
 * could be blank action as you want it to answer the question.
 *
 */

public final class BlankAction implements JavaMethodAction {
    @Action
    public String askAdditionalQuestion(String additionalQuestion) {
        if (additionalQuestion == null) {
            return null;
        }
        if (additionalQuestion.isEmpty()) {
            return "";
        }
        return "provide answer for this query : " + additionalQuestion;
    }
}
