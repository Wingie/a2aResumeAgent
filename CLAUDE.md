# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
PROJECT NAME: a2aTravelAgent - AI-Powered Travel Research Agent 
to t
## Repository Overview

This is a Spring Boot-based web automation agent that provides A2A (Agent-to-Agent) and MCP (Model Context Protocol) protocol support using Playwright for web interactions. The system enables AI agents to perform web automation tasks through natural language commands.

## ⚠️ IMPORTANT: Actual Project Architecture (CORRECTED)

### **Real Implementation vs Previous Documentation**

**❌ What Documentation Previously Claimed:**
- Two separate projects: a2ajava (library) and a2awebagent (application)
- External dependency on a2ajava from Maven Central
- tools4ai framework as external dependency

**✅ Actual Current Architecture:**

```
a2aTravelAgent/
├── a2ajava/              # 📁 MINIMAL/UNUSED (legacy reference only)
│   └── (Empty Maven project, not actively used)
│
└── a2awebagent/          # 🚀 ACTIVE Multi-module Spring Boot Project
    ├── pom.xml           # Parent aggregator POM
    ├── a2acore/          # 🔧 MCP Framework Library (Internal)
    │   ├── pom.xml
    │   └── src/main/java/io/wingie/a2acore/
    └── a2awebapp/        # 🌐 Spring Boot Web Application
        ├── pom.xml
        └── src/main/java/io/wingie/
```

### **Key Architecture Facts:**

| Component | Status | Purpose | Package |
|-----------|--------|---------|----------|
| **a2ajava** | UNUSED | Legacy reference | `io.github.vishalmysore.*` |
| **a2acore** | ACTIVE | Internal MCP framework | `io.wingie.a2acore.*` |
| **a2awebapp** | ACTIVE | Web automation application | `io.wingie.*` |

### **Actual Module Dependencies:**

#### **Multi-Module Maven Structure:**
```xml
<!-- a2awebagent/pom.xml (Parent) -->
<groupId>io.wingie</groupId>
<artifactId>a2awebagent</artifactId>
<packaging>pom</packaging>
<modules>
    <module>a2acore</module>    <!-- Framework Library -->
    <module>a2awebapp</module>  <!-- Spring Boot App -->
</modules>

<!-- a2awebapp/pom.xml -->
<dependency>
    <groupId>io.wingie</groupId>
    <artifactId>a2acore</artifactId>
    <version>0.0.1</version>  <!-- Local dependency, NOT Maven Central -->
</dependency>
```

### **Development Workflow:**

#### **Working Directory:** `/Users/wingston/code/a2aTravelAgent/a2awebagent`

```bash
# Build entire multi-module project
mvn clean compile

# Run the Spring Boot application
mvn spring-boot:run -pl a2awebapp

# Package all modules
mvn package

# Run packaged application
java -jar a2awebapp/target/a2awebapp-0.0.1.jar
```

#### **Module Responsibilities:**
- **a2acore**: MCP protocol implementation, tool discovery, annotations
- **a2awebapp**: Web automation, PostgreSQL caching, business logic
- **Integration**: Clean separation with internal framework dependency

## Architecture

### Core Components

### **a2acore Framework** (Internal MCP Library)
- **A2aCoreController**: MCP JSON-RPC endpoint handling
- **ToolDiscoveryService**: Fast <100ms auto-discovery of @Action methods
- **ToolExecutor**: Method invocation with timeout protection
- **StaticToolRegistry**: Bean and tool mapping consistency
- **Custom Annotations**: @EnableA2ACore, @Action, @Parameter, @Agent

### **a2awebapp Application** (Spring Boot Web App)
- **Application.java**: Main class with `@EnableA2ACore` annotation
- **MemeGeneratorTool**: Advanced meme generation (855 lines, not HelloWorld)
- **LinkedInSearchTool**: Professional profile discovery
- **ToolDescriptionCacheService**: PostgreSQL-backed AI description caching
- **Multi-database Integration**: PostgreSQL + Redis + Neo4j

### Key Dependencies (CORRECTED)

- **Spring Boot 3.2.4**: Web framework and application container
- **Microsoft Playwright 1.51.0**: Web automation engine
- **a2acore 0.0.1**: Internal MCP framework (NOT external a2ajava)
- **PostgreSQL + Redis + Neo4j**: Multi-database architecture
- **No external tools4ai**: Functionality embedded in a2acore

## Serena Code Intelligence Integration

### Overview
This project includes comprehensive **Serena tools integration** for enhanced code navigation and development productivity. Serena provides intelligent symbol analysis across the entire Spring Boot MCP architecture.

### Quick Reference
📖 **Complete Documentation**: See `serena.md` for comprehensive Serena usage guide  
🔧 **Installation**: `uvx --from git+https://github.com/oraios/serena index-project .`  
📊 **Status**: Successfully indexed 13 Java files with Eclipse JDT Language Server  

### Mandatory Serena Sub-Agent Pattern
**ALWAYS** create Serena sub-agents using the `Task()` tool for:
- Implementing new MCP tools and web automation features
- Debugging Spring Boot integration issues  
- Analyzing callback patterns and AI provider integrations
- Refactoring complex async task processing logic

### Required Fallback Strategy
When Serena tools fail or provide incomplete results, **IMMEDIATELY** use web-based debugging:
1. **WebSearch**: Research Spring Boot patterns and best practices
2. **WebFetch**: Access official documentation (Spring, Selenium, MCP protocols)
3. **Stack Overflow Research**: Find solutions for specific error patterns

### Example Sub-Agent Creation
```bash
# Primary approach with Serena
Task(
  description="Debug MCP integration",
  prompt="Use Serena find_symbol 'MCPController', read_file to analyze implementation, find_referencing_symbols for @Action annotations. Identify integration issues and provide fixes."
)

# Web fallback when needed
Task(
  description="Research MCP solutions", 
  prompt="Use WebSearch for 'Spring Boot MCP protocol debugging' and WebFetch MCP documentation. Compile troubleshooting steps and implementation best practices."
)
```

### Available Serena Tools (30 Active)
- **Symbol Navigation**: `find_symbol`, `find_referencing_symbols`, `get_symbols_overview`
- **Code Modification**: `replace_symbol_body`, `insert_after_symbol`, `replace_regex`  
- **File Operations**: `read_file`, `create_text_file`, `list_dir`, `find_file`
- **Analysis Tools**: `think_about_collected_information`, `search_for_pattern`
- **Memory Management**: `write_memory`, `read_memory`, `list_memories`

### Development Best Practices
1. **Start with Serena**: Use code intelligence as primary analysis tool
2. **Web Research Fallback**: Don't struggle with incomplete Serena results  
3. **User Collaboration**: Keep user informed of analysis approach and reasoning
4. **Hybrid Solutions**: Combine Serena insights with external documentation research

For complete Serena usage patterns, debugging workflows, and integration examples, refer to the comprehensive `serena.md` documentation.

## Development Commands

### Build and Run
```bash
# Build multi-module project
mvn clean compile

# Run tests
mvn test

# Package all modules
mvn package

# Run Spring Boot application (from a2awebagent root)
mvn spring-boot:run -pl a2awebapp

# OR run packaged JAR
java -jar a2awebapp/target/a2awebapp-0.0.1.jar
```

### Docker Support
```bash
# Build Docker image
docker build -t a2a-playwright .

# Run container
docker run -p 7860:7860 a2a-playwright
```

## Configuration

### Application Configuration
- **Server Port**: 7860 (configurable via `application.properties`)
- **Multi-Database**: PostgreSQL (primary) + Redis (cache) + Neo4j (future)
- **Environment Profiles**: application.yml, application-docker.yml, application-test.yml
- **Performance**: <100ms tool discovery, async task processing

### AI Provider Integration
- **Multiple Providers**: OpenAI, Anthropic Claude, Google Gemini
- **Intelligent Fallback**: Provider switching with error handling
- **Rate Limiting**: Built-in request throttling
- **Caching**: PostgreSQL-backed description caching for performance

### API Keys Configuration
**⚠️ Security Note**: Remove hardcoded credentials before production
```bash
# Environment variables (recommended)
export OPENAI_KEY=your_key
export CLAUDE_KEY=your_key

# JVM properties (development)
-DopenAiKey=your_key
-DclaudeKey=your_key
```

## Web Automation Features

### Supported Actions
- **Navigation**: URL navigation with safety validation
- **Interaction**: Click, type, fill forms, check/uncheck
- **Extraction**: Screenshot capture, text extraction
- **Waiting**: Wait for selectors, load states

### Usage Examples
```bash
# Text extraction via JSON-RPC
curl -X POST -H "Content-Type: application/json" \
-d '{"method": "tools/call", "params": {"name": "browseWebAndReturnText", "arguments": {"provideAllValuesInPlainEnglish": "Go to Google.com, search for \"a2ajava\""}}, "jsonrpc": "2.0", "id": 17}' \
http://localhost:7860

# Screenshot capture
curl -X POST -H "Content-Type: application/json" \
-d '{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "browseWebAndReturnImage", "arguments": {"provideAllValuesInPlainEnglish": "Navigate to https://dev.to and take a screenshot"}}, "id": 17}' \
http://localhost:7860
```

## Development Notes

### Safety Features
- URL safety validation before navigation
- Exception handling for web automation failures
- Headless browser context with proper resource management

### Extension Points
- Implement `PlaywrightProcessor` interface for custom web automation logic
- Add new callback classes for additional AI providers
- Extend `PlaywrightActions` for new action types

### Java Version Requirements
- Java 17+ (configured for Java 18 in Maven)
- Maven 3.1+
- Spring Boot 3.x compatible dependencies

## Testing

### Unit Tests
The project uses Spring Boot's test framework. Run tests with:
```bash
mvn test
```

### Testing Web Automation with Custom Actions

The system consists of two components:
- **a2awebagent**: Executes the web.action program (a comprehensive travel research script)
- **a2aTravelAgent**: Receives natural language queries from external tools and orchestrates the research

#### Travel Research Program (web.action)
The `a2awebagent/src/main/resources/web.action` file contains a reusable program that:
1. Searches for outbound and return flights
2. Finds hotels sorted by rating and price
3. Discovers attractions, museums, and tours
4. Compiles a comprehensive research report with screenshots and pricing

The program uses placeholder variables like `{origin_city}`, `{destination_city}`, `{travel_date}` that get replaced with actual values from the user's query.

#### Usage Flow
1. **External Tool (e.g., Claude)** → Sends natural language query to a2aTravelAgent
2. **a2aTravelAgent** → Processes query and triggers a2awebagent
3. **a2awebagent** → Executes web.action program with filled parameters
4. **Results** → Screenshots and extracted data compiled into research presentation

#### Example Usage
```bash
# From an external tool, send a travel research request
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Research travel options from Amsterdam to Palma on July 6th 2025, then from Palma to Ibiza the same day. Include flights, hotels, and attractions. Create a comprehensive travel plan."
    }
  },
  "id": 1
}'
```

The agent will execute the web.action program, replacing placeholders with:
- `{origin_city}`: Amsterdam
- `{destination_city}`: Palma, then Ibiza
- `{travel_date}`: July 6th
- And generate a complete research report with pricing options

### Port Conflict Resolution
Both a2aTravelAgent and a2awebagent default to port 7860. To run both:
1. Change port in `src/main/resources/application.properties`: `server.port=7861`
2. Or run with: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=7861`

## Deployment

### Local Development
```bash
mvn spring-boot:run
```

### Production Deployment
Use the pre-built Docker image or deploy the JAR file:
```bash
java -Dloader.path=/path/to/conf -jar a2aPlaywright-0.2.3.jar
```

## Protocol Support

### MCP Protocol (Primary)
- **JSON-RPC 2.0**: Complete compliance via a2acore framework
- **Tool Discovery**: Automatic @Action method registration
- **Performance**: <100ms discovery, method caching
- **Content Types**: TextContent and ImageContent support
- **Error Handling**: Proper error codes and messages

### A2A Protocol (Legacy)
- **Basic Support**: Natural language command processing
- **Integration**: Bridge pattern with MCP protocol
- **Compatibility**: Maintained for backward compatibility

## Project Execution Notes

### Multi-Module Project Execution
- **Working Directory**: `/Users/wingston/code/a2aTravelAgent/a2awebagent`
- **Build Command**: `mvn clean package` (builds both a2acore and a2awebapp)
- **Run Command**: `mvn spring-boot:run -pl a2awebapp`
- **JAR Location**: `a2awebapp/target/a2awebapp-0.0.1.jar`
- **Features**: MCP protocol, PostgreSQL caching, async task processing

## Personal Development Preferences

- I prefer to run the server externally

## Code Editing and Task Execution Guidelines

- Always use a Task() agent to:
  * Read and think through code changes
  * Make precise edits
  * Verify that the code change has the desired effect
  * Ensure no unintended negative consequences occur

## Development Memories

- When testing things, you should do a down and build of just that one container: `docker-compose down a2awebagent && docker-compose up --build a2awebagent -d`