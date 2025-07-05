# Tutorial 5: AI-Powered Web Automation with Playwright and a2acore

## 1. Introduction

Modern web automation has evolved beyond traditional scripting approaches. The a2aTravelAgent project demonstrates how AI-powered automation using Microsoft Playwright can create intelligent web interactions that understand visual content and respond to natural language instructions.

This tutorial shows how to build sophisticated web automation using the a2acore framework with Playwright for browser control and AI for visual understanding.

## 2. Understanding the Architecture

### 2.1 Technology Stack Overview
```
a2awebagent Architecture:
├── a2acore/                    # Fast MCP Framework
│   ├── @EnableA2ACore         # Autoconfiguration
│   ├── @Agent & @Action       # Tool annotations
│   └── JsonRpcHandler         # MCP protocol
├── a2awebapp/                 # Web Automation Application
│   ├── PlaywrightProcessor    # Browser automation interface
│   ├── PlaywrightActions      # Web automation service
│   └── ScreenshotUtils        # Visual capture utilities
└── PostgreSQL                 # Tool description caching
```

### 2.2 Why Playwright Over Traditional Approaches

**Traditional Selenium Limitations:**
- Brittle element selectors tied to DOM structure
- Manual maintenance when UI changes
- Limited browser compatibility
- Complex setup and driver management

**Playwright + AI Advantages:**
- **Natural language instructions**: "Go to booking.com and search for hotels in Paris"
- **Visual understanding**: AI interprets screenshots and extracts structured data
- **Modern browser support**: Chromium, Firefox, Safari with consistent API
- **Container-ready**: Built for Docker and CI/CD environments
- **Auto-waiting**: Intelligent waiting for page states and elements

## 3. Setting Up Playwright Web Automation

### 3.1 Project Dependencies
```xml
<!-- a2awebapp/pom.xml -->
<dependency>
    <groupId>io.wingie</groupId>
    <artifactId>a2acore</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.51.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.4</version>
</dependency>
```

### 3.2 Playwright Configuration
```java
package io.wingie.config;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PlaywrightConfig {
    
    @Bean
    public Playwright playwright() {
        log.info("Initializing Playwright");
        return Playwright.create();
    }
    
    @Bean
    public Browser browser(Playwright playwright) {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setTimeout(30_000);
            
        // Docker compatibility arguments
        if (isRunningInDocker()) {
            launchOptions.setArgs(java.util.List.of(
                "--no-sandbox",
                "--disable-dev-shm-usage", 
                "--disable-gpu",
                "--disable-web-security"
            ));
        }
        
        Browser browser = playwright.chromium().launch(launchOptions);
        log.info("Playwright browser created successfully");
        return browser;
    }
    
    private boolean isRunningInDocker() {
        return System.getenv("DOCKER_CONTAINER") != null;
    }
}
```

## 4. Creating AI-Powered Web Automation Agents

### 4.1 Basic Web Automation Agent
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.microsoft.playwright.*;

@Service
@Agent(name = "web-automation", 
       description = "AI-powered web automation using Playwright")
public class PlaywrightWebAgent {
    
    @Autowired
    private Browser browser;
    
    @Action(description = "Navigate to a website and extract information using natural language")
    public String browseAndExtract(
        @Parameter(description = "Natural language instructions for web interaction") 
        String instructions) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            
            // AI interprets the instructions and executes web actions
            return processWebInstructions(page, instructions);
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @Action(description = "Take a screenshot of a website and analyze the content")
    public String captureAndAnalyze(
        @Parameter(description = "Website URL to capture") String url) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Capture screenshot
            byte[] screenshot = page.screenshot();
            
            // Save and analyze with AI
            String screenshotPath = saveScreenshot(screenshot);
            return analyzeScreenshot(screenshotPath);
            
        } catch (Exception e) {
            return "Screenshot failed: " + e.getMessage();
        }
    }
}
```

### 4.2 Advanced Travel Research Agent
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "travel-research", 
       description = "Comprehensive travel research with booking site automation")
public class TravelResearchAgent {
    
    @Autowired
    private PlaywrightProcessor playwrightProcessor;
    
    @Action(description = "Research complete travel options including flights, hotels, and attractions")
    public String researchTravel(
        @Parameter(description = "Origin city") String origin,
        @Parameter(description = "Destination city") String destination,
        @Parameter(description = "Travel date (YYYY-MM-DD)") String travelDate) {
        
        // Use the web.action template system
        String webActionScript = loadTravelResearchScript();
        
        // Replace placeholders with actual values
        webActionScript = webActionScript
            .replace("{origin_city}", origin)
            .replace("{destination_city}", destination)
            .replace("{travel_date}", travelDate);
            
        // Execute the research workflow
        return playwrightProcessor.processSteps(webActionScript);
    }
    
    @Action(description = "Extract pricing information from booking websites")
    public TravelPricing extractPricing(
        @Parameter(description = "Booking website screenshot") String screenshotPath) {
        
        // AI-powered price extraction from visual content
        return aiProcessor.imageToPojo(screenshotPath, TravelPricing.class);
    }
}
```

## 5. Visual Content Analysis with AI

### 5.1 Structured Data Extraction from Screenshots

**Define your data structure:**
```java
@Getter
@Setter
@NoArgsConstructor
@ToString
public class HotelSearchResults {
    @ListType(HotelOption.class)
    List<HotelOption> hotels;
    
    String searchLocation;
    String checkInDate;
    String checkOutDate;
}

@Getter
@Setter  
@NoArgsConstructor
@ToString
public class HotelOption {
    String hotelName;
    Double pricePerNight;
    Double rating;
    String location;
    List<String> amenities;
}
```

**Extract structured data from hotel booking screenshots:**
```java
@Action(description = "Extract hotel search results from booking.com screenshot")
public HotelSearchResults extractHotelData(
    @Parameter(description = "Screenshot of hotel search results") String screenshotPath) {
    
    try {
        // AI processes the screenshot and extracts structured data
        HotelSearchResults results = aiProcessor.imageToPojo(
            screenshotPath, HotelSearchResults.class);
            
        // Store results in PostgreSQL for caching
        hotelSearchRepository.save(results);
        
        return results;
        
    } catch (Exception e) {
        log.error("Failed to extract hotel data", e);
        return new HotelSearchResults();
    }
}
```

### 5.2 Real-World Example: Flight Price Monitoring

**Capture flight search results:**
```java
@Action(description = "Monitor flight prices and capture changes")
public FlightPriceReport monitorFlightPrices(
    @Parameter(description = "Flight route (e.g., 'JFK to LAX')") String route,
    @Parameter(description = "Monitoring duration in days") int durationDays) {
    
    FlightPriceReport report = new FlightPriceReport();
    report.setRoute(route);
    report.setMonitoringStarted(LocalDateTime.now());
    
    try (BrowserContext context = browser.newContext()) {
        Page page = context.newPage();
        
        // Navigate to flight search
        page.navigate("https://www.google.com/flights");
        
        // AI-powered interaction: enter search criteria
        String searchInstructions = String.format(
            "Search for flights %s for tomorrow", route);
            
        processWebInstructions(page, searchInstructions);
        
        // Wait for results and capture
        page.waitForSelector("[data-testid='flight-results']", 
            new Page.WaitForSelectorOptions().setTimeout(30000));
            
        byte[] screenshot = page.screenshot();
        String screenshotPath = saveScreenshot(screenshot);
        
        // Extract pricing data with AI
        FlightPricing pricing = aiProcessor.imageToPojo(
            screenshotPath, FlightPricing.class);
            
        report.addPriceSnapshot(pricing);
        
        return report;
        
    } catch (Exception e) {
        report.addError("Monitoring failed: " + e.getMessage());
        return report;
    }
}
```

## 6. Integration with MCP Protocol

### 6.1 Exposing Web Automation as MCP Tools

**Enable automatic tool discovery:**
```java
package io.wingie;

import io.wingie.a2acore.config.EnableA2ACore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableA2ACore  // Automatically exposes @Action methods as MCP tools
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Generated MCP tool from Playwright action:**
```json
{
    "name": "browseAndExtract",
    "description": "Navigate to a website and extract information using natural language",
    "inputSchema": {
        "type": "object",
        "properties": {
            "instructions": {
                "type": "string",
                "description": "Natural language instructions for web interaction"
            }
        },
        "required": ["instructions"]
    }
}
```

### 6.2 Testing Web Automation via MCP

**Test via JSON-RPC:**
```bash
curl -X POST http://localhost:7860/v1 \
-H "Content-Type: application/json" \
-d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseAndExtract",
        "arguments": {
            "instructions": "Go to booking.com, search for hotels in Paris for next week, and extract the top 3 hotel names with prices"
        }
    },
    "id": 1
}'
```

**Expected response:**
```json
{
    "jsonrpc": "2.0",
    "result": "Found 3 top hotels: 1) Hotel du Louvre (€299/night), 2) Le Meurice (€450/night), 3) Hotel Plaza Athénée (€650/night). All hotels have excellent ratings and central locations.",
    "id": 1
}
```

## 7. Advanced Automation Patterns

### 7.1 Multi-Site Price Comparison
```java
@Action(description = "Compare prices across multiple travel booking sites")
public PriceComparisonReport compareFlightPrices(
    @Parameter(description = "Flight search criteria") FlightSearchCriteria criteria) {
    
    List<String> sites = List.of(
        "https://www.google.com/flights",
        "https://www.kayak.com",
        "https://www.expedia.com"
    );
    
    PriceComparisonReport report = new PriceComparisonReport();
    
    for (String site : sites) {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(site);
            
            // AI-powered site-specific search
            String searchInstructions = buildSearchInstructions(criteria, site);
            String results = processWebInstructions(page, searchInstructions);
            
            // Extract pricing with AI
            FlightPricing pricing = extractPricingFromText(results);
            report.addSiteResult(site, pricing);
            
        } catch (Exception e) {
            report.addSiteError(site, e.getMessage());
        }
    }
    
    return report;
}
```

### 7.2 Dynamic Content Handling
```java
@Action(description = "Handle dynamic content loading and infinite scroll")
public String extractDynamicContent(
    @Parameter(description = "URL with dynamic content") String url,
    @Parameter(description = "Content type to extract") String contentType) {
    
    try (BrowserContext context = browser.newContext()) {
        Page page = context.newPage();
        page.navigate(url);
        
        // Wait for initial content
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Handle infinite scroll or dynamic loading
        if (contentType.contains("infinite-scroll")) {
            simulateInfiniteScroll(page);
        }
        
        // Extract all content after dynamic loading
        String instructions = String.format(
            "Extract all %s from this page, including dynamically loaded content", 
            contentType);
            
        return processWebInstructions(page, instructions);
        
    } catch (Exception e) {
        return "Dynamic content extraction failed: " + e.getMessage();
    }
}

private void simulateInfiniteScroll(Page page) {
    // Scroll to bottom multiple times to trigger content loading
    for (int i = 0; i < 5; i++) {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        page.waitForTimeout(2000); // Wait for content to load
    }
}
```

## 8. Performance and Reliability

### 8.1 Browser Resource Management
```java
@Configuration
public class PlaywrightPerformanceConfig {
    
    @Bean
    public BrowserContext createOptimizedContext(Browser browser) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setLocale("en-US")
            .setTimezoneId("America/New_York")
            // Block unnecessary resources for faster loading
            .setExtraHTTPHeaders(Map.of(
                "Accept-Language", "en-US,en;q=0.9"
            ));
            
        BrowserContext context = browser.newContext(options);
        
        // Block ads and tracking to improve performance
        context.route("**/*.{png,jpg,jpeg,gif,svg,css}", Route::abort);
        
        return context;
    }
}
```

### 8.2 Error Handling and Retry Logic
```java
@Component
public class RobustWebAutomation {
    
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String executeWithRetry(String instructions) {
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            
            // Set up error handlers
            page.onDialog(dialog -> dialog.accept());
            page.onPageError(error -> log.warn("Page error: {}", error));
            
            return processWebInstructions(page, instructions);
            
        } catch (TimeoutException e) {
            log.warn("Timeout occurred, retrying...");
            throw new RuntimeException("Web automation timeout", e);
        }
    }
}
```

## 9. Database Integration and Caching

### 9.1 Caching Web Automation Results
```java
@Entity
@Table(name = "web_automation_results")
public class WebAutomationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String instructionsHash;  // MD5 of instructions
    private String resultData;        // JSON result
    private String screenshotPath;    // Path to screenshot
    private LocalDateTime executedAt;
    private Long executionTimeMs;
    
    // Getters and setters
}

@Service
public class WebAutomationCacheService {
    
    @Autowired
    private WebAutomationResultRepository repository;
    
    public Optional<String> getCachedResult(String instructions) {
        String hash = generateHash(instructions);
        return repository.findByInstructionsHashAndExecutedAtAfter(
            hash, LocalDateTime.now().minusHours(24))
            .map(WebAutomationResult::getResultData);
    }
    
    public void cacheResult(String instructions, String result, String screenshotPath) {
        WebAutomationResult cached = new WebAutomationResult();
        cached.setInstructionsHash(generateHash(instructions));
        cached.setResultData(result);
        cached.setScreenshotPath(screenshotPath);
        cached.setExecutedAt(LocalDateTime.now());
        
        repository.save(cached);
    }
}
```

## 10. Docker and Production Deployment

### 10.1 Docker Configuration for Playwright
```dockerfile
# Dockerfile
FROM mcr.microsoft.com/playwright/java:v1.51.0-focal

WORKDIR /app

# Copy application
COPY target/a2awebapp-0.0.1.jar app.jar

# Install Playwright browsers
RUN npx playwright install chromium

# Environment for browser compatibility
ENV DOCKER_CONTAINER=true
ENV DISPLAY=:99

EXPOSE 7860

CMD ["java", "-jar", "app.jar"]
```

### 10.2 Docker Compose Integration
```yaml
# docker-compose.yml
version: '3.8'
services:
  a2awebagent:
    build: .
    ports:
      - "7860:7860"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DOCKER_CONTAINER=true
    depends_on:
      - postgres
      - redis
    volumes:
      - ./screenshots:/app/screenshots
      
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: a2awebagent
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
volumes:
  postgres_data:
```

## 11. Testing and Validation

### 11.1 Automated Testing Suite
```java
@SpringBootTest
@Testcontainers
class PlaywrightWebAutomationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
        
    @Autowired
    private TravelResearchAgent travelAgent;
    
    @Test
    void testTravelResearchIntegration() {
        String result = travelAgent.researchTravel(
            "New York", "Paris", "2025-08-15");
            
        assertThat(result).contains("flight");
        assertThat(result).contains("hotel");
        assertThat(result).contains("price");
    }
    
    @Test
    void testScreenshotAnalysis() {
        String result = travelAgent.extractPricing("test-screenshot.png");
        assertThat(result).isNotEmpty();
    }
}
```

## 12. Conclusion

The combination of Playwright's modern browser automation with AI-powered visual understanding creates a powerful platform for intelligent web automation. Key advantages of this approach:

### 12.1 Benefits Over Traditional Automation
- **Natural Language Control**: No need to write complex selectors
- **Visual Understanding**: AI interprets screenshots like a human would
- **Resilient to UI Changes**: Instructions adapt to layout modifications
- **Multi-Site Compatibility**: Same approach works across different websites
- **Structured Data Extraction**: Convert visual content to typed objects

### 12.2 Production Readiness
- **Docker-compatible**: Runs consistently across environments
- **Caching System**: PostgreSQL stores results for performance
- **MCP Integration**: Exposes capabilities to AI assistants
- **Error Handling**: Robust retry logic and failure recovery
- **Performance Optimized**: Resource management and selective loading

### 12.3 Future Enhancements
- **Machine Learning**: Train models on specific website patterns
- **Multi-language Support**: International site automation
- **Real-time Monitoring**: Continuous price tracking and alerts
- **Advanced Analytics**: Pattern recognition in extracted data

This tutorial demonstrates how the a2aTravelAgent project transforms traditional web automation into an intelligent, AI-driven system that can understand and interact with websites using natural language instructions and visual analysis.