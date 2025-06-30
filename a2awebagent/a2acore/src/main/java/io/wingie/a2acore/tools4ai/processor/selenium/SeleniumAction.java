package io.wingie.a2acore.tools4ai.processor.selenium;

import io.wingie.a2acore.tools4ai.annotations.Action;
import io.wingie.a2acore.tools4ai.annotations.Agent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
@Setter
@Getter
@Agent(groupName = "Selenium", groupDescription = "Selenium actions")
public class SeleniumAction  {

    @Action(description = "Perform action on web page")
    public DriverActions webPageAction(DriverActions webDriverActions) {
        if (webDriverActions == null) {
            return null;
        }
        DriverActions copy = new DriverActions();
        copy.setTypeOfActionToTakeOnWebDriver(webDriverActions.getTypeOfActionToTakeOnWebDriver());
        return copy;
    }
}
