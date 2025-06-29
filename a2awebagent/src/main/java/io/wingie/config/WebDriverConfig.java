package io.wingie.config;

import io.wingie.CustomChromeOptions;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for WebDriver initialization with Docker support.
 * Handles both local (Chrome) and containerized (Chromium) environments.
 * Uses CGLIB proxies to ensure proper interface casting.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class WebDriverConfig {

    @Value("${webdriver.chrome.driver.path:}")
    private String chromeDriverPath;

    @Value("${webdriver.chrome.binary.path:}")
    private String chromeBinaryPath;

    @Value("${webdriver.headless:false}")
    private boolean headless;

    @Value("${webdriver.retry.attempts:3}")
    private int retryAttempts;

    @Value("${webdriver.retry.delay:2000}")
    private long retryDelay;

    /**
     * Creates a WebDriver bean with proper error handling and Docker support.
     * Bean initialization is immediate to ensure browser system fails fast if Chrome/Chromium is not available.
     * This ensures that 'mvn clean package test' will fail immediately in Docker if browser dependencies are missing.
     * Uses @Primary to ensure this bean is used when multiple WebDriver beans exist.
     */
    @Bean
    @Primary
    @Lazy
    public WebDriver webDriver() {
        log.info("Initializing WebDriver with headless={}, retryAttempts={}", headless, retryAttempts);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.info("WebDriver initialization attempt {}/{}", attempt, retryAttempts);
                
                // Configure Chrome options using standardized CustomChromeOptions
                ChromeOptions options = CustomChromeOptions.createOptions();
                
                // Set ChromeDriver path if specified
                if (chromeDriverPath != null && !chromeDriverPath.isEmpty()) {
                    System.setProperty("webdriver.chrome.driver", chromeDriverPath);
                    log.info("Using ChromeDriver path: {}", chromeDriverPath);
                }
                
                // Create and test the driver
                ChromeDriver driver = new ChromeDriver(options);
                
                // Quick test to ensure driver is working
                driver.getTitle(); // This will throw if driver is not properly initialized
                
                log.info("WebDriver initialized successfully on attempt {}", attempt);
                return driver;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("WebDriver initialization failed on attempt {}/{}: {}", 
                    attempt, retryAttempts, e.getMessage());
                
                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All attempts failed
        boolean isDocker = isRunningInDocker();
        String errorMsg = String.format(
            "Failed to initialize WebDriver after %d attempts. " +
            "Browser automation system is not functional. " +
            (isDocker ? 
                "DOCKER ENVIRONMENT: Install browser dependencies with 'RUN apk add --no-cache chromium chromium-driver' (Alpine) or 'RUN apt-get update && apt-get install -y chromium-browser chromium-driver' (Debian/Ubuntu). " :
                "LOCAL ENVIRONMENT: Install Chrome or Chromium browser. ") +
            "This will cause 'mvn clean package test' to fail as intended.",
            retryAttempts
        );
        log.error(errorMsg, lastException);
        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * Creates ChromeOptions with Docker-aware configuration.
     */
    private ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        
        // Detect if running in Docker
        boolean isDocker = isRunningInDocker();
        log.info("Running in Docker: {}", isDocker);
        
        if (isDocker || headless) {
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            log.info("Added Docker/headless Chrome arguments");
        }
        
        // Common options
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        
        // Set binary path if specified or detect Chromium in Docker
        String binaryPath = detectChromeBinaryPath();
        if (binaryPath != null) {
            options.setBinary(binaryPath);
            log.info("Using Chrome binary: {}", binaryPath);
        }
        
        return options;
    }

    /**
     * Detects the Chrome/Chromium binary path based on environment.
     */
    private String detectChromeBinaryPath() {
        // If explicitly configured, use that
        if (chromeBinaryPath != null && !chromeBinaryPath.isEmpty()) {
            return chromeBinaryPath;
        }
        
        // Common paths to check
        String[] possiblePaths = {
            "/usr/bin/chromium",           // Docker Debian/Alpine
            "/usr/bin/chromium-browser",   // Alternative name
            "/usr/bin/google-chrome",      // Google Chrome
            "/opt/google/chrome/chrome",   // Google Chrome alternative
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", // macOS
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",   // Windows
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe" // Windows 32-bit
        };
        
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                log.info("Found Chrome/Chromium at: {}", path);
                return path;
            }
        }
        
        log.warn("Chrome/Chromium binary not found in common locations");
        return null;
    }

    /**
     * Detects if the application is running inside a Docker container.
     */
    private boolean isRunningInDocker() {
        // Check for .dockerenv file
        if (new File("/.dockerenv").exists()) {
            return true;
        }
        
        // Check cgroup for docker
        try {
            String cgroup = Files.readString(Path.of("/proc/1/cgroup"));
            if (cgroup.contains("docker") || cgroup.contains("containerd")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore, not in container or can't read file
        }
        
        // Check for common Docker environment variables
        return System.getenv("DOCKER_CONTAINER") != null ||
               System.getenv("KUBERNETES_SERVICE_HOST") != null;
    }
}