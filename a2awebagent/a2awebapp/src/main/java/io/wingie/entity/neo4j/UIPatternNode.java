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
 * Neo4j node representing UI patterns detected across web pages.
 * Enables identification of common UI elements and interaction patterns.
 */
@Node("UIPattern")
public class UIPatternNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("patternType")
    private String patternType; // LOGIN, SEARCH, PAGINATION, FORM, BUTTON, etc.
    
    @Property("patternName")
    private String patternName; // Descriptive name
    
    @Property("description")
    private String description; // Pattern description
    
    @Property("firstDetected")
    private LocalDateTime firstDetected;
    
    @Property("lastSeen")
    private LocalDateTime lastSeen;
    
    @Property("occurrenceCount")
    private Integer occurrenceCount;
    
    @Property("successRate")
    private Double successRate; // Success rate when interacting with this pattern
    
    @Property("avgInteractionTime")
    private Double avgInteractionTime; // Average time to interact
    
    @Property("confidence")
    private Double confidence; // Detection confidence
    
    @Property("elementSelector")
    private String elementSelector; // CSS/XPath selector if applicable
    
    @Property("visualSignature")
    private String visualSignature; // Visual characteristics as JSON
    
    @Property("commonText")
    private String commonText; // Common text associated with pattern
    
    @Property("colorSignature")
    private String colorSignature; // Common color patterns
    
    @Property("sizeRange")
    private String sizeRange; // Size characteristics as JSON
    
    @Property("positionPattern")
    private String positionPattern; // Common positioning as JSON
    
    @Property("isInteractive")
    private Boolean isInteractive; // Whether pattern is interactive
    
    @Property("requiresInput")
    private Boolean requiresInput; // Whether pattern requires user input
    
    @Property("isNavigational")
    private Boolean isNavigational; // Whether pattern is for navigation
    
    @Property("domains")
    private String domains; // JSON list of domains where pattern appears
    
    // Relationships
    @Relationship(type = "APPEARS_ON", direction = Relationship.Direction.OUTGOING)
    private List<WebPageNode> appearsOnPages = new ArrayList<>();
    
    @Relationship(type = "SHOWN_IN", direction = Relationship.Direction.OUTGOING)
    private List<ScreenshotNode> shownInScreenshots = new ArrayList<>();
    
    @Relationship(type = "SIMILAR_PATTERN", direction = Relationship.Direction.OUTGOING)
    private List<UIPatternNode> similarPatterns = new ArrayList<>();
    
    @Relationship(type = "PART_OF_WORKFLOW", direction = Relationship.Direction.OUTGOING)
    private List<WorkflowNode> workflows = new ArrayList<>();
    
    // Constructors
    public UIPatternNode() {}
    
    public UIPatternNode(String patternType, String patternName) {
        this.patternType = patternType;
        this.patternName = patternName;
        this.firstDetected = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.occurrenceCount = 1;
        this.isInteractive = false;
        this.requiresInput = false;
        this.isNavigational = false;
    }
    
    // Helper methods
    public void recordOccurrence(boolean successful, double interactionTime) {
        this.occurrenceCount++;
        this.lastSeen = LocalDateTime.now();
        
        // Update success rate
        if (successRate == null) {
            successRate = successful ? 1.0 : 0.0;
        } else {
            successRate = (successRate * (occurrenceCount - 1) + (successful ? 1.0 : 0.0)) / occurrenceCount;
        }
        
        // Update average interaction time
        if (avgInteractionTime == null) {
            avgInteractionTime = interactionTime;
        } else {
            avgInteractionTime = (avgInteractionTime * (occurrenceCount - 1) + interactionTime) / occurrenceCount;
        }
    }
    
    public boolean isHighConfidence() {
        return confidence != null && confidence > 0.8;
    }
    
    public boolean isReliablePattern() {
        return occurrenceCount > 5 && successRate != null && successRate > 0.7;
    }
    
    public boolean isProblematicPattern() {
        return occurrenceCount > 3 && successRate != null && successRate < 0.3;
    }
    
    public String getPatternCategory() {
        if (Boolean.TRUE.equals(isNavigational)) return "NAVIGATION";
        if (Boolean.TRUE.equals(requiresInput)) return "INPUT";
        if (Boolean.TRUE.equals(isInteractive)) return "INTERACTIVE";
        return "DISPLAY";
    }
    
    // Factory methods
    public static UIPatternNode createLoginPattern(String description) {
        UIPatternNode pattern = new UIPatternNode("LOGIN", "Login Form");
        pattern.description = description;
        pattern.isInteractive = true;
        pattern.requiresInput = true;
        return pattern;
    }
    
    public static UIPatternNode createSearchPattern(String description) {
        UIPatternNode pattern = new UIPatternNode("SEARCH", "Search Form");
        pattern.description = description;
        pattern.isInteractive = true;
        pattern.requiresInput = true;
        return pattern;
    }
    
    public static UIPatternNode createButtonPattern(String description) {
        UIPatternNode pattern = new UIPatternNode("BUTTON", "Button Element");
        pattern.description = description;
        pattern.isInteractive = true;
        pattern.requiresInput = false;
        return pattern;
    }
    
    public static UIPatternNode createNavigationPattern(String description) {
        UIPatternNode pattern = new UIPatternNode("NAVIGATION", "Navigation Element");
        pattern.description = description;
        pattern.isInteractive = true;
        pattern.isNavigational = true;
        return pattern;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }
    
    public String getPatternName() { return patternName; }
    public void setPatternName(String patternName) { this.patternName = patternName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getFirstDetected() { return firstDetected; }
    public void setFirstDetected(LocalDateTime firstDetected) { this.firstDetected = firstDetected; }
    
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    
    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
    
    public Double getAvgInteractionTime() { return avgInteractionTime; }
    public void setAvgInteractionTime(Double avgInteractionTime) { this.avgInteractionTime = avgInteractionTime; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public String getElementSelector() { return elementSelector; }
    public void setElementSelector(String elementSelector) { this.elementSelector = elementSelector; }
    
    public String getVisualSignature() { return visualSignature; }
    public void setVisualSignature(String visualSignature) { this.visualSignature = visualSignature; }
    
    public String getCommonText() { return commonText; }
    public void setCommonText(String commonText) { this.commonText = commonText; }
    
    public String getColorSignature() { return colorSignature; }
    public void setColorSignature(String colorSignature) { this.colorSignature = colorSignature; }
    
    public String getSizeRange() { return sizeRange; }
    public void setSizeRange(String sizeRange) { this.sizeRange = sizeRange; }
    
    public String getPositionPattern() { return positionPattern; }
    public void setPositionPattern(String positionPattern) { this.positionPattern = positionPattern; }
    
    public Boolean getIsInteractive() { return isInteractive; }
    public void setIsInteractive(Boolean isInteractive) { this.isInteractive = isInteractive; }
    
    public Boolean getRequiresInput() { return requiresInput; }
    public void setRequiresInput(Boolean requiresInput) { this.requiresInput = requiresInput; }
    
    public Boolean getIsNavigational() { return isNavigational; }
    public void setIsNavigational(Boolean isNavigational) { this.isNavigational = isNavigational; }
    
    public String getDomains() { return domains; }
    public void setDomains(String domains) { this.domains = domains; }
    
    public List<WebPageNode> getAppearsOnPages() { return appearsOnPages; }
    public void setAppearsOnPages(List<WebPageNode> appearsOnPages) { this.appearsOnPages = appearsOnPages; }
    
    public List<ScreenshotNode> getShownInScreenshots() { return shownInScreenshots; }
    public void setShownInScreenshots(List<ScreenshotNode> shownInScreenshots) { this.shownInScreenshots = shownInScreenshots; }
    
    public List<UIPatternNode> getSimilarPatterns() { return similarPatterns; }
    public void setSimilarPatterns(List<UIPatternNode> similarPatterns) { this.similarPatterns = similarPatterns; }
    
    public List<WorkflowNode> getWorkflows() { return workflows; }
    public void setWorkflows(List<WorkflowNode> workflows) { this.workflows = workflows; }
}