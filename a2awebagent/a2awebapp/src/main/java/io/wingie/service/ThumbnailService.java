package io.wingie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Service for generating thumbnails from screenshots.
 * 
 * Provides asynchronous thumbnail generation with multiple size options
 * and caching capabilities for enhanced screenshot gallery performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {
    
    private static final int DEFAULT_THUMBNAIL_WIDTH = 300;
    private static final int DEFAULT_THUMBNAIL_HEIGHT = 200;
    private static final int SMALL_THUMBNAIL_WIDTH = 150;
    private static final int SMALL_THUMBNAIL_HEIGHT = 100;
    private static final String THUMBNAIL_SUFFIX = "_thumb";
    private static final String SMALL_THUMBNAIL_SUFFIX = "_small";
    
    /**
     * Generate thumbnail asynchronously with default dimensions
     */
    @Async
    public CompletableFuture<String> generateThumbnail(String screenshotPath) {
        return generateThumbnail(screenshotPath, DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT);
    }
    
    /**
     * Generate thumbnail asynchronously with custom dimensions
     */
    @Async
    public CompletableFuture<String> generateThumbnail(String screenshotPath, int width, int height) {
        return generateThumbnail(screenshotPath, width, height, THUMBNAIL_SUFFIX);
    }
    
    /**
     * Generate small thumbnail for grid views
     */
    @Async
    public CompletableFuture<String> generateSmallThumbnail(String screenshotPath) {
        return generateThumbnail(screenshotPath, SMALL_THUMBNAIL_WIDTH, SMALL_THUMBNAIL_HEIGHT, SMALL_THUMBNAIL_SUFFIX);
    }
    
    /**
     * Core thumbnail generation method
     */
    private CompletableFuture<String> generateThumbnail(String screenshotPath, int width, int height, String suffix) {
        try {
            // Handle both file paths and base64 data
            BufferedImage original;
            String thumbnailPath;
            
            if (screenshotPath.startsWith("data:image/")) {
                // Handle base64 data URLs
                original = decodeBase64Image(screenshotPath);
                thumbnailPath = generateThumbnailPathFromBase64(screenshotPath, suffix);
            } else {
                // Handle file paths
                File screenshotFile = new File(screenshotPath);
                if (!screenshotFile.exists()) {
                    log.warn("Screenshot file not found: {}", screenshotPath);
                    return CompletableFuture.completedFuture(null);
                }
                
                original = ImageIO.read(screenshotFile);
                thumbnailPath = generateThumbnailPath(screenshotPath, suffix);
            }
            
            if (original == null) {
                log.warn("Failed to read image from: {}", screenshotPath);
                return CompletableFuture.completedFuture(null);
            }
            
            // Generate thumbnail using Scalr for high quality
            BufferedImage thumbnail = Scalr.resize(original, 
                Scalr.Method.QUALITY, 
                Scalr.Mode.FIT_TO_WIDTH, 
                width, height);
            
            // Ensure thumbnail directory exists
            Path thumbnailDir = Paths.get(thumbnailPath).getParent();
            if (thumbnailDir != null) {
                Files.createDirectories(thumbnailDir);
            }
            
            // Save thumbnail
            ImageIO.write(thumbnail, "PNG", new File(thumbnailPath));
            
            log.debug("Generated thumbnail: {} -> {}", screenshotPath, thumbnailPath);
            return CompletableFuture.completedFuture(thumbnailPath);
            
        } catch (IOException e) {
            log.error("Failed to generate thumbnail for: {}", screenshotPath, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Generate thumbnail from base64 image data
     */
    @Async
    public CompletableFuture<String> generateThumbnailFromBase64(String base64Data, String taskId) {
        try {
            BufferedImage original = decodeBase64Image(base64Data);
            if (original == null) {
                return CompletableFuture.completedFuture(null);
            }
            
            // Generate thumbnail
            BufferedImage thumbnail = Scalr.resize(original, 
                Scalr.Method.QUALITY, 
                Scalr.Mode.FIT_TO_WIDTH, 
                DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT);
            
            // Save to temp file with task ID
            String thumbnailPath = String.format("screenshots/thumbnails/%s_thumb.png", taskId);
            Path thumbnailDir = Paths.get(thumbnailPath).getParent();
            if (thumbnailDir != null) {
                Files.createDirectories(thumbnailDir);
            }
            
            ImageIO.write(thumbnail, "PNG", new File(thumbnailPath));
            
            log.debug("Generated thumbnail from base64 data for task: {}", taskId);
            return CompletableFuture.completedFuture(thumbnailPath);
            
        } catch (IOException e) {
            log.error("Failed to generate thumbnail from base64 data for task: {}", taskId, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Check if thumbnail already exists
     */
    public boolean thumbnailExists(String screenshotPath) {
        String thumbnailPath = generateThumbnailPath(screenshotPath, THUMBNAIL_SUFFIX);
        return Files.exists(Paths.get(thumbnailPath));
    }
    
    /**
     * Get thumbnail path (whether it exists or not)
     */
    public String getThumbnailPath(String screenshotPath) {
        return generateThumbnailPath(screenshotPath, THUMBNAIL_SUFFIX);
    }
    
    /**
     * Get small thumbnail path
     */
    public String getSmallThumbnailPath(String screenshotPath) {
        return generateThumbnailPath(screenshotPath, SMALL_THUMBNAIL_SUFFIX);
    }
    
    /**
     * Generate multiple thumbnail sizes for a screenshot
     */
    @Async
    public CompletableFuture<ThumbnailSet> generateThumbnailSet(String screenshotPath) {
        CompletableFuture<String> regularThumbnail = generateThumbnail(screenshotPath);
        CompletableFuture<String> smallThumbnail = generateSmallThumbnail(screenshotPath);
        
        return CompletableFuture.allOf(regularThumbnail, smallThumbnail)
            .thenApply(v -> ThumbnailSet.builder()
                .originalPath(screenshotPath)
                .thumbnailPath(regularThumbnail.join())
                .smallThumbnailPath(smallThumbnail.join())
                .build());
    }
    
    // Helper methods
    
    private String generateThumbnailPath(String originalPath, String suffix) {
        if (originalPath.contains(".")) {
            int lastDot = originalPath.lastIndexOf('.');
            String baseName = originalPath.substring(0, lastDot);
            String extension = originalPath.substring(lastDot);
            return baseName + suffix + extension;
        } else {
            return originalPath + suffix + ".png";
        }
    }
    
    private String generateThumbnailPathFromBase64(String base64Data, String suffix) {
        // Generate a unique filename based on hash of base64 data
        int hash = base64Data.hashCode();
        return String.format("screenshots/thumbnails/%d%s.png", hash, suffix);
    }
    
    private BufferedImage decodeBase64Image(String base64Data) throws IOException {
        String base64String = base64Data;
        
        // Remove data URL prefix if present
        if (base64String.startsWith("data:image/")) {
            int commaIndex = base64String.indexOf(',');
            if (commaIndex != -1) {
                base64String = base64String.substring(commaIndex + 1);
            }
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(base64String);
        return ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
    }
    
    /**
     * Data class for thumbnail set
     */
    @lombok.Data
    @lombok.Builder
    public static class ThumbnailSet {
        private String originalPath;
        private String thumbnailPath;
        private String smallThumbnailPath;
    }
}