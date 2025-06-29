package io.wingie.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * Playwright Configuration for web automation
 * Provides enterprise-grade browser automation configuration using Microsoft Playwright
 */
@Configuration
@Slf4j
public class PlaywrightConfig {

    @Value("${app.playwright.headless:true}")
    private boolean headless;

    @Value("${app.playwright.timeout:30000}")
    private int timeout;

    @Value("${app.playwright.viewport.width:1920}")
    private int viewportWidth;

    @Value("${app.playwright.viewport.height:1080}")
    private int viewportHeight;

    @Bean
    @Primary
    public Playwright playwright() {
        log.info("Creating Playwright instance");
        return Playwright.create();
    }

    @Bean
    @Lazy
    public Browser playwrightBrowser(Playwright playwright) {
        log.info("Creating Playwright browser instance (headless: {})", headless);
        
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setTimeout(timeout);

        // Add Docker-specific arguments for container compatibility
        if (isRunningInDocker()) {
            log.info("Detected Docker environment, adding container-specific arguments");
            launchOptions.setArgs(java.util.List.of(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--disable-web-security",
                    "--disable-extensions"
            ));
        }

        Browser browser = playwright.chromium().launch(launchOptions);
        log.info("Playwright browser created successfully");
        return browser;
    }

    @Bean
    @Lazy
    public BrowserContext playwrightContext(Browser browser) {
        log.info("Creating Playwright browser context");
        
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setIgnoreHTTPSErrors(true);

        BrowserContext context = browser.newContext(contextOptions);
        log.info("Playwright browser context created successfully");
        return context;
    }

    /**
     * Detect if we're running in a Docker container
     */
    private boolean isRunningInDocker() {
        try {
            return java.nio.file.Files.exists(java.nio.file.Paths.get("/.dockerenv")) ||
                   System.getenv("DOCKER_CONTAINER") != null;
        } catch (Exception e) {
            log.debug("Could not detect Docker environment", e);
            return false;
        }
    }
}