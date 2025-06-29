package io.wingie;

import com.t4a.annotations.Action;
import com.t4a.annotations.Agent;
import io.wingie.playwright.PlaywrightWebBrowsingAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Web browsing action service - now delegates to Playwright implementation
 * Maintains compatibility with existing @Action annotations while using Playwright under the hood
 */
@Service
@Slf4j
@Agent(groupName = "web browsing", groupDescription = "actions related to web browsing and searching using Playwright")
public class WebBrowsingAction {
    
    @Autowired
    private PlaywrightWebBrowsingAction playwrightWebBrowsingAction;

    @Action(description = "perform actions on the web with Playwright and return text")
    public String browseWebAndReturnText(String webBrowsingSteps) throws IOException {
        log.info("Delegating web browsing to Playwright implementation");
        return playwrightWebBrowsingAction.browseWebAndReturnText(webBrowsingSteps);
    }

    @Action(description = "perform actions on the web with Playwright and return image")
    public String browseWebAndReturnImage(String webBrowsingSteps) throws IOException {
        log.info("Delegating web browsing with image capture to Playwright implementation");
        return playwrightWebBrowsingAction.browseWebAndReturnImage(webBrowsingSteps);
    }

    /**
     * Legacy compatibility method
     * @deprecated Use browseWebAndReturnText instead
     */
    @Deprecated
    @Action(description = "take a screenshot of the current page using Playwright")
    public String takeCurrentPageScreenshot() throws IOException {
        log.info("Taking current page screenshot using Playwright");
        // Use a simple navigation to current page and take screenshot
        return playwrightWebBrowsingAction.browseWebAndReturnImage("Take a screenshot of the current page");
    }
}