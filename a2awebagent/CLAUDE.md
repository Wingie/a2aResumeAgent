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
