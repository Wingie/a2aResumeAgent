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
 * Neo4j node representing a web page visited during agent navigation.
 * Tracks page metadata, performance, and navigation patterns.
 */
@Node("WebPage")
public class WebPageNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("url")
    private String url; // Full URL
    
    @Property("domain")
    private String domain; // Extracted domain
    
    @Property("path")
    private String path; // URL path component
    
    @Property("pageTitle")
    private String pageTitle; // Page title
    
    @Property("pageType")
    private String pageType; // SEARCH, RESULTS, DETAIL, CHECKOUT, LOGIN, etc.
    
    @Property("firstVisited")
    private LocalDateTime firstVisited;
    
    @Property("lastVisited")
    private LocalDateTime lastVisited;
    
    @Property("visitCount")
    private Integer visitCount;
    
    @Property("totalTimeSpentSeconds")
    private Long totalTimeSpentSeconds;
    
    @Property("avgLoadTimeMs")
    private Double avgLoadTimeMs;
    
    @Property("avgActionSuccessRate")
    private Double avgActionSuccessRate; // Success rate of actions on this page
    
    // Content Analysis
    @Property("hasSearchForm")
    private Boolean hasSearchForm;
    
    @Property("hasLoginForm")
    private Boolean hasLoginForm;
    
    @Property("hasCheckoutForm")
    private Boolean hasCheckoutForm;
    
    @Property("hasPagination")
    private Boolean hasPagination;
    
    @Property("primaryLanguage")
    private String primaryLanguage;
    
    @Property("detectedFrameworks")
    private String detectedFrameworks; // JSON list of detected web frameworks
    
    // Performance Metrics
    @Property("avgScreenshotsPerVisit")
    private Double avgScreenshotsPerVisit;
    
    @Property("avgActionsPerVisit")
    private Double avgActionsPerVisit;
    
    @Property("taskSuccessRate")
    private Double taskSuccessRate; // Rate of successful task completion from this page
    
    @Property("bounceRate")
    private Double bounceRate; // Rate of immediate exits
    
    @Property("isEntryPage")
    private Boolean isEntryPage; // Common starting point for tasks
    
    @Property("isExitPage")
    private Boolean isExitPage; // Common ending point for tasks
    
    @Property("isErrorPage")
    private Boolean isErrorPage; // Indicates error/404 pages
    
    // Accessibility & Quality
    @Property("accessibilityScore")
    private Double accessibilityScore; // Accessibility assessment
    
    @Property("loadErrorRate")
    private Double loadErrorRate; // Rate of load failures
    
    @Property("lastContentHash")
    private String lastContentHash; // Hash of page content for change detection
    
    @Property("lastAnalyzed")
    private LocalDateTime lastAnalyzed;
    
    // Relationships for Navigation Flow Analysis
    @Relationship(type = "NAVIGATES_TO", direction = Relationship.Direction.OUTGOING)
    private List<WebPageNode> navigatesTo = new ArrayList<>();
    
    @Relationship(type = "NAVIGATED_FROM", direction = Relationship.Direction.INCOMING)  
    private List<WebPageNode> navigatedFrom = new ArrayList<>();
    
    @Relationship(type = "SIMILAR_PAGE", direction = Relationship.Direction.OUTGOING)
    private List<WebPageNode> similarPages = new ArrayList<>();
    
    @Relationship(type = "CONTAINS_PATTERN", direction = Relationship.Direction.OUTGOING)
    private List<UIPatternNode> uiPatterns = new ArrayList<>();
    
    @Relationship(type = "VISITED_IN_TASK", direction = Relationship.Direction.INCOMING)
    private List<TaskNode> tasksVisited = new ArrayList<>();
    
    @Relationship(type = "SCREENSHOT_TAKEN", direction = Relationship.Direction.INCOMING)
    private List<ScreenshotNode> screenshots = new ArrayList<>();
    
    // Constructors
    public WebPageNode() {}
    
    public WebPageNode(String url) {
        this.url = url;
        this.domain = extractDomain(url);
        this.path = extractPath(url);
        this.firstVisited = LocalDateTime.now();
        this.lastVisited = LocalDateTime.now();
        this.visitCount = 1;
        this.totalTimeSpentSeconds = 0L;
        this.isEntryPage = false;
        this.isExitPage = false;
        this.isErrorPage = false;
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
    
    private String extractPath(String url) {
        if (url == null || !url.startsWith("http")) return "/";
        try {
            String[] parts = url.split("/");
            if (parts.length > 3) {
                StringBuilder path = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    path.append("/").append(parts[i]);
                }
                return path.toString();
            }
            return "/";
        } catch (Exception e) {
            return "/";
        }
    }
    
    public void recordVisit(long timeSpentSeconds, double loadTimeMs) {
        this.visitCount++;
        this.lastVisited = LocalDateTime.now();
        this.totalTimeSpentSeconds += timeSpentSeconds;
        
        // Update average load time
        if (avgLoadTimeMs == null) {
            avgLoadTimeMs = loadTimeMs;
        } else {
            avgLoadTimeMs = (avgLoadTimeMs * (visitCount - 1) + loadTimeMs) / visitCount;
        }
    }
    
    public void updateSuccessMetrics(boolean actionSuccessful, boolean taskCompleted) {
        // Update action success rate
        if (avgActionSuccessRate == null) {
            avgActionSuccessRate = actionSuccessful ? 1.0 : 0.0;
        } else {
            avgActionSuccessRate = (avgActionSuccessRate * (visitCount - 1) + (actionSuccessful ? 1.0 : 0.0)) / visitCount;
        }
        
        // Update task success rate
        if (taskCompleted) {
            if (taskSuccessRate == null) {
                taskSuccessRate = 1.0;
            } else {
                int taskCount = tasksVisited != null ? tasksVisited.size() : 1;
                taskSuccessRate = (taskSuccessRate * (taskCount - 1) + 1.0) / taskCount;
            }
        }
    }
    
    public boolean isHighPerformingPage() {
        return avgActionSuccessRate != null && avgActionSuccessRate > 0.8 &&
               taskSuccessRate != null && taskSuccessRate > 0.7;
    }
    
    public boolean isProblematicPage() {
        return (avgActionSuccessRate != null && avgActionSuccessRate < 0.5) ||
               (loadErrorRate != null && loadErrorRate > 0.2) ||
               (bounceRate != null && bounceRate > 0.8);
    }
    
    public String getPageCategory() {
        if (Boolean.TRUE.equals(isErrorPage)) return "ERROR";
        if (Boolean.TRUE.equals(hasLoginForm)) return "LOGIN";
        if (Boolean.TRUE.equals(hasCheckoutForm)) return "CHECKOUT";
        if (Boolean.TRUE.equals(hasSearchForm)) return "SEARCH";
        if (pageType != null) return pageType;
        return "GENERAL";
    }
    
    public double getAvgTimePerVisit() {
        return visitCount > 0 ? (double) totalTimeSpentSeconds / visitCount : 0.0;
    }
    
    // Factory method
    public static WebPageNode fromUrl(String url, String pageTitle, String pageType) {
        WebPageNode node = new WebPageNode(url);
        node.pageTitle = pageTitle;
        node.pageType = pageType;
        return node;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
    
    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }
    
    public LocalDateTime getFirstVisited() { return firstVisited; }
    public void setFirstVisited(LocalDateTime firstVisited) { this.firstVisited = firstVisited; }
    
    public LocalDateTime getLastVisited() { return lastVisited; }
    public void setLastVisited(LocalDateTime lastVisited) { this.lastVisited = lastVisited; }
    
    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }
    
    public Long getTotalTimeSpentSeconds() { return totalTimeSpentSeconds; }
    public void setTotalTimeSpentSeconds(Long totalTimeSpentSeconds) { this.totalTimeSpentSeconds = totalTimeSpentSeconds; }
    
    public Double getAvgLoadTimeMs() { return avgLoadTimeMs; }
    public void setAvgLoadTimeMs(Double avgLoadTimeMs) { this.avgLoadTimeMs = avgLoadTimeMs; }
    
    public Double getAvgActionSuccessRate() { return avgActionSuccessRate; }
    public void setAvgActionSuccessRate(Double avgActionSuccessRate) { this.avgActionSuccessRate = avgActionSuccessRate; }
    
    public Boolean getHasSearchForm() { return hasSearchForm; }
    public void setHasSearchForm(Boolean hasSearchForm) { this.hasSearchForm = hasSearchForm; }
    
    public Boolean getHasLoginForm() { return hasLoginForm; }
    public void setHasLoginForm(Boolean hasLoginForm) { this.hasLoginForm = hasLoginForm; }
    
    public Boolean getHasCheckoutForm() { return hasCheckoutForm; }
    public void setHasCheckoutForm(Boolean hasCheckoutForm) { this.hasCheckoutForm = hasCheckoutForm; }
    
    public Boolean getHasPagination() { return hasPagination; }
    public void setHasPagination(Boolean hasPagination) { this.hasPagination = hasPagination; }
    
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }
    
    public String getDetectedFrameworks() { return detectedFrameworks; }
    public void setDetectedFrameworks(String detectedFrameworks) { this.detectedFrameworks = detectedFrameworks; }
    
    public Double getAvgScreenshotsPerVisit() { return avgScreenshotsPerVisit; }
    public void setAvgScreenshotsPerVisit(Double avgScreenshotsPerVisit) { this.avgScreenshotsPerVisit = avgScreenshotsPerVisit; }
    
    public Double getAvgActionsPerVisit() { return avgActionsPerVisit; }
    public void setAvgActionsPerVisit(Double avgActionsPerVisit) { this.avgActionsPerVisit = avgActionsPerVisit; }
    
    public Double getTaskSuccessRate() { return taskSuccessRate; }
    public void setTaskSuccessRate(Double taskSuccessRate) { this.taskSuccessRate = taskSuccessRate; }
    
    public Double getBounceRate() { return bounceRate; }
    public void setBounceRate(Double bounceRate) { this.bounceRate = bounceRate; }
    
    public Boolean getIsEntryPage() { return isEntryPage; }
    public void setIsEntryPage(Boolean isEntryPage) { this.isEntryPage = isEntryPage; }
    
    public Boolean getIsExitPage() { return isExitPage; }
    public void setIsExitPage(Boolean isExitPage) { this.isExitPage = isExitPage; }
    
    public Boolean getIsErrorPage() { return isErrorPage; }
    public void setIsErrorPage(Boolean isErrorPage) { this.isErrorPage = isErrorPage; }
    
    public Double getAccessibilityScore() { return accessibilityScore; }
    public void setAccessibilityScore(Double accessibilityScore) { this.accessibilityScore = accessibilityScore; }
    
    public Double getLoadErrorRate() { return loadErrorRate; }
    public void setLoadErrorRate(Double loadErrorRate) { this.loadErrorRate = loadErrorRate; }
    
    public String getLastContentHash() { return lastContentHash; }
    public void setLastContentHash(String lastContentHash) { this.lastContentHash = lastContentHash; }
    
    public LocalDateTime getLastAnalyzed() { return lastAnalyzed; }
    public void setLastAnalyzed(LocalDateTime lastAnalyzed) { this.lastAnalyzed = lastAnalyzed; }
    
    public List<WebPageNode> getNavigatesTo() { return navigatesTo; }
    public void setNavigatesTo(List<WebPageNode> navigatesTo) { this.navigatesTo = navigatesTo; }
    
    public List<WebPageNode> getNavigatedFrom() { return navigatedFrom; }
    public void setNavigatedFrom(List<WebPageNode> navigatedFrom) { this.navigatedFrom = navigatedFrom; }
    
    public List<WebPageNode> getSimilarPages() { return similarPages; }
    public void setSimilarPages(List<WebPageNode> similarPages) { this.similarPages = similarPages; }
    
    public List<UIPatternNode> getUiPatterns() { return uiPatterns; }
    public void setUiPatterns(List<UIPatternNode> uiPatterns) { this.uiPatterns = uiPatterns; }
    
    public List<TaskNode> getTasksVisited() { return tasksVisited; }
    public void setTasksVisited(List<TaskNode> tasksVisited) { this.tasksVisited = tasksVisited; }
    
    public List<ScreenshotNode> getScreenshots() { return screenshots; }
    public void setScreenshots(List<ScreenshotNode> screenshots) { this.screenshots = screenshots; }
}