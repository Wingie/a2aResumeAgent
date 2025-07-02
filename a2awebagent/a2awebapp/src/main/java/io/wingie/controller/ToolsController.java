package io.wingie.controller;

import io.wingie.entity.ToolDescription;
import io.wingie.service.ToolDescriptionCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tools")
@Slf4j
public class ToolsController {
    
    @Autowired
    private ToolDescriptionCacheService cacheService;
    
    @GetMapping
    public String toolsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String providerFilter,
            @RequestParam(required = false) String toolFilter,
            @RequestParam(required = false) String performanceFilter,
            Model model) {
        
        log.info("Tools page accessed - page: {}, size: {}, sortBy: {}, filters: provider={}, tool={}, performance={}", 
                page, size, sortBy, providerFilter, toolFilter, performanceFilter);
        
        try {
            // Get all descriptions for filtering
            List<ToolDescription> allDescriptions = cacheService.getCurrentProviderDescriptions();
            
            // Apply filters
            List<ToolDescription> filteredDescriptions = applyFilters(allDescriptions, providerFilter, toolFilter, performanceFilter);
            
            // Apply sorting
            filteredDescriptions = applySorting(filteredDescriptions, sortBy, sortDir);
            
            // Manual pagination
            int totalElements = filteredDescriptions.size();
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            List<ToolDescription> pageContent = filteredDescriptions.subList(start, end);
            
            // Calculate statistics
            Map<String, Object> statistics = calculateToolStatistics(allDescriptions);
            Map<String, Object> filteredStats = calculateToolStatistics(filteredDescriptions);
            
            // Get filter options
            Set<String> availableProviders = getAvailableProviders(allDescriptions);
            Set<String> availableTools = getAvailableTools(allDescriptions);
            
            // Pagination info
            Map<String, Object> pagination = createPaginationInfo(page, size, totalElements);
            
            // Add data to model
            model.addAttribute("tools", pageContent);
            model.addAttribute("statistics", statistics);
            model.addAttribute("filteredStats", filteredStats);
            model.addAttribute("availableProviders", availableProviders);
            model.addAttribute("availableTools", availableTools);
            model.addAttribute("pagination", pagination);
            model.addAttribute("currentFilters", createCurrentFilters(providerFilter, toolFilter, performanceFilter));
            model.addAttribute("sortInfo", createSortInfo(sortBy, sortDir));
            
            return "tools-dashboard";
            
        } catch (Exception e) {
            log.error("Error loading tools page: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load tools data: " + e.getMessage());
            model.addAttribute("tools", Collections.emptyList());
            model.addAttribute("statistics", createEmptyStatistics());
            return "tools-dashboard";
        }
    }
    
    private List<ToolDescription> applyFilters(List<ToolDescription> descriptions, String providerFilter, String toolFilter, String performanceFilter) {
        return descriptions.stream()
                .filter(desc -> providerFilter == null || providerFilter.isEmpty() || desc.getProviderModel().toLowerCase().contains(providerFilter.toLowerCase()))
                .filter(desc -> toolFilter == null || toolFilter.isEmpty() || desc.getToolName().toLowerCase().contains(toolFilter.toLowerCase()))
                .filter(desc -> performanceFilter == null || performanceFilter.isEmpty() || matchesPerformanceFilter(desc, performanceFilter))
                .collect(Collectors.toList());
    }
    
    private boolean matchesPerformanceFilter(ToolDescription desc, String performanceFilter) {
        if (desc.getGenerationTimeMs() == null) return performanceFilter.equals("unknown");
        
        long timeMs = desc.getGenerationTimeMs();
        switch (performanceFilter.toLowerCase()) {
            case "fast": return timeMs < 20000; // < 20 seconds
            case "medium": return timeMs >= 20000 && timeMs < 35000; // 20-35 seconds  
            case "slow": return timeMs >= 35000; // > 35 seconds
            case "unknown": return false;
            default: return true;
        }
    }
    
    private List<ToolDescription> applySorting(List<ToolDescription> descriptions, String sortBy, String sortDir) {
        Comparator<ToolDescription> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        return descriptions.stream().sorted(comparator).collect(Collectors.toList());
    }
    
    private Comparator<ToolDescription> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "toolname":
                return Comparator.comparing(ToolDescription::getToolName);
            case "providermodel":
                return Comparator.comparing(ToolDescription::getProviderModel);
            case "generationtime":
                return Comparator.comparing(desc -> desc.getGenerationTimeMs() != null ? desc.getGenerationTimeMs() : 0L);
            case "usagecount":
                return Comparator.comparing(ToolDescription::getUsageCount);
            case "lastusedat":
                return Comparator.comparing(desc -> desc.getLastUsedAt() != null ? desc.getLastUsedAt() : LocalDateTime.MIN);
            case "createdat":
            default:
                return Comparator.comparing(ToolDescription::getCreatedAt);
        }
    }
    
    private Map<String, Object> calculateToolStatistics(List<ToolDescription> descriptions) {
        Map<String, Object> stats = new HashMap<>();
        
        if (descriptions.isEmpty()) {
            stats.put("totalTools", 0);
            stats.put("totalProviders", 0);
            stats.put("averageGenerationTime", 0.0);
            stats.put("totalUsage", 0);
            stats.put("fastestTool", null);
            stats.put("mostUsedTool", null);
            return stats;
        }
        
        // Basic counts
        stats.put("totalTools", descriptions.size());
        stats.put("totalProviders", descriptions.stream().map(ToolDescription::getProviderModel).collect(Collectors.toSet()).size());
        
        // Generation time stats
        double avgGenTime = descriptions.stream()
                .mapToLong(d -> d.getGenerationTimeMs() != null ? d.getGenerationTimeMs() : 0)
                .average().orElse(0.0);
        stats.put("averageGenerationTime", Math.round(avgGenTime));
        
        // Usage stats
        int totalUsage = descriptions.stream().mapToInt(ToolDescription::getUsageCount).sum();
        stats.put("totalUsage", totalUsage);
        
        // Performance insights
        Optional<ToolDescription> fastestTool = descriptions.stream()
                .filter(d -> d.getGenerationTimeMs() != null)
                .min(Comparator.comparing(ToolDescription::getGenerationTimeMs));
        stats.put("fastestTool", fastestTool.orElse(null));
        
        Optional<ToolDescription> mostUsedTool = descriptions.stream()
                .filter(d -> d.getUsageCount() > 0)
                .max(Comparator.comparing(ToolDescription::getUsageCount));
        stats.put("mostUsedTool", mostUsedTool.orElse(null));
        
        return stats;
    }
    
    private Set<String> getAvailableProviders(List<ToolDescription> descriptions) {
        return descriptions.stream()
                .map(ToolDescription::getProviderModel)
                .collect(Collectors.toSet());
    }
    
    private Set<String> getAvailableTools(List<ToolDescription> descriptions) {
        return descriptions.stream()
                .map(ToolDescription::getToolName)
                .collect(Collectors.toSet());
    }
    
    private Map<String, Object> createPaginationInfo(int page, int size, int totalElements) {
        Map<String, Object> pagination = new HashMap<>();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        pagination.put("currentPage", page);
        pagination.put("pageSize", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages - 1);
        pagination.put("hasPrev", page > 0);
        pagination.put("nextPage", page + 1);
        pagination.put("prevPage", page - 1);
        
        return pagination;
    }
    
    private Map<String, String> createCurrentFilters(String providerFilter, String toolFilter, String performanceFilter) {
        Map<String, String> filters = new HashMap<>();
        filters.put("provider", providerFilter != null ? providerFilter : "");
        filters.put("tool", toolFilter != null ? toolFilter : "");
        filters.put("performance", performanceFilter != null ? performanceFilter : "");
        return filters;
    }
    
    private Map<String, String> createSortInfo(String sortBy, String sortDir) {
        Map<String, String> sortInfo = new HashMap<>();
        sortInfo.put("sortBy", sortBy);
        sortInfo.put("sortDir", sortDir);
        return sortInfo;
    }
    
    private Map<String, Object> createEmptyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTools", 0);
        stats.put("totalProviders", 0);
        stats.put("averageGenerationTime", 0.0);
        stats.put("totalUsage", 0);
        stats.put("fastestTool", null);
        stats.put("mostUsedTool", null);
        return stats;
    }
}