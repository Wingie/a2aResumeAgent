CLAUDE.md - a2aWebAgent
MCP Web Automation Demo using Spring Boot + Playwright with basic tool examples.

🛠️ **Educational MCP Implementation** - Demonstrates browser automation, ImageContent handling, and simple tool integration.

⚠️ Critical: Project Structure - Three Components
a2aTravelAgent/
├── a2ajava/              # Maven Library (A2A/MCP protocols)
│   └── io.github.vishalmysore.*
└── a2awebagent/          # Multi-module Spring Boot App
    ├── a2acore/          # (NEW) Fast MCP Framework Library
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
Current Tools (7 available):

🛠️ generateMeme: Simple meme generation demo (HelloWorld replacement) using memegen.link
🍽️ askTasteBeforeYouWaste: Food safety research from tastebeforeyouwaste.org
📸 getTasteBeforeYouWasteScreenshot: Visual food safety guide capture
💼 searchLinkedInProfile: LinkedIn profile discovery with screenshots (features Wingston Sharon)
📝 getWingstonsProjectsExpertiseResume: Technical portfolio showcase
🌐 browseWebAndReturnText: General web automation with text extraction
📷 browseWebAndReturnImage: Web automation with ImageContent screenshot capture

Key Features:
- ImageContent MCP protocol support for all screenshot-based tools
- Base64 encoding for seamless image transfer
- Content-based screenshot naming with absolute paths
- Real-time browser automation via Playwright
- Automatic tool discovery via @Action annotations

Playwright Actions: Navigate, click, type, fill, screenshot, wait for selectors

🛠️ HelloWorld Meme Demo
The generateMeme tool is a simple HelloWorld replacement demonstrating basic MCP ImageContent:

Technical Flow:
1. Input: template (e.g., "drake"), topText, bottomText
2. URL Building: https://api.memegen.link/images/{template}/{topText}/{bottomText}.png
3. Browser Navigation: Playwright visits the meme URL
4. Screenshot Capture: Basic PNG capture via browser automation
5. ImageContent Response: Base64 encoded image returned via MCP protocol

Example Usage:
generateMeme("drake", "Hello World", "Hello Meme")
Result: Simple Drake meme demonstrating browser automation

Common Templates:
- drake: Basic choice demonstration
- distracted-boyfriend: Simple comparison
- woman-yelling-at-cat: Basic argument format
- this-is-fine: Acceptance scenario

Performance: ~1-3 seconds (basic browser automation demo)

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
