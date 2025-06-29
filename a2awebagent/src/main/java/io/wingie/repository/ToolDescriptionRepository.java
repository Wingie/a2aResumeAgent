package io.wingie.repository;

import io.wingie.entity.ToolDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing cached tool descriptions in PostgreSQL
 */
@Repository
public interface ToolDescriptionRepository extends JpaRepository<ToolDescription, Long> {

    /**
     * Find cached description for specific provider model and tool name
     */
    Optional<ToolDescription> findByProviderModelAndToolName(String providerModel, String toolName);

    /**
     * Get all descriptions for a specific provider model
     */
    List<ToolDescription> findByProviderModel(String providerModel);

    /**
     * Get all descriptions for a specific tool across all providers
     */
    List<ToolDescription> findByToolName(String toolName);

    /**
     * Find descriptions created after a specific date
     */
    List<ToolDescription> findByCreatedAtAfter(LocalDateTime createdAt);

    /**
     * Update usage statistics when a cached description is used
     */
    @Modifying
    @Query("UPDATE ToolDescription t SET t.usageCount = t.usageCount + 1, t.lastUsedAt = :lastUsedAt WHERE t.id = :id")
    void incrementUsageCount(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    /**
     * Get provider comparison statistics
     */
    @Query("SELECT t.providerModel, COUNT(t), AVG(t.generationTimeMs), AVG(t.qualityScore) " +
           "FROM ToolDescription t " +
           "GROUP BY t.providerModel " +
           "ORDER BY t.providerModel")
    List<Object[]> getProviderStatistics();

    /**
     * Get most frequently used tools
     */
    @Query("SELECT t.toolName, t.providerModel, SUM(t.usageCount) " +
           "FROM ToolDescription t " +
           "GROUP BY t.toolName, t.providerModel " +
           "ORDER BY SUM(t.usageCount) DESC")
    List<Object[]> getMostUsedTools();

    /**
     * Check if description exists for provider and tool
     */
    boolean existsByProviderModelAndToolName(String providerModel, String toolName);

    /**
     * Delete old descriptions (cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime createdAt);

    /**
     * Count total cached descriptions
     */
    @Query("SELECT COUNT(t) FROM ToolDescription t")
    long countTotalDescriptions();

    /**
     * Count descriptions per provider
     */
    @Query("SELECT t.providerModel, COUNT(t) FROM ToolDescription t GROUP BY t.providerModel")
    List<Object[]> countByProvider();
}