package io.wingie.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to cache AI-generated tool descriptions in PostgreSQL
 * Stores descriptions per provider model to avoid repeated AI calls during startup
 */
@Entity
@Table(name = "tool_descriptions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"provider_model", "tool_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The provider and model name (e.g., "deepseek/deepseek-r1:free", "gemini-2.0-flash-lite")
     */
    @Column(name = "provider_model", nullable = false, length = 100)
    private String providerModel;

    /**
     * The tool/action name (e.g., "askTasteBeforeYouWaste", "searchLinkedInProfile")
     */
    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    /**
     * AI-generated description of what the tool does
     */
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    /**
     * AI-generated parameters information
     */
    @Column(name = "parameters_info", length = 1000)
    private String parametersInfo;

    /**
     * Additional tool properties as JSON
     */
    @Column(name = "tool_properties", length = 2000)
    private String toolProperties;

    /**
     * Time taken to generate this description (in milliseconds)
     */
    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    /**
     * Quality score or rating (optional, for later evaluation)
     */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /**
     * When this description was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When this description was last used
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * How many times this cached description has been used
     */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
        if (usageCount == null) {
            usageCount = 0;
        }
    }

    /**
     * Update usage statistics when description is retrieved from cache
     */
    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    /**
     * Constructor for new descriptions
     */
    public ToolDescription(String providerModel, String toolName, String description, 
                          String parametersInfo, String toolProperties, Long generationTimeMs) {
        this.providerModel = providerModel;
        this.toolName = toolName;
        this.description = description;
        this.parametersInfo = parametersInfo;
        this.toolProperties = toolProperties;
        this.generationTimeMs = generationTimeMs;
        this.usageCount = 0;
    }
}