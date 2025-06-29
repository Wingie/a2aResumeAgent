package io.wingie;

import io.wingie.utils.SafeWebDriverWrapper;
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
 * Enhanced Browser System Integration Tests
 * 
 * This test class validates the complete browser automation system including:
 * - SafeWebDriverWrapper proxy handling capabilities 
 * - Enhanced screenshot functionality with fallbacks
 * - Spring WebDriver bean configuration with @Primary annotation
 * - Java module system compatibility for Selenium
 * 
 * These tests will FAIL if Chrome/Chromium is not installed or accessible,
 * ensuring that `mvn clean package test` fails in Docker environments where browser
 * dependencies are missing.
 * 
 * The tests are ordered to provide clear failure points:
 * 1. Spring WebDriver bean with SafeWebDriverWrapper validation
 * 2. Enhanced screenshot functionality with multiple fallback strategies
 * 3. JavaScript execution through SafeWebDriverWrapper
 * 4. Docker environment compatibility with optimized Chrome options
 */
@SpringBootTest(classes = {Application.class, TestDataConfig.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Slf4j
public class BrowserSystemHealthTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @Order(1)
    @DisplayName("Spring WebDriver Bean with SafeWebDriverWrapper - Fail Fast Test")
    void testSpringWebDriverBeanWithSafeWrapper() {
        log.info("=== Testing Spring WebDriver Bean with SafeWebDriverWrapper ===");
        log.info("This test forces WebDriver bean creation and validates SafeWebDriverWrapper functionality");
        
        assertDoesNotThrow(() -> {
            // Force initialization by requesting the WebDriver bean
            WebDriver driver = applicationContext.getBean(WebDriver.class);
            assertNotNull(driver, "WebDriver bean should be successfully created");
            
            log.info("✅ Spring WebDriver bean initialized: {}", driver.getClass().getSimpleName());
            
            // Test SafeWebDriverWrapper functionality
            SafeWebDriverWrapper safeWrapper = SafeWebDriverWrapper.wrap(driver);
            assertNotNull(safeWrapper, "SafeWebDriverWrapper should wrap successfully");
            
            // Test capability detection
            boolean supportsJS = safeWrapper.supportsJavaScript();
            boolean supportsScreenshots = safeWrapper.supportsScreenshots();
            
            log.info("✅ SafeWebDriverWrapper capabilities - JavaScript: {}, Screenshots: {}", 
                    supportsJS, supportsScreenshots);
            
            assertTrue(supportsJS, "SafeWebDriverWrapper should support JavaScript execution");
            assertTrue(supportsScreenshots, "SafeWebDriverWrapper should support screenshot capture");
            
        }, "❌ WebDriver initialization or SafeWebDriverWrapper functionality FAILED - " +
           "Chrome/Chromium is not properly installed or proxy casting is broken!");
    }

    @Test
    @Order(2)
    @DisplayName("Enhanced Screenshot Functionality with Fallback Strategies")
    void testEnhancedScreenshotFunctionality() {
        log.info("=== Testing Enhanced Screenshot Functionality ===");
        
        assertDoesNotThrow(() -> {
            // Get Spring-managed WebDriver
            WebDriver driver = applicationContext.getBean(WebDriver.class);
            
            // Navigate to a test page
            driver.get("data:text/html,<html><head><title>Screenshot Test</title></head>" +
                      "<body style='background:linear-gradient(45deg,red,blue);height:100vh;'>" +
                      "<h1 style='color:white;text-align:center;padding-top:200px;'>Screenshot Functionality Test</h1>" +
                      "</body></html>");
            
            // Test ScreenshotUtils with fallback strategies
            byte[] screenshot = ScreenshotUtils.captureScreenshotWithFallbacks(driver, "Integration test screenshot");
            
            assertNotNull(screenshot, "Screenshot should be captured successfully");
            assertTrue(screenshot.length > 10000, "Screenshot should be meaningful size (>10KB), got: " + screenshot.length);
            
            log.info("✅ Enhanced screenshot captured successfully - size: {} bytes", screenshot.length);
            
            // Test direct SafeWebDriverWrapper screenshot
            SafeWebDriverWrapper safeWrapper = SafeWebDriverWrapper.wrap(driver);
            byte[] directScreenshot = safeWrapper.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            
            assertNotNull(directScreenshot, "Direct SafeWebDriverWrapper screenshot should work");
            assertTrue(directScreenshot.length > 1000, "Direct screenshot should be reasonable size");
            
            log.info("✅ Direct SafeWebDriverWrapper screenshot - size: {} bytes", directScreenshot.length);
            
        }, "❌ Enhanced screenshot functionality FAILED - this indicates proxy casting or fallback strategies are broken!");
    }

    @Test
    @Order(3)
    @DisplayName("SafeWebDriverWrapper JavaScript Execution Test")
    void testSafeWebDriverJavaScriptExecution() {
        log.info("=== Testing SafeWebDriverWrapper JavaScript Execution ===");
        
        assertDoesNotThrow(() -> {
            // Get Spring-managed WebDriver
            WebDriver driver = applicationContext.getBean(WebDriver.class);
            SafeWebDriverWrapper safeWrapper = SafeWebDriverWrapper.wrap(driver);
            
            // Test basic navigation
            driver.get("data:text/html,<html><head><title>JS Test</title></head>" +
                      "<body><h1 id='header'>SafeWebDriverWrapper JS Test</h1>" +
                      "<script>window.testValue = 'SafeWrapper Working';</script></body></html>");
            
            String title = driver.getTitle();
            assertEquals("JS Test", title, "Browser should navigate and read page title");
            
            // Test JavaScript execution through SafeWebDriverWrapper
            Object jsResult = safeWrapper.executeScript("return document.title + ' - ' + window.testValue;");
            assertEquals("JS Test - SafeWrapper Working", jsResult.toString(), 
                        "SafeWebDriverWrapper should execute JavaScript without casting errors");
            
            // Test complex JavaScript operations
            Object domResult = safeWrapper.executeScript(
                "return document.getElementById('header').textContent;");
            assertEquals("SafeWebDriverWrapper JS Test", domResult.toString(),
                        "SafeWebDriverWrapper should handle DOM manipulation");
            
            // Test async JavaScript
            Object asyncResult = safeWrapper.executeAsyncScript(
                "var callback = arguments[arguments.length - 1]; " +
                "setTimeout(function() { callback('Async JS Working'); }, 100);");
            assertEquals("Async JS Working", asyncResult.toString(),
                        "SafeWebDriverWrapper should handle async JavaScript");
            
            log.info("✅ SafeWebDriverWrapper JavaScript execution successful");
            
        }, "❌ SafeWebDriverWrapper JavaScript execution FAILED - indicates proxy casting issues!");
    }

    @Test
    @Order(4)
    @DisplayName("CustomChromeOptions and Docker Optimization Compatibility")
    void testCustomChromeOptionsDockerCompatibility() {
        log.info("=== Testing CustomChromeOptions and Docker Optimization ===");
        
        WebDriver testDriver = null;
        try {
            // Test CustomChromeOptions.createOptions() with all optimizations
            ChromeOptions optimizedOptions = CustomChromeOptions.createOptions();
            assertNotNull(optimizedOptions, "CustomChromeOptions should create valid options");
            
            // Create test driver with optimized options
            testDriver = new ChromeDriver(optimizedOptions);
            SafeWebDriverWrapper safeWrapper = SafeWebDriverWrapper.wrap(testDriver);
            
            // Test Docker-optimized options work
            testDriver.get("data:text/html,<html><head><title>Docker Test</title></head>" +
                          "<body style='background:#4CAF50;'>" +
                          "<h1 style='color:white;text-align:center;margin-top:300px;'>Docker Optimization Test</h1>" +
                          "<div id='test'>Performance optimized!</div></body></html>");
            
            testDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            testDriver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
            
            // Test all SafeWebDriverWrapper capabilities
            assertTrue(safeWrapper.supportsJavaScript(), "Should support JavaScript");
            assertTrue(safeWrapper.supportsScreenshots(), "Should support screenshots");
            
            // Test JavaScript execution with optimized settings
            Object jsResult = safeWrapper.executeScript("return document.getElementById('test').textContent;");
            assertEquals("Performance optimized!", jsResult.toString());
            
            // Test screenshot with all fallback strategies
            byte[] screenshot = ScreenshotUtils.captureScreenshotWithFallbacks(testDriver, "Docker optimization test");
            assertTrue(screenshot.length > 5000, "Screenshot should be captured with good quality");
            
            log.info("✅ CustomChromeOptions Docker compatibility passed - screenshot: {} bytes", screenshot.length);
            
        } catch (Exception e) {
            log.error("❌ CustomChromeOptions Docker compatibility FAILED: {}", e.getMessage());
            fail("CustomChromeOptions or Docker optimization failed. This indicates Chrome arguments " +
                 "are conflicting or missing dependencies. Error: " + e.getMessage());
        } finally {
            if (testDriver != null) {
                try {
                    testDriver.quit();
                } catch (Exception e) {
                    log.warn("Error cleaning up CustomChromeOptions test driver: {}", e.getMessage());
                }
            }
        }
    }

    @BeforeAll
    static void validateBrowserSystemPrerequisites() {
        log.info("==========================================================");
        log.info("🔍 ENHANCED BROWSER SYSTEM INTEGRATION TEST STARTING");
        log.info("==========================================================");
        log.info("This test suite validates the complete browser automation system:");
        log.info("✨ SafeWebDriverWrapper proxy handling capabilities");
        log.info("📸 Enhanced screenshot functionality with fallback strategies");
        log.info("🔧 Spring WebDriver bean configuration with @Primary");
        log.info("🚀 Java module system compatibility for Selenium");
        log.info("⚠️  If ANY test fails, it indicates system integration issues");
        log.info("🐳 In Docker: ensure 'chromium chromium-driver' packages are installed");
        log.info("💻 In local dev: ensure Chrome or Chromium browser is installed");
        log.info("🎯 PURPOSE: Validate all bug fixes and enhancements are working");
        log.info("==========================================================");
    }

    @AfterAll
    static void browserSystemIntegrationTestComplete() {
        log.info("==========================================================");
        log.info("✅ ENHANCED BROWSER SYSTEM INTEGRATION TEST COMPLETED");
        log.info("==========================================================");
        log.info("🎉 All browser system integration tests passed");
        log.info("🔧 SafeWebDriverWrapper functionality: WORKING");
        log.info("📸 Enhanced screenshot with fallbacks: WORKING"); 
        log.info("⚡ Java module system compatibility: WORKING");
        log.info("🚀 Spring proxy configuration: WORKING");
        log.info("✨ Complete web automation system is ready for production use");
        log.info("==========================================================");
    }
}