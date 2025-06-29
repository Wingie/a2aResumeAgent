# a2aTravelAgent Project Overview

## Executive Summary

The a2aTravelAgent project is a sophisticated web automation system that combines AI language models with browser automation to create intelligent agents capable of performing complex web research tasks. It implements both Google's A2A (Agent-to-Agent) protocol and Anthropic's MCP (Model Context Protocol), making it interoperable with various AI systems.

## Project Structure

```
a2aTravelAgent/
├── Main Project (Playwright-based)
│   ├── src/main/java/io/vishalmysore/
│   │   ├── Application.java              # Spring Boot entry point
│   │   ├── PlaywrightProcessor.java      # Core web automation interface
│   │   ├── PlaywrightActions.java        # AI-annotated action models
│   │   └── Various AI provider implementations
│   └── target/
│       └── a2aPlaywright-0.2.3.jar       # Deployable JAR
│
└── a2awebagent/ (Playwright-based subproject)
    ├── src/main/java/io/wingie/
    │   ├── Application.java              # Spring Boot entry point
    │   ├── WebBrowsingAction.java        # Core automation actions
    │   └── PlaywrightTaskController.java # A2A task handling
    ├── src/main/resources/
    │   └── web.action                    # Automation scripts
    └── target/
        └── a2awebagent-0.0.1.jar        # Deployable JAR
```

## Key Components

### 1. Main a2aTravelAgent (Playwright-based)

**Purpose**: Modern web automation using Microsoft Playwright with AI integration

**Key Features**:
- Fast and reliable browser automation
- Built-in error recovery with AI assistance
- Support for multiple AI providers (OpenAI, Gemini, Claude)
- URL safety validation
- Screenshot and text extraction capabilities

**Tools Exposed**:
- `browseWebAndReturnText`: Execute web actions and extract text
- `browseWebAndReturnImage`: Execute web actions and capture screenshots

### 2. a2awebagent Subproject (Playwright-based)

**Purpose**: Modern web automation using Microsoft Playwright

**Key Features**:
- A2A and MCP protocol support
- Web.action file execution for scripted automation
- Modern browser automation with Playwright
- Real-time WebSocket updates
- Multi-agent workflow support

**Tools Exposed**:
- `webPageAction`: Low-level Playwright actions
- `browseWebAndReturnText`: High-level text extraction
- `browseWebAndReturnImage`: Screenshot capture

## Protocol Support

### A2A (Agent-to-Agent) Protocol

Google's protocol for inter-agent communication:
- JSON-RPC based communication
- Tool discovery via `tools/list`
- Tool invocation via `tools/call`
- Agent capabilities exposed at `/.well-known/agent.json`

### MCP (Model Context Protocol)

Anthropic's protocol for AI tool integration:
- Requires MCP connector bridge (`mcp-connector-full.jar`)
- Translates MCP messages to A2A format
- Enables integration with Claude Desktop

## Travel Research Automation

The system specializes in travel research through:

### 1. Natural Language Processing
Users provide queries like: "Find flights from Amsterdam to Palma on July 6th"

### 2. AI Decomposition
The AI breaks down the request into actionable steps:
- Navigate to booking.com
- Fill search fields
- Extract results

### 3. Web Automation Execution
The web.action file contains comprehensive Booking.com automation:
- Flight searches (outbound and return)
- Hotel searches (by rating and price)
- Attraction discovery
- Data compilation into research reports

### 4. Result Aggregation
- Screenshots of search results
- Extracted pricing and availability data
- Structured travel itineraries

## Running the System

### Option 1: a2aTravelAgent (Playwright)
```bash
cd a2aTravelAgent
mvn spring-boot:run
# OR
java -jar target/a2aPlaywright-0.2.3.jar
```

### Option 2: a2awebagent (Playwright)
```bash
cd a2awebagent
mvn spring-boot:run
# OR
java -jar target/a2awebagent-0.0.1.jar
```

### Port Configuration
Both projects default to port 7860. To run simultaneously:
```bash
# Change port for one application
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=7861
```

## Integration Methods

### 1. Direct JSON-RPC API
```bash
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Search for flights on booking.com"
    }
  },
  "id": 1
}'
```

### 2. MCP Integration (Claude Desktop)
```json
{
  "webbrowsingagent": {
    "command": "java",
    "args": [
      "-jar",
      "/path/to/mcp-connector-full.jar",
      "http://localhost:7860/"
    ],
    "timeout": 30000
  }
}
```

### 3. Web Interface
Navigate to http://localhost:7860 for the interactive UI

## Use Cases

1. **Travel Planning**: Automated research across flight, hotel, and attraction sites
2. **Price Monitoring**: Track changes in travel costs over time
3. **Competitive Analysis**: Compare offerings across platforms
4. **Data Extraction**: Gather structured data from unstructured websites
5. **Test Automation**: Validate web applications with AI-assisted error recovery

## Technical Innovation

The project's key innovation is its **AI-powered self-healing automation**:

1. **Traditional Approach**: Fails when website structure changes
2. **a2aTravelAgent Approach**: 
   - Captures error context and screenshots
   - Sends to AI for analysis
   - Receives corrected instructions
   - Retries with new approach
   - Learns from failures

This makes the system remarkably resilient to website changes, solving a major pain point in web automation.

## Future Enhancements

Based on the current architecture, potential improvements include:

1. **Multi-site Support**: Extend beyond Booking.com to Expedia, Kayak, etc.
2. **Visual AI**: Use computer vision for element detection
3. **Distributed Execution**: Run automation across multiple browsers/machines
4. **API Fallbacks**: Use direct APIs when available
5. **Result Caching**: Store and reuse search results

## Conclusion

The a2aTravelAgent project represents a sophisticated approach to web automation that combines:
- Modern browser automation (Playwright)
- AI language understanding
- Multi-protocol support (A2A/MCP)
- Domain-specific optimization (travel research)
- Self-healing capabilities

This makes it an ideal solution for automating complex web research tasks, particularly in the travel domain, while remaining flexible enough for general web automation needs.