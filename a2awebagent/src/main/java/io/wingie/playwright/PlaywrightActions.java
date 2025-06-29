package io.wingie.playwright;

import com.t4a.annotations.Prompt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model for Playwright actions with AI prompt annotations
 * Based on the a2aPlaywrightReference implementation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaywrightActions {

    @Prompt(describe = "from the given text what is the type of action to take on web browser? " +
            "like GET, NAVIGATE, CLICK, TYPETEXT, TAKESCREENSHOT, WAITFOR, EXTRACTTEXT, SCROLLTO, CLOSE")
    private String typeOfActionToTakeOnWebDriver;

    @Prompt(describe = "from the given text what is the url to click or navigate to?")
    private String urlToClick;

    @Prompt(describe = "from the given text what is the text of the element to click?")
    private String textOfElementToClick;

    @Prompt(describe = "from the given text what is the selector to type into?")
    private String selectorToTypeInto;

    @Prompt(describe = "from the given text what is the text to type?")
    private String textToType;

    @Prompt(describe = "from the given text what is the file name to save screenshot?")
    private String fileNameToSaveScreenshot;

    @Prompt(describe = "from the given text what is the selector to wait for?")
    private String selectorToWaitFor;

    @Prompt(describe = "from the given text what is the selector to extract text from?")
    private String selectorToExtractTextFrom;

    @Prompt(describe = "from the given text what is the selector to scroll to?")
    private String selectorToScrollTo;
}