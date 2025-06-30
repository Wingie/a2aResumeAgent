# Tutorial 2: Annotations Deep Dive - a2acore Framework and MCP Tool Development

## 1. Introduction

The a2acore framework provides a powerful annotation-driven approach to creating MCP (Model Context Protocol) tools and AI-powered agents. This comprehensive guide explores how to leverage annotations for building sophisticated web automation and travel research systems like a2aTravelAgent.

## 2. Core Annotation System

### 2.1 Essential Annotations Overview
The a2acore framework uses four key annotations to transform Spring Boot applications into MCP-compliant agent systems:

```java
import io.wingie.a2acore.annotations.*;
import io.wingie.a2acore.config.EnableA2ACore;

@EnableA2ACore              // Converts Spring Boot app into MCP server
@Agent(name, description)   // Creates agent groups for tool organization  
@Action(description)        // Marks methods as MCP-callable tools
@Parameter(description)     // Provides detailed parameter descriptions
```

### 2.2 Framework Architecture
```
a2acore Annotation Processing:
├── @EnableA2ACore          # Autoconfiguration
│   ├── ToolDiscoveryService    # Scans for @Action methods
│   ├── SchemaGenerator         # Creates JSON schemas  
│   └── A2aCoreController       # MCP endpoints (/v1/tools)
├── @Agent Classes          # Logical grouping of tools
├── @Action Methods         # Individual MCP tools
└── PostgreSQL Cache        # Performance optimization
```

## 3. Agent Creation with @Agent

### 3.1 Basic Agent Structure
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "travel-research", 
       description = "Comprehensive travel research and booking automation")
public class TravelResearchAgent {
    
    @Autowired
    private PlaywrightProcessor playwrightProcessor;
    
    @Autowired  
    private TravelDataService travelDataService;
    
    // Action methods defined here...
}
```

### 3.2 Agent Naming and Organization
```java
// Functional grouping approach
@Agent(name = "web-automation", 
       description = "Browser automation and web scraping tools")
public class WebAutomationAgent { }

@Agent(name = "data-extraction", 
       description = "Extract and structure data from web pages")  
public class DataExtractionAgent { }

@Agent(name = "travel-booking",
       description = "Automated travel booking and price monitoring")
public class TravelBookingAgent { }
```

### 3.3 Agent with Database Integration
```java
@Service
@Agent(name = "travel-analytics",
       description = "Travel data analytics and historical price analysis")
@Transactional
public class TravelAnalyticsAgent {
    
    @Autowired
    private TravelSearchRepository searchRepository;
    
    @Autowired
    private PriceHistoryRepository priceRepository;
    
    // Database-integrated actions...
}
```

## 4. Action Methods with @Action

### 4.1 Basic Action Definition
```java
@Action(description = "Browse websites and extract information using natural language instructions")
public String browseWebAndReturnText(
    @Parameter(description = "Natural language web browsing instructions") 
    String provideAllValuesInPlainEnglish) {
    
    try {
        return playwrightProcessor.processSteps(provideAllValuesInPlainEnglish);
    } catch (Exception e) {
        return "Web automation failed: " + e.getMessage();
    }
}
```

### 4.2 Complex Parameter Types
```java
@Action(description = "Research comprehensive travel options with detailed criteria")
public TravelResearchReport researchTravelWithCriteria(
    @Parameter(description = "Complete travel search criteria") 
    TravelSearchCriteria criteria) {
    
    // Framework automatically deserializes JSON to TravelSearchCriteria
    return travelService.conductResearch(criteria);
}

// Supporting POJO
@Data
public class TravelSearchCriteria {
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private int passengers;
    private String preferredClass;
    private List<String> preferredAirlines;
    private PriceRange priceRange;
}
```

### 4.3 Multiple Parameter Actions
```java
@Action(description = "Compare flight prices across multiple booking websites")
public FlightComparisonReport compareFlightPrices(
    @Parameter(description = "Origin airport code (e.g., 'JFK')") String origin,
    @Parameter(description = "Destination airport code (e.g., 'CDG')") String destination,
    @Parameter(description = "Departure date (YYYY-MM-DD)") String departureDate,
    @Parameter(description = "Return date (YYYY-MM-DD), optional") String returnDate,
    @Parameter(description = "Number of passengers") int passengers) {
    
    FlightSearchRequest request = FlightSearchRequest.builder()
        .origin(origin)
        .destination(destination)  
        .departureDate(LocalDate.parse(departureDate))
        .returnDate(returnDate != null ? LocalDate.parse(returnDate) : null)
        .passengers(passengers)
        .build();
        
    return flightComparisonService.compareAcrossSites(request);
}
```

## 5. Advanced Parameter Handling

### 5.1 Optional Parameters
```java
@Action(description = "Search for hotels with optional filters")
public HotelSearchResults searchHotels(
    @Parameter(description = "Destination city") String destination,
    @Parameter(description = "Check-in date (YYYY-MM-DD)") String checkIn,
    @Parameter(description = "Check-out date (YYYY-MM-DD)") String checkOut,
    @Parameter(description = "Number of guests (optional, default: 2)") Integer guests,
    @Parameter(description = "Price range filter (optional)") String priceRange,
    @Parameter(description = "Star rating filter (optional, 1-5)") Integer starRating) {
    
    HotelSearchRequest request = HotelSearchRequest.builder()
        .destination(destination)
        .checkIn(LocalDate.parse(checkIn))
        .checkOut(LocalDate.parse(checkOut))
        .guests(guests != null ? guests : 2)
        .priceRange(priceRange)
        .starRating(starRating)
        .build();
        
    return hotelSearchService.search(request);
}
```

### 5.2 Collection Parameters
```java
@Action(description = "Monitor prices for multiple flight routes simultaneously")
public List<PriceMonitoringResult> monitorMultipleRoutes(
    @Parameter(description = "List of flight routes to monitor") 
    List<FlightRoute> routes,
    @Parameter(description = "Monitoring duration in days") int durationDays) {
    
    return routes.stream()
        .map(route -> priceMonitoringService.startMonitoring(route, durationDays))
        .collect(Collectors.toList());
}

@Data
public class FlightRoute {
    private String origin;
    private String destination;
    private LocalDate travelDate;
    private String preferredTime;
}
```

### 5.3 Nested Object Parameters
```java
@Action(description = "Book complete travel package with flights and accommodation")
public BookingConfirmation bookTravelPackage(
    @Parameter(description = "Complete travel package details") 
    TravelPackageRequest packageRequest) {
    
    return bookingService.createPackageBooking(packageRequest);
}

@Data  
public class TravelPackageRequest {
    private PassengerDetails primaryPassenger;
    private List<PassengerDetails> additionalPassengers;
    private FlightPreferences flightPreferences;
    private AccommodationPreferences accommodationPreferences;
    private List<ActivityPreference> activityPreferences;
}

@Data
public class PassengerDetails {
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String passportNumber;
    private String nationality;
}
```

## 6. Generated MCP Tool Schemas

### 6.1 Simple Parameter Schema
```java
@Action(description = "Get flight status information")
public FlightStatus getFlightStatus(
    @Parameter(description = "Flight number (e.g., 'AA123')") String flightNumber,
    @Parameter(description = "Date (YYYY-MM-DD)") String date) {
    // Implementation
}
```

**Generated MCP Schema:**
```json
{
    "name": "getFlightStatus",
    "description": "Get flight status information",
    "inputSchema": {
        "type": "object",
        "properties": {
            "flightNumber": {
                "type": "string",
                "description": "Flight number (e.g., 'AA123')"
            },
            "date": {
                "type": "string", 
                "description": "Date (YYYY-MM-DD)"
            }
        },
        "required": ["flightNumber", "date"]
    }
}
```

### 6.2 Complex Object Schema  
```java
@Action(description = "Search for vacation packages")
public List<VacationPackage> searchVacationPackages(
    @Parameter(description = "Vacation search criteria") VacationSearchCriteria criteria) {
    // Implementation
}
```

**Generated MCP Schema:**
```json
{
    "name": "searchVacationPackages",
    "inputSchema": {
        "type": "object",
        "properties": {
            "criteria": {
                "type": "object",
                "properties": {
                    "destination": {"type": "string"},
                    "departureDate": {"type": "string"},
                    "duration": {"type": "integer"},
                    "budget": {
                        "type": "object",
                        "properties": {
                            "min": {"type": "number"},
                            "max": {"type": "number"},
                            "currency": {"type": "string"}
                        }
                    },
                    "travelers": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "age": {"type": "integer"},
                                "type": {"type": "string"}
                            }
                        }
                    }
                },
                "required": ["destination", "departureDate", "duration"]
            }
        },
        "required": ["criteria"]
    }
}
```

## 7. Database Integration with Annotations

### 7.1 Entity-Aware Actions
```java
@Service
@Agent(name = "travel-history", 
       description = "Manage and analyze travel history data")
public class TravelHistoryAgent {
    
    @Autowired
    private TravelSearchRepository travelSearchRepository;
    
    @Action(description = "Save travel search results for future reference")
    @Transactional
    public String saveTravelSearch(
        @Parameter(description = "Travel search results to save") 
        TravelSearchResults results) {
        
        TravelSearch entity = new TravelSearch();
        entity.setOrigin(results.getOrigin());
        entity.setDestination(results.getDestination());
        entity.setSearchDate(LocalDateTime.now());
        entity.setResults(JsonUtils.toJson(results));
        
        TravelSearch saved = travelSearchRepository.save(entity);
        return "Travel search saved with ID: " + saved.getId();
    }
    
    @Action(description = "Retrieve previous travel searches by destination")
    public List<TravelSearchSummary> getPreviousSearches(
        @Parameter(description = "Destination to search for") String destination) {
        
        return travelSearchRepository.findByDestinationContaining(destination)
            .stream()
            .map(this::convertToSummary)
            .collect(Collectors.toList());
    }
}
```

### 7.2 Caching with Annotations
```java
@Service
@Agent(name = "price-cache", 
       description = "Cached price lookup and comparison tools")
public class PriceCacheAgent {
    
    @Action(description = "Get cached flight prices for route")
    @Cacheable(value = "flight-prices", key = "#route + '-' + #date")
    public List<FlightPrice> getCachedFlightPrices(
        @Parameter(description = "Flight route (e.g., 'JFK-LAX')") String route,
        @Parameter(description = "Travel date (YYYY-MM-DD)") String date) {
        
        return priceService.getFlightPrices(route, date);
    }
    
    @Action(description = "Clear price cache for specific route")
    @CacheEvict(value = "flight-prices", key = "#route + '-*'")
    public String clearPriceCache(
        @Parameter(description = "Flight route to clear from cache") String route) {
        
        return "Price cache cleared for route: " + route;
    }
}
```

## 8. Asynchronous Processing

### 8.1 Long-Running Operations
```java
@Service
@Agent(name = "async-research", 
       description = "Asynchronous travel research and monitoring")
public class AsyncTravelAgent {
    
    @Action(description = "Start comprehensive travel research (async)")
    @Async
    public CompletableFuture<String> startAsyncTravelResearch(
        @Parameter(description = "Travel research parameters") 
        TravelResearchRequest request) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String results = conductResearch(request);
                
                // Store results in database
                ResearchResult entity = new ResearchResult();
                entity.setRequestId(request.getRequestId());
                entity.setResults(results);
                entity.setCompletedAt(LocalDateTime.now());
                researchResultRepository.save(entity);
                
                return "Research completed for request: " + request.getRequestId();
                
            } catch (Exception e) {
                return "Research failed: " + e.getMessage();
            }
        });
    }
    
    @Action(description = "Check status of async travel research")
    public ResearchStatus checkResearchStatus(
        @Parameter(description = "Research request ID") String requestId) {
        
        return researchStatusService.getStatus(requestId);
    }
}
```

### 8.2 Progress Callbacks
```java
@Action(description = "Monitor flight prices with progress updates")
public String monitorFlightPricesWithProgress(
    @Parameter(description = "Flight monitoring request") 
    FlightMonitoringRequest request) {
    
    String monitoringId = UUID.randomUUID().toString();
    
    // Start async monitoring with progress updates
    CompletableFuture.runAsync(() -> {
        for (int day = 1; day <= request.getDurationDays(); day++) {
            try {
                // Update progress
                progressService.updateProgress(monitoringId, 
                    "Day " + day + " of " + request.getDurationDays());
                
                // Check prices
                List<FlightPrice> prices = priceService.checkPrices(request);
                priceHistoryService.recordPrices(monitoringId, prices);
                
                // Wait until next check
                Thread.sleep(24 * 60 * 60 * 1000); // 24 hours
                
            } catch (Exception e) {
                progressService.updateProgress(monitoringId, "Error: " + e.getMessage());
                break;
            }
        }
        
        progressService.updateProgress(monitoringId, "Monitoring completed");
    });
    
    return "Flight price monitoring started with ID: " + monitoringId;
}
```

## 9. Error Handling and Validation

### 9.1 Input Validation
```java
@Service
@Agent(name = "validated-booking", 
       description = "Travel booking with comprehensive validation")
public class ValidatedBookingAgent {
    
    @Action(description = "Book flight with validation")
    public BookingResult bookFlightWithValidation(
        @Parameter(description = "Flight booking details") 
        @Valid FlightBookingRequest request) {
        
        // Validation happens automatically via @Valid
        // Custom validation logic
        if (request.getDepartureDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Departure date cannot be in the past");
        }
        
        if (request.getPassengers().isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }
        
        return bookingService.bookFlight(request);
    }
}

@Data
public class FlightBookingRequest {
    @NotBlank(message = "Origin is required")
    private String origin;
    
    @NotBlank(message = "Destination is required") 
    private String destination;
    
    @Future(message = "Departure date must be in the future")
    private LocalDate departureDate;
    
    @NotEmpty(message = "Passenger list cannot be empty")
    @Valid
    private List<PassengerDetails> passengers;
}
```

### 9.2 Exception Handling
```java
@Service
@Agent(name = "robust-search", 
       description = "Search tools with comprehensive error handling")
public class RobustSearchAgent {
    
    @Action(description = "Search flights with retry logic") 
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public FlightSearchResults searchFlightsWithRetry(
        @Parameter(description = "Flight search parameters") 
        FlightSearchRequest request) {
        
        try {
            return flightSearchService.search(request);
            
        } catch (TemporaryServiceException e) {
            // This will trigger retry
            throw e;
            
        } catch (Exception e) {
            // Log error and return user-friendly message
            log.error("Flight search failed", e);
            throw new FlightSearchException("Unable to search flights at this time", e);
        }
    }
    
    @Recover
    public FlightSearchResults recoverFromSearchFailure(
        FlightSearchException ex, FlightSearchRequest request) {
        
        // Return cached results or default response
        return flightSearchService.getCachedResults(request)
            .orElse(FlightSearchResults.empty("Search temporarily unavailable"));
    }
}
```

## 10. Performance and Monitoring

### 10.1 Metrics Collection
```java
@Service
@Agent(name = "monitored-travel", 
       description = "Travel tools with performance monitoring")
public class MonitoredTravelAgent {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Action(description = "Search hotels with performance tracking")
    public HotelSearchResults searchHotelsWithMetrics(
        @Parameter(description = "Hotel search criteria") 
        HotelSearchRequest request) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            HotelSearchResults results = hotelSearchService.search(request);
            
            // Record successful search
            meterRegistry.counter("hotel.search.success", 
                "destination", request.getDestination()).increment();
                
            return results;
            
        } catch (Exception e) {
            // Record failed search
            meterRegistry.counter("hotel.search.failure", 
                "destination", request.getDestination(),
                "error", e.getClass().getSimpleName()).increment();
            throw e;
            
        } finally {
            sample.stop(Timer.builder("hotel.search.duration")
                .tag("destination", request.getDestination())
                .register(meterRegistry));
        }
    }
}
```

### 10.2 Tool Description Caching
```java
@Service
@Agent(name = "cached-descriptions", 
       description = "Tools with AI-enhanced descriptions")
public class CachedDescriptionAgent {
    
    @Action(description = "Search with AI-enhanced descriptions")
    public SearchResults searchWithEnhancedDescriptions(
        @Parameter(description = "Search query") String query) {
        
        // Tool descriptions are automatically cached in PostgreSQL
        // for improved startup performance
        return searchService.search(query);
    }
}
```

## 11. Testing Annotated Actions

### 11.1 Unit Testing
```java
@SpringBootTest
class TravelResearchAgentTest {
    
    @Autowired
    private TravelResearchAgent travelAgent;
    
    @MockBean
    private PlaywrightProcessor playwrightProcessor;
    
    @Test
    void testTravelResearch() {
        // Given
        when(playwrightProcessor.processSteps(anyString()))
            .thenReturn("Mock travel research results");
            
        // When  
        String result = travelAgent.researchTravelOptions(
            "New York", "Paris", "2025-08-15");
            
        // Then
        assertThat(result).contains("travel research results");
        verify(playwrightProcessor).processSteps(contains("New York"));
    }
}
```

### 11.2 Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.profiles.active=test"})
class MCPIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    void testMCPToolGeneration() {
        // Test that @Action methods are converted to MCP tools
        String url = "http://localhost:" + port + "/v1/tools";
        
        ResponseEntity<ToolsResponse> response = restTemplate.getForEntity(
            url, ToolsResponse.class);
            
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTools())
            .extracting("name")
            .contains("researchTravelOptions", "browseWebAndReturnText");
    }
}
```

## 12. Best Practices

### 12.1 Annotation Guidelines
- **Clear Descriptions**: Use specific, actionable descriptions for @Action and @Parameter
- **Logical Grouping**: Organize related tools into coherent @Agent classes
- **Parameter Naming**: Use descriptive parameter names that match their purpose
- **Type Safety**: Prefer strongly-typed parameters over generic strings

### 12.2 Performance Considerations
- **Caching**: Use @Cacheable for expensive operations
- **Async Processing**: Use @Async for long-running tasks
- **Database Optimization**: Use @Transactional appropriately
- **Resource Management**: Properly handle browser contexts and database connections

### 12.3 Error Handling
- **Validation**: Use Bean Validation annotations for input validation
- **Recovery**: Implement @Recover methods for graceful failure handling
- **Monitoring**: Add metrics collection for production monitoring
- **User-Friendly Messages**: Return clear error messages for MCP clients

## 13. Conclusion

The a2acore annotation system provides a powerful, declarative approach to building MCP tools and AI-powered agents. Key benefits include:

### 13.1 Developer Productivity
- **Minimal Boilerplate**: Annotations eliminate manual MCP protocol handling
- **Automatic Schema Generation**: JSON schemas generated from method signatures
- **Spring Integration**: Full Spring Boot ecosystem compatibility
- **Type Safety**: Compile-time checking and automatic type conversion

### 13.2 Production Features
- **Performance Optimization**: PostgreSQL caching for tool descriptions
- **Error Handling**: Comprehensive exception handling and recovery
- **Monitoring**: Built-in metrics collection and health checks
- **Scalability**: Async processing and database integration

### 13.3 Flexibility
- **Complex Types**: Support for nested objects and collections
- **Validation**: Integrated Bean Validation support
- **Caching**: Multiple caching strategies available
- **Testing**: Easy unit and integration testing

This annotation-driven approach enables rapid development of sophisticated AI-powered applications while maintaining production-grade reliability and performance.