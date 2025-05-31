package io.vishalmysore;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.t4a.JsonUtils;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;

import com.t4a.transform.PromptTransformer;
import com.microsoft.playwright.options.LoadState;

import java.nio.file.Paths;

public interface PlaywrightProcessor  {

    public default void processWebAction(String prompt) throws AIProcessingException {
        PlaywrightActions actions = (PlaywrightActions) getTransformer().transformIntoPojo(prompt, PlaywrightActions.class);
        String act = actions.getTypeOfActionToTakeOnWebDriver().toUpperCase();
        Browser browser = getBrowser();
        BrowserContext context = getContext();

        Page page;
        if (context.pages().isEmpty()) {
            page = context.newPage();
        } else {
            page = context.pages().get(0);
        }

        try {
            switch (act) {
                case "GET":
                case "NAVIGATE":
                    String url = getStringFromPrompt(prompt, "urlToClick");
                    if (url == null || url.isEmpty()) {
                        break;
                    } else {
                        URLSafety safe = (URLSafety) getTransformer().transformIntoPojo("Is this URL safe to navigate? "+url, URLSafety.class);
                        if (!safe.isItSafeAndValid()) {
                            throw new AIProcessingException("The URL is not safe to navigate: {" + url+ "}");
                        }
                        page.navigate(url);
                        page.waitForLoadState(LoadState.NETWORKIDLE);
                        break;
                    }

                case "CLICK":
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    String clickText = getStringFromPrompt(prompt, "textOfElementToClick");
                    page.getByText(clickText).click();
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    break;

                case "TYPETEXT":
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    String selectorToType = getStringFromPrompt(prompt, "selectorToTypeInto");
                    String textToType = getStringFromPrompt(prompt, "textToType");
                    page.locator(selectorToType).fill(textToType);
                    break;

                case "TAKESCREENSHOT":
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    String fileName = getStringFromPrompt(prompt, "fileNameToSaveScreenshot");
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Paths.get(fileName)));
                    break;

                case "WAITFOR":
                    String selectorToWait = getStringFromPrompt(prompt, "selectorToWaitFor");
                    page.waitForSelector(selectorToWait);
                    break;

                case "EXTRACTTEXT":
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    String selectorToExtract = getStringFromPrompt(prompt, "selectorToExtractTextFrom");
                    String extractedText = page.locator(selectorToExtract).textContent();
                    System.out.println("Extracted Text: " + extractedText);
                    break;

                case "SCROLLTO":
                    String scrollSelector = getStringFromPrompt(prompt, "selectorToScrollTo");
                    page.locator(scrollSelector).scrollIntoViewIfNeeded();
                    break;

                case "CLOSE":
                    page.close();
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            throw new AIProcessingException("Failed to perform action [" + act + "]: " + e.getMessage());
        }
    }


    public default String getStringFromPrompt(String prompt,  String key) throws AIProcessingException {
        String urlOfTheWebPage = getTransformer().transformIntoJson(getUtils().createJson(key).toString(), prompt);

        urlOfTheWebPage = getUtils().getFieldValue(urlOfTheWebPage,key);
        return urlOfTheWebPage;
    }

     boolean trueFalseQuery(String question) throws AIProcessingException ;


     Browser getBrowser() ;


     JsonUtils getUtils() ;


     PromptTransformer getTransformer() ;

    public BrowserContext getContext();

     AIProcessor getActionProcessor() ;
}
