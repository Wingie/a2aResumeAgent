package io.wingie.playwright;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Analyzes web page content to generate meaningful screenshot filenames
 * and handles file collision management with overwrite behavior
 */
@Component
@Slf4j
public class ScreenshotContentAnalyzer {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final int MAX_FILENAME_LENGTH = 100;
    
    /**
     * Generates a content-based filename for a screenshot
     * @param page The Playwright page to analyze
     * @param baseDir The base directory where screenshots are stored
     * @return Full absolute path for the screenshot file
     */
    public String generateContentBasedFilename(Page page, String baseDir) {
        try {
            // Extract page identifiers
            String title = extractPageTitle(page);
            String domain = extractDomain(page);
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            
            // Generate base filename
            String baseFilename = buildFilename(domain, title, timestamp);
            String filename = sanitizeFilename(baseFilename) + ".png";
            
            // Resolve to absolute path
            Path screenshotDir = Paths.get(baseDir).toAbsolutePath();
            Path fullPath = screenshotDir.resolve(filename);
            
            // Ensure directory exists
            Files.createDirectories(screenshotDir);
            
            log.info("Generated content-based screenshot filename: {}", fullPath);
            return fullPath.toString();
            
        } catch (Exception e) {
            log.warn("Failed to generate content-based filename, falling back to timestamp", e);
            return generateFallbackFilename(baseDir);
        }
    }
    
    /**
     * Checks if a file with the same content-based name exists and handles collision
     * @param fullPath The full path to check
     * @return The path to use (may be the same or modified for collision handling)
     */
    public String handleFileCollision(String fullPath) {
        Path path = Paths.get(fullPath);
        
        if (Files.exists(path)) {
            log.info("Screenshot file already exists, will overwrite: {}", fullPath);
            // Per requirement: "if it comes to be that the name is same as one already in screenshots its just overwritten"
            try {
                Files.deleteIfExists(path);
                log.info("Existing screenshot file deleted for overwrite: {}", fullPath);
            } catch (Exception e) {
                log.warn("Failed to delete existing screenshot file, proceeding with overwrite", e);
            }
        }
        
        return fullPath;
    }
    
    private String extractPageTitle(Page page) {
        try {
            String title = page.title();
            if (title != null && !title.trim().isEmpty()) {
                // Clean and truncate title
                title = title.trim()
                    .replaceAll("[^a-zA-Z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .toLowerCase();
                return title.length() > 30 ? title.substring(0, 30) : title;
            }
        } catch (Exception e) {
            log.debug("Failed to extract page title", e);
        }
        return "untitled";
    }
    
    private String extractDomain(Page page) {
        try {
            String url = page.url();
            if (url != null && url.startsWith("http")) {
                java.net.URL parsedUrl = new java.net.URL(url);
                String domain = parsedUrl.getHost();
                if (domain != null) {
                    // Remove www. prefix and common TLD
                    domain = domain.replaceFirst("^www\\.", "");
                    int dotIndex = domain.indexOf('.');
                    if (dotIndex > 0) {
                        domain = domain.substring(0, dotIndex);
                    }
                    return domain.toLowerCase();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract domain", e);
        }
        return "unknown-site";
    }
    
    private String buildFilename(String domain, String title, String timestamp) {
        StringBuilder filename = new StringBuilder();
        
        // Start with domain
        filename.append(domain);
        
        // Add title if meaningful and different from domain
        if (!title.equals("untitled") && !title.contains(domain)) {
            filename.append("_").append(title);
        }
        
        // Add timestamp
        filename.append("_").append(timestamp);
        
        return filename.toString();
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "screenshot_" + System.currentTimeMillis();
        }
        
        // Remove/replace invalid characters for filesystems
        String sanitized = filename
            .replaceAll("[<>:\"/\\\\|?*]", "_")  // Windows forbidden chars
            .replaceAll("[\\x00-\\x1f\\x80-\\x9f]", "_")  // Control chars
            .replaceAll("_{2,}", "_")  // Multiple underscores
            .replaceAll("^_+|_+$", "");  // Leading/trailing underscores
        
        // Ensure reasonable length
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
        }
        
        // Ensure not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "screenshot_" + System.currentTimeMillis();
        }
        
        return sanitized;
    }
    
    private String generateFallbackFilename(String baseDir) {
        try {
            Path screenshotDir = Paths.get(baseDir).toAbsolutePath();
            Files.createDirectories(screenshotDir);
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path fullPath = screenshotDir.resolve("playwright_" + timestamp + ".png");
            
            return fullPath.toString();
        } catch (Exception e) {
            log.error("Failed to generate fallback filename", e);
            return Paths.get(baseDir, "screenshot_error.png").toAbsolutePath().toString();
        }
    }
}