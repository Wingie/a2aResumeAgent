package io.wingie.repository.neo4j;

import io.wingie.entity.neo4j.ScreenshotNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Neo4j repository for ScreenshotNode entities.
 * Provides screenshot similarity search and knowledge graph queries.
 */
@Repository
public interface ScreenshotNodeRepository extends Neo4jRepository<ScreenshotNode, Long> {
    
    // Basic finders
    Optional<ScreenshotNode> findByScreenshotId(String screenshotId);
    List<ScreenshotNode> findByTaskId(String taskId);
    List<ScreenshotNode> findByPageDomain(String pageDomain);
    List<ScreenshotNode> findByUiPatternType(String uiPatternType);
    
    // Find screenshots by quality and characteristics
    @Query("MATCH (s:Screenshot) WHERE s.qualityScore > $minQuality AND s.isDuplicate = false RETURN s ORDER BY s.capturedAt DESC LIMIT $limit")
    List<ScreenshotNode> findHighQualityScreenshots(@Param("minQuality") double minQuality, @Param("limit") int limit);
    
    @Query("MATCH (s:Screenshot) WHERE s.isKeyFrame = true RETURN s ORDER BY s.capturedAt DESC")
    List<ScreenshotNode> findKeyFrameScreenshots();
    
    // Similarity and duplicate detection
    @Query("MATCH (s:Screenshot) WHERE s.imageHash = $imageHash AND s.screenshotId <> $excludeId RETURN s")
    List<ScreenshotNode> findByImageHashExcluding(@Param("imageHash") String imageHash, @Param("excludeId") String excludeId);
    
    @Query("MATCH (s1:Screenshot)-[:SIMILAR_TO]->(s2:Screenshot) WHERE s1.screenshotId = $screenshotId RETURN s2 ORDER BY s2.capturedAt DESC")
    List<ScreenshotNode> findSimilarScreenshots(@Param("screenshotId") String screenshotId);
    
    // Page and domain analysis
    @Query("MATCH (s:Screenshot) WHERE s.pageDomain = $domain AND s.capturedAt >= $since RETURN s ORDER BY s.capturedAt DESC")
    List<ScreenshotNode> findByDomainSince(@Param("domain") String domain, @Param("since") LocalDateTime since);
    
    @Query("MATCH (s:Screenshot) WHERE s.pageUrl CONTAINS $urlFragment RETURN s ORDER BY s.capturedAt DESC LIMIT $limit")
    List<ScreenshotNode> findByUrlFragment(@Param("urlFragment") String urlFragment, @Param("limit") int limit);
    
    // UI pattern analysis
    @Query("MATCH (s:Screenshot) WHERE s.uiPatternType = $patternType AND s.confidenceScore > $minConfidence RETURN s ORDER BY s.confidenceScore DESC")
    List<ScreenshotNode> findByUIPatternWithConfidence(@Param("patternType") String patternType, @Param("minConfidence") double minConfidence);
    
    @Query("MATCH (s:Screenshot) WHERE s.extractedText CONTAINS $text RETURN s ORDER BY s.capturedAt DESC LIMIT $limit")
    List<ScreenshotNode> findByExtractedText(@Param("text") String text, @Param("limit") int limit);
    
    // Navigation flow analysis
    @Query("MATCH (s1:Screenshot)-[:FOLLOWS_SCREENSHOT]->(s2:Screenshot) WHERE s1.screenshotId = $screenshotId RETURN s2")
    List<ScreenshotNode> findNextScreenshots(@Param("screenshotId") String screenshotId);
    
    @Query("MATCH (s1:Screenshot)<-[:FOLLOWS_SCREENSHOT]-(s2:Screenshot) WHERE s1.screenshotId = $screenshotId RETURN s2")
    List<ScreenshotNode> findPreviousScreenshots(@Param("screenshotId") String screenshotId);
    
    // Task relationship queries
    @Query("MATCH (t:Task)-[:CAPTURES_SCREENSHOT]->(s:Screenshot) WHERE t.taskId = $taskId RETURN s ORDER BY s.capturedAt")
    List<ScreenshotNode> findScreenshotsByTask(@Param("taskId") String taskId);
    
    @Query("MATCH (t1:Task)-[:CAPTURES_SCREENSHOT]->(s1:Screenshot), (t2:Task)-[:CAPTURES_SCREENSHOT]->(s2:Screenshot) " +
           "WHERE t1.taskType = $taskType AND s1.uiPatternType = s2.uiPatternType " +
           "RETURN s2, t2 ORDER BY s2.capturedAt DESC LIMIT $limit")
    List<ScreenshotNode> findSimilarScreenshotsByTaskType(@Param("taskType") String taskType, @Param("limit") int limit);
    
    // Statistics and analytics
    @Query("MATCH (s:Screenshot) WHERE s.capturedAt >= $since RETURN COUNT(s) as count")
    long countScreenshotsSince(@Param("since") LocalDateTime since);
    
    @Query("MATCH (s:Screenshot) WHERE s.pageDomain = $domain RETURN COUNT(s) as count")
    long countScreenshotsByDomain(@Param("domain") String domain);
    
    @Query("MATCH (s:Screenshot) WHERE s.uiPatternType = $patternType RETURN COUNT(s) as count")
    long countScreenshotsByUIPattern(@Param("patternType") String patternType);
    
    @Query("MATCH (s:Screenshot) RETURN s.pageDomain as domain, COUNT(s) as count ORDER BY count DESC LIMIT $limit")
    List<DomainCount> getTopDomainsByScreenshotCount(@Param("limit") int limit);
    
    @Query("MATCH (s:Screenshot) WHERE s.uiPatternType IS NOT NULL RETURN s.uiPatternType as pattern, COUNT(s) as count ORDER BY count DESC")
    List<PatternCount> getUIPatternStatistics();
    
    // Vector similarity search (for future CLIP embedding integration)
    @Query("MATCH (s:Screenshot) WHERE s.clipEmbedding IS NOT NULL RETURN s LIMIT $limit")
    List<ScreenshotNode> findScreenshotsWithEmbeddings(@Param("limit") int limit);
    
    // Cleanup and maintenance
    @Query("MATCH (s:Screenshot) WHERE s.isDuplicate = true RETURN s")
    List<ScreenshotNode> findDuplicateScreenshots();
    
    @Query("MATCH (s:Screenshot) WHERE s.capturedAt < $cutoffDate DELETE s")
    void deleteScreenshotsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("MATCH (s:Screenshot) WHERE s.qualityScore < $minQuality DELETE s")
    void deleteLowQualityScreenshots(@Param("minQuality") double minQuality);
    
    // Projection interfaces for statistics
    interface DomainCount {
        String getDomain();
        Long getCount();
    }
    
    interface PatternCount {
        String getPattern();
        Long getCount();
    }
}