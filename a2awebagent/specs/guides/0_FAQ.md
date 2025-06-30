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


## How do I test my A2A Agent?
You can use Curl command to test if you application is running properly:

```bash
curl -H "Content-Type: application/json" -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 1
}' https://vishalmysore-a2amcpspring.hf.space/
````
The above one will get the list of tools available in the agent. You can also use the `tools/call` method to call a specific tool.

```json
{
    "method": "tools/call",
    "params": {
        "name": "whatThisPersonFavFood",
        "arguments": {
            "provideAllValuesInPlainEnglish": "vishal is coming home what should i cook"
        }
    },
    "jsonrpc": "2.0",
    "id": 17
}
```

## How can i connect to the Claude Desktop?

Claude Desktop can be connected to A2A and MCP sever through the pass through server. Please look at the details [here](https://github.com/vishalmysore/mcp-connector/)



## What different types of processors are there?
Available processors include:
- `GeminiV2ActionProcessor`: For Google's Gemini AI
- `OpenAiActionProcessor`: For OpenAI integration
- `SpringGeminiProcessor`: Spring-integrated Gemini processor
- `SpringOpenAIProcessor`: Spring-integrated OpenAI processor
- `SeleniumProcessor`: For UI automation integration
- `AnthropicActionProcessor` : Claude 
- `LocalAiActionProcessor` : Local AI integration

## How do I add risk types to agents?
Use the `riskLevel` parameter in the `@Action` annotation:

```java
@Agent(groupName = "banking")
public class BankingAgent {
    @Action(description = "Check balance", riskLevel = ActionRisk.LOW)
    public String checkBalance(String accountId) {
        // Implementation
    }
    
    @Action(description = "Transfer funds", riskLevel = ActionRisk.HIGH)
    public String transferFunds(String from, String to, double amount) {
        // Implementation with additional validation
    }
}
```

Risk levels: LOW, MEDIUM, HIGH. High-risk actions require human validation.

## How can I do image processing?
Use the `GeminiImageActionProcessor` for image processing:

```java
public class ImageProcessor {
    public void processImage(String imagePath) throws AIProcessingException {
        GeminiImageActionProcessor processor = new GeminiImageActionProcessor();
        String imageDescription = processor.imageToText(imagePath);
        
        // Process the description with an action processor
        GeminiV2ActionProcessor actionProcessor = new GeminiV2ActionProcessor();
        Object result = actionProcessor.processSingleAction(imageDescription);
    }
}
```

## What are different prompt annotations?
Key annotations include:
- `@Agent`: Defines an agent group and description
- `@Action`: Marks methods as AI-callable actions
- `@ActionParameter`: Describes parameters for better AI understanding
- `@Predict`: Used for automatic action prediction
- `@ListType`: Specifies collection types for serialization

## How do I handle complex Java types?
Complex types are handled through:
1. Automatic parameter mapping:
```java
@Action(description = "Process customer data")
public Response processCustomer(@ActionParameter(
    name = "customer",
    description = "Customer details including name, age, and preferences"
) CustomerDTO customer) {
    // Implementation
}
```

2. PromptTransformer for complex type conversion:
```java
@Override
public PromptTransformer getPromptTransformer() {
    return new GeminiV2PromptTransformer();
}
```

The framework automatically handles JSON serialization/deserialization of complex types.

## How Can i persist the Task?

By Default the tasks are persisted in memory and not persisted to any database. You can use the property 

```
a2a.persistence=database
```
to save the data in db

## How can I build agentic mesh applications?
 Yes source code for agentic mesh is https://github.com/vishalmysore/agenticmesh