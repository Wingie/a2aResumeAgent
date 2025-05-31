package io.vishalmysore;

import com.microsoft.playwright.*;
import com.t4a.JsonUtils;
import com.t4a.processor.AIProcessingException;
import com.t4a.processor.AIProcessor;
import com.t4a.processor.OpenAiActionProcessor;
import com.t4a.transform.OpenAIPromptTransformer;
import com.t4a.transform.PromptTransformer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import lombok.Setter;
import lombok.extern.java.Log;



@Log
@AllArgsConstructor
public class PlaywrightOpenAIProcessor extends OpenAiActionProcessor implements PlaywrightProcessor {
    Playwright playwright;
    Browser browser;
    @Setter
    private JsonUtils utils ;
    @Getter
    @Setter
    private OpenAIPromptTransformer transformer ;
    BrowserContext context;
    public PlaywrightOpenAIProcessor() {

            Playwright playwright = Playwright.create();
             browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             context = browser.newContext();
        this.utils = new JsonUtils();
        this.transformer = new OpenAIPromptTransformer();
    }
    public PlaywrightOpenAIProcessor(Playwright playwright) {
        this.playwright = playwright;
        this.utils = new JsonUtils();
        this.transformer = new OpenAIPromptTransformer();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
    }

    public PlaywrightOpenAIProcessor(Browser browser) {
        this.browser = browser;
        this.utils = new JsonUtils();
        this.transformer = new OpenAIPromptTransformer();
        context = browser.newContext();
    }
    @Override
    public boolean trueFalseQuery(String question) throws AIProcessingException {
        Page page = context.pages().get(0);
        String htmlSource = page.content();
        String str =  query(" this is your html { "+htmlSource+"} now answer this question in true or false only "+question);
        return Boolean.valueOf(str.trim());

    }

    @Override
    public Browser getBrowser() {
        return browser;
    }

    @Override
    public BrowserContext getContext() {
        return context;
    }

    @Override
    public JsonUtils getUtils() {
        return utils;
    }

    @Override
    public PromptTransformer getTransformer() {
        return transformer;
    }

    @Override
    public AIProcessor getActionProcessor() {
        return this;
    }
}
