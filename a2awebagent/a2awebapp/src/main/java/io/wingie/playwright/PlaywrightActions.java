package io.wingie.playwright;

// Prompt annotations removed - using static data model
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

    private String typeOfActionToTakeOnBrowser;
    private String urlToClick;
    private String textOfElementToClick;
    private String selectorToTypeInto;
    private String textToType;
    private String fileNameToSaveScreenshot;
    private String selectorToWaitFor;
    private String selectorToExtractTextFrom;
    private String selectorToScrollTo;
}