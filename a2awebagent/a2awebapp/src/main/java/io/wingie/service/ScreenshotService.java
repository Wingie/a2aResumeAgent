package io.wingie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for managing screenshot storage and URL generation.
 * Handles saving base64 images to static directory and generating accessible URLs.
 */
@Service
@Slf4j
public class ScreenshotService {

    @Value("${app.storage.screenshots:./screenshots}")
    private String screenshotsPath;

    @Value("${server.port:7860}")
    private int serverPort;

    private Path screenshotsDirectory;

    @PostConstruct
    public void init() {
        try {
            screenshotsDirectory = Paths.get(screenshotsPath).toAbsolutePath();
            if (!Files.exists(screenshotsDirectory)) {
                Files.createDirectories(screenshotsDirectory);
                log.info("Created screenshots directory: {}", screenshotsDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to create screenshots directory: {}", screenshotsPath, e);
            throw new RuntimeException("Failed to initialize screenshot service", e);
        }
    }

    /**
     * Saves a base64 encoded image to the screenshots directory and returns the accessible URL.
     * 
     * @param base64Data Base64 encoded image data
     * @param filePrefix Prefix for the filename (e.g., "meme", "screenshot")
     * @return URL to access the saved image
     */
    public String saveScreenshotAndGetUrl(String base64Data, String filePrefix) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            log.warn("Empty base64 data provided, cannot save screenshot");
            return null;
        }

        try {
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = String.format("%s_%s_%s.png", filePrefix, timestamp, uniqueId);
            
            // Decode base64 data
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            
            // Save to screenshots directory
            Path imagePath = screenshotsDirectory.resolve(filename);
            Files.write(imagePath, imageBytes);
            
            // Generate accessible URL
            String url = String.format("http://localhost:%d/screenshots/%s", serverPort, filename);
            
            log.info("Saved screenshot: {} (size: {} bytes)", filename, imageBytes.length);
            return url;
            
        } catch (Exception e) {
            log.error("Failed to save screenshot with prefix '{}'", filePrefix, e);
            return null;
        }
    }

    /**
     * Saves a meme screenshot and returns the URL.
     */
    public String saveMemeScreenshot(String base64Data) {
        return saveScreenshotAndGetUrl(base64Data, "meme");
    }

    /**
     * Saves a general screenshot and returns the URL.
     */
    public String saveGeneralScreenshot(String base64Data) {
        return saveScreenshotAndGetUrl(base64Data, "screenshot");
    }

    /**
     * Cleans up old screenshots (older than 24 hours) to prevent disk space issues.
     * Runs automatically every 6 hours.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // Every 6 hours
    public void cleanupOldScreenshots() {
        try {
            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
            
            Files.list(screenshotsDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.debug("Deleted old screenshot: {}", path.getFileName());
                    } catch (IOException e) {
                        log.warn("Failed to delete old screenshot: {}", path.getFileName(), e);
                    }
                });
                
        } catch (IOException e) {
            log.warn("Failed to cleanup old screenshots", e);
        }
    }
}