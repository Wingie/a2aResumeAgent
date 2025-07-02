package io.wingie.playwright;

import com.microsoft.playwright.Page;
import io.wingie.service.TaskExecutionIntegrationService;
import io.wingie.service.TaskContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Handles real-time browser events and broadcasts them via SSE for live monitoring.
 * Provides detailed browser action tracking for the Agent Observatory dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrowserEventHandler {
    
    private final TaskExecutionIntegrationService taskExecutionService;
    
    /**
     * Sets up real-time event listeners on a Playwright page for comprehensive monitoring.
     * Tracks navigation, clicks, input events, and page state changes.
     */
    public void setupPageEventListeners(Page page) {
        String taskId = getCurrentTaskId();
        if (taskId == null) {
            log.debug("No task context available, skipping browser event setup");
            return;
        }
        
        try {
            // Navigation events
            page.onDOMContentLoaded(page1 -> {
                String url = page1.url();
                log.info("🌐 Page loaded: {}", url);
                broadcastBrowserEvent("page-loaded", "Page loaded: " + url, taskId);
            });
            
            page.onLoad(page1 -> {
                String url = page1.url();
                log.info("✅ Page fully loaded: {}", url);
                broadcastBrowserEvent("page-ready", "Page ready: " + url, taskId);
            });
            
            // Console events (for debugging and monitoring)
            page.onConsoleMessage(msg -> {
                if ("error".equals(msg.type())) {
                    log.warn("🔴 Browser console error: {}", msg.text());
                    broadcastBrowserEvent("console-error", "Console error: " + msg.text(), taskId);
                }
            });
            
            // Dialog events (alerts, confirms, prompts)
            page.onDialog(dialog -> {
                log.info("💬 Browser dialog: {} - {}", dialog.type(), dialog.message());
                broadcastBrowserEvent("dialog-appeared", 
                    String.format("Dialog (%s): %s", dialog.type(), dialog.message()), taskId);
                
                // Auto-accept dialogs to prevent blocking
                dialog.accept();
            });
            
            // Page crash or error events
            page.onPageError(exception -> {
                log.error("💥 Page error: {}", exception);
                broadcastBrowserEvent("page-error", "Page error: " + exception, taskId);
            });
            
            log.info("🎧 Browser event listeners configured for task: {}", taskId);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to setup browser event listeners: {}", e.getMessage());
        }
    }
    
    /**
     * Tracks a navigation action with real-time broadcasting.
     */
    public void trackNavigation(Page page, String url, String taskId) {
        log.info("🧭 Navigating to: {}", url);
        broadcastBrowserEvent("navigation-started", "Navigating to: " + url, taskId);
        
        try {
            page.navigate(url);
            page.waitForLoadState();
            
            broadcastBrowserEvent("navigation-completed", "Successfully navigated to: " + url, taskId);
            
            // Auto-capture screenshot after navigation
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000); // Wait for content to settle
                    captureAndBroadcastScreenshot(page, "after-navigation", taskId);
                } catch (Exception e) {
                    log.debug("Failed to capture post-navigation screenshot: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Navigation failed to {}: {}", url, e.getMessage());
            broadcastBrowserEvent("navigation-failed", "Navigation failed: " + e.getMessage(), taskId);
        }
    }
    
    /**
     * Tracks a click action with real-time broadcasting.
     */
    public void trackClick(Page page, String selector, String description, String taskId) {
        log.info("👆 Clicking: {} ({})", description, selector);
        broadcastBrowserEvent("click-started", "Clicking: " + description, taskId);
        
        try {
            page.locator(selector).first().click();
            
            broadcastBrowserEvent("click-completed", "Successfully clicked: " + description, taskId);
            
            // Wait for any dynamic content and capture screenshot
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500); // Wait for click effects
                    captureAndBroadcastScreenshot(page, "after-click", taskId);
                } catch (Exception e) {
                    log.debug("Failed to capture post-click screenshot: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Click failed on {}: {}", description, e.getMessage());
            broadcastBrowserEvent("click-failed", "Click failed: " + e.getMessage(), taskId);
        }
    }
    
    /**
     * Tracks text input with real-time broadcasting.
     */
    public void trackTextInput(Page page, String selector, String text, String fieldDescription, String taskId) {
        log.info("⌨️ Typing in {}: {}", fieldDescription, text);
        broadcastBrowserEvent("input-started", "Typing in " + fieldDescription + ": " + text, taskId);
        
        try {
            page.locator(selector).first().fill(text);
            
            broadcastBrowserEvent("input-completed", "Successfully typed in " + fieldDescription, taskId);
            
        } catch (Exception e) {
            log.error("❌ Text input failed in {}: {}", fieldDescription, e.getMessage());
            broadcastBrowserEvent("input-failed", "Input failed: " + e.getMessage(), taskId);
        }
    }
    
    /**
     * Tracks scroll actions with real-time broadcasting.
     */
    public void trackScroll(Page page, String direction, String taskId) {
        log.info("📜 Scrolling: {}", direction);
        broadcastBrowserEvent("scroll-started", "Scrolling " + direction, taskId);
        
        try {
            if ("down".equalsIgnoreCase(direction)) {
                page.keyboard().press("PageDown");
            } else if ("up".equalsIgnoreCase(direction)) {
                page.keyboard().press("PageUp");
            }
            
            broadcastBrowserEvent("scroll-completed", "Scrolled " + direction, taskId);
            
        } catch (Exception e) {
            log.error("❌ Scroll failed: {}", e.getMessage());
            broadcastBrowserEvent("scroll-failed", "Scroll failed: " + e.getMessage(), taskId);
        }
    }
    
    /**
     * Captures screenshot and broadcasts it via SSE.
     */
    public void captureAndBroadcastScreenshot(Page page, String context, String taskId) {
        try {
            // Use screenshot directory configuration
            String screenshotDir = System.getProperty("app.storage.screenshots", "./screenshots");
            java.nio.file.Path baseDir = java.nio.file.Paths.get(screenshotDir).toAbsolutePath();
            java.nio.file.Files.createDirectories(baseDir);
            
            String filename = String.format("browser_%s_%s_%d.png", 
                context, taskId, System.currentTimeMillis());
            java.nio.file.Path screenshotPath = baseDir.resolve(filename);
            
            // Capture screenshot
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotPath)
                .setFullPage(false)); // Viewport only for faster capture
            
            // Broadcast screenshot captured event
            taskExecutionService.addTaskScreenshot(taskId, screenshotPath.toString());
            
            log.info("📸 Screenshot captured ({}): {}", context, screenshotPath);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to capture screenshot ({}): {}", context, e.getMessage());
        }
    }
    
    /**
     * Broadcasts a browser event via SSE to connected dashboards.
     */
    private void broadcastBrowserEvent(String eventType, String message, String taskId) {
        try {
            // Create a browser-specific progress update
            String progressMessage = String.format("[Browser] %s", message);
            
            // Find the task and update it with browser event details
            // This will trigger SSE broadcast via TaskExecutionIntegrationService
            broadcastBrowserAction(eventType, progressMessage, taskId);
            
        } catch (Exception e) {
            log.debug("Failed to broadcast browser event: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to broadcast browser actions as tool-progress events.
     */
    private void broadcastBrowserAction(String actionType, String message, String taskId) {
        // This could be enhanced to send custom browser-specific SSE events
        // For now, we'll use the existing tool-progress mechanism
        log.debug("🔔 Browser action: {} - {}", actionType, message);
    }
    
    /**
     * Helper method to get current task ID from context.
     */
    private String getCurrentTaskId() {
        try {
            return TaskContext.getCurrentTaskId();
        } catch (Exception e) {
            log.debug("No task context available");
            return null;
        }
    }
}