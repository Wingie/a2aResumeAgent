package io.wingie.service;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BenchmarkTaskTemplate {
    private String name;
    private String description;
    private String prompt;
    private String expectedResult;
    private Double maxScore;
    private String evaluationCriteria;
    private String category;
    private Integer difficultyLevel; // 1-5 scale
    private List<String> tags;
    private Integer timeoutSeconds;
    
    // Factory methods for common task types
    public static BenchmarkTaskTemplate createNavigationTask(String name, String url, String expectedElement) {
        return BenchmarkTaskTemplate.builder()
            .name(name)
            .description("Navigate to a website and verify specific elements")
            .prompt("Navigate to " + url + " and verify that the page contains: " + expectedElement)
            .expectedResult(expectedElement)
            .maxScore(10.0)
            .evaluationCriteria("Page loads successfully and expected element is present")
            .category("navigation")
            .difficultyLevel(1)
            .tags(List.of("navigation", "basic", "web"))
            .timeoutSeconds(60)
            .build();
    }
    
    public static BenchmarkTaskTemplate createSearchTask(String name, String searchEngine, String query, String expectedResult) {
        return BenchmarkTaskTemplate.builder()
            .name(name)
            .description("Perform a search and verify results")
            .prompt("Go to " + searchEngine + " and search for '" + query + "'. Verify that the results contain: " + expectedResult)
            .expectedResult(expectedResult)
            .maxScore(15.0)
            .evaluationCriteria("Search is performed successfully and expected result appears in search results")
            .category("search")
            .difficultyLevel(2)
            .tags(List.of("search", "interaction", "web"))
            .timeoutSeconds(90)
            .build();
    }
    
    public static BenchmarkTaskTemplate createFormTask(String name, String url, String formData, String expectedOutcome) {
        return BenchmarkTaskTemplate.builder()
            .name(name)
            .description("Fill out and submit a form")
            .prompt("Navigate to " + url + " and fill out the form with: " + formData + ". Submit the form and verify: " + expectedOutcome)
            .expectedResult(expectedOutcome)
            .maxScore(20.0)
            .evaluationCriteria("Form is filled correctly, submitted successfully, and expected outcome is achieved")
            .category("form_filling")
            .difficultyLevel(3)
            .tags(List.of("forms", "interaction", "complex"))
            .timeoutSeconds(120)
            .build();
    }
    
    public static BenchmarkTaskTemplate createDataExtractionTask(String name, String url, String dataToExtract) {
        return BenchmarkTaskTemplate.builder()
            .name(name)
            .description("Extract specific data from a webpage")
            .prompt("Navigate to " + url + " and extract the following information: " + dataToExtract)
            .expectedResult(dataToExtract)
            .maxScore(15.0)
            .evaluationCriteria("Correct data is extracted from the specified webpage")
            .category("data_extraction")
            .difficultyLevel(2)
            .tags(List.of("extraction", "scraping", "data"))
            .timeoutSeconds(90)
            .build();
    }
    
    public static BenchmarkTaskTemplate createComplexInteractionTask(String name, String description, String prompt, 
                                                           String expectedResult, Integer difficulty) {
        return BenchmarkTaskTemplate.builder()
            .name(name)
            .description(description)
            .prompt(prompt)
            .expectedResult(expectedResult)
            .maxScore(25.0)
            .evaluationCriteria("Complex multi-step interaction completed successfully")
            .category("complex_interaction")
            .difficultyLevel(difficulty)
            .tags(List.of("complex", "multi-step", "advanced"))
            .timeoutSeconds(300)
            .build();
    }
}