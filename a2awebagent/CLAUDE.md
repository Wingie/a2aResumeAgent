# CLAUDE.md - a2aTravelAgent Web Automation Agent

## Repository Overview

This is a sophisticated Spring Boot-based web automation agent providing AI-powered travel research through MCP (Model Context Protocol) and A2A (Agent-to-Agent) protocols using Playwright for intelligent web interactions.

## ⚠️ IMPORTANT: Actual Project Architecture

### **Current Reality** (Updated Based on Code Review)

```
a2aTravelAgent/
├── a2ajava/              # 📚 MINIMAL/UNUSED - Legacy reference only
│   └── (Empty Maven project)
└── a2awebagent/          # 🚀 ACTIVE Multi-module Spring Boot Application
    ├── a2acore/          # 🔧 MCP Framework Library (NEW - Primary Framework)
    │   └── io.wingie.a2acore.*
    └── a2awebapp/        # 🌐 Web Automation Application
        └── io.wingie.*
```

### **Key Architecture Correction**

**Previous Documentation Said:** External dependency on a2ajava Maven Central library  
**Actual Implementation:** Self-contained a2acore framework embedded within the project

- **a2ajava**: Currently unused/minimal - NOT the active dependency
- **a2acore**: Primary MCP framework with custom annotations and discovery system
- **a2awebapp**: Spring Boot application using internal a2acore framework
### **Maven Multi-Module Structure**

```xml
<!-- Parent POM: a2awebagent/pom.xml -->
<groupId>io.wingie</groupId>
<artifactId>a2awebagent</artifactId>
<packaging>pom</packaging>
<modules>
    <module>a2acore</module>    <!-- MCP Framework Library -->
    <module>a2awebapp</module>  <!-- Spring Boot Application -->
</modules>
```

**Module Dependencies:**
- `a2acore`: Standalone MCP framework library (internal)
- `a2awebapp`: Depends on `a2acore` version 0.0.1 (local)
## Development Workflow

**Working Directory:** `/Users/wingston/code/a2aTravelAgent/a2awebagent`

### **Build Commands**
```bash
# Build entire multi-module project
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run Spring Boot application (from a2awebagent root)
mvn spring-boot:run -pl a2awebapp

# Alternative: Run packaged JAR
java -jar a2awebapp/target/a2awebapp-0.0.1.jar
```

### **Docker Commands**
```bash
# Single container rebuild
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d

# Full rebuild
docker-compose down && docker-compose up --build -d
```
## Core Components

### **a2acore Framework** (Internal MCP Library)
- **A2aCoreController**: MCP JSON-RPC endpoint handling
- **ToolDiscoveryService**: Fast <100ms auto-discovery of @Action methods
- **ToolExecutor**: Method invocation with timeout protection
- **StaticToolRegistry**: Bean and tool mapping consistency
- **Custom Annotations**: @EnableA2ACore, @Action, @Parameter, @Agent

### **a2awebapp Application** (Spring Boot Web App)
- **Application.java**: Main class with @EnableA2ACore annotation
- **MemeGeneratorTool**: Advanced meme generation (HelloWorld replacement)
- **LinkedInSearchTool**: Professional profile discovery and demonstration
- **ToolDescriptionCacheService**: PostgreSQL-backed AI description caching
- **Multi-database Integration**: PostgreSQL + Redis + Neo4j
- **Web Automation**: Playwright-based intelligent browser control
## Key Dependencies

### **a2acore Module** (Lightweight Framework)
- Spring Framework 6.1.6
- Jackson 2.16.2 (JSON processing)
- SLF4J + Logback (logging)
- Custom reflection-based tool discovery

### **a2awebapp Module** (Full Application)
- Spring Boot 3.2.4 (web framework)
- Microsoft Playwright 1.51.0 (web automation)
- PostgreSQL Driver (primary database)
- Redis Starter (caching and pub/sub)
- Neo4j Starter (future knowledge graph)
- Micrometer + Prometheus (metrics)
- **Note**: No external tools4ai dependency - functionality embedded in a2acore

## Configuration

### **Application Settings**
- **Port**: 7860 (configurable via `application.properties`)
- **Databases**: PostgreSQL (primary) + Redis (cache) + Neo4j (future)
- **AI Provider Keys**: Set via environment variables or JVM properties
  ```bash
  -DopenAiKey=your_key
  -DclaudeKey=your_key
  -DserperKey=your_key
  ```

### **Environment Profiles**
- `application.yml` (default/local development)
- `application-docker.yml` (containerized deployment)
- `application-test.yml` (testing configuration)

## Available MCP Tools

### **Current Tools** (Auto-discovered via @Action annotations)

1. **🎨 generateMeme** - Advanced meme generation with 73 mood mappings
   - Template discovery and validation
   - Special character encoding for URLs
   - Mixed content response (text + base64 image)
   - Mood-to-template intelligent mapping

2. **💼 searchLinkedInProfile** - Professional profile discovery
   - Features Wingston Sharon's profile demonstration
   - Screenshot capabilities with fallback patterns
   - International experience showcase (Amsterdam/Booking.com)

3. **🌐 browseWebAndReturnText** - General web automation
   - Natural language to web action translation
   - Text extraction and content analysis

4. **📷 browseWebAndReturnImage** - Visual content capture
   - ImageContent MCP protocol compliance
   - Base64 encoding optimization
   - Screenshot naming with absolute paths

### **Key Technical Features**
- **Performance**: <100ms tool discovery time
- **Protocol Compliance**: MCP JSON-RPC 2.0 with proper error handling
- **Mixed Content**: TextContent + ImageContent responses
- **Caching**: PostgreSQL-backed AI description caching
- **Safety**: URL validation and error boundaries
- **Async Support**: Non-blocking task execution

## MemeGeneratorTool - Advanced Implementation

**Far Beyond HelloWorld** - This is a sophisticated 855-line production tool:

### **Technical Architecture**
1. **Mood Intelligence**: 73 mood categories mapped to 29 verified templates
2. **Template Validation**: Real-time template existence checking
3. **Special Character Encoding**: Comprehensive URL encoding rules
   - Spaces → underscores or dashes
   - Special chars: ?→~q, &→~a, %→~p, #→~h, /→~s
4. **Direct API Integration**: Fetches base64 images directly from memegen.link
5. **Mixed Content Response**: Combines markdown explanation + base64 image
6. **Error Handling**: Multiple fallback strategies and user guidance

### **Key Features**
- **MoodTemplateMapper**: Intelligent mood-to-template selection
- **Template Discovery**: 29 templates with keywords and examples
- **Production Ready**: Comprehensive error handling and validation
- **MCP Compliant**: Proper ToolCallResult with List<Content> structure

## Development Guidelines

### **Code Modification Pattern**
Always use Task() agents for analysis and changes:

```bash
Task(
  description="Debug MCP integration",
  prompt="Use find_symbol, read_file, analyze @Action annotations and tool discovery"
)
```

### **Project-Specific Patterns**
- **Bridge Architecture**: a2acore handles MCP protocol, a2awebapp provides business logic
- **Multi-module Builds**: Use `-pl a2awebapp` for running specific modules
- **Performance Caching**: Tool descriptions cached in PostgreSQL via ToolDescriptionCacheService
- **Async Processing**: CompletableFuture-based non-blocking execution
- **Discovery Pattern**: Reflection-based @Action method discovery with <100ms target
## Protocol Support

### **MCP (Model Context Protocol)** - Primary
- **JSON-RPC 2.0**: Complete message format compliance
- **Tool Registration**: Automatic discovery and registration
- **Error Handling**: Proper error codes and messages
- **Content Types**: TextContent and ImageContent support
- **Performance**: <100ms tool discovery, method caching

### **A2A (Agent-to-Agent)** - Secondary
- **Legacy Integration**: Basic compatibility maintained
- **Natural Language**: Command processing for human-friendly interaction

## Database Architecture

### **Multi-Database Integration**
- **PostgreSQL**: Primary data storage, tool description caching
- **Redis**: Session management, pub/sub, quick caching
- **Neo4j**: Future knowledge graph and relationship mapping

### **Performance Optimizations**
- **Connection Pooling**: HikariCP with optimized settings
- **Query Caching**: JPA second-level cache
- **Async Processing**: Non-blocking database operations

## MCP Protocol Testing

### **Correct MCP Endpoints**

The application provides complete MCP (Model Context Protocol) JSON-RPC 2.0 support via the A2aCoreController:

- **Primary MCP JSON-RPC**: `POST http://localhost:7860/v1` ✅ **WORKING**
- **Tools List**: `GET http://localhost:7860/v1/tools` ✅ **WORKING**
- **Health Check**: `GET http://localhost:7860/v1/health` ✅ **WORKING**
- **Metrics**: `GET http://localhost:7860/v1/metrics` ✅ **WORKING**
- **Tool Execution**: `POST http://localhost:7860/v1/tools/call` ❌ **HAS BUG** - Use `/v1` instead

### **Testing MCP Tools**

#### **1. List Available Tools**
```bash
# Get all available tools via JSON-RPC
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Alternative REST endpoint
curl -s http://localhost:7860/v1/tools | jq .
```

#### **2. Execute Web Automation Tools**

**Text Extraction Example:**
```bash
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", 
    "method": "tools/call", 
    "params": {
      "name": "browseWebAndReturnText",
      "arguments": {
        "provideAllValuesInPlainEnglish": "Go to example.com and extract the page title"
      }
    }, 
    "id": 2
  }'
```

**Screenshot Capture Example:**
```bash
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", 
    "method": "tools/call", 
    "params": {
      "name": "browseWebAndReturnImage", 
      "arguments": {
        "provideAllValuesInPlainEnglish": "Navigate to https://dev.to and take a screenshot"
      }
    }, 
    "id": 3
  }'
```

#### **3. Meme Generation Example**
```bash
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", 
    "method": "tools/call", 
    "params": {
      "name": "generateMeme",
      "arguments": {
        "mood": "excited", 
        "text": "MCP Protocol Working!"
      }
    }, 
    "id": 4
  }'
```

#### **4. LinkedIn Profile Search**
```bash
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0", 
    "method": "tools/call", 
    "params": {
      "name": "searchLinkedInProfile",
      "arguments": {
        "searchQuery": "software engineer Amsterdam"
      }
    }, 
    "id": 5
  }'
```

### **Automated Testing Script**

Use the comprehensive test script for full MCP integration testing:

```bash
# Run automated MCP testing suite
./test_mcp_server.sh

# Generate detailed test report
./test_mcp_server.sh --report
```

The test script validates:
- All 9 MCP tools functionality
- JSON-RPC 2.0 compliance
- Error handling and timeouts
- Mixed content responses (text + images)
- Tool discovery performance (<100ms)

### **Interactive Testing Interface**

Access the web-based tool testing interface:
- **URL**: `http://localhost:7860/tools-test`
- **Features**: Interactive forms for each tool
- **Tool Categories**: Web automation, content generation, data extraction
- **Response Preview**: Real-time JSON-RPC response display

### **Neo4j Knowledge Graph Integration Testing**

Test the Neo4j knowledge graph analytics endpoints:

```bash
# Knowledge graph overview
curl -s http://localhost:7860/api/graph/overview | jq .

# Screenshot analytics
curl -s http://localhost:7860/api/graph/screenshots/stats | jq .

# Task performance analytics  
curl -s http://localhost:7860/api/graph/tasks/performance | jq .
```

### **Real-Time Task Monitoring**

Monitor task execution via Server-Sent Events:
- **SSE Endpoint**: `http://localhost:7860/agents/stream`
- **Dashboard**: `http://localhost:7860/agents`
- **Events**: tool-started, tool-progress, tool-completed, screenshot-captured

### **Expected Response Format**

All MCP tools return standardized JSON-RPC 2.0 responses:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Task completed successfully..."
      },
      {
        "type": "image", 
        "data": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
        "mimeType": "image/png"
      }
    ]
  },
  "id": 1
}
```

### **Error Handling**

MCP protocol errors follow JSON-RPC 2.0 specification:

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32603,
    "message": "Internal error", 
    "data": "Detailed error information"
  },
  "id": 1
}
```

### **Performance Benchmarks**

- **Tool Discovery**: <100ms (all 9 tools)
- **Web Automation**: 2-5 seconds (typical page load)
- **Meme Generation**: 1-3 seconds (API + processing)
- **Screenshot Capture**: 1-2 seconds (headless browser)
- **Neo4j Logging**: Async, non-blocking

## Troubleshooting

### **Known Issues & Fixes**

#### **1. `/v1/tools/call` Endpoint Bug**
**Error**: `Cannot invoke "Object.hashCode()" because "key" is null`
**Cause**: NullPointerException in tool name parsing
**Solution**: Use the main `/v1` endpoint for tool execution instead

```bash
# ❌ Don't use this (has bug)
curl -X POST http://localhost:7860/v1/tools/call

# ✅ Use this instead
curl -X POST http://localhost:7860/v1 -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/call", "params": {...}, "id": 1}'
```

#### **2. Neo4j Warnings (Normal for New Deployments)**
**Warnings**: `Unknown LabelWarning`, `Unknown PropertyKeyWarning`
**Cause**: Neo4j database is empty or nodes lack certain properties
**Status**: Expected for new deployments, will disappear as data is created

**Example warnings (safe to ignore initially):**
```
One of the labels in your query is not available: Screenshot
One of the property names in your query is not available: durationSeconds
```

#### **3. Duration Calculation Issues (FIXED)**
**Previous Issue**: TaskNode entities had NULL `durationSeconds` values
**Root Cause**: Duration calculation wasn't being stored in database
**Fix Applied**: TaskExecutionIntegrationService now calculates and stores duration

**Verification**:
```bash
# Check PostgreSQL
docker exec a2a-postgres psql -U agent -d a2awebagent \
  -c "SELECT task_id, actual_duration_seconds FROM task_executions ORDER BY created DESC LIMIT 3;"

# Check Neo4j
docker exec a2a-neo4j cypher-shell -u neo4j -p password123 \
  "MATCH (t:Task) RETURN t.taskId, t.durationSeconds ORDER BY t.startedAt DESC LIMIT 3;"
```

### **Configuration Validation**

#### **Database Connectivity**
```bash
# PostgreSQL
docker exec a2a-postgres psql -U agent -d a2awebagent -c "SELECT 1;"

# Redis  
docker exec a2a-redis redis-cli ping

# Neo4j
docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "RETURN 1;"
```

#### **MCP Tool Registration**
```bash
# Verify all 9 tools are registered
curl -s http://localhost:7860/v1/tools | jq '.toolCount'

# Check framework health
curl -s http://localhost:7860/v1/health | jq '.'
```

### **Performance Monitoring**

#### **Real-time Task Monitoring**
- **Dashboard**: http://localhost:7860/agents
- **SSE Stream**: http://localhost:7860/agents/stream
- **Graph Analytics**: http://localhost:7860/api/graph/overview

#### **Database Health Checks**
```bash
# Task execution statistics
curl -s http://localhost:7860/api/graph/tasks/performance | jq '.'

# Knowledge graph overview
curl -s http://localhost:7860/api/graph/overview | jq '.'
```
