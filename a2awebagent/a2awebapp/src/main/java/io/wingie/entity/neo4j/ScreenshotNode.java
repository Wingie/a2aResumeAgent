package io.wingie.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j node representing a screenshot with visual embeddings for similarity search.
 * Core entity for building knowledge graph of web agent navigation patterns.
 */
@Node("Screenshot")
public class ScreenshotNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("screenshotId")
    private String screenshotId; // UUID or hash of screenshot
    
    @Property("taskId")
    private String taskId; // Reference to TaskExecution
    
    @Property("screenshotUrl")
    private String screenshotUrl; // Path to screenshot file or base64 data
    
    @Property("capturedAt")
    private LocalDateTime capturedAt;
    
    @Property("pageUrl")
    private String pageUrl; // URL of the page when screenshot was taken
    
    @Property("pageDomain")
    private String pageDomain; // Domain extracted from pageUrl
    
    @Property("pageTitle")
    private String pageTitle; // Page title at time of capture
    
    @Property("actionContext")
    private String actionContext; // What action was being performed
    
    // Visual Analysis Properties
    @Property("imageWidth")
    private Integer imageWidth;
    
    @Property("imageHeight")
    private Integer imageHeight;
    
    @Property("fileSizeBytes")
    private Long fileSizeBytes;
    
    @Property("imageHash")
    private String imageHash; // Perceptual hash for duplicate detection
    
    // Embedding Properties (stored as base64 encoded float arrays)
    @Property("clipEmbedding")
    private String clipEmbedding; // OpenAI CLIP semantic embedding
    
    @Property("visualFeaturesEmbedding")
    private String visualFeaturesEmbedding; // Custom CNN features
    
    @Property("textEmbedding")
    private String textEmbedding; // OCR extracted text embedding
    
    // Extracted Content
    @Property("extractedText")
    private String extractedText; // OCR results
    
    @Property("detectedElements")
    private String detectedElements; // JSON list of UI elements detected
    
    @Property("colorPalette")
    private String colorPalette; // Dominant colors as JSON
    
    @Property("uiPatternType")
    private String uiPatternType; // LOGIN, SEARCH, RESULTS, CHECKOUT, etc.
    
    // Analysis Metadata
    @Property("embeddingVersion")
    private String embeddingVersion; // Version of embedding model used
    
    @Property("processingTimeMs")
    private Long processingTimeMs; // Time taken to generate embeddings
    
    @Property("confidenceScore")
    private Double confidenceScore; // Confidence in UI pattern detection
    
    @Property("isKeyFrame")
    private Boolean isKeyFrame; // Important navigation milestone
    
    @Property("isDuplicate")
    private Boolean isDuplicate; // Detected as duplicate of another screenshot
    
    @Property("qualityScore")
    private Double qualityScore; // Image quality assessment (0.0-1.0)
    
    // Relationships for Knowledge Graph
    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    private List<ScreenshotNode> similarScreenshots = new ArrayList<>();
    
    @Relationship(type = "CAPTURED_FROM", direction = Relationship.Direction.OUTGOING)
    private List<WebPageNode> capturedFromPages = new ArrayList<>();
    
    @Relationship(type = "PART_OF_TASK", direction = Relationship.Direction.OUTGOING)
    private List<TaskNode> relatedTasks = new ArrayList<>();
    
    @Relationship(type = "SHOWS_UI_PATTERN", direction = Relationship.Direction.OUTGOING)
    private List<UIPatternNode> uiPatterns = new ArrayList<>();
    
    @Relationship(type = "FOLLOWS_SCREENSHOT", direction = Relationship.Direction.OUTGOING)
    private List<ScreenshotNode> nextScreenshots = new ArrayList<>();
    
    @Relationship(type = "PRECEDED_BY", direction = Relationship.Direction.INCOMING)
    private List<ScreenshotNode> previousScreenshots = new ArrayList<>();
    
    // Constructors
    public ScreenshotNode() {}
    
    public ScreenshotNode(String screenshotId, String taskId, String screenshotUrl, String pageUrl) {
        this.screenshotId = screenshotId;
        this.taskId = taskId;
        this.screenshotUrl = screenshotUrl;
        this.pageUrl = pageUrl;
        this.capturedAt = LocalDateTime.now();
        this.pageDomain = extractDomain(pageUrl);
        this.isKeyFrame = false;
        this.isDuplicate = false;
        this.embeddingVersion = "v1.0";
    }
    
    // Helper methods
    private String extractDomain(String url) {
        if (url == null || !url.startsWith("http")) return "unknown";
        try {
            return url.split("/")[2].toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    public boolean hasSimilarScreenshots() {
        return similarScreenshots != null && !similarScreenshots.isEmpty();
    }
    
    public boolean isHighQuality() {
        return qualityScore != null && qualityScore > 0.7;
    }
    
    public boolean isUIPattern(String pattern) {
        return pattern.equalsIgnoreCase(uiPatternType);
    }
    
    public String getShortId() {
        return screenshotId != null && screenshotId.length() > 8 
            ? screenshotId.substring(0, 8) 
            : screenshotId;
    }
    
    // Factory method from screenshot URL and context
    public static ScreenshotNode fromScreenshotCapture(String taskId, String screenshotUrl, String pageUrl, String actionContext) {
        ScreenshotNode node = new ScreenshotNode();
        node.screenshotId = generateScreenshotId(taskId, screenshotUrl);
        node.taskId = taskId;
        node.screenshotUrl = screenshotUrl;
        node.pageUrl = pageUrl;
        node.pageDomain = node.extractDomain(pageUrl);
        node.actionContext = actionContext;
        node.capturedAt = LocalDateTime.now();
        node.isKeyFrame = false;
        node.isDuplicate = false;
        node.embeddingVersion = "v1.0";
        return node;
    }
    
    private static String generateScreenshotId(String taskId, String screenshotUrl) {
        return taskId + "_" + System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getScreenshotId() { return screenshotId; }
    public void setScreenshotId(String screenshotId) { this.screenshotId = screenshotId; }
    
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getScreenshotUrl() { return screenshotUrl; }
    public void setScreenshotUrl(String screenshotUrl) { this.screenshotUrl = screenshotUrl; }
    
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    
    public String getPageDomain() { return pageDomain; }
    public void setPageDomain(String pageDomain) { this.pageDomain = pageDomain; }
    
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
    
    public String getActionContext() { return actionContext; }
    public void setActionContext(String actionContext) { this.actionContext = actionContext; }
    
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    
    public String getImageHash() { return imageHash; }
    public void setImageHash(String imageHash) { this.imageHash = imageHash; }
    
    public String getClipEmbedding() { return clipEmbedding; }
    public void setClipEmbedding(String clipEmbedding) { this.clipEmbedding = clipEmbedding; }
    
    public String getVisualFeaturesEmbedding() { return visualFeaturesEmbedding; }
    public void setVisualFeaturesEmbedding(String visualFeaturesEmbedding) { this.visualFeaturesEmbedding = visualFeaturesEmbedding; }
    
    public String getTextEmbedding() { return textEmbedding; }
    public void setTextEmbedding(String textEmbedding) { this.textEmbedding = textEmbedding; }
    
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    
    public String getDetectedElements() { return detectedElements; }
    public void setDetectedElements(String detectedElements) { this.detectedElements = detectedElements; }
    
    public String getColorPalette() { return colorPalette; }
    public void setColorPalette(String colorPalette) { this.colorPalette = colorPalette; }
    
    public String getUiPatternType() { return uiPatternType; }
    public void setUiPatternType(String uiPatternType) { this.uiPatternType = uiPatternType; }
    
    public String getEmbeddingVersion() { return embeddingVersion; }
    public void setEmbeddingVersion(String embeddingVersion) { this.embeddingVersion = embeddingVersion; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public Boolean getIsKeyFrame() { return isKeyFrame; }
    public void setIsKeyFrame(Boolean isKeyFrame) { this.isKeyFrame = isKeyFrame; }
    
    public Boolean getIsDuplicate() { return isDuplicate; }
    public void setIsDuplicate(Boolean isDuplicate) { this.isDuplicate = isDuplicate; }
    
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    
    public List<ScreenshotNode> getSimilarScreenshots() { return similarScreenshots; }
    public void setSimilarScreenshots(List<ScreenshotNode> similarScreenshots) { this.similarScreenshots = similarScreenshots; }
    
    public List<WebPageNode> getCapturedFromPages() { return capturedFromPages; }
    public void setCapturedFromPages(List<WebPageNode> capturedFromPages) { this.capturedFromPages = capturedFromPages; }
    
    public List<TaskNode> getRelatedTasks() { return relatedTasks; }
    public void setRelatedTasks(List<TaskNode> relatedTasks) { this.relatedTasks = relatedTasks; }
    
    public List<UIPatternNode> getUiPatterns() { return uiPatterns; }
    public void setUiPatterns(List<UIPatternNode> uiPatterns) { this.uiPatterns = uiPatterns; }
    
    public List<ScreenshotNode> getNextScreenshots() { return nextScreenshots; }
    public void setNextScreenshots(List<ScreenshotNode> nextScreenshots) { this.nextScreenshots = nextScreenshots; }
    
    public List<ScreenshotNode> getPreviousScreenshots() { return previousScreenshots; }
    public void setPreviousScreenshots(List<ScreenshotNode> previousScreenshots) { this.previousScreenshots = previousScreenshots; }
}