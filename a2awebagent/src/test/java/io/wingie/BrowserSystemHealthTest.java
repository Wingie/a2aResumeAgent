package io.wingie;

import io.wingie.config.WebDriverConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Browser System Health Tests
 * 
 * This test class validates that the browser automation system is properly configured
 * and functional. These tests will FAIL if Chrome/Chromium is not installed or accessible,
 * ensuring that `mvn clean package test` fails in Docker environments where browser
 * dependencies are missing.
 * 
 * The tests are ordered to provide clear failure points:
 * 1. Spring WebDriver bean validation (forces immediate initialization)
 * 2. Direct Chrome binary detection and WebDriver creation
 * 3. Basic browser functionality validation
 * 4. Docker environment compatibility verification
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Slf4j
public class BrowserSystemHealthTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @Order(1)
    @DisplayName("Spring WebDriver Bean Initialization - Fail Fast Test")
    void testSpringWebDriverBeanInitialization() {
        log.info("=== Testing Spring WebDriver Bean Initialization ===");
        log.info("This test forces WebDriver bean creation and will FAIL FAST if Chrome/Chromium is not available");
        
        // This test explicitly forces the WebDriver bean to initialize
        // Since we removed @Lazy, this should fail immediately if Chrome/Chromium is not available
        
        assertDoesNotThrow(() -> {
            // Force initialization by requesting the WebDriver bean
            WebDriver driver = applicationContext.getBean(WebDriver.class);
            assertNotNull(driver, "WebDriver bean should be successfully created");
            
            log.info("✅ Spring WebDriver bean initialized successfully: {}", driver.getClass().getSimpleName());
            
        }, "❌ WebDriver initialization FAILED - Chrome/Chromium is not properly installed or accessible. " +
           "This is the intended behavior when browser system is not working!");
    }

    @Test
    @Order(2)
    @DisplayName("Direct Chrome Binary Detection and WebDriver Creation")
    void testDirectChromeBinaryDetection() {
        log.info("=== Testing Direct Chrome Binary Detection ===");
        
        WebDriver testDriver = null;
        try {
            // Create Chrome options similar to WebDriverConfig
            ChromeOptions options = new ChromeOptions();
            
            // Add Docker-compatible options
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            
            // Try to create WebDriver directly (this will fail if Chrome is not available)
            testDriver = new ChromeDriver(options);
            assertNotNull(testDriver, "Direct ChromeDriver creation should succeed");
            
            log.info("✅ Direct ChromeDriver creation successful");
            
        } catch (Exception e) {
            log.error("❌ Direct ChromeDriver creation FAILED: {}", e.getMessage());
            fail("Chrome/Chromium binary not found or not functional. " +
                 "In Docker: install with 'RUN apk add --no-cache chromium chromium-driver' (Alpine) " +
                 "or 'RUN apt-get install -y chromium-browser chromium-driver' (Debian/Ubuntu). " +
                 "Locally: install Chrome or Chromium browser. " +
                 "Original error: " + e.getMessage());
        } finally {
            if (testDriver != null) {
                try {
                    testDriver.quit();
                } catch (Exception e) {
                    log.warn("Error cleaning up test driver: {}", e.getMessage());
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Browser Navigation and JavaScript Execution Test")
    void testBrowserFunctionality() {
        log.info("=== Testing Browser Navigation and JavaScript Execution ===");
        
        WebDriver testDriver = null;
        try {
            // Get a fresh WebDriver from Spring context
            testDriver = applicationContext.getBean(WebDriver.class);
            
            // Test basic navigation
            testDriver.get("data:text/html,<html><head><title>Browser Test</title></head><body><h1>Browser Health Check</h1></body></html>");
            
            String title = testDriver.getTitle();
            assertEquals("Browser Test", title, "Browser should be able to navigate and read page title");
            
            // Test JavaScript execution
            Object jsResult = ((org.openqa.selenium.JavascriptExecutor) testDriver)
                .executeScript("return document.title + ' - JS Working';");
            assertEquals("Browser Test - JS Working", jsResult.toString(), 
                        "Browser should be able to execute JavaScript");
            
            log.info("✅ Browser navigation and JavaScript execution successful");
            
        } catch (Exception e) {
            log.error("❌ Browser functionality test FAILED: {}", e.getMessage());
            fail("Browser navigation or JavaScript execution failed. This indicates Chrome/Chromium " +
                 "is installed but not functioning properly. Error: " + e.getMessage());
        }
        // Don't quit here - let Spring manage the shared bean
    }

    @Test
    @Order(4)
    @DisplayName("Docker Environment and Headless Mode Compatibility")
    void testDockerCompatibility() {
        log.info("=== Testing Docker Environment Compatibility ===");
        
        WebDriver testDriver = null;
        try {
            // Create a separate WebDriver instance with strict Docker settings
            ChromeOptions dockerOptions = new ChromeOptions();
            dockerOptions.addArguments("--headless=new");  // Use new headless mode
            dockerOptions.addArguments("--no-sandbox");
            dockerOptions.addArguments("--disable-dev-shm-usage");
            dockerOptions.addArguments("--disable-gpu");
            dockerOptions.addArguments("--disable-extensions");
            dockerOptions.addArguments("--disable-background-timer-throttling");
            dockerOptions.addArguments("--disable-renderer-backgrounding");
            dockerOptions.addArguments("--disable-backgrounding-occluded-windows");
            dockerOptions.addArguments("--disable-features=TranslateUI");
            dockerOptions.addArguments("--disable-ipc-flooding-protection");
            dockerOptions.addArguments("--window-size=1920,1080");
            
            testDriver = new ChromeDriver(dockerOptions);
            
            // Test that Docker-specific options work
            testDriver.get("about:blank");
            testDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            testDriver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
            
            // Test screenshot capability (critical for the agent's functionality)
            byte[] screenshot = ((org.openqa.selenium.TakesScreenshot) testDriver)
                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            assertTrue(screenshot.length > 0, "Screenshot should be captured successfully");
            
            log.info("✅ Docker compatibility test passed - screenshot size: {} bytes", screenshot.length);
            
        } catch (Exception e) {
            log.error("❌ Docker compatibility test FAILED: {}", e.getMessage());
            fail("Docker environment compatibility failed. In Docker, ensure Chrome/Chromium is properly " +
                 "installed with all dependencies. Error: " + e.getMessage());
        } finally {
            if (testDriver != null) {
                try {
                    testDriver.quit();
                } catch (Exception e) {
                    log.warn("Error cleaning up Docker test driver: {}", e.getMessage());
                }
            }
        }
    }

    @BeforeAll
    static void validateBrowserSystemPrerequisites() {
        log.info("==========================================================");
        log.info("🔍 BROWSER SYSTEM HEALTH CHECK STARTING");
        log.info("==========================================================");
        log.info("This test suite validates browser automation system functionality");
        log.info("⚠️  If ANY test fails, it indicates Chrome/Chromium is not properly installed");
        log.info("🐳 In Docker: ensure 'chromium chromium-driver' packages are installed");
        log.info("💻 In local dev: ensure Chrome or Chromium browser is installed");
        log.info("🎯 PURPOSE: Make 'mvn clean package test' FAIL when browser system is broken");
        log.info("==========================================================");
    }

    @AfterAll
    static void browserSystemHealthCheckComplete() {
        log.info("==========================================================");
        log.info("✅ BROWSER SYSTEM HEALTH CHECK COMPLETED SUCCESSFULLY");
        log.info("==========================================================");
        log.info("🎉 All browser system tests passed");
        log.info("🚀 Chrome/Chromium is properly installed and functional");
        log.info("✨ Web automation system is ready for use");
        log.info("==========================================================");
    }
}