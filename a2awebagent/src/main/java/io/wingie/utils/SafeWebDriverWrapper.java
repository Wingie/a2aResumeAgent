package io.wingie.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.aop.framework.AopProxyUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Safe wrapper for WebDriver that handles Spring proxy casting issues.
 * Provides safe access to JavascriptExecutor and TakesScreenshot interfaces.
 */
@Slf4j
public class SafeWebDriverWrapper implements WebDriver, JavascriptExecutor, TakesScreenshot {
    
    private final WebDriver delegate;
    
    public SafeWebDriverWrapper(WebDriver delegate) {
        this.delegate = delegate;
        log.info("SafeWebDriverWrapper created for delegate: {}", delegate.getClass().getName());
    }
    
    /**
     * Get the actual WebDriver instance, unwrapping Spring proxies if necessary
     */
    public WebDriver getActualWebDriver() {
        try {
            // Try to unwrap Spring proxy to get the actual target
            Object target = AopProxyUtils.getSingletonTarget(delegate);
            if (target instanceof WebDriver) {
                log.debug("Unwrapped Spring proxy to get actual WebDriver: {}", target.getClass().getName());
                return (WebDriver) target;
            }
        } catch (Exception e) {
            log.debug("Could not unwrap Spring proxy, using delegate directly: {}", e.getMessage());
        }
        return delegate;
    }
    
    /**
     * Safe cast to JavascriptExecutor with fallback handling
     */
    public JavascriptExecutor getJavaScriptExecutor() {
        WebDriver actual = getActualWebDriver();
        
        // First try direct casting
        if (actual instanceof JavascriptExecutor) {
            return (JavascriptExecutor) actual;
        }
        
        // If that fails, try the original delegate
        if (delegate instanceof JavascriptExecutor) {
            return (JavascriptExecutor) delegate;
        }
        
        throw new UnsupportedOperationException("WebDriver does not support JavaScript execution. " +
            "Actual type: " + actual.getClass().getName() + ", Delegate type: " + delegate.getClass().getName());
    }
    
    /**
     * Safe cast to TakesScreenshot with fallback handling
     */
    public TakesScreenshot getScreenshotTaker() {
        WebDriver actual = getActualWebDriver();
        
        // First try direct casting
        if (actual instanceof TakesScreenshot) {
            return (TakesScreenshot) actual;
        }
        
        // If that fails, try the original delegate
        if (delegate instanceof TakesScreenshot) {
            return (TakesScreenshot) delegate;
        }
        
        throw new UnsupportedOperationException("WebDriver does not support screenshot capture. " +
            "Actual type: " + actual.getClass().getName() + ", Delegate type: " + delegate.getClass().getName());
    }
    
    // JavascriptExecutor implementation
    @Override
    public Object executeScript(String script, Object... args) {
        try {
            return getJavaScriptExecutor().executeScript(script, args);
        } catch (Exception e) {
            log.error("JavaScript execution failed: {}", e.getMessage());
            throw new WebDriverException("Failed to execute JavaScript: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        try {
            return getJavaScriptExecutor().executeAsyncScript(script, args);
        } catch (Exception e) {
            log.error("Async JavaScript execution failed: {}", e.getMessage());
            throw new WebDriverException("Failed to execute async JavaScript: " + e.getMessage(), e);
        }
    }
    
    // TakesScreenshot implementation
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        try {
            return getScreenshotTaker().getScreenshotAs(target);
        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            throw new WebDriverException("Failed to capture screenshot: " + e.getMessage(), e);
        }
    }
    
    // WebDriver delegation methods
    @Override
    public void get(String url) {
        delegate.get(url);
    }
    
    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }
    
    @Override
    public String getTitle() {
        return delegate.getTitle();
    }
    
    @Override
    public List<WebElement> findElements(By by) {
        return delegate.findElements(by);
    }
    
    @Override
    public WebElement findElement(By by) {
        return delegate.findElement(by);
    }
    
    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }
    
    @Override
    public void close() {
        delegate.close();
    }
    
    @Override
    public void quit() {
        delegate.quit();
    }
    
    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }
    
    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }
    
    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }
    
    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }
    
    @Override
    public Options manage() {
        return delegate.manage();
    }
    
    /**
     * Utility method to create a safe wrapper for any WebDriver
     */
    public static SafeWebDriverWrapper wrap(WebDriver driver) {
        if (driver instanceof SafeWebDriverWrapper) {
            return (SafeWebDriverWrapper) driver;
        }
        return new SafeWebDriverWrapper(driver);
    }
    
    /**
     * Check if the driver supports JavaScript execution
     */
    public boolean supportsJavaScript() {
        try {
            getJavaScriptExecutor();
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
    
    /**
     * Check if the driver supports screenshot capture
     */
    public boolean supportsScreenshots() {
        try {
            getScreenshotTaker();
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}