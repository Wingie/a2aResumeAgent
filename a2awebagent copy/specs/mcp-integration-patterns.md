# MCP Integration Patterns for Wingie Async Agent

## Analysis of Existing MCP Setup

Based on your Claude Desktop configuration, I've identified several excellent patterns we can adopt for the Wingie async browsing agent.

## Current MCP Servers in Your Setup

### 1. Web Automation & Research
- **puppeteer**: Browser automation via Puppeteer
- **deepwebresearch**: Deep web research capabilities  
- **webbrowsingagent**: Your current a2awebagent setup

### 2. Development Tools
- **filesystem**: File system access for `/Users/wingston/code`
- **codemcp**: Code development assistance
- **sequential-thinking**: Structured thinking processes

### 3. Creative & Multimedia
- **mcp-youtube**: YouTube integration
- **canva**: Design and creative tools
- **ableton-mcp**: Music production integration

### 4. System Integration
- **@mcp-get-community-server-macos**: macOS system integration
- **serena**: Code indexing and search

## UX/API Patterns to Adopt

### 1. Timeout Configuration Pattern
Your current `webbrowsingagent` uses a 30-second timeout:
```json
"timeout": 30000
```

**Wingie Enhancement**: Implement dynamic timeouts based on job complexity
```json
{
  "command": "java",
  "args": ["-jar", "/path/to/wingie-async-agent.jar"],
  "timeout": 120000, // Extended for async operations
  "retries": 3,
  "fallbackTimeout": 30000
}
```

### 2. Multi-Server Architecture Pattern
Your setup shows multiple specialized servers working together.

**Wingie Opportunity**: Create a suite of related servers:
```json
{
  "wingie-browser": {
    "command": "java",
    "args": ["-jar", "/path/to/wingie-browser-agent.jar"],
    "timeout": 60000
  },
  "wingie-analyzer": {
    "command": "java", 
    "args": ["-jar", "/path/to/wingie-analysis-agent.jar"],
    "timeout": 30000
  },
  "wingie-scheduler": {
    "command": "java",
    "args": ["-jar", "/path/to/wingie-job-scheduler.jar"],
    "timeout": 10000
  }
}
```

### 3. Directory-Based Execution (from Serena)
Serena uses directory specification:
```json
"args": ["run", "--directory", "/Users/wingston/code/serena", "serena-mcp-server"]
```

**Wingie Application**: Support multiple configurations
```json
{
  "wingie-travel": {
    "command": "java",
    "args": [
      "-jar", "/path/to/wingie-agent.jar",
      "--config-dir", "/Users/wingston/code/wingie-configs/travel",
      "--profile", "booking.com"
    ]
  },
  "wingie-ecommerce": {
    "command": "java",
    "args": [
      "-jar", "/path/to/wingie-agent.jar", 
      "--config-dir", "/Users/wingston/code/wingie-configs/ecommerce",
      "--profile", "amazon"
    ]
  }
}
```

## Enhanced API Endpoints Based on MCP Patterns

### 1. Tool Discovery (Following MCP Standard)
```
GET /.well-known/mcp-capabilities
{
  "capabilities": {
    "tools": {
      "list": true,
      "call": true
    },
    "resources": {
      "list": true,
      "read": true,
      "subscribe": true
    },
    "prompts": {
      "list": true,
      "get": true
    },
    "logging": {
      "level": "info"
    }
  }
}
```

### 2. Resource-Based API (Inspired by Filesystem MCP)
```
GET /resources/list
{
  "resources": [
    {
      "uri": "job://current",
      "name": "Current Jobs",
      "description": "List of active jobs",
      "mimeType": "application/json"
    },
    {
      "uri": "screenshot://latest", 
      "name": "Latest Screenshots",
      "description": "Recent screenshots from jobs",
      "mimeType": "image/png"
    }
  ]
}

GET /resources/read?uri=job://12345
{
  "contents": [
    {
      "uri": "job://12345",
      "mimeType": "application/json",
      "text": "{\"status\": \"processing\", \"progress\": 45}"
    }
  ]
}
```

### 3. Prompt Templates (Following Sequential-Thinking Pattern)
```
GET /prompts/list
{
  "prompts": [
    {
      "name": "travel_search",
      "description": "Search for travel options",
      "arguments": [
        {
          "name": "origin",
          "description": "Departure city",
          "required": true
        },
        {
          "name": "destination", 
          "description": "Arrival city",
          "required": true
        },
        {
          "name": "date",
          "description": "Travel date",
          "required": true
        }
      ]
    }
  ]
}
```

### 4. Real-time Subscriptions (Enhanced)
```
POST /resources/subscribe
{
  "uri": "job://stream",
  "subscriptionId": "sub-123"
}

// Server sends updates
{
  "method": "notifications/resources/updated",
  "params": {
    "uri": "job://12345", 
    "subscriptionId": "sub-123",
    "data": {
      "progress": 67,
      "currentStep": "Extracting flight prices"
    }
  }
}
```

## UX Enhancements Inspired by Your Setup

### 1. Smart Fallbacks (from Multiple Browser Tools)
Your setup has both `puppeteer` and `webbrowsingagent`. Wingie could:
- Try Playwright first (faster)
- Fallback to Selenium if Playwright fails
- Use different strategies based on website compatibility

### 2. Integrated Workflow (from Creative Tools Integration)
Like `ableton-mcp` for music, Wingie could integrate with:
- **Travel Planning Apps**: Export results to TripIt, Google Calendar
- **Expense Tracking**: Send price data to expense management tools
- **Communication**: Auto-send travel options via Slack/email

### 3. Context Awareness (from Filesystem/Code Integration)
Leverage the filesystem MCP to:
- Save travel research to user's preferred folders
- Read user's travel history from files
- Auto-organize screenshots by destination/date

## Proposed Wingie MCP Configuration

### Option 1: Single Async Agent
```json
{
  "wingie-async": {
    "command": "java",
    "args": [
      "-jar", "/Users/wingston/code/a2aTravelAgent/target/wingie-async-agent.jar",
      "--async-mode", "true",
      "--max-concurrent-jobs", "5",
      "--redis-url", "redis://localhost:6379"
    ],
    "timeout": 300000,
    "env": {
      "WINGIE_PROFILE": "travel",
      "WINGIE_CONFIG_DIR": "/Users/wingston/code/wingie-configs"
    }
  }
}
```

### Option 2: Microservice Architecture
```json
{
  "wingie-jobs": {
    "command": "java",
    "args": ["-jar", "/path/to/wingie-job-manager.jar"],
    "timeout": 10000
  },
  "wingie-browser": {
    "command": "java", 
    "args": ["-jar", "/path/to/wingie-browser.jar"],
    "timeout": 120000
  },
  "wingie-storage": {
    "command": "java",
    "args": ["-jar", "/path/to/wingie-storage.jar"],
    "timeout": 30000
  }
}
```

## New Endpoint Categories Inspired by MCP Patterns

### 1. Workflow Endpoints (Sequential-Thinking Style)
```
POST /workflows/travel-planning
{
  "steps": [
    {"action": "search_flights", "params": {...}},
    {"action": "search_hotels", "params": {...}},
    {"action": "compare_prices", "params": {...}},
    {"action": "generate_report", "params": {...}}
  ]
}
```

### 2. Integration Endpoints (Cross-Tool Communication)
```
POST /integrations/export
{
  "target": "filesystem",
  "format": "json",
  "path": "/Users/wingston/travel-plans/",
  "jobId": "job-123"
}

POST /integrations/calendar
{
  "target": "calendar",
  "jobId": "job-123",
  "eventType": "travel"
}
```

### 3. Smart Context Endpoints
```
GET /context/user-preferences
{
  "preferredAirlines": ["KLM", "Lufthansa"],
  "budgetRange": {"min": 500, "max": 1500},
  "travelStyle": "business"
}

POST /context/learn-from-search
{
  "jobId": "job-123",
  "userFeedback": "preferred the morning flights"
}
```

## Implementation Recommendations

1. **Start with Single Async Agent**: Easier to manage, deploy, and debug
2. **Add MCP Standard Compliance**: Follow MCP protocol for better integration
3. **Implement Resource Subscriptions**: Real-time updates are crucial for async UX
4. **Support Multiple Profiles**: Travel, e-commerce, research, etc.
5. **Add Smart Fallbacks**: Multiple automation strategies
6. **Enable Cross-Tool Integration**: Leverage your existing MCP ecosystem

This approach will make Wingie a first-class citizen in your MCP ecosystem while providing superior async capabilities.