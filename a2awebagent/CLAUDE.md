# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**a2aTravelAgent** - AI-powered web automation agent with Spring Boot 3.2.4, Microsoft Playwright 1.51.0, and multi-protocol support (A2A & MCP). Fully migrated from Selenium to Playwright for superior performance and reliability.

### Core Purpose
- Natural language → automated web research tasks
- Real-time progress tracking with Server-Sent Events
- Multi-provider AI integration (Gemini, OpenAI, Claude)
- Enterprise-grade async task processing

## Repository Structure & Development Guidelines

### ⚠️ IMPORTANT: a2ajava Library Relationship

**a2ajava folder is READ-ONLY for reference purposes only**

- `/a2ajava/` - Published Maven library (io.github.vishalmysore:a2ajava:0.1.9.6) 
- `/a2awebagent/` - **THIS PROJECT** - All modifications must be made here only

**Development Rules:**
- ✅ **DO**: Read a2ajava code for understanding library behavior
- ✅ **DO**: Make all changes in a2awebagent project only  
- ❌ **DON'T**: Edit any files in a2ajava/ directory
- ❌ **DON'T**: Treat a2ajava as a local dependency

**Why this separation exists:**
- a2ajava is a published library on Maven Central
- We use the published version via Maven dependency in pom.xml
- Local a2ajava folder is for code reference and understanding only
- Any customizations must be implemented via Spring Boot overrides in a2awebagent

## Quick Commands

### Development
```bash
# Run locally
mvn spring-boot:run

# Run tests
mvn test

# Specific test
mvn test -Dtest=BrowserSystemHealthTest
```

### Docker (Recommended)
```bash
# Quick restart with admin tools
docker-compose --profile admin down && docker-compose up --build --profile admin

# Standard startup
docker-compose up -d

# Full rebuild (separate terminal)
docker build -t a2awebagent . --no-cache
```

### Key Files & Components

```
src/main/java/io/wingie/
├── Application.java                    # Spring Boot main class
├── MainEntryPoint.java                # JSON-RPC & A2A/MCP endpoint
├── config/
│   ├── PlaywrightConfig.java          # Playwright Spring configuration
│   └── AsyncConfig.java               # Async task processing
├── playwright/
│   ├── PlaywrightTaskController.java  # Main task orchestration
│   └── PlaywrightWebBrowsingAction.java # Web automation actions
└── service/
    └── TaskExecutorService.java       # Async execution engine
```

## Architecture Essentials

### Web Automation Stack
- **Playwright 1.51.0**: Modern browser automation with superior reliability
- **Spring Configuration**: `PlaywrightConfig` manages browser lifecycle
- **Docker**: Uses `mcr.microsoft.com/playwright/java:v1.51.0-noble`
- **Legacy Support**: All Selenium dependencies removed and excluded

### AI Integration
- **Multi-Provider**: Gemini (primary), OpenAI (fallback), Claude (creative)
- **tools4ai Framework**: `@Action` annotations for tool discovery
- **Configuration**: `tools4ai.properties` for AI settings

### Protocols
- **A2A (Agent-to-Agent)**: `POST /` - JSON-RPC 2.0 endpoint
- **MCP (Model Context Protocol)**: Tool discovery and execution
- **WebSocket/SSE**: Real-time progress updates

### Data Layer
- **PostgreSQL**: Task execution tracking
- **Redis**: Real-time caching and pub/sub
- **Neo4j**: Travel knowledge graphs (optional)
- **H2**: In-memory testing

## Common Tasks

### Add New Web Automation Action
1. Add method to `PlaywrightWebBrowsingAction.java` with `@Action` annotation
2. Update `PlaywrightTaskController` if needed
3. Test with `BrowserSystemHealthTest` pattern

### Debug Web Automation
- Check `PlaywrightConfig` bean creation
- Verify browser startup in logs
- Use screenshot capture for debugging

### AI Provider Configuration
```properties
# tools4ai.properties
agent.provider=gemini
gemini.modelName=gemini-2.0-flash-001
gemini.projectId=your-project-id
```

### Environment Setup
- **All API Keys**: Set in `.env` file (copy from `.env.template`)
- **Docker**: Automatically reads `.env` file and passes keys as JVM system properties
- **Local Development**: Use `-DopenAiKey=your_key` or `.env` file
- **Security**: Keys never stored in code/config files, masked in logs by Logback

```bash
# Copy template and edit with your keys
cp .env.template .env
# Edit .env file with your API keys
```

## Testing Strategy

### Critical Tests
- `BrowserSystemHealthTest`: Validates Playwright integration
- `AsyncTaskIntegrationTest`: Tests task execution pipeline
- All tests use H2 + test profile for isolation

### Test Categories
- **Unit**: Service layer logic
- **Integration**: Playwright + Spring Boot
- **System**: End-to-end automation workflows

## Troubleshooting

### Common Issues
- **Playwright startup fails**: Check Docker base image and browser dependencies
- **AI provider errors**: Verify API keys and credentials
- **Test timeouts**: Use simplified test patterns, avoid complex DOM operations
- **Docker build slow**: Use `--profile admin` for faster restarts

### Performance Notes
- App startup: ~30-40 seconds (includes AI tool scanning)
- Playwright initialization: ~3-5 seconds
- Test execution: Run externally due to startup time

## Serena Code Intelligence Workflow

This project is fully indexed with Serena for enhanced development productivity. Use Serena as your primary analysis tool before making code changes.

### Serena Setup & Status
```bash
# Initialize Serena (already done)
uvx --from git+https://github.com/oraios/serena index-project .

# Status: ✅ Successfully indexed Spring Boot MCP architecture
# Cache location: .serena/cache/typescript/document_symbols_cache_v23-06-25.pkl
```

### Mandatory Serena-First Development Pattern

**ALWAYS use Serena sub-agents for coding tasks via Task() tool:**

```bash
# Example: Adding new web automation feature
Task(
  description="Add Playwright action",
  prompt="Use Serena find_symbol 'PlaywrightWebBrowsingAction', read_file to analyze current actions, then find_referencing_symbols to understand usage patterns. Design and implement a new @Action method for form filling with proper Spring integration."
)
```

### Core Serena Commands for This Project

#### 1. Architecture Analysis
```bash
# Project overview
get_symbols_overview

# Key components
find_symbol "PlaywrightConfig"
find_symbol "PlaywrightTaskController" 
find_symbol "Application"
find_symbol "TaskExecutorService"
```

#### 2. Spring Boot Integration Analysis
```bash
# Find all Spring configurations
find_symbol "Config"

# Analyze autowiring patterns
find_referencing_symbols "PlaywrightConfig"
find_referencing_symbols "TaskExecutorService"

# Check component scanning
search_for_pattern "@Component|@Service|@Controller"
```

#### 3. Playwright Integration Deep Dive
```bash
# Core Playwright components
find_symbol "PlaywrightProcessor"
find_symbol "PlaywrightActions"

# Action annotations for AI integration
search_for_pattern "@Action"
find_referencing_symbols "PlaywrightWebBrowsingAction"
```

#### 4. MCP/A2A Protocol Analysis
```bash
# Protocol implementations
find_symbol "MCPController"
find_symbol "MainEntryPoint"

# JSON-RPC endpoint analysis
search_for_pattern "JsonRpcRequest"
find_referencing_symbols "SpringAwareJSONRpcController"
```

#### 5. Testing Infrastructure
```bash
# Test patterns
find_symbol "BrowserSystemHealthTest"
find_symbol "AsyncTaskIntegrationTest"

# Test configurations
search_for_pattern "@Test|@SpringBootTest"
```

### Serena Development Workflows

#### Adding New Web Actions
1. `find_symbol "PlaywrightWebBrowsingAction"` - Study existing patterns
2. `search_for_pattern "@Action"` - Understand annotation usage
3. `find_referencing_symbols "PlaywrightProcessor"` - Check interface compliance
4. `read_file` on relevant test files to understand testing patterns

#### Debugging Spring Integration Issues
1. `find_symbol "PlaywrightConfig"` - Check bean configuration
2. `find_referencing_symbols "Playwright"` - See all usage points
3. `search_for_pattern "@Autowired.*Playwright"` - Find injection points
4. `get_symbols_overview` - Verify component relationships

#### Implementing MCP Protocol Features
1. `find_symbol "MCPController"` - Understand current implementation
2. `search_for_pattern "tools/call|tools/list"` - Find protocol endpoints
3. `find_referencing_symbols "JsonRpcRequest"` - Trace request handling
4. `read_file` relevant MCP files for context

#### Async Task Processing Enhancement
1. `find_symbol "TaskExecutorService"` - Analyze current implementation
2. `find_referencing_symbols "AsyncConfig"` - Check configuration
3. `search_for_pattern "@Async"` - Find all async methods
4. `find_symbol "TaskExecution"` - Study data model

### Serena + Web Research Pattern

When debugging web automation issues:

```bash
# 1. Understand the component
Task(
  description="Debug Playwright issue",
  prompt="Use Serena find_symbol 'PlaywrightTaskController', read_file to analyze implementation. Then WebSearch 'Spring Boot Playwright integration best practices' and WebFetch official Playwright Java docs. Combine insights to identify the issue."
)

# 2. Research solutions
Task(
  description="Research fix approaches", 
  prompt="Use WebSearch for 'Playwright [specific error]' and WebFetch Stack Overflow solutions. Compare with Serena analysis of our current implementation to propose fixes."
)
```

### Serena Effectiveness Testing

As we move to coding, we'll test Serena's effectiveness by:

1. **Speed**: How quickly can Serena locate relevant code vs manual search?
2. **Context**: Does Serena provide better understanding of component relationships?
3. **Code Quality**: Do Serena-guided changes integrate better with existing patterns?
4. **Debugging**: Can Serena help identify root causes faster?

### Fallback Strategy
If Serena tools fail or provide incomplete results:
1. **WebSearch**: Research Spring Boot patterns and best practices
2. **WebFetch**: Access official documentation (Spring, Playwright, MCP)
3. **Stack Overflow**: Find solutions for specific error patterns

### Available Serena Tools (30 Active)
- **Symbol Navigation**: `find_symbol`, `find_referencing_symbols`, `get_symbols_overview`
- **Code Analysis**: `read_file`, `search_for_pattern`, `think_about_collected_information`
- **Code Modification**: `replace_symbol_body`, `insert_after_symbol`, `replace_regex`
- **File Operations**: `create_text_file`, `list_dir`, `find_file`
- **Memory Management**: `write_memory`, `read_memory`, `list_memories`

## Project State & Design Patterns

### Current State Analysis

#### Application Architecture
- **Spring Boot 3.2.4**: Fully configured with async processing
- **Playwright 1.51.0**: Complete migration from Selenium (June 2025)
- **Multi-Protocol**: A2A + MCP protocol support for AI integration
- **Production Ready**: Docker optimized with comprehensive testing

#### Component Status
- **PlaywrightConfig**: ✅ Docker-aware browser configuration
- **TaskExecutorService**: ✅ Async processing with Redis progress tracking
- **@Action Methods**: ✅ 6 AI-integrated web automation actions
- **Testing**: ✅ BrowserSystemHealthTest validates full stack
- **@EnableAgent**: ⚠️ Commented out (TODO: enable when tools4ai-annotations available)

### Design Patterns Implementation

#### 1. Dependency Injection Pattern
```java
// Spring-managed Playwright lifecycle
@Autowired private Browser playwrightBrowser;
@Autowired private BrowserContext playwrightContext;
```

#### 2. Strategy Pattern
```java
// TaskExecutorService task routing
switch (taskType) {
    case "travel_search" -> travelProcessor.process()
    case "web_browsing" -> webBrowsingProcessor.process()
}
```

#### 3. Template Method Pattern
```java
// web.action scripted automation steps
// Step 1: Navigate and search flights
// Step 2: Extract and process results
// Step 3: Take screenshots and compile report
```

#### 4. Observer Pattern
```java
// Redis pub/sub for real-time progress updates
RedisTemplate<String, Object> redisTemplate;
// Publishers: TaskExecutorService
// Observers: WebSocket clients, dashboard UI
```

#### 5. Adapter Pattern
```java
// Playwright adaptation of legacy Selenium interface
public class PlaywrightWebBrowsingAction implements WebBrowsingProcessor {
    // Adapts Playwright APIs to existing web automation interface
}
```

#### 6. Factory Pattern
```java
// PlaywrightConfig bean factory
@Bean public Playwright playwright() { return Playwright.create(); }
@Bean public Browser playwrightBrowser() { /* Docker-aware configuration */ }
```

### Data Flow Architecture

#### Request Processing
1. **Input**: Natural language via MCP/A2A protocols
2. **AI Processing**: tools4ai @Action annotation transformation
3. **Task Routing**: Strategy pattern based on task type
4. **Async Execution**: CompletableFuture with progress tracking
5. **Web Automation**: Playwright browser orchestration
6. **Results**: Screenshots + extracted data + structured response

#### State Management
- **Task State**: QUEUED → RUNNING → COMPLETED/FAILED/TIMEOUT
- **Progress Tracking**: Real-time percentage + status messages
- **Resource Management**: Playwright page/context lifecycle
- **Cleanup**: Scheduled tasks for timeout/old task removal

### Integration Patterns

#### Multi-Protocol Support
- **A2A Protocol**: Agent-to-agent communication via a2ajava framework
- **MCP Protocol**: Model Context Protocol for AI tool integration
- **JSON-RPC 2.0**: Standardized request/response format
- **WebSocket/SSE**: Real-time bidirectional communication

#### AI Framework Integration
- **@Agent**: Groups related automation capabilities
- **@Action**: Exposes methods as AI-callable tools
- **@Parameter**: Provides natural language parameter descriptions
- **Prompt Engineering**: Built-in step decomposition for complex tasks

### Performance Characteristics

#### Startup Performance
- **Application**: ~30-40 seconds (includes AI tool scanning)
- **Playwright**: ~3-5 seconds browser initialization
- **Test Suite**: Run externally due to startup overhead

#### Runtime Performance
- **Async Processing**: Non-blocking with configurable thread pools
- **Redis Caching**: Sub-millisecond progress updates
- **Resource Management**: Lazy initialization patterns throughout

## MCP Configuration & Testing

### Current MCP Setup Issues

Your MCP configuration has several problems:

```json
// INCORRECT - Don't use this
"wingston-mcp-agent": {
  "command": "node",
  "args": ["/Users/wingston/code/a2aTravelAgent/a2awebagent/src/main/resources/mcpserver.js"]
}
```

**Problems:**
1. **Direct Path**: Uses local file path, won't work in Docker
2. **Hardcoded URLs**: mcpserver.js has hardcoded "localhost:7860" 
3. **Dual Architecture**: Both Node.js proxy + Spring Boot MCP endpoint (redundant)
4. **@EnableAgent Disabled**: Core issue - annotation is commented out in Application.java

### Correct MCP Configuration

#### Option 1: Direct Spring Boot MCP (Recommended)
```json
{
  "mcpServers": {
    "a2a-travel-agent": {
      "command": "curl",
      "args": [
        "-X", "GET",
        "http://localhost:7860/v1/tools"
      ],
      "transport": "http",
      "baseUrl": "http://localhost:7860/v1"
    }
  }
}
```

#### Option 2: Docker Compose Setup
```json
{
  "mcpServers": {
    "a2a-travel-agent": {
      "transport": "http", 
      "baseUrl": "http://a2awebagent:7860/v1"
    }
  }
}
```

### Fix the Root Issues

#### 1. Enable @EnableAgent Annotation
```java
// In Application.java - UNCOMMENT THIS LINE
@EnableAgent
@SpringBootApplication
public class Application {
```

#### 2. Docker-Aware Configuration
The mcpserver.js needs environment variable support:
```javascript
// Instead of hardcoded localhost:7860
const SERVER_BASE_URL = process.env.SERVER_BASE_URL || "http://localhost:7860";
```

### Testing MCP Tools

#### Local Testing (Development)
```bash
# 1. Start the application
mvn spring-boot:run

# 2. Test tool discovery
curl http://localhost:7860/v1/tools

# 3. Test specific tool
curl -X POST -H "Content-Type: application/json" \
-d '{"name": "getWingstonsProjectsExpertiseResume", "arguments": {"provideAllValuesInPlainEnglish": "overview"}}' \
http://localhost:7860/v1/tools/call
```

#### Docker Testing
```bash
# 1. Start with Docker Compose
docker-compose up -d

# 2. Test from outside container
curl http://localhost:7860/v1/tools

# 3. Test from inside Docker network
docker exec a2awebagent curl http://localhost:7860/v1/tools
```

#### Integration Testing Script
```bash
#!/bin/bash
# test-mcp-tools.sh

BASE_URL=${1:-"http://localhost:7860"}

echo "Testing MCP Tools at $BASE_URL"

# Test 1: List tools
echo "1. Listing available tools..."
curl -s "$BASE_URL/v1/tools" | jq '.tools[].name'

# Test 2: Test resume tool
echo "2. Testing resume tool..."
curl -X POST -H "Content-Type: application/json" \
-d '{"name": "getWingstonsProjectsExpertiseResume", "arguments": {"provideAllValuesInPlainEnglish": "overview"}}' \
"$BASE_URL/v1/tools/call"

# Test 3: Test web browsing
echo "3. Testing web browsing..."
curl -X POST -H "Content-Type: application/json" \
-d '{"name": "browseWebAndReturnText", "arguments": {"provideAllValuesInPlainEnglish": "Go to example.com"}}' \
"$BASE_URL/v1/tools/call"
```

### Available Tools List

Based on the MCP endpoint analysis, these tools are available:
1. `getWingstonsProjectsExpertiseResume` - Project portfolio information
2. `askTasteBeforeYouWaste` - Food safety queries  
3. `getTasteBeforeYouWasteScreenshot` - Food app screenshots
4. `searchLinkedInProfile` - LinkedIn profile search
5. `searchHelloWorld` - Demo search functionality
6. `browseWebAndReturnText` - Web automation with text output
7. `browseWebAndReturnImage` - Web automation with image output
8. `takeCurrentPageScreenshot` - Screenshot capture

### Development Guidelines

#### Code Quality Rules
- **No Emojis**: Avoid emojis in logs/output (causes language server errors)
- **Logging**: Use SLF4J with structured log messages
- **Error Handling**: Centralized exception patterns with AIProcessingException
- **Testing**: Integration tests for all @Action methods

#### Serena Integration Notes
- **Indexed**: Project successfully indexed with Serena
- **Language Server**: Avoid emoji characters to prevent parsing errors
- **Analysis**: Use Task() agents for complex code analysis
- **Fallback**: Manual analysis + WebSearch when Serena unavailable

---

**Status**: Fully migrated to Playwright-only architecture with comprehensive test coverage and Docker optimization. All Selenium dependencies removed.