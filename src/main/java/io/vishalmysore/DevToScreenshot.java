package io.vishalmysore;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.nio.file.Paths;

public class DevToScreenshot {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate("https://dev.to");

            // Wait until the network is idle
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Take screenshot
            String screenshotPath = "devto.png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));

            System.out.println("Screenshot saved to: " + screenshotPath);
        }
    }
}
