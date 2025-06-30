# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
PROJECT NAME: a2aTravelAgent - AI-Powered Travel Research Agent 
to t
## Repository Overview

This is a Spring Boot-based web automation agent that provides A2A (Agent-to-Agent) and MCP (Model Context Protocol) protocol support using Playwright for web interactions. The system enables AI agents to perform web automation tasks through natural language commands.

## ⚠️ IMPORTANT: Project Structure - Two Separate Projects

### **a2ajava/** vs **a2awebagent/** - Different Projects!

```
a2aTravelAgent/
├── a2ajava/          # 📚 LIBRARY PROJECT (tools4ai framework)
│   ├── pom.xml       # Maven library project
│   ├── src/main/java/io/github/vishalmysore/
│   └── README.MD     # Library documentation
│
└── a2awebagent/      # 🚀 APPLICATION PROJECT (web automation agent)
    ├── pom.xml       # Spring Boot application
    ├── src/main/java/io/wingie/
    └── README.MD     # Application documentation
```

### **Key Differences:**

| Aspect | a2ajava | a2awebagent |
|--------|---------|-------------|
| **Type** | Maven Library | Spring Boot Application |
| **Purpose** | tools4ai framework, MCP/A2A protocols | Web automation agent with Playwright |
| **Package** | `io.github.vishalmysore.*` | `io.wingie.*` |
| **Runs** | No (library dependency) | Yes (`mvn spring-boot:run`) |
| **Depends On** | External tools4ai JAR | Local a2ajava + tools4ai |
| **Git Status** | Now tracked in main repo | Always tracked in main repo |

### **How They're Linked:**

#### **Currently (Maven Central):**
```xml
<!-- a2awebagent/pom.xml -->
<dependency>
    <groupId>io.github.vishalmysore</groupId>
    <artifactId>a2ajava</artifactId>
    <version>0.1.9.6</version>  <!-- From Maven Central -->
</dependency>
```

#### **For Local Development:**
When you need to modify both projects:
1. **Modify a2ajava**: Make changes to the library
2. **Build & Install**: `cd a2ajava && mvn clean install`
3. **Update a2awebagent**: Use local version in pom.xml
4. **Test Integration**: `cd a2awebagent && mvn spring-boot:run`

### **Working with Both Projects:**

#### **Editing a2ajava (Library):**
- Contains MCPToolsController, protocol implementations
- Changes affect all applications using this library
- Must be built and installed locally for testing

#### **Editing a2awebagent (Application):**
- Contains PlaywrightProcessor, web automation logic
- Uses a2ajava as dependency
- Can extend/override library functionality

#### **Current Integration Challenge:**
The PostgreSQL caching service is in **a2awebagent**, but the tool description generation happens in **a2ajava**. We're creating bridge interfaces to connect them while maintaining separation of concerns.

## Architecture

### Core Components

- **Application.java**: Spring Boot main class with `@EnableAgent` annotation for tools4ai framework
- **PlaywrightProcessor**: Interface defining web automation operations (navigate, click, type, screenshot, etc.)
- **PlaywrightActions**: Data model for web actions with AI prompt annotations
- **Callback System**: Multiple callback implementations for different AI providers (OpenAI, Gemini, Claude)
- **URL Safety**: Built-in URL validation to prevent navigation to unsafe sites

### Key Dependencies

- **Spring Boot 3.2.4**: Web framework and application container
- **Microsoft Playwright 1.51.0**: Web automation engine
- **a2ajava 0.1.9.3**: Agent-to-agent communication framework
- **tools4ai 1.1.6.1**: AI tool integration framework with prompt annotations
- **Lombok**: Code generation for POJOs

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
# Build the project
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run Spring Boot application
mvn spring-boot:run
# OR
java -jar target/a2aPlaywright-0.2.3.jar
```

### Docker Support
```bash
# Build Docker image
docker build -t a2a-playwright .

# Run container
docker run -p 7860:7860 a2a-playwright
```

## Configuration

### Application Properties
- **Server Port**: 7860 (configurable via `application.properties`)
- **A2A Persistence**: Cache-based storage
- **AI Provider Settings**: Configured in `tools4ai.properties`

### Supported AI Providers
- **OpenAI**: GPT-4o-mini (default)
- **Google Gemini**: gemini-2.0-flash-001
- **Anthropic Claude**: claude-3-haiku-20240307

### API Keys Configuration
Set via system properties or environment variables:
- `-DopenAiKey=your_key`
- `-DclaudeKey=your_key`
- `-DserperKey=your_key` (for Google search)

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

### A2A Protocol
- Agent registration via a2ajava framework
- JSON-RPC tool calling capabilities
- Natural language command processing

### MCP Protocol
- Direct integration with MCP network
- Standardized message format
- Tool registration and discovery

## Project Execution Notes

### a2awebagent Project Execution
- To build and run the a2awebagent project:
  * Run `mvn clean package test` in `/Users/wingston/code/a2aTravelAgent/a2awebagent`
  * Execute the jar with `java -jar target/a2awebagent-0.0.1.jar`
  * This is the parent project for auth/SSO/SSE and webAgent for async job handling

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