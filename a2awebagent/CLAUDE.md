CLAUDE.md - a2aTravelAgent
AI-Powered Travel Research Agent using Spring Boot + Playwright web automation with A2A/MCP protocol support.

⚠️ Critical: Project Structure - Three Components
a2aTravelAgent/
├── a2ajava/              # Maven Library (A2A/MCP protocols)
│   └── io.github.vishalmysore.*
└── a2awebagent/          # Multi-module Spring Boot App
    ├── a2acore/          # Fast MCP Framework Library
    │   └── io.wingie.a2acore.*
    └── a2awebapp/        # Web Automation Application  
        └── io.wingie.*
Key Architecture:

a2ajava: Standalone library for A2A/MCP protocols (Maven Central: v0.1.9.6)
a2acore: Fast-starting MCP framework (replaces heavy a2ajava dependency)
a2awebapp: Spring Boot app with Playwright, uses a2acore framework
Build Structure:

a2awebagent is a multi-module Maven aggregator (parent POM)
a2acore: Library module with MCP framework
a2awebapp: Application module depends on a2acore
Development Workflow
Working Directory: /Users/wingston/code/a2aTravelAgent/a2awebagent

bash
# Build entire project
mvn clean compile

# Run the application (from a2awebagent root)
mvn spring-boot:run -pl a2awebapp

# Docker single container rebuild
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d
Core Components
a2acore Framework:

A2aCoreController: MCP JSON-RPC endpoint
ToolDiscoveryService: Auto-discovery of @Action methods
JsonRpcHandler: Protocol message processing
Annotations: @EnableA2ACore, @Action, @Parameter
a2awebapp Application:

Application.java: Spring Boot main class
PlaywrightProcessor: Web automation interface
PlaywrightActions: Data model with AI annotations
CachedMCPToolsController: Performance optimization bridge
PostgreSQL + Redis caching services
Key Dependencies
a2acore: Spring 6.1.6, Jackson 2.16.2 (lightweight) a2awebapp: Spring Boot 3.2.4, Playwright 1.51.0, tools4ai 1.1.6.2

Configuration
Port: 7860 (change via application.properties) Database: PostgreSQL + Redis AI Keys: Set via env vars or -DopenAiKey=key

Web Automation APIs
Main Tools:

Web browsing with text/image extraction
Travel research via template system
LinkedIn search, screenshot utilities
Playwright Actions: Navigate, click, type, fill, screenshot, wait for selectors

Task Guidelines
Always use Task() for code changes:

bash
Task(
  description="Debug MCP integration", 
  prompt="Use find_symbol, read_file, analyze implementation"
)
Project-specific patterns:

Bridge pattern: a2acore handles MCP, a2awebapp provides caching
Multi-module builds require -pl a2awebapp for running
Tool descriptions cached in PostgreSQL via ToolDescriptionCacheService
Protocols
MCP: Fast JSON-RPC via a2acore framework A2A: Legacy support via a2ajava integration
