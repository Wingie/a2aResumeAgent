package io.wingie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BenchmarkDefinitionService {
    
    private final Map<String, List<BenchmarkTaskTemplate>> benchmarks;
    private final Map<String, String> benchmarkVersions;
    
    public BenchmarkDefinitionService() {
        this.benchmarks = new HashMap<>();
        this.benchmarkVersions = new HashMap<>();
        initializeBenchmarks();
    }
    
    private void initializeBenchmarks() {
        log.info("Initializing benchmark definitions");
        
        // Basic Web Navigation Benchmark
        createBasicWebNavigationBenchmark();
        
        // Search Engine Benchmark
        createSearchEngineBenchmark();
        
        // E-commerce Interaction Benchmark
        createEcommerceBenchmark();
        
        // Form Filling Benchmark
        createFormFillingBenchmark();
        
        // Travel Research Benchmark
        createTravelResearchBenchmark();
        
        // Advanced Web Automation Benchmark
        createAdvancedAutomationBenchmark();
        
        log.info("Initialized {} benchmarks", benchmarks.size());
    }
    
    private void createBasicWebNavigationBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createNavigationTask(
            "Navigate to Google",
            "https://google.com", 
            "Google search box"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createNavigationTask(
            "Navigate to GitHub",
            "https://github.com", 
            "Sign in button"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createNavigationTask(
            "Navigate to Stack Overflow",
            "https://stackoverflow.com", 
            "Questions link"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createDataExtractionTask(
            "Extract current time",
            "https://time.is",
            "current time"
        ));
        
        benchmarks.put("basic_navigation", tasks);
        benchmarkVersions.put("basic_navigation", "1.0");
    }
    
    private void createSearchEngineBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createSearchTask(
            "Google search for a2ajava",
            "https://google.com",
            "a2ajava",
            "GitHub repository"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createSearchTask(
            "DuckDuckGo search for Spring Boot",
            "https://duckduckgo.com",
            "Spring Boot tutorial",
            "Spring Boot documentation"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createSearchTask(
            "Bing search for Playwright",
            "https://bing.com",
            "Playwright automation",
            "Microsoft Playwright"
        ));
        
        benchmarks.put("search_engines", tasks);
        benchmarkVersions.put("search_engines", "1.0");
    }
    
    private void createEcommerceBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createSearchTask(
            "Amazon product search",
            "https://amazon.com",
            "iPhone 15",
            "iPhone 15 price"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "Add item to cart",
            "Navigate to Amazon, search for a product, and add it to cart",
            "Go to Amazon.com, search for 'wireless headphones', find a product under $50, and add it to your cart",
            "Item added to cart",
            3
        ));
        
        tasks.add(BenchmarkTaskTemplate.createDataExtractionTask(
            "Extract product price",
            "https://amazon.com",
            "price of first search result for 'laptop'"
        ));
        
        benchmarks.put("ecommerce", tasks);
        benchmarkVersions.put("ecommerce", "1.0");
    }
    
    private void createFormFillingBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createFormTask(
            "Contact form submission",
            "https://httpbin.org/forms/post",
            "Name: John Doe, Email: john@example.com, Subject: Test",
            "form submitted successfully"
        ));
        
        tasks.add(BenchmarkTaskTemplate.createFormTask(
            "Newsletter signup",
            "https://the-internet.herokuapp.com",
            "Email: test@example.com",
            "subscription confirmation"
        ));
        
        benchmarks.put("form_filling", tasks);
        benchmarkVersions.put("form_filling", "1.0");
    }
    
    private void createTravelResearchBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "Flight search on Expedia",
            "Search for flights between two cities",
            "Go to Expedia.com and search for round-trip flights from New York to Los Angeles for next month. Find the cheapest option.",
            "flight prices displayed",
            4
        ));
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "Hotel search on Booking.com",
            "Search for hotels in a specific city",
            "Go to Booking.com and search for hotels in Paris for a 3-night stay next month. Sort by price and find options under $200/night.",
            "hotel results sorted by price",
            3
        ));
        
        tasks.add(BenchmarkTaskTemplate.createDataExtractionTask(
            "Extract travel deals",
            "https://kayak.com",
            "current travel deals or promotions"
        ));
        
        benchmarks.put("travel_research", tasks);
        benchmarkVersions.put("travel_research", "1.0");
    }
    
    private void createAdvancedAutomationBenchmark() {
        List<BenchmarkTaskTemplate> tasks = new ArrayList<>();
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "Multi-tab navigation",
            "Open multiple tabs and compare information",
            "Open Google in one tab and Bing in another. Search for 'weather today' in both and compare the results.",
            "weather information from both search engines",
            4
        ));
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "Dynamic content interaction",
            "Interact with dynamically loaded content",
            "Go to a news website, scroll down to load more articles, and click on the third article that appears after scrolling.",
            "article content loaded",
            5
        ));
        
        tasks.add(BenchmarkTaskTemplate.createComplexInteractionTask(
            "File download simulation",
            "Navigate to a file download page and interact with download elements",
            "Go to a website that offers file downloads, find a PDF document, and initiate the download process.",
            "download initiated successfully",
            3
        ));
        
        benchmarks.put("advanced_automation", tasks);
        benchmarkVersions.put("advanced_automation", "1.0");
    }
    
    // Public API methods
    
    public List<String> getAvailableBenchmarks() {
        return new ArrayList<>(benchmarks.keySet());
    }
    
    public List<BenchmarkTaskTemplate> getBenchmarkTasks(String benchmarkName) {
        List<BenchmarkTaskTemplate> tasks = benchmarks.get(benchmarkName);
        if (tasks == null) {
            throw new IllegalArgumentException("Unknown benchmark: " + benchmarkName);
        }
        return new ArrayList<>(tasks); // Return a copy
    }
    
    public String getBenchmarkVersion(String benchmarkName) {
        return benchmarkVersions.getOrDefault(benchmarkName, "1.0");
    }
    
    public Map<String, Object> getBenchmarkInfo(String benchmarkName) {
        List<BenchmarkTaskTemplate> tasks = getBenchmarkTasks(benchmarkName);
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", benchmarkName);
        info.put("version", getBenchmarkVersion(benchmarkName));
        info.put("totalTasks", tasks.size());
        info.put("maxPossibleScore", tasks.stream().mapToDouble(BenchmarkTaskTemplate::getMaxScore).sum());
        info.put("categories", tasks.stream().map(BenchmarkTaskTemplate::getCategory).distinct().toList());
        info.put("difficultyRange", getDifficultyRange(tasks));
        info.put("estimatedDuration", getEstimatedDuration(tasks));
        
        return info;
    }
    
    public List<Map<String, Object>> getAllBenchmarksInfo() {
        return getAvailableBenchmarks().stream()
            .map(this::getBenchmarkInfo)
            .toList();
    }
    
    private Map<String, Integer> getDifficultyRange(List<BenchmarkTaskTemplate> tasks) {
        int min = tasks.stream().mapToInt(BenchmarkTaskTemplate::getDifficultyLevel).min().orElse(1);
        int max = tasks.stream().mapToInt(BenchmarkTaskTemplate::getDifficultyLevel).max().orElse(1);
        return Map.of("min", min, "max", max);
    }
    
    private int getEstimatedDuration(List<BenchmarkTaskTemplate> tasks) {
        return tasks.stream().mapToInt(BenchmarkTaskTemplate::getTimeoutSeconds).sum();
    }
    
    public boolean benchmarkExists(String benchmarkName) {
        return benchmarks.containsKey(benchmarkName);
    }
    
    public int getTaskCount(String benchmarkName) {
        List<BenchmarkTaskTemplate> tasks = benchmarks.get(benchmarkName);
        return tasks != null ? tasks.size() : 0;
    }
    
    public double getMaxScore(String benchmarkName) {
        List<BenchmarkTaskTemplate> tasks = benchmarks.get(benchmarkName);
        if (tasks == null) return 0.0;
        return tasks.stream().mapToDouble(BenchmarkTaskTemplate::getMaxScore).sum();
    }
}