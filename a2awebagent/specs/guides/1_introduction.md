# Tutorial 1: a2aTravelAgent - Quick Start Guide to AI-Powered Web Automation

## 1. Introduction

Welcome to a2aTravelAgent, a sophisticated AI-powered web automation platform built with Spring Boot, Playwright, and the a2acore MCP framework. This guide will get you started with creating intelligent web automation agents that can understand natural language instructions and perform complex browser interactions.

## 2. Understanding the Architecture

### 2.1 Project Structure Overview
```
a2aTravelAgent/
├── a2ajava/              # Legacy Library (Maven Central v0.1.9.6)
│   └── io.github.vishalmysore.*  # READ-ONLY reference
└── a2awebagent/          # Current Multi-module Application
    ├── a2acore/          # Fast MCP Framework Library
    │   ├── @EnableA2ACore      # Autoconfiguration
    │   ├── @Agent/@Action      # Tool annotations  
    │   └── JsonRpcHandler      # MCP protocol
    └── a2awebapp/        # Spring Boot Web Automation App
        ├── PlaywrightProcessor # Browser automation
        ├── AI Integration     # Multi-provider support
        └── PostgreSQL        # Tool description caching
```

### 2.2 Technology Stack
- **Spring Boot 3.2.4**: Modern Java framework with auto-configuration
- **Microsoft Playwright 1.51.0**: Cross-browser automation (replacing Selenium)
- **a2acore Framework**: Fast-starting MCP protocol implementation  
- **PostgreSQL**: Tool description caching for performance
- **Redis**: Session and response caching
- **Docker**: Container-based development and deployment

## 3. Installation and Setup

### 3.1 Prerequisites
- Java 17 or higher
- Maven 3.6+  
- Docker and Docker Compose (recommended)
- PostgreSQL (for local development)

### 3.2 Project Dependencies
Create your `pom.xml` with the multi-module structure:

```xml
<!-- Parent POM (a2awebagent/pom.xml) -->
<project>
    <groupId>io.wingie</groupId>
    <artifactId>a2awebagent</artifactId>
    <version>0.0.1</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>a2acore</module>
        <module>a2awebapp</module>
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.4</spring-boot.version>
        <playwright.version>1.51.0</playwright.version>
    </properties>
</project>
```

**Application Dependencies (a2awebapp/pom.xml):**
```xml
<dependencies>
    <!-- Local a2acore framework -->
    <dependency>
        <groupId>io.wingie</groupId>
        <artifactId>a2acore</artifactId>
        <version>0.0.1</version>
    </dependency>
    
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Playwright for web automation -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.51.0</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Redis caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

### 3.3 Basic Configuration

**1. Application Configuration (application.yml):**
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

# a2acore configuration
a2a:
  persistence: database
```

**2. AI Provider Configuration (tools4ai.properties):**
```properties
# Cost-optimized AI provider
agent.provider=openrouter
openAiBaseURL=https://openrouter.ai/api/v1
openAiModelName=google/gemma-3n-e4b-it:free

# Performance settings
cache.enabled=true
startup.provider=openrouter
```

## 4. Creating Your First Agent

### 4.1 Main Application Class
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

### 4.2 Basic Web Automation Agent
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.microsoft.playwright.*;

@Service
@Agent(name = "web-research", 
       description = "AI-powered web research and automation")
public class WebResearchAgent {
    
    @Autowired
    private Browser browser;
    
    @Action(description = "Browse websites and extract information using natural language")
    public String browseAndExtract(
        @Parameter(description = "Natural language instructions for web browsing") 
        String instructions) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            
            // AI interprets instructions and executes browser actions
            if (instructions.toLowerCase().contains("google")) {
                page.navigate("https://www.google.com");
                page.waitForLoadState(LoadState.NETWORKIDLE);
                
                // Extract page title
                String title = page.title();
                return "Visited Google. Page title: " + title;
            }
            
            return "Executed: " + instructions;
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @Action(description = "Take screenshot of a website")
    public String captureScreenshot(
        @Parameter(description = "Website URL to capture") String url) {
        
        try (BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Capture screenshot
            byte[] screenshot = page.screenshot();
            String screenshotPath = saveScreenshot(screenshot);
            
            return "Screenshot saved: " + screenshotPath;
            
        } catch (Exception e) {
            return "Screenshot failed: " + e.getMessage();
        }
    }
}
```

### 4.3 Travel Research Agent (Advanced Example)
```java
package io.wingie;

import io.wingie.a2acore.annotations.*;
import org.springframework.stereotype.Service;

@Service  
@Agent(name = "travel-research",
       description = "Comprehensive travel research with booking automation")
public class TravelResearchAgent {
    
    @Autowired
    private PlaywrightProcessor playwrightProcessor;
    
    @Action(description = "Research flights, hotels, and attractions for travel planning")
    public String researchTravel(
        @Parameter(description = "Origin city") String origin,
        @Parameter(description = "Destination city") String destination,
        @Parameter(description = "Travel date (YYYY-MM-DD)") String travelDate) {
        
        // Load the travel research template
        String webActionScript = """
            1. Navigate to https://www.google.com/flights
            2. Search for flights from {origin_city} to {destination_city} on {travel_date}
            3. Take screenshot of flight results
            4. Navigate to https://www.booking.com
            5. Search for hotels in {destination_city} for {travel_date}
            6. Take screenshot of hotel results
            7. Compile comprehensive travel report
            """;
            
        // Replace placeholders with actual values
        webActionScript = webActionScript
            .replace("{origin_city}", origin)
            .replace("{destination_city}", destination)
            .replace("{travel_date}", travelDate);
            
        // Execute automated travel research
        return playwrightProcessor.processSteps(webActionScript);
    }
}
```

## 5. MCP Protocol Integration

### 5.1 Automatic Tool Discovery
With `@EnableA2ACore`, your agents are automatically exposed as MCP tools:

**Your Java Code:**
```java
@Action(description = "Research travel options")
public String researchTravel(String origin, String destination) {
    // Implementation
}
```

**Generated MCP Tool:**
```json
{
    "name": "researchTravel",
    "description": "Research travel options", 
    "inputSchema": {
        "type": "object",
        "properties": {
            "origin": {"type": "string"},
            "destination": {"type": "string"}
        },
        "required": ["origin", "destination"]
    }
}
```

### 5.2 Available MCP Endpoints
```bash
# List all available tools
GET /v1/tools

# Execute a specific tool
POST /v1/tools/call

# Health check
GET /v1/health

# Performance metrics
GET /v1/metrics
```

## 6. Testing Your Agent

### 6.1 Docker Development (Recommended)
```bash
# Clone and setup
git clone <repository>
cd a2awebagent

# Start all services (PostgreSQL, Redis, Application)
docker-compose up -d

# View logs
docker-compose logs -f a2awebagent

# Rebuild application only
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d
```

### 6.2 Local Development
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run -pl a2awebapp

# Alternative: Run from specific module
cd a2awebapp
mvn spring-boot:run
```

### 6.3 Testing via API
```bash
# Health check
curl http://localhost:7860/v1/health

# List available tools
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 1
}' http://localhost:7860/v1

# Execute web automation
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0", 
    "method": "tools/call",
    "params": {
        "name": "browseAndExtract",
        "arguments": {
            "instructions": "Go to Google.com and search for travel to Paris"
        }
    },
    "id": 2
}' http://localhost:7860/v1
```

## 7. Advanced Features

### 7.1 Database Integration
```java
// Entity for storing automation results
@Entity
@Table(name = "automation_results")
public class AutomationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String instructions;
    private String result;
    private String screenshotPath;
    private LocalDateTime executedAt;
}

// Repository
@Repository
public interface AutomationResultRepository extends JpaRepository<AutomationResult, Long> {
    List<AutomationResult> findByInstructionsContaining(String keyword);
}

// Service with database integration
@Service
public class AutomationService {
    
    @Autowired
    private AutomationResultRepository repository;
    
    @Action(description = "Execute and save automation result")
    @Transactional
    public String executeAndSave(String instructions) {
        String result = executeWebAutomation(instructions);
        
        AutomationResult entity = new AutomationResult();
        entity.setInstructions(instructions);
        entity.setResult(result);
        entity.setExecutedAt(LocalDateTime.now());
        
        repository.save(entity);
        return result;
    }
}
```

### 7.2 Caching and Performance
```java
@Service
public class PerformantWebAgent {
    
    @Action(description = "Cached web automation")
    @Cacheable(value = "web-results", key = "#instructions.hashCode()")
    public String cachedWebAutomation(String instructions) {
        // Expensive web automation
        return executeWebAutomation(instructions);
    }
    
    @Action(description = "Async web automation")
    @Async
    public CompletableFuture<String> asyncWebAutomation(String instructions) {
        return CompletableFuture.supplyAsync(() -> 
            executeWebAutomation(instructions));
    }
}
```

### 7.3 Error Handling and Monitoring
```java
@Service
public class RobustWebAgent {
    
    @Action(description = "Robust web automation with retry")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String robustWebAutomation(String instructions) {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            String result = executeWebAutomation(instructions);
            sample.stop(Timer.builder("web.automation.duration").register(meterRegistry));
            
            meterRegistry.counter("web.automation.success").increment();
            return result;
            
        } catch (Exception e) {
            meterRegistry.counter("web.automation.error").increment();
            throw new WebAutomationException("Automation failed", e);
        }
    }
}
```

## 8. Claude Desktop Integration

### 8.1 MCP Server Configuration
Add to your Claude Desktop configuration:

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

### 8.2 Natural Language Usage
Once configured, you can use natural language in Claude Desktop:

```
"Research travel options from New York to Paris for next week. 
Include flights, hotels, and attractions."
```

Claude will automatically call your `researchTravel` action through the MCP protocol.

## 9. Best Practices

### 9.1 Agent Design
- **Clear Descriptions**: Use descriptive `@Agent` and `@Action` annotations
- **Appropriate Parameters**: Use typed parameters with clear descriptions  
- **Error Handling**: Implement comprehensive error handling and logging
- **Performance**: Use caching and async processing for long-running operations

### 9.2 Security Considerations
```java
@Action(description = "Secure web automation", riskLevel = ActionRisk.MEDIUM)
public String secureWebAutomation(
    @Parameter(description = "Validated URL only") @URL String url) {
    
    // URL safety validation
    if (!urlValidator.isSafe(url)) {
        throw new SecurityException("Unsafe URL blocked");
    }
    
    return executeWebAutomation(url);
}
```

### 9.3 Production Deployment
- **Environment Variables**: Use environment-specific configuration
- **Health Checks**: Implement comprehensive health monitoring
- **Logging**: Use structured logging with appropriate levels
- **Metrics**: Expose metrics for monitoring and alerting

## 10. Next Steps

### 10.1 Explore Advanced Tutorials
1. **Tutorial 2**: Annotations Deep Dive - Advanced parameter handling
2. **Tutorial 5**: UI Automation with Playwright - Browser automation patterns
3. **Tutorial 6**: Spring Boot Integration - Enterprise patterns
4. **Tutorial 11**: MCP Protocol - Advanced protocol integration

### 10.2 Extend Your Agents
- **Custom AI Providers**: Integrate additional AI services
- **Multi-Site Automation**: Create agents that work across multiple websites
- **Data Extraction**: Build agents that extract and structure web data
- **Monitoring Systems**: Create agents that monitor websites for changes

### 10.3 Community and Support
- **GitHub Repository**: Contribute to the project
- **Documentation**: Refer to detailed guides in `specs/guides/`
- **Examples**: Study the travel research use case implementation

This introduction provides a solid foundation for building sophisticated AI-powered web automation agents with the a2aTravelAgent platform. The combination of Spring Boot's enterprise features, Playwright's modern browser automation, and a2acore's MCP integration creates a powerful development environment for intelligent automation systems.