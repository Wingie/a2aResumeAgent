# Frequently Asked Questions (FAQ)

## 1. How do I set up the a2aTravelAgent project?

### 1.1 Understanding the Project Structure
This project has evolved into a sophisticated multi-module Spring Boot application:

```
a2aTravelAgent/
├── a2ajava/              # Legacy Library (Maven Central v0.1.9.6)
│   └── io.github.vishalmysore.*  
└── a2awebagent/          # Current Application (Multi-module)
    ├── a2acore/          # Fast MCP Framework Library
    │   └── io.wingie.a2acore.*
    └── a2awebapp/        # Spring Boot Web Automation App
        └── io.wingie.*
```

### 1.2 Project Dependencies
Add these to your `pom.xml` (multi-module structure):

```xml
<!-- Parent POM (a2awebagent/pom.xml) -->
<groupId>io.wingie</groupId>
<artifactId>a2awebagent</artifactId>
<version>0.0.1</version>
<packaging>pom</packaging>

<modules>
    <module>a2acore</module>
    <module>a2awebapp</module>
</modules>

<!-- Key Dependencies in a2awebapp -->
<dependency>
    <groupId>io.wingie</groupId>
    <artifactId>a2acore</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.4</version>
</dependency>
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.51.0</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### 1.3 Essential Configuration Files

**1. Application Configuration (`a2awebapp/src/main/resources/application.yml`):**
```yaml
server:
  port: 7860

spring:
  application:
    name: a2awebagent
  datasource:
    url: jdbc:postgresql://localhost:5432/a2awebagent
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379

a2a:
  persistence: database
```

**2. AI Provider Configuration (`tools4ai.properties`):**
```properties
# Primary Provider (Cost-optimized)
agent.provider=openrouter
openAiBaseURL=https://openrouter.ai/api/v1
openAiModelName=google/gemma-3n-e4b-it:free

# Tool Description Caching
cache.enabled=true
startup.provider=openrouter
web.automation.provider=openrouter
```

## 2. How do I create agents with the current architecture?

### 2.1 Modern Agent Creation Pattern

```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "travel-research", 
       description = "AI-powered travel research with web automation")
public class TravelAgent {
    
    @Action(description = "Research flights, hotels, and attractions for travel planning")
    public String researchTravel(
        @Parameter(description = "Origin city") String origin,
        @Parameter(description = "Destination city") String destination,
        @Parameter(description = "Travel date") String travelDate) {
        
        // Implementation using PlaywrightProcessor
        return "Complete travel research with pricing and recommendations";
    }
}
```

### 2.2 Spring Boot Main Application

```java
package io.wingie;

import io.wingie.a2acore.config.EnableA2ACore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableA2ACore  // Enables fast MCP framework
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```



## 3. How does the MCP protocol integration work?

### 3.1 Automatic Tool Discovery
With `@EnableA2ACore`, the framework automatically:

1. **Scans for @Action annotations** during startup
2. **Generates MCP tool definitions** with JSON schemas
3. **Caches descriptions in PostgreSQL** for performance
4. **Exposes via JSON-RPC endpoints** at `/v1`

### 3.2 MCP Tool Generation Process
```java
// Your action automatically becomes an MCP tool
@Service
@Agent(name = "web-automation")
public class WebAutomationAgent {
    
    @Action(description = "Browse web pages and extract information")
    public String browseWebAndReturnText(
        @Parameter(description = "Natural language web browsing instructions") 
        String instructions) {
        
        // Playwright automation implementation
        return playwrightProcessor.processSteps(instructions);
    }
}
```

**Generated MCP Tool:**
```json
{
    "name": "browseWebAndReturnText",
    "description": "Browse web pages and extract information",
    "inputSchema": {
        "type": "object",
        "properties": {
            "instructions": {
                "type": "string", 
                "description": "Natural language web browsing instructions"
            }
        },
        "required": ["instructions"]
    }
}
```

### 3.3 MCP Endpoints Available
```bash
# List all available tools
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/list", 
    "id": 1
}' http://localhost:7860/v1

# Execute a tool
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "instructions": "Go to booking.com and search for hotels in Paris"
        }
    },
    "id": 2
}' http://localhost:7860/v1
```

## 4. How do I connect to Claude Desktop?

### 4.1 Claude Desktop Configuration
Add to your `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
    "mcpServers": {
        "a2a-travel-agent": {
            "command": "node",
            "args": [
                "/Users/wingston/code/a2aTravelAgent/a2awebagent/a2awebapp/src/main/resources/mcpserver.js"
            ]
        }
    }
}
```

### 4.2 MCP Proxy Server
The included `mcpserver.js` provides:
- **JSON-RPC bridge** to Spring Boot application
- **WebSocket support** for real-time communication  
- **Error handling** and connection management
- **Claude Desktop compatibility**

## 5. How do I set up the development environment?

### 5.1 Docker Development (Recommended)
```bash
# Start all services (PostgreSQL, Redis, Neo4j, App)
cd a2awebagent
docker-compose up -d

# Rebuild just the application
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d

# View logs
docker-compose logs -f a2awebagent
```

### 5.2 Local Development
```bash
# Prerequisites: PostgreSQL, Redis running locally
cd a2awebagent

# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run -pl a2awebapp

# Alternative: Run specific module
cd a2awebapp
mvn spring-boot:run
```

### 5.3 Database Setup
```sql
-- PostgreSQL setup
CREATE DATABASE a2awebagent;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE a2awebagent TO postgres;

-- Tables are auto-created via JPA
```


## 6. How do I test the application?

### 6.1 Health Check and Basic Testing
```bash
# Check if application is running
curl http://localhost:7860/v1/health

# List available tools
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 1
}' http://localhost:7860/v1

# Test web automation tool
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Go to Google.com and search for travel to Paris"
        }
    },
    "id": 2
}' http://localhost:7860/v1
```

### 6.2 Travel Research Testing
```bash
# Test complete travel research workflow
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Research travel from Amsterdam to Palma on July 6th 2025. Find flights, hotels, and attractions."
        }
    },
    "id": 3
}' http://localhost:7860/v1
```

### 6.3 Screenshot Testing
```bash
# Test screenshot capabilities
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnImage",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Take a screenshot of booking.com homepage"
        }
    },
    "id": 4
}' http://localhost:7860/v1
```

## 7. What are the available processors and integrations?

### 7.1 Web Automation Processors
- **`PlaywrightProcessor`**: Modern browser automation (replaces Selenium)
- **`PlaywrightWebBrowsingAction`**: Main service for web browsing tasks
- **`PlaywrightScreenshotUtils`**: Screenshot capture and processing
- **`PlaywrightTaskController`**: Async task management

### 7.2 AI Provider Integration
- **OpenRouter**: Cost-effective multi-model provider (primary)
- **OpenAI**: GPT-4o-mini integration via tools4ai
- **Gemini**: Google's Gemini Flash via tools4ai  
- **Claude**: Anthropic Claude Haiku via tools4ai
- **LocalAI**: Self-hosted model support

### 7.3 Caching and Performance
- **`ToolDescriptionCacheService`**: PostgreSQL caching layer
- **`CachedMCPToolsController`**: Bridge between a2acore and a2awebapp
- **Redis integration**: Session and response caching
- **Fast startup**: <5 second target with static tool definitions

## 8. How do I implement risk management?

### 8.1 Risk Level Configuration
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service
@Agent(name = "financial-operations")
public class FinancialAgent {
    
    @Action(description = "Check account balance", riskLevel = ActionRisk.LOW)
    public String checkBalance(@Parameter(description = "Account ID") String accountId) {
        // Low-risk operation - no additional validation needed
        return "Balance: $1,234.56";
    }
    
    @Action(description = "Transfer funds between accounts", riskLevel = ActionRisk.HIGH)
    public String transferFunds(
        @Parameter(description = "Source account") String fromAccount,
        @Parameter(description = "Destination account") String toAccount,
        @Parameter(description = "Transfer amount") double amount) {
        
        // High-risk operation - requires human approval
        if (actionCallback != null) {
            actionCallback.sendStatus("Transfer requires approval", ActionState.WAITING_APPROVAL);
        }
        return "Transfer pending approval";
    }
}
```

### 8.2 Risk Levels Available
- **`ActionRisk.LOW`**: Automated execution, logging only
- **`ActionRisk.MEDIUM`**: Additional validation, enhanced logging  
- **`ActionRisk.HIGH`**: Human approval required, audit trail

## 9. How do I handle image processing and screenshots?

### 9.1 Screenshot Capture with Playwright
```java
@Service
@Agent(name = "visual-automation")
public class VisualAgent {
    
    @Autowired
    private PlaywrightScreenshotUtils screenshotUtils;
    
    @Action(description = "Capture website screenshot")
    public String captureScreenshot(@Parameter(description = "Website URL") String url) {
        try {
            String screenshotPath = screenshotUtils.captureScreenshot(url);
            return "Screenshot saved: " + screenshotPath;
        } catch (Exception e) {
            return "Screenshot failed: " + e.getMessage();
        }
    }
}
```

### 9.2 Image Analysis Integration
```java
@Action(description = "Analyze image content")
public String analyzeImage(@Parameter(description = "Image path or URL") String imagePath) {
    // Use AI provider for image analysis
    GeminiImageActionProcessor processor = new GeminiImageActionProcessor();
    String description = processor.imageToText(imagePath);
    
    // Store analysis in PostgreSQL
    ImageAnalysis analysis = new ImageAnalysis();
    analysis.setImagePath(imagePath);
    analysis.setDescription(description);
    analysis.setAnalyzedAt(LocalDateTime.now());
    
    imageAnalysisRepository.save(analysis);
    return description;
}
```

## 10. What are the core annotations and their usage?

### 10.1 Essential Annotations
- **`@EnableA2ACore`**: Enables the fast MCP framework on main application class
- **`@Agent`**: Defines an agent with name and description for tool grouping
- **`@Action`**: Marks methods as MCP-callable tools with automatic schema generation
- **`@Parameter`**: Provides detailed parameter descriptions for AI understanding
- **`@Service`**: Spring annotation for dependency injection

### 10.2 Complex Type Handling
```java
@Action(description = "Process travel booking request")
public BookingResponse processBooking(
    @Parameter(description = "Complete travel booking details") 
    TravelBookingRequest request) {
    
    // Framework automatically handles:
    // - JSON deserialization of complex objects
    // - Validation of required fields
    // - Type conversion and mapping
    // - Response serialization
    
    return new BookingResponse("Booking confirmed", request.getBookingId());
}
```

## 11. How do I enable database persistence?

### 11.1 Database Configuration
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/a2awebagent
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

# Enable database persistence
a2a:
  persistence: database
```

### 11.2 Entity Persistence
```java
@Entity
@Table(name = "tool_descriptions")
public class ToolDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String providerModel;     // "openrouter/gemma-3n-e4b-it:free"
    private String toolName;          // "browseWebAndReturnText"
    private String description;       // AI-generated description
    private Long generationTimeMs;    // Performance tracking
    private Integer usageCount;       // Usage analytics
    
    // Getters and setters
}
```

## 12. How do I build multi-agent applications?

### 12.1 Service Composition Pattern
```java
@Service
@Agent(name = "travel-orchestrator")
public class TravelOrchestrator {
    
    @Autowired
    private FlightSearchAgent flightAgent;
    
    @Autowired 
    private HotelSearchAgent hotelAgent;
    
    @Autowired
    private AttractionAgent attractionAgent;
    
    @Action(description = "Complete travel research workflow")
    public String orchestrateTravel(
        @Parameter(description = "Travel destination") String destination,
        @Parameter(description = "Travel dates") String dates) {
        
        // Coordinate multiple agents
        String flights = flightAgent.searchFlights(destination, dates);
        String hotels = hotelAgent.searchHotels(destination, dates);
        String attractions = attractionAgent.findAttractions(destination);
        
        return compileReport(flights, hotels, attractions);
    }
}
```

### 12.2 Async Agent Communication
```java
@Service
public class AsyncTravelService {
    
    @Async
    @Action(description = "Start async travel research")
    public CompletableFuture<String> startResearch(String destination) {
        // Long-running research process
        return CompletableFuture.completedFuture("Research completed");
    }
}
```

This FAQ provides a comprehensive foundation for understanding and working with the a2aTravelAgent architecture. For more detailed tutorials, see the numbered guides in this directory.