package io.wingie.service.neo4j;

import io.wingie.entity.neo4j.ScreenshotNode;
import io.wingie.repository.neo4j.ScreenshotNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

/**
 * Service for generating and managing screenshot embeddings using CLIP and visual similarity analysis.
 * Implements vector-based screenshot knowledge graph capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenshotEmbeddingService {
    
    private final ScreenshotNodeRepository screenshotRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    
    @Value("${openai.api-key:}")
    private String openAiApiKey;
    
    @Value("${screenshot.embedding.enabled:false}")
    private boolean embeddingEnabled;
    
    @Value("${screenshot.storage.path:/app/screenshots}")
    private String screenshotStoragePath;
    
    /**
     * Asynchronously processes screenshot for embedding generation and similarity analysis
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<String> processScreenshotEmbedding(String screenshotId, String screenshotUrl) {
        if (!embeddingEnabled) {
            log.debug("📊 Screenshot embedding disabled, skipping processing for {}", screenshotId);
            return CompletableFuture.completedFuture("disabled");
        }
        
        try {
            log.debug("🧠 Starting embedding processing for screenshot: {}", screenshotId);
            long startTime = System.currentTimeMillis();
            
            // Find the screenshot node
            ScreenshotNode screenshot = screenshotRepository.findByScreenshotId(screenshotId).orElse(null);
            if (screenshot == null) {
                log.warn("⚠️ Screenshot node not found: {}", screenshotId);
                return CompletableFuture.completedFuture("not_found");
            }
            
            // Load and analyze image
            BufferedImage image = loadScreenshotImage(screenshotUrl);
            if (image == null) {
                log.warn("⚠️ Failed to load screenshot image: {}", screenshotUrl);
                return CompletableFuture.completedFuture("image_load_failed");
            }
            
            // Extract basic image properties
            extractImageProperties(screenshot, image);
            
            // Generate visual embeddings
            generateVisualEmbeddings(screenshot, image);
            
            // Perform duplicate detection
            detectDuplicates(screenshot);
            
            // Find similar screenshots
            findSimilarScreenshots(screenshot);
            
            // Extract UI patterns
            extractUIPatterns(screenshot, image);
            
            // Save updated screenshot node
            screenshotRepository.save(screenshot);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Screenshot embedding completed for {} in {}ms", screenshotId, duration);
            
            return CompletableFuture.completedFuture("completed");
            
        } catch (Exception e) {
            log.error("❌ Failed to process screenshot embedding for {}: {}", screenshotId, e.getMessage(), e);
            return CompletableFuture.completedFuture("failed");
        }
    }
    
    /**
     * Loads screenshot image from URL or file path
     */
    private BufferedImage loadScreenshotImage(String screenshotUrl) {
        try {
            if (screenshotUrl.startsWith("data:image")) {
                // Handle base64 data URLs
                String base64Data = screenshotUrl.substring(screenshotUrl.indexOf(",") + 1);
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                return ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            } else if (screenshotUrl.startsWith("http")) {
                // Handle HTTP URLs
                return ImageIO.read(new java.net.URL(screenshotUrl));
            } else {
                // Handle file paths
                Path imagePath = Path.of(screenshotStoragePath, screenshotUrl);
                if (Files.exists(imagePath)) {
                    return ImageIO.read(imagePath.toFile());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load screenshot image: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts basic image properties (dimensions, file size, quality assessment)
     */
    private void extractImageProperties(ScreenshotNode screenshot, BufferedImage image) {
        screenshot.setImageWidth(image.getWidth());
        screenshot.setImageHeight(image.getHeight());
        
        // Calculate basic quality score based on dimensions and content
        double qualityScore = calculateQualityScore(image);
        screenshot.setQualityScore(qualityScore);
        
        // Generate perceptual hash for duplicate detection
        String perceptualHash = generatePerceptualHash(image);
        screenshot.setImageHash(perceptualHash);
        
        log.debug("📏 Image properties: {}x{}, quality: {:.2f}, hash: {}", 
            image.getWidth(), image.getHeight(), qualityScore, perceptualHash);
    }
    
    /**
     * Generates visual embeddings using various techniques
     */
    private void generateVisualEmbeddings(ScreenshotNode screenshot, BufferedImage image) {
        try {
            // Generate CLIP embedding if OpenAI API is available
            if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
                String clipEmbedding = generateCLIPEmbedding(image);
                screenshot.setClipEmbedding(clipEmbedding);
            }
            
            // Generate custom visual features
            String visualFeatures = generateVisualFeatures(image);
            screenshot.setVisualFeaturesEmbedding(visualFeatures);
            
            // Extract color palette
            String colorPalette = extractColorPalette(image);
            screenshot.setColorPalette(colorPalette);
            
            screenshot.setEmbeddingVersion("v1.0");
            
        } catch (Exception e) {
            log.error("Failed to generate embeddings: {}", e.getMessage());
        }
    }
    
    /**
     * Generates CLIP embedding using OpenAI API (placeholder implementation)
     */
    private String generateCLIPEmbedding(BufferedImage image) {
        try {
            // Convert image to base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            
            // Note: This is a placeholder - OpenAI doesn't currently provide direct CLIP API
            // In practice, you would use a local CLIP model or a service like Hugging Face
            log.debug("🎨 Would generate CLIP embedding for image (placeholder)");
            
            // Return mock embedding vector as base64 encoded float array
            float[] mockEmbedding = new float[512]; // CLIP typical dimension
            for (int i = 0; i < mockEmbedding.length; i++) {
                mockEmbedding[i] = (float) Math.random();
            }
            
            return encodeFloatArrayToBase64(mockEmbedding);
            
        } catch (Exception e) {
            log.error("Failed to generate CLIP embedding: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates custom visual features (edges, textures, shapes)
     */
    private String generateVisualFeatures(BufferedImage image) {
        try {
            // Placeholder for custom CNN features or traditional computer vision features
            // Could include: edge density, texture analysis, color distribution, etc.
            
            float[] features = new float[256]; // Custom feature vector
            for (int i = 0; i < features.length; i++) {
                features[i] = (float) Math.random(); // Placeholder
            }
            
            return encodeFloatArrayToBase64(features);
            
        } catch (Exception e) {
            log.error("Failed to generate visual features: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts dominant color palette from image
     */
    private String extractColorPalette(BufferedImage image) {
        try {
            // Simple color palette extraction - could be enhanced with k-means clustering
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Sample colors from grid
            StringBuilder palette = new StringBuilder("[");
            int sampleCount = 0;
            
            for (int y = 0; y < height; y += height / 10) {
                for (int x = 0; x < width; x += width / 10) {
                    if (x < width && y < height) {
                        int rgb = image.getRGB(x, y);
                        String hex = String.format("#%06X", (0xFFFFFF & rgb));
                        if (sampleCount > 0) palette.append(",");
                        palette.append("\"").append(hex).append("\"");
                        sampleCount++;
                    }
                }
            }
            palette.append("]");
            
            return palette.toString();
            
        } catch (Exception e) {
            log.error("Failed to extract color palette: {}", e.getMessage());
            return "[]";
        }
    }
    
    /**
     * Detects duplicate screenshots using perceptual hashing
     */
    private void detectDuplicates(ScreenshotNode screenshot) {
        if (screenshot.getImageHash() == null) return;
        
        try {
            List<ScreenshotNode> potentialDuplicates = screenshotRepository
                .findByImageHashExcluding(screenshot.getImageHash(), screenshot.getScreenshotId());
            
            if (!potentialDuplicates.isEmpty()) {
                screenshot.setIsDuplicate(true);
                log.debug("🔍 Found {} potential duplicates for screenshot {}", 
                    potentialDuplicates.size(), screenshot.getScreenshotId());
            }
            
        } catch (Exception e) {
            log.error("Failed duplicate detection: {}", e.getMessage());
        }
    }
    
    /**
     * Finds visually similar screenshots using embedding similarity
     */
    private void findSimilarScreenshots(ScreenshotNode screenshot) {
        try {
            // Placeholder for vector similarity search
            // In production, this would use cosine similarity on embeddings
            
            if (screenshot.getClipEmbedding() != null) {
                // Would perform vector similarity search here
                log.debug("🔗 Would find similar screenshots using CLIP embeddings");
            }
            
            // For now, find screenshots with same UI pattern
            if (screenshot.getUiPatternType() != null) {
                List<ScreenshotNode> similarByPattern = screenshotRepository
                    .findByUIPatternWithConfidence(screenshot.getUiPatternType(), 0.7);
                
                // Establish similarity relationships
                for (ScreenshotNode similar : similarByPattern) {
                    if (!similar.getScreenshotId().equals(screenshot.getScreenshotId())) {
                        screenshot.getSimilarScreenshots().add(similar);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed similarity search: {}", e.getMessage());
        }
    }
    
    /**
     * Extracts UI patterns and elements from screenshot
     */
    private void extractUIPatterns(ScreenshotNode screenshot, BufferedImage image) {
        try {
            // Placeholder for UI pattern recognition
            // Could use computer vision to detect: buttons, forms, navigation, etc.
            
            // Simple heuristics based on image properties
            int width = image.getWidth();
            int height = image.getHeight();
            
            if (width > 1200 && height > 800) {
                screenshot.setUiPatternType("DESKTOP");
                screenshot.setConfidenceScore(0.8);
            } else if (width < 500) {
                screenshot.setUiPatternType("MOBILE");
                screenshot.setConfidenceScore(0.7);
            } else {
                screenshot.setUiPatternType("GENERAL");
                screenshot.setConfidenceScore(0.6);
            }
            
            // Mark as key frame if high quality and unique
            if (screenshot.getQualityScore() != null && screenshot.getQualityScore() > 0.8 && 
                screenshot.getIsDuplicate() != null && !screenshot.getIsDuplicate()) {
                screenshot.setIsKeyFrame(true);
            }
            
        } catch (Exception e) {
            log.error("Failed UI pattern extraction: {}", e.getMessage());
        }
    }
    
    /**
     * Calculates quality score based on image characteristics
     */
    private double calculateQualityScore(BufferedImage image) {
        try {
            double score = 0.0;
            
            // Resolution score (0-40%)
            int pixels = image.getWidth() * image.getHeight();
            if (pixels > 1920 * 1080) score += 0.4;
            else if (pixels > 1280 * 720) score += 0.3;
            else if (pixels > 640 * 480) score += 0.2;
            else score += 0.1;
            
            // Aspect ratio score (0-20%)
            double aspectRatio = (double) image.getWidth() / image.getHeight();
            if (aspectRatio >= 1.2 && aspectRatio <= 2.0) score += 0.2; // Good web aspect ratios
            else score += 0.1;
            
            // Content variety score (0-40%) - placeholder
            score += 0.3; // Would analyze color distribution, edge density, etc.
            
            return Math.min(score, 1.0);
            
        } catch (Exception e) {
            return 0.5; // Default quality
        }
    }
    
    /**
     * Generates simple perceptual hash for duplicate detection
     */
    private String generatePerceptualHash(BufferedImage image) {
        try {
            // Simple 8x8 grayscale hash (placeholder for more sophisticated pHash)
            BufferedImage resized = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
            resized.getGraphics().drawImage(image, 0, 0, 8, 8, null);
            
            StringBuilder hash = new StringBuilder();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int gray = (resized.getRGB(x, y) & 0xFF);
                    hash.append(gray > 128 ? "1" : "0");
                }
            }
            
            return hash.toString();
            
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis()); // Fallback unique hash
        }
    }
    
    /**
     * Encodes float array as base64 for storage
     */
    private String encodeFloatArrayToBase64(float[] array) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (float f : array) {
                int bits = Float.floatToIntBits(f);
                baos.write((bits >> 24) & 0xFF);
                baos.write((bits >> 16) & 0xFF);
                baos.write((bits >> 8) & 0xFF);
                baos.write(bits & 0xFF);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to encode float array: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds visually similar screenshots for a given screenshot ID
     */
    public List<ScreenshotNode> findSimilarScreenshots(String screenshotId, int limit) {
        try {
            ScreenshotNode target = screenshotRepository.findByScreenshotId(screenshotId).orElse(null);
            if (target == null) {
                return List.of();
            }
            
            // Find by UI pattern first
            if (target.getUiPatternType() != null) {
                return screenshotRepository.findByUIPatternWithConfidence(target.getUiPatternType(), 0.6);
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("Failed to find similar screenshots: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Gets embedding statistics
     */
    public EmbeddingStats getEmbeddingStats() {
        try {
            EmbeddingStats stats = new EmbeddingStats();
            
            List<ScreenshotNode> withEmbeddings = screenshotRepository.findScreenshotsWithEmbeddings(1000);
            stats.setTotalWithEmbeddings(withEmbeddings.size());
            
            List<ScreenshotNode> duplicates = screenshotRepository.findDuplicateScreenshots();
            stats.setDuplicateCount(duplicates.size());
            
            List<ScreenshotNode> keyFrames = screenshotRepository.findKeyFrameScreenshots();
            stats.setKeyFrameCount(keyFrames.size());
            
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get embedding stats: {}", e.getMessage());
            return new EmbeddingStats();
        }
    }
    
    // Stats DTO
    public static class EmbeddingStats {
        private int totalWithEmbeddings;
        private int duplicateCount;
        private int keyFrameCount;
        
        public int getTotalWithEmbeddings() { return totalWithEmbeddings; }
        public void setTotalWithEmbeddings(int totalWithEmbeddings) { this.totalWithEmbeddings = totalWithEmbeddings; }
        
        public int getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
        
        public int getKeyFrameCount() { return keyFrameCount; }
        public void setKeyFrameCount(int keyFrameCount) { this.keyFrameCount = keyFrameCount; }
    }
}