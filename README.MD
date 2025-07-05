# A foundation for an Enterprise Agentic Framework in Spring/Java

This repository showcases a personal demo - its a versatile system that you can use for a lot of things.

- **Native Tools4AI Classe Integration**: Complete framework studied and recreated locally under `io.wingie.a2acore.tools4ai.*` from https://github.com/vishalmysore/Tools4AI/tree/main/src/main/java/com/t4a/transform
- **Java 17+ Compatibility**: Resolved JAXB and modern Java runtime issues

This repo demonstrates my Java/Spring Boot expertise, strategic architecture decisions, and the ability to take ownership of complex dependencies rather than relying on external libraries.

## 🖼️ Interface Showcase

### **Main Application Interface**
The startup interface provides a comprehensive view of agent capabilities and real-time interactions:

<img src="a2awebagent/startup.png" alt="Main Startup Interface" width="600">

### **Resume & Professional Portfolio**
Interactive resume showcase demonstrating technical expertise:

<img src="a2awebagent/resume.png" alt="Professional Resume Interface" width="600">

### **Live Reasoning & AI Interactions**
Real-time AI reasoning and decision-making process visualization:

<img src="a2awebagent/live_reasoning_log.png" alt="Live AI Reasoning" width="600">

## 🔧 System Features

### **Meme Generation Server**
Advanced meme generation with mood-based template selection:

<img src="a2awebagent/meme_server.png" alt="Meme Generation Interface" width="500">

### **Screenshot Capture System**
Automated screenshot capture and processing capabilities:

<img src="a2awebagent/captureScreenshot.png" alt="Screenshot Capture System" width="500">

### **System Testing & Validation**
Comprehensive testing interface for system reliability:

<img src="a2awebagent/sanity_check_tets.png" alt="System Testing Interface" width="500">

## 📊 Agent Analytics

### **Agent Evaluation Dashboard**
Performance metrics and evaluation of agent capabilities:

<img src="a2awebagent/agent_eval.png" alt="Agent Evaluation Dashboard" width="600">

## 🎯 Integration Examples

### **Claude Desktop Integration**
Seamless integration with Claude for enhanced AI capabilities:

<img src="a2awebagent/claude.png" alt="Claude Integration" width="600">


---

## Credits
https://github.com/vishalmysore/Tools4AI/tree/main/src/main/java/com/t4a/transform
@vishalmysore for the amazing work on ^, your code was a joy to read

#### ** Framework**
```
a2awebagent/
├── a2acore/                          # 🎯 LOCAL TOOLS4AI FRAMEWORK
│   └── src/main/java/io/wingie/a2acore/tools4ai/
│       ├── JsonUtils.java            # JSON processing utilities
│       ├── annotations/              # @Prompt, @Action, @Parameter
│       ├── predict/                  # AI prediction capabilities  
│       ├── processor/                # Core processing engine
│       ├── transform/                # Data transformation
│       └── [132 total classes]       # Complete framework recreation
│
└── a2awebapp/                        # APPLICATION USING LOCAL FRAMEWORK
    ├── Application.java              # @EnableA2ACore integration
    ├── PlaywrightProcessor.java      # Web automation interface
    └── [64 application classes]      # Travel research application
```

### **Framework Integration Architecture**


```java
// a2awebapp/Application.java
@SpringBootApplication
@EnableA2ACore  // Activates local Tools4AI framework
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 📁 Project Structure

### **a2awebagent** (Multi-Module Web Automation Engine) 
- **Location**: `a2awebagent/` subdirectory
- **Structure**: Multi-module Maven project with:
  - **a2acore/**: Fast MCP framework library (replaces heavy a2ajava dependency)
  - **a2awebapp/**: Spring Boot application with Playwright automation
- **Purpose**: AI-powered travel research and web automation with locally controlled Tools4AI framework
- **Key Features**: Travel research automation, flight/hotel search, web browsing with screenshots, PostgreSQL/Redis caching, complete framework recreation

## 🎯 Project Purpose

This project serves as:
- A **sophisticated travel research agent** with comprehensive automation capabilities
- A **framework recreation showcase** demonstrating advanced dependency management
- A **technical achievement demonstration** of Tools4AI local integration
- A **production-ready MCP protocol implementation** for AI-powerlayered agents  
- A **good base for red teaming attack vectors at architecture example** of taking control over external dependencies

<img src="a2awebagent/startup.png" alt="Agent Interface" width="600">

<img src="a2awebagent/claude.png" alt="Claude Integration" width="600">

## 🏗️ Architecture Overview

The system is built using a modern multi-module Spring Boot architecture with enhanced performance optimization:

<img src="a2awebagent/dashboard-one-shot.png" alt="Multiple Agent Architecture" width="600">

### Core Architecture Components

#### **a2acore Framework** (New Fast MCP Implementation)
- **A2aCoreController**: Lightweight MCP JSON-RPC endpoint
- **ToolDiscoveryService**: Auto-discovery of @Action annotated methods
- **JsonRpcHandler**: Protocol message processing
- **Annotations**: @EnableA2ACore, @Action, @Parameter
- **Performance**: Fast-starting replacement for heavy a2ajava library

#### **a2awebapp Application** (Demo Showcase)
- **Application.java**: Spring Boot main class with @EnableA2ACore
- **PlaywrightProcessor**: Modern web automation interface using Microsoft Playwright
- **CachedMCPToolsController**: Performance optimization bridge between a2acore and PostgreSQL
- **Professional Tools**: LinkedIn search, expertise showcase, screenshot utilities
- **Caching Services**: PostgreSQL + Redis for tool descriptions and performance

#### **Database & Caching Layer**
- **PostgreSQL**: Tool description caching and persistent storage
- **Redis**: High-performance session and data caching
- **ToolDescriptionCacheService**: Intelligent caching bridge service
- **Admin Interfaces**: 
  - `/tools` page: Tool descriptions with tabular filtering by model/tool
  - `/agents` page: Neo4j-logged tool invocations with async job tracking

## 🚀 Professional Resume Features

### **Core Resume Capabilities**
- **`getWingstonsProjectsExpertiseResume`**: Comprehensive technical expertise showcase
- **`searchLinkedInProfile`**: Professional profile search and analysis
- **`browseWebAndReturnText`**: Intelligent web research and data extraction
- **`browseWebAndReturnImage`**: Screenshot capture for visual documentation
- **`askTasteBeforeYouWaste`**: Specialized food/restaurant research tool

### **Web Automation Excellence**
- **Microsoft Playwright**: Modern, reliable browser automation (replacing legacy Selenium)
- **AI-Powered Error Recovery**: Self-healing automation with intelligent retry mechanisms
- **Multi-Provider AI Support**: OpenAI, Gemini, Claude, Mistral integration
- **URL Safety Validation**: Built-in security for safe web navigation
- **Real-time WebSocket Updates**: Live progress tracking and notifications

## 🔧 Serena Code Intelligence Integration

### **Enhanced Development Workflow**
This project features comprehensive **Serena tools integration** for professional code analysis:

- **Indexed Files**: 13 Java source files with Eclipse JDT Language Server
- **Symbol Navigation**: Fast lookup of classes, methods, and architectural patterns
- **Code Analysis**: Intelligent understanding of Spring Boot MCP architecture
- **Performance**: 8.40 files/second indexing with persistent cache

### **Professional Development Capabilities**
- **Architecture Visualization**: Component relationships and design patterns
- **Code Navigation**: Cross-references and call graph analysis
- **Documentation**: Automated insights for technical communication
- **Quality Metrics**: Maintainability and complexity analysis

## 🚀 Getting Started

### Prerequisites

- **Java 21+** (configured for modern development)
- **Maven 3.8+**
- **PostgreSQL** (for caching and persistence)
- **Redis** (for high-performance caching)
- **Chrome/Chromium** (for Playwright automation)

### ✅ Quick Start (Professional Demo)

#### **Step 1: Build the Multi-Module Project**
```bash
# Navigate to the web agent project
cd a2awebagent

# Build both modules (a2acore + a2awebapp)
mvn clean compile

# Run the professional showcase application
mvn spring-boot:run -pl a2awebapp
```

#### **Step 2: Database Setup (Required for Production Features)**
```bash
# PostgreSQL setup for tool caching
createdb a2aresume

# Redis setup for performance caching
redis-server
```

### 🔑 API Key Configuration (Required for AI Features)

**For professional demonstration and AI-powered features**, configure API keys:

#### Method 1: Environment Variables (Recommended)
```bash
# Professional AI providers
export OPENAI_API_KEY=sk-your-actual-key-here
export CLAUDE_KEY=sk-ant-your-key-here  
export GEMINI_KEY=your-gemini-key-here

# Launch the resume agent
java -jar a2awebapp/target/a2awebapp-0.0.1.jar
```

#### Method 2: Configuration Files (Development)
Create `a2awebagent/a2awebapp/src/main/resources/tools4ai.properties`:

```properties
##Gemini Configuration (Recommended for resume features)
gemini.modelName=gemini-2.0-flash-001
gemini.location=us-central1
gemini.projectId=your-google-project-id
geminiKey=your-gemini-api-key-here
agent.provider=gemini

##Professional AI Integration
openAiKey=sk-your-openai-key-here
openAiModelName=gpt-4o-mini
claudeKey=sk-ant-your-claude-key-here

##LinkedIn & Web Research
serperKey=your-serper-key-here
```

### 🤖 MCP (Model Context Protocol) Integration

#### **Professional Claude Desktop Setup**

1. **Build the complete system**:
```bash
cd a2awebagent
./build-mcp.sh  # Builds both a2acore and a2awebapp
```

2. **Configure Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "professional-resume-agent": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/a2awebagent/mcp-connector-full.jar",
        "http://localhost:7860/"
      ],
      "timeout": 30000
    }
  }
}
```

3. **Launch and test**:
```bash
# Start the resume agent
java -jar a2awebapp/target/a2awebapp-0.0.1.jar

# Test professional features
curl http://localhost:7860/v1/tools
```

## 🎯 Professional Use Cases

### **1. Interactive Resume Showcase**
- **Live Expertise Display**: Real-time demonstration of technical capabilities
- **Project Portfolio**: Interactive exploration of development work
- **Skills Validation**: AI-powered verification of technical competencies

### **2. Professional Research**
- **LinkedIn Integration**: Automated profile search and analysis
- **Competitive Analysis**: Research industry trends and opportunities
- **Technical Documentation**: Automated creation of professional materials

### **3. Web Automation Portfolio**
- **Modern Playwright Implementation**: Demonstrating cutting-edge automation
- **AI-Powered Error Recovery**: Showcasing intelligent problem-solving
- **Multi-Protocol Support**: A2A and MCP integration expertise

## 🌐 Professional Access Points

Once running, access the professional features at:

- **Main Resume Interface**: http://localhost:7860
- **Tools Administration**: http://localhost:7860/tools (PostgreSQL-cached tool descriptions)
- **Agent Analytics**: http://localhost:7860/agents (Neo4j async job tracking)
- **API Documentation**: http://localhost:7860/swagger-ui/index.html
- **MCP Tools**: `curl -X POST http://localhost:7860 -H "Content-Type: application/json" -d '{"jsonrpc": "2.0", "method": "tools/list", "params": {}, "id": 1}'`

### 🚀 Professional Startup Indicators

When properly configured for professional demonstration:
```
🎯 Professional Resume Agent - Technical Showcase
📋 Available Professional Tools:
  ✅ getWingstonsProjectsExpertiseResume  - Interactive expertise showcase
  ✅ searchLinkedInProfile                - Professional networking research
  ✅ askTasteBeforeYouWaste              - Specialized research capabilities
  ✅ browseWebAndReturnText              - Intelligent web automation
  ✅ browseWebAndReturnImage             - Visual documentation capture
  ✅ webPageAction                       - Advanced Playwright automation

🏗️ Architecture Status:
  ✅ a2acore Framework                   - Fast MCP implementation loaded
  ✅ PostgreSQL Cache                    - Tool descriptions optimized
  ✅ Redis Performance                   - High-speed data caching
  ✅ Playwright Engine                   - Modern web automation ready
  ✅ Multi-AI Integration                - OpenAI/Gemini/Claude available

🌐 Professional Interface: http://localhost:7860
📊 Admin Tools: /tools | /agents
```

## 💼 Technical Excellence Demonstration

This project showcases **enterprise-grade** technical implementation:

### **Modern Architecture Patterns**
- ✅ **Multi-module Maven structure** with separation of concerns
- ✅ **Dependency injection** with Spring Boot 3.x
- ✅ **Protocol abstraction** (A2A/MCP support)
- ✅ **Performance optimization** (PostgreSQL/Redis caching)
- ✅ **Error handling & recovery** with AI-assisted debugging

### **Professional Development Practices**
- ✅ **Code intelligence** with Serena integration
- ✅ **Database design** for scalable caching
- ✅ **API design** following JSON-RPC standards
- ✅ **Security considerations** with URL validation
- ✅ **Documentation standards** with comprehensive README

### **Cutting-Edge Technology Integration**
- ✅ **Microsoft Playwright** (modern web automation)
- ✅ **AI/LLM integration** (multiple providers)
- ✅ **Real-time communication** (WebSocket support)
- ✅ **Containerization** (Docker support)
- ✅ **Protocol compliance** (MCP/A2A standards)

## 🔄 Development Workflow

### **Multi-Module Development**
```bash
# Working directory
cd /Users/wingston/code/a2aTravelAgent/a2awebagent

# Build entire project (both modules)
mvn clean compile

# Run application module specifically
mvn spring-boot:run -pl a2awebapp

# Docker single container rebuild
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d
```

### **Professional Testing**
```bash
# Test professional resume features
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "getWingstonsProjectsExpertiseResume",
    "arguments": {
      "query": "Show comprehensive technical expertise and project portfolio"
    }
  },
  "id": 1
}'

# Test LinkedIn professional search
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0", 
  "method": "tools/call",
  "params": {
    "name": "searchLinkedInProfile",
    "arguments": {
      "searchCriteria": "AI engineering expertise"
    }
  },
  "id": 2
}'
```

## 📊 Performance & Caching Architecture

### **Database Layer**
- **PostgreSQL**: Tool description caching with model/tool filtering
- **Redis**: High-performance session and data caching
- **Neo4j**: Async job tracking and agent analytics

### **Optimization Features**
- **CachedMCPToolsController**: Bridge between a2acore framework and PostgreSQL
- **ToolDescriptionCacheService**: Intelligent caching for tool descriptions
- **Async Processing**: Background job handling for intensive operations

## 🏆 Professional Portfolio Value

This project demonstrates:

1. **Full-Stack Development**: From database design to AI integration
2. **Modern Architecture**: Multi-module, microservices-ready design
3. **Protocol Implementation**: Standards-compliant MCP/A2A support
4. **Performance Engineering**: Intelligent caching and optimization
5. **AI Integration**: Multi-provider LLM capabilities
6. **Professional Tools**: LinkedIn integration and resume showcase
7. **Quality Standards**: Comprehensive testing and documentation

## 🎖️ "Professional Resume Agent" = Technical Excellence Showcase

This project serves as a **living technical resume** that demonstrates:
- **Advanced Spring Boot architecture** with multi-module design
- **Modern web automation** using Microsoft Playwright
- **AI integration expertise** across multiple providers
- **Database design** with PostgreSQL/Redis optimization
- **Protocol implementation** following MCP/A2A standards
- **Professional development** practices and code quality

**Perfect for demonstrating technical capabilities to potential employers, clients, or collaborators.**

