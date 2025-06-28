package io.wingie;

import org.openqa.selenium.chrome.ChromeOptions;

public class CustomChromeOptions {
    public static ChromeOptions createOptions() {
        ChromeOptions options = new ChromeOptions();
        boolean useCommandLineOptions = Boolean.getBoolean("driverOptions");

        if (useCommandLineOptions) {
            String commandLineArgs = System.getProperty("chrome.options", "");
            if (!commandLineArgs.isEmpty()) {
                String[] args = commandLineArgs.split(",");
                options.addArguments(args);
                System.out.println(commandLineArgs);
            }
        } else {
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-background-timer-throttling");
            options.addArguments("--disable-renderer-backgrounding");
            options.addArguments("--disable-backgrounding-occluded-windows");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--start-maximized");
            options.addArguments("--force-device-scale-factor=1");
            // Enable better rendering for screenshots
            options.addArguments("--enable-features=VizDisplayCompositor");
            options.addArguments("--run-all-compositor-stages-before-draw");
        }

        return options;
    }
}