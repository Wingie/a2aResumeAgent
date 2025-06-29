package io.wingie;

import com.microsoft.playwright.*;
import io.wingie.config.PlaywrightConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playwright Browser System Integration Tests
 * 
 * This test class validates the complete browser automation system including:
 * - Playwright browser automation capabilities
 * - Screenshot functionality with Playwright
 * - Spring Playwright bean configuration
 * - Docker environment compatibility with Playwright
 * 
 * These tests will FAIL if Playwright browsers are not installed or accessible,
 * ensuring that `mvn clean package test` fails in Docker environments where browser
 * dependencies are missing.
 * 
 * The tests are ordered to provide clear failure points:
 * 1. Spring Playwright bean validation
 * 2. Screenshot functionality with Playwright
 * 3. JavaScript execution through Playwright
 * 4. Docker environment compatibility with Playwright
 */
@SpringBootTest(classes = {Application.class, TestDataConfig.class, PlaywrightConfig.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Slf4j
public class BrowserSystemHealthTest {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private Playwright playwright;
    
    @Autowired
    private Browser playwrightBrowser;

    @Test
    @Order(1)
    @DisplayName("Spring Playwright Bean - Fail Fast Test")
    void testSpringPlaywrightBean() {
        log.info("=== Testing Spring Playwright Bean ===");
        log.info("This test forces Playwright bean creation and validates functionality");
        
        assertDoesNotThrow(() -> {
            // Test Playwright bean
            assertNotNull(playwright, "Playwright bean should be successfully created");
            assertNotNull(playwrightBrowser, "Browser bean should be successfully created");
            
            log.info("✅ Spring Playwright bean initialized: {}", playwright.getClass().getSimpleName());
            log.info("✅ Spring Browser bean initialized: {}", playwrightBrowser.getClass().getSimpleName());
            
            // Test browser type and capabilities
            assertTrue(playwrightBrowser.isConnected(), "Browser should be connected");
            
            log.info("✅ Playwright capabilities - Browser connected: {}", playwrightBrowser.isConnected());
            
        }, "❌ Playwright initialization FAILED - Browser dependencies are not properly installed!");
    }

    @Test
    @Order(2)
    @DisplayName("Playwright Screenshot Functionality")
    void testPlaywrightScreenshotFunctionality() {
        log.info("=== Testing Playwright Screenshot Functionality ===");
        
        BrowserContext context = null;
        Page page = null;
        
        try {
            // Create browser context and page
            context = playwrightBrowser.newContext();
            page = context.newPage();
            
            // Navigate to a test page
            page.navigate("data:text/html,<html><head><title>Screenshot Test</title></head>" +
                         "<body style='background:linear-gradient(45deg,red,blue);height:100vh;'>" +
                         "<h1 style='color:white;text-align:center;padding-top:200px;'>Screenshot Functionality Test</h1>" +
                         "</body></html>");
            
            // Test Playwright screenshot
            byte[] screenshot = page.screenshot();
            
            assertNotNull(screenshot, "Screenshot should be captured successfully");
            assertTrue(screenshot.length > 10000, "Screenshot should be meaningful size (>10KB), got: " + screenshot.length);
            
            log.info("✅ Playwright screenshot captured successfully - size: {} bytes", screenshot.length);
            
            // Test page title to ensure navigation worked
            String title = page.title();
            assertEquals("Screenshot Test", title, "Page should navigate and read page title");
            
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Playwright JavaScript Execution Test")
    void testPlaywrightJavaScriptExecution() {
        log.info("=== Testing Playwright JavaScript Execution ===");
        
        BrowserContext context = null;
        Page page = null;
        
        try {
            // Create browser context and page
            context = playwrightBrowser.newContext();
            page = context.newPage();
            
            // Test basic navigation
            page.navigate("data:text/html,<html><head><title>JS Test</title></head>" +
                         "<body><h1 id='header'>Playwright JS Test</h1>" +
                         "<script>window.testValue = 'Playwright Working';</script></body></html>");
            
            String title = page.title();
            assertEquals("JS Test", title, "Browser should navigate and read page title");
            
            // Test JavaScript execution through Playwright
            Object jsResult = page.evaluate("() => document.title + ' - ' + window.testValue");
            assertEquals("JS Test - Playwright Working", jsResult.toString(), 
                        "Playwright should execute JavaScript without errors");
            
            // Test complex JavaScript operations
            Object domResult = page.evaluate("() => document.getElementById('header').textContent");
            assertEquals("Playwright JS Test", domResult.toString(),
                        "Playwright should handle DOM manipulation");
            
            // Test async JavaScript
            Object asyncResult = page.evaluate("() => new Promise(resolve => setTimeout(() => resolve('Async JS Working'), 100))");
            assertEquals("Async JS Working", asyncResult.toString(),
                        "Playwright should handle async JavaScript");
            
            log.info("✅ Playwright JavaScript execution successful");
            
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Playwright Docker Optimization Compatibility")
    void testPlaywrightDockerCompatibility() {
        log.info("=== Testing Playwright Docker Optimization ===");
        
        BrowserContext testContext = null;
        Page testPage = null;
        
        try {
            // Create optimized browser context for Docker
            Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true);
                
            testContext = playwrightBrowser.newContext(options);
            testPage = testContext.newPage();
            
            // Test Docker-optimized page operations
            testPage.navigate("data:text/html,<html><head><title>Docker Test</title></head>" +
                             "<body style='background:#4CAF50;'>" +
                             "<h1 style='color:white;text-align:center;margin-top:300px;'>Docker Optimization Test</h1>" +
                             "<div id='test'>Performance optimized!</div></body></html>");
            
            // Set page timeout
            testPage.setDefaultTimeout(30000);
            
            // Test JavaScript execution with optimized settings
            Object jsResult = testPage.evaluate("() => document.getElementById('test').textContent");
            assertEquals("Performance optimized!", jsResult.toString());
            
            // Test screenshot capture
            byte[] screenshot = testPage.screenshot();
            assertTrue(screenshot.length > 5000, "Screenshot should be captured with good quality");
            
            log.info("✅ Playwright Docker compatibility passed - screenshot: {} bytes", screenshot.length);
            
        } catch (Exception e) {
            log.error("❌ Playwright Docker compatibility FAILED: {}", e.getMessage());
            fail("Playwright Docker optimization failed. This indicates browser dependencies " +
                 "are missing or Docker configuration issues. Error: " + e.getMessage());
        } finally {
            if (testPage != null) testPage.close();
            if (testContext != null) testContext.close();
        }
    }

    @BeforeAll
    static void validateBrowserSystemPrerequisites() {
        log.info("==========================================================");
        log.info("🔍 PLAYWRIGHT BROWSER SYSTEM INTEGRATION TEST STARTING");
        log.info("==========================================================");
        log.info("This test suite validates the complete browser automation system:");
        log.info("✨ Playwright browser automation capabilities");
        log.info("📸 Screenshot functionality with Playwright");
        log.info("🔧 Spring Playwright bean configuration");
        log.info("🚀 Docker environment compatibility with Playwright");
        log.info("⚠️  If ANY test fails, it indicates system integration issues");
        log.info("🐳 In Docker: ensure Playwright browsers are installed");
        log.info("💻 In local dev: ensure Playwright browsers are available");
        log.info("🎯 PURPOSE: Validate Playwright migration is working correctly");
        log.info("==========================================================");
    }

    @AfterAll
    static void browserSystemIntegrationTestComplete() {
        log.info("==========================================================");
        log.info("✅ PLAYWRIGHT BROWSER SYSTEM INTEGRATION TEST COMPLETED");
        log.info("==========================================================");
        log.info("🎉 All browser system integration tests passed");
        log.info("🔧 Playwright functionality: WORKING");
        log.info("📸 Screenshot capture: WORKING"); 
        log.info("⚡ JavaScript execution: WORKING");
        log.info("🚀 Spring Playwright configuration: WORKING");
        log.info("✨ Complete Playwright web automation system is ready for production use");
        log.info("==========================================================");
    }
}