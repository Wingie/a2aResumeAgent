package io.wingie.service;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for detecting and handling dynamic page state changes
 * Handles popups, cookie banners, overlays, and other interruptions during workflows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageStateDetectionService {
    
    // Cookie banner selectors - most common patterns
    private static final String[] COOKIE_BANNER_SELECTORS = {
        "[id*='cookie']", "[class*='cookie']", "[id*='consent']", "[class*='consent']",
        "[id*='gdpr']", "[class*='gdpr']", "[id*='privacy']", "[class*='privacy']",
        "#cookieNotice", "#cookie-notice", "#cookie-banner", "#cookie-consent",
        ".cookie-notice", ".cookie-banner", ".cookie-consent", ".cookie-bar",
        ".gdpr-notice", ".privacy-notice", ".consent-banner", ".consent-bar",
        "div[data-testid*='cookie']", "div[data-testid*='consent']",
        "div[role='dialog'][aria-label*='cookie']", "div[role='dialog'][aria-label*='consent']"
    };
    
    // Cookie accept button selectors
    private static final String[] COOKIE_ACCEPT_SELECTORS = {
        "button[id*='accept']", "button[class*='accept']", "button[id*='agree']",
        "button[class*='agree']", "button[id*='allow']", "button[class*='allow']",
        "button:has-text('Accept')", "button:has-text('Accept All')", "button:has-text('Agree')",
        "button:has-text('Allow')", "button:has-text('OK')", "button:has-text('Got it')",
        "button:has-text('I Accept')", "button:has-text('I Agree')", "button:has-text('Continue')",
        "a:has-text('Accept')", "a:has-text('Agree')", "a:has-text('Allow')",
        "[data-testid*='accept']", "[data-testid*='agree']", "[data-testid*='allow']"
    };
    
    // Modal/popup selectors
    private static final String[] MODAL_SELECTORS = {
        "div[role='dialog']", "div[role='alertdialog']", "div[role='modal']",
        ".modal", ".popup", ".overlay", ".dialog", ".alert-dialog",
        "[aria-modal='true']", "[data-modal='true']", "[data-popup='true']",
        ".modal-dialog", ".popup-dialog", ".overlay-dialog", ".lightbox",
        "#modal", "#popup", "#overlay", "#dialog", "#lightbox"
    };
    
    // Close button selectors for modals
    private static final String[] MODAL_CLOSE_SELECTORS = {
        "button[aria-label*='close']", "button[aria-label*='Close']", 
        "button[title*='close']", "button[title*='Close']",
        "button:has-text('×')", "button:has-text('✕')", "button:has-text('Close')",
        ".close", ".close-button", ".modal-close", ".popup-close",
        "[data-dismiss='modal']", "[data-close='modal']", "[data-action='close']",
        "button.btn-close", "button[class*='close']", "span[class*='close']"
    };
    
    // Age verification selectors
    private static final String[] AGE_VERIFICATION_SELECTORS = {
        "[id*='age']", "[class*='age']", "[id*='adult']", "[class*='adult']",
        "button:has-text('I am 18')", "button:has-text('I am 21')", "button:has-text('Yes, I am')",
        "button:has-text('Enter')", "button:has-text('Continue')", "button:has-text('Confirm')",
        ".age-verification", ".adult-verification", "#age-gate", "#adult-gate"
    };
    
    /**
     * Detect and handle common page state interruptions
     * Returns true if any interruption was handled
     */
    public boolean handlePageStateInterruptions(Page page, String sessionId) {
        log.debug("Checking for page state interruptions on session: {}", sessionId);
        
        boolean handledAny = false;
        
        try {
            // Wait a brief moment for any dynamic content to load
            page.waitForTimeout(1000);
            
            // Handle cookie banners first (most common)
            if (handleCookieBanners(page, sessionId)) {
                handledAny = true;
                page.waitForTimeout(1000); // Wait after handling
            }
            
            // Handle modal dialogs
            if (handleModalDialogs(page, sessionId)) {
                handledAny = true;
                page.waitForTimeout(1000);
            }
            
            // Handle age verification
            if (handleAgeVerification(page, sessionId)) {
                handledAny = true;
                page.waitForTimeout(1000);
            }
            
            // Handle other overlay elements
            if (handleOverlayElements(page, sessionId)) {
                handledAny = true;
                page.waitForTimeout(1000);
            }
            
            if (handledAny) {
                log.info("Successfully handled page state interruptions for session: {}", sessionId);
                // Wait for page to stabilize after handling interruptions
                page.waitForLoadState();
            }
            
        } catch (Exception e) {
            log.warn("Error handling page state interruptions for session {}: {}", sessionId, e.getMessage());
        }
        
        return handledAny;
    }
    
    /**
     * Handle cookie banners and consent dialogs
     */
    private boolean handleCookieBanners(Page page, String sessionId) {
        try {
            // First check if any cookie banner is visible
            for (String selector : COOKIE_BANNER_SELECTORS) {
                try {
                    Locator bannerLocator = page.locator(selector);
                    if (bannerLocator.isVisible()) {
                        log.info("Found cookie banner with selector: {} for session: {}", selector, sessionId);
                        
                        // Try to find and click accept button within the banner
                        for (String acceptSelector : COOKIE_ACCEPT_SELECTORS) {
                            try {
                                Locator acceptButton = bannerLocator.locator(acceptSelector);
                                if (acceptButton.isVisible()) {
                                    log.info("Clicking cookie accept button: {} for session: {}", acceptSelector, sessionId);
                                    acceptButton.click();
                                    return true;
                                }
                            } catch (Exception e) {
                                // Continue to next selector
                            }
                        }
                        
                        // If no accept button found within banner, try page-wide accept buttons
                        for (String acceptSelector : COOKIE_ACCEPT_SELECTORS) {
                            try {
                                Locator acceptButton = page.locator(acceptSelector);
                                if (acceptButton.isVisible()) {
                                    log.info("Clicking page-wide cookie accept button: {} for session: {}", acceptSelector, sessionId);
                                    acceptButton.click();
                                    return true;
                                }
                            } catch (Exception e) {
                                // Continue to next selector
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }
        } catch (Exception e) {
            log.debug("Error handling cookie banners for session {}: {}", sessionId, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Handle modal dialogs and popups
     */
    private boolean handleModalDialogs(Page page, String sessionId) {
        try {
            for (String selector : MODAL_SELECTORS) {
                try {
                    Locator modalLocator = page.locator(selector);
                    if (modalLocator.isVisible()) {
                        log.info("Found modal dialog with selector: {} for session: {}", selector, sessionId);
                        
                        // Try to find and click close button
                        for (String closeSelector : MODAL_CLOSE_SELECTORS) {
                            try {
                                Locator closeButton = modalLocator.locator(closeSelector);
                                if (closeButton.isVisible()) {
                                    log.info("Clicking modal close button: {} for session: {}", closeSelector, sessionId);
                                    closeButton.click();
                                    return true;
                                }
                            } catch (Exception e) {
                                // Continue to next selector
                            }
                        }
                        
                        // If no close button found within modal, try page-wide close buttons
                        for (String closeSelector : MODAL_CLOSE_SELECTORS) {
                            try {
                                Locator closeButton = page.locator(closeSelector);
                                if (closeButton.isVisible()) {
                                    log.info("Clicking page-wide modal close button: {} for session: {}", closeSelector, sessionId);
                                    closeButton.click();
                                    return true;
                                }
                            } catch (Exception e) {
                                // Continue to next selector
                            }
                        }
                        
                        // As last resort, try pressing Escape key
                        try {
                            page.keyboard().press("Escape");
                            log.info("Pressed Escape key to close modal for session: {}", sessionId);
                            return true;
                        } catch (Exception e) {
                            // Continue
                        }
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }
        } catch (Exception e) {
            log.debug("Error handling modal dialogs for session {}: {}", sessionId, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Handle age verification popups
     */
    private boolean handleAgeVerification(Page page, String sessionId) {
        try {
            for (String selector : AGE_VERIFICATION_SELECTORS) {
                try {
                    Locator ageElement = page.locator(selector);
                    if (ageElement.isVisible()) {
                        log.info("Found age verification with selector: {} for session: {}", selector, sessionId);
                        
                        // If it's a button, click it
                        if (selector.contains("button")) {
                            ageElement.click();
                            return true;
                        }
                        
                        // If it's a container, look for buttons within it
                        try {
                            Locator confirmButton = ageElement.locator("button:has-text('Yes'), button:has-text('Confirm'), button:has-text('Enter'), button:has-text('Continue')");
                            if (confirmButton.isVisible()) {
                                log.info("Clicking age verification confirm button for session: {}", sessionId);
                                confirmButton.click();
                                return true;
                            }
                        } catch (Exception e) {
                            // Continue
                        }
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }
        } catch (Exception e) {
            log.debug("Error handling age verification for session {}: {}", sessionId, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Handle other overlay elements that might block interaction
     */
    private boolean handleOverlayElements(Page page, String sessionId) {
        try {
            // Check for general overlay elements
            String[] overlaySelectors = {
                ".overlay", ".backdrop", ".mask", ".cover",
                "[data-overlay='true']", "[data-backdrop='true']",
                ".loading-overlay", ".spinner-overlay"
            };
            
            for (String selector : overlaySelectors) {
                try {
                    Locator overlayElement = page.locator(selector);
                    if (overlayElement.isVisible()) {
                        log.info("Found overlay element with selector: {} for session: {}", selector, sessionId);
                        
                        // Try clicking the overlay to dismiss it
                        try {
                            overlayElement.click();
                            log.info("Clicked overlay element to dismiss for session: {}", sessionId);
                            return true;
                        } catch (Exception e) {
                            // If clicking fails, try pressing Escape
                            try {
                                page.keyboard().press("Escape");
                                log.info("Pressed Escape to dismiss overlay for session: {}", sessionId);
                                return true;
                            } catch (Exception e2) {
                                // Continue
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }
        } catch (Exception e) {
            log.debug("Error handling overlay elements for session {}: {}", sessionId, e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if page has any blocking elements that need to be handled
     */
    public boolean hasBlockingElements(Page page) {
        try {
            // Quick check for common blocking elements
            for (String selector : COOKIE_BANNER_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        return true;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            for (String selector : MODAL_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        return true;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            for (String selector : AGE_VERIFICATION_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        return true;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
        } catch (Exception e) {
            log.debug("Error checking for blocking elements: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Wait for page to stabilize after handling interruptions
     */
    public void waitForPageStabilization(Page page, String sessionId) {
        try {
            // Wait for network activity to settle
            page.waitForLoadState();
            
            // Additional wait for dynamic content
            page.waitForTimeout(2000);
            
            // Check if any new interruptions appeared
            if (hasBlockingElements(page)) {
                log.info("New blocking elements detected after stabilization, handling again for session: {}", sessionId);
                handlePageStateInterruptions(page, sessionId);
            }
            
        } catch (Exception e) {
            log.warn("Error waiting for page stabilization for session {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Get description of current page state for debugging
     */
    public String getPageStateDescription(Page page) {
        try {
            StringBuilder state = new StringBuilder();
            
            // Check for cookie banners
            boolean hasCookieBanner = false;
            for (String selector : COOKIE_BANNER_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        hasCookieBanner = true;
                        break;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            // Check for modals
            boolean hasModal = false;
            for (String selector : MODAL_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        hasModal = true;
                        break;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            // Check for age verification
            boolean hasAgeVerification = false;
            for (String selector : AGE_VERIFICATION_SELECTORS) {
                try {
                    if (page.locator(selector).isVisible()) {
                        hasAgeVerification = true;
                        break;
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
            
            state.append("Page State: ");
            if (hasCookieBanner) state.append("Cookie Banner, ");
            if (hasModal) state.append("Modal Dialog, ");
            if (hasAgeVerification) state.append("Age Verification, ");
            
            if (state.toString().equals("Page State: ")) {
                state.append("Clean (no interruptions)");
            } else {
                // Remove trailing comma and space
                state.setLength(state.length() - 2);
            }
            
            return state.toString();
            
        } catch (Exception e) {
            return "Page State: Unknown (error checking)";
        }
    }
}