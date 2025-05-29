package io.vishalmysore;

import com.t4a.annotations.Prompt;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;



@Setter
@Getter
@NoArgsConstructor
@ToString
public class PlaywrightActions {

    @Prompt(describe = "What action should be performed using Playwright page. Choose one of: " +
            "{navigate, click, type, fill, check, uncheck, screenshot, getText, getAttribute, waitForSelector}")
    private String typeOfActionToTakeOnWebDriver;
}
