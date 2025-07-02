package io.wingie.repository.neo4j;

import io.wingie.entity.neo4j.WebPageNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Neo4j repository for WebPageNode entities.
 * Provides web navigation analysis and page performance queries.
 */
@Repository
public interface WebPageNodeRepository extends Neo4jRepository<WebPageNode, Long> {
    
    // Basic finders
    Optional<WebPageNode> findByUrl(String url);
    List<WebPageNode> findByDomain(String domain);
    List<WebPageNode> findByPageType(String pageType);
    
    // Navigation flow analysis
    @Query("MATCH (p1:WebPage)-[:NAVIGATES_TO]->(p2:WebPage) WHERE p1.url = $fromUrl RETURN p2 ORDER BY p2.visitCount DESC")
    List<WebPageNode> findNavigationTargets(@Param("fromUrl") String fromUrl);
    
    @Query("MATCH (p1:WebPage)<-[:NAVIGATES_TO]-(p2:WebPage) WHERE p1.url = $toUrl RETURN p2 ORDER BY p2.visitCount DESC")
    List<WebPageNode> findNavigationSources(@Param("toUrl") String toUrl);
    
    @Query("MATCH path = (start:WebPage)-[:NAVIGATES_TO*1..5]->(end:WebPage) " +
           "WHERE start.url = $startUrl AND end.url = $endUrl " +
           "RETURN path ORDER BY length(path) LIMIT $limit")
    List<WebPageNode> findNavigationPaths(@Param("startUrl") String startUrl, @Param("endUrl") String endUrl, @Param("limit") int limit);
    
    // Performance analysis
    @Query("MATCH (p:WebPage) WHERE p.avgActionSuccessRate > $minSuccessRate AND p.visitCount > $minVisits " +
           "RETURN p ORDER BY p.avgActionSuccessRate DESC, p.visitCount DESC LIMIT $limit")
    List<WebPageNode> findHighPerformingPages(@Param("minSuccessRate") double minSuccessRate, @Param("minVisits") int minVisits, @Param("limit") int limit);
    
    @Query("MATCH (p:WebPage) WHERE p.avgActionSuccessRate < $maxSuccessRate OR p.loadErrorRate > $maxErrorRate " +
           "RETURN p ORDER BY p.avgActionSuccessRate ASC, p.loadErrorRate DESC LIMIT $limit")
    List<WebPageNode> findProblematicPages(@Param("maxSuccessRate") double maxSuccessRate, @Param("maxErrorRate") double maxErrorRate, @Param("limit") int limit);
    
    @Query("MATCH (p:WebPage) WHERE p.domain = $domain RETURN p ORDER BY p.taskSuccessRate DESC LIMIT $limit")
    List<WebPageNode> findBestPerformingPagesByDomain(@Param("domain") String domain, @Param("limit") int limit);
    
    // Page characteristics
    @Query("MATCH (p:WebPage) WHERE p.hasSearchForm = true RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findSearchPages();
    
    @Query("MATCH (p:WebPage) WHERE p.hasLoginForm = true RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findLoginPages();
    
    @Query("MATCH (p:WebPage) WHERE p.hasCheckoutForm = true RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findCheckoutPages();
    
    @Query("MATCH (p:WebPage) WHERE p.isEntryPage = true RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findEntryPages();
    
    @Query("MATCH (p:WebPage) WHERE p.isExitPage = true RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findExitPages();
    
    // Task relationship queries
    @Query("MATCH (t:Task)-[:VISITS_PAGE]->(p:WebPage) WHERE t.taskType = $taskType " +
           "RETURN p, COUNT(t) as visitCount ORDER BY visitCount DESC LIMIT $limit")
    List<WebPageNode> findMostVisitedPagesByTaskType(@Param("taskType") String taskType, @Param("limit") int limit);
    
    @Query("MATCH (t:Task)-[:VISITS_PAGE]->(p:WebPage) WHERE t.isSuccessful = true " +
           "RETURN p, COUNT(t) as successfulVisits ORDER BY successfulVisits DESC LIMIT $limit")
    List<WebPageNode> findPagesWithMostSuccessfulTasks(@Param("limit") int limit);
    
    @Query("MATCH (t:Task)-[:VISITS_PAGE]->(p:WebPage) WHERE p.url = $url " +
           "RETURN AVG(CASE WHEN t.isSuccessful = true THEN 1.0 ELSE 0.0 END) as successRate")
    Double getTaskSuccessRateForPage(@Param("url") String url);
    
    // Time-based queries
    @Query("MATCH (p:WebPage) WHERE p.lastVisited >= $since RETURN p ORDER BY p.lastVisited DESC")
    List<WebPageNode> findRecentlyVisitedPages(@Param("since") LocalDateTime since);
    
    @Query("MATCH (p:WebPage) WHERE p.firstVisited >= $since RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findNewPagesDiscoveredSince(@Param("since") LocalDateTime since);
    
    // Domain analysis
    @Query("MATCH (p:WebPage) WHERE p.domain = $domain RETURN p ORDER BY p.visitCount DESC")
    List<WebPageNode> findPagesByDomainSortedByVisits(@Param("domain") String domain);
    
    @Query("MATCH (p:WebPage) RETURN p.domain as domain, COUNT(p) as pageCount, SUM(p.visitCount) as totalVisits " +
           "ORDER BY totalVisits DESC LIMIT $limit")
    List<DomainStats> getDomainStatistics(@Param("limit") int limit);
    
    // Similar pages
    @Query("MATCH (p1:WebPage)-[:SIMILAR_PAGE]->(p2:WebPage) WHERE p1.url = $url RETURN p2")
    List<WebPageNode> findSimilarPages(@Param("url") String url);
    
    @Query("MATCH (p:WebPage) WHERE p.pageType = $pageType AND p.domain <> $excludeDomain " +
           "RETURN p ORDER BY p.avgActionSuccessRate DESC LIMIT $limit")
    List<WebPageNode> findSimilarPagesByType(@Param("pageType") String pageType, @Param("excludeDomain") String excludeDomain, @Param("limit") int limit);
    
    // Content and quality analysis
    @Query("MATCH (p:WebPage) WHERE p.accessibilityScore > $minScore RETURN p ORDER BY p.accessibilityScore DESC")
    List<WebPageNode> findHighAccessibilityPages(@Param("minScore") double minScore);
    
    @Query("MATCH (p:WebPage) WHERE p.avgLoadTimeMs < $maxLoadTime AND p.visitCount > $minVisits " +
           "RETURN p ORDER BY p.avgLoadTimeMs ASC")
    List<WebPageNode> findFastLoadingPages(@Param("maxLoadTime") double maxLoadTime, @Param("minVisits") int minVisits);
    
    // Statistics
    @Query("MATCH (p:WebPage) RETURN COUNT(p) as totalPages, " +
           "COUNT(CASE WHEN p.isEntryPage = true THEN 1 END) as entryPages, " +
           "COUNT(CASE WHEN p.isExitPage = true THEN 1 END) as exitPages, " +
           "AVG(p.visitCount) as avgVisitCount")
    PageStatistics getPageStatistics();
    
    @Query("MATCH (p:WebPage) WHERE p.domain = $domain " +
           "RETURN COUNT(p) as pageCount, SUM(p.visitCount) as totalVisits, " +
           "AVG(p.avgActionSuccessRate) as avgSuccessRate, AVG(p.avgLoadTimeMs) as avgLoadTime")
    DomainDetailStats getDomainDetailStats(@Param("domain") String domain);
    
    // Cleanup
    @Query("MATCH (p:WebPage) WHERE p.lastVisited < $cutoffDate AND p.visitCount = 1 DELETE p")
    void deleteStalePages(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Projection interfaces
    interface DomainStats {
        String getDomain();
        Long getPageCount();
        Long getTotalVisits();
    }
    
    interface DomainDetailStats {
        Long getPageCount();
        Long getTotalVisits();
        Double getAvgSuccessRate();
        Double getAvgLoadTime();
    }
    
    interface PageStatistics {
        Long getTotalPages();
        Long getEntryPages();
        Long getExitPages();
        Double getAvgVisitCount();
    }
}