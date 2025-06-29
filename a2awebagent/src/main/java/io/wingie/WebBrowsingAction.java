package io.wingie;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import io.wingie.playwright.PlaywrightWebBrowsingAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Web browsing action service using Playwright for all web automation
 * Provides @Action methods for AI integration with comprehensive web automation capabilities
 */
@Service
@Slf4j
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching using Playwright")
public class WebBrowsingAction {
    
    @Autowired
    private PlaywrightWebBrowsingAction playwrightWebBrowsingAction;

    @Action(description = "perform actions on the web with Playwright and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) throws IOException {
        log.info("Executing web browsing with Playwright - returning text content");
        return playwrightWebBrowsingAction.browseWebAndReturnText(webBrowsingSteps);
    }

    @Action(description = "perform actions on the web with Playwright and return image")
    public String browseWebAndReturnImage(String webBrowsingSteps) throws IOException {
        log.info("Executing web browsing with Playwright - returning screenshot");
        return playwrightWebBrowsingAction.browseWebAndReturnImage(webBrowsingSteps);
    }

    @Action(description = "take a screenshot of the current page using Playwright")
    public String takeCurrentPageScreenshot() throws IOException {
        log.info("Taking current page screenshot using Playwright");
        return playwrightWebBrowsingAction.browseWebAndReturnImage("Take a screenshot of the current page");
    }
}