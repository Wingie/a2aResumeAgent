# Wingston MCP Agent Setup

This project provides a complete MCP (Model Context Protocol) server for web automation and multi-purpose AI agent capabilities using the a2ajava framework.

## 🚀 Quick Start

### 1. Build Everything
```bash
./build-mcp.sh
```

### 2. Set API Keys
```bash
export GEMINI_API_KEY="your_gemini_key_here"
export CLAUDE_API_KEY="your_claude_key_here"  # optional
export SERPER_API_KEY="your_serper_key_here"  # optional
```

### 3. Run the Server
```bash
./run-mcp.sh
```

### 4. Configure Claude Desktop
Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "wingston-mcp-agent": {
      "command": "node",
      "args": [
        "/Users/wingston/code/a2aTravelAgent/a2awebagent/src/main/resources/mcpserver.js"
      ]
    }
  }
}
```

### 5. Restart Claude Desktop

## 🛠️ Manual Build Steps

If you prefer to build manually:

```bash
# Build Java application
mvn clean package -DskipTests

# Install Node.js dependencies
cd src/main/resources
npm install

# Run with API key
java -DgeminiKey=your_key -jar target/a2awebagent-0.0.1.jar
```

## 🔧 Available Tools

The MCP server exposes these tools to Claude:

- **browseWebAndReturnText** - Web automation with text extraction
- **browseWebAndReturnImage** - Web automation with screenshot capture
- **searchLinkedInProfile** - LinkedIn profile search
- **getWingstonsProjectsExpertiseResume** - Personal expertise and resume
- **askTasteBeforeYouWaste** - Food research and recommendations

## 🌐 API Endpoints

Once running, these endpoints are available:

- `GET http://localhost:7860/v1/tools` - List available tools
- `POST http://localhost:7860/v1/tools/call` - Execute tools
- `GET http://localhost:7860/v1/config` - Server configuration
- `GET http://localhost:7860/.well-known/agent.json` - Agent discovery

## 🔐 Security & Configuration

### API Key Management
- **Environment Variables** (Recommended):
  ```bash
  export GEMINI_API_KEY="your_key"
  ./run-mcp.sh
  ```

- **JVM Arguments**:
  ```bash
  java -DgeminiKey=your_key -jar target/a2awebagent-0.0.1.jar
  ```

- **Properties File** (Not recommended for production):
  Edit `src/main/resources/tools4ai.properties`

### Provider Configuration
Current configuration uses Gemini V2 as the primary AI provider:
```properties
agent.provider=gemini
gemini.modelName=gemini-2.0-flash-001
```

## 🧪 Testing

Test the MCP integration:

```bash
# Test tools listing
curl http://localhost:7860/v1/tools

# Test tool execution
curl -X POST http://localhost:7860/v1/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "getWingstonsProjectsExpertiseResume",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Tell me about Wingston's AI and web development expertise"
    }
  }'
```

## 🗂️ Project Structure

```
a2awebagent/
├── build-mcp.sh              # Build script
├── run-mcp.sh                # Run script
├── MCP-SETUP.md              # This file
├── src/main/
│   ├── java/io/wingie/
│   │   ├── MCPController.java # MCP REST endpoints
│   │   ├── MainEntryPoint.java # A2A JSON-RPC endpoints
│   │   └── ...
│   └── resources/
│       ├── mcpserver.js       # Node.js MCP proxy
│       ├── package.json       # Node.js dependencies
│       └── tools4ai.properties # Configuration
└── target/
    └── a2awebagent-0.0.1.jar  # Built application
```

## 🔄 Protocol Architecture

```
Claude Desktop (MCP Client)
    ↓ MCP Protocol
Node.js Proxy (mcpserver.js)
    ↓ HTTP REST
Spring Boot App (MCPController)
    ↓ Internal calls
a2ajava Framework (tools4ai)
    ↓ Web automation
Microsoft Playwright
```

## 🐛 Troubleshooting

### Common Issues

1. **"Tool not found" errors**
   - Ensure the Java application is running on port 7860
   - Check that the MCP controller is properly initialized

2. **Authentication errors**
   - Verify API keys are set correctly
   - Check logs for specific error messages

3. **Node.js proxy issues**
   - Ensure Node.js dependencies are installed: `npm install`
   - Check that mcpserver.js has correct permissions

4. **Port conflicts**
   - Change server port: `export SERVER_PORT=7861`
   - Update mcpserver.js to use the new port

### Logs
- Java application logs: Console output
- MCP proxy logs: `/temp/proxy_server.log` (if enabled)
- Claude Desktop logs: `~/Library/Logs/Claude/`

## 📝 Development

To extend the MCP server:

1. Add new tools in Java classes with `@Action` annotations
2. The a2ajava framework automatically discovers and exposes them
3. Rebuild with `./build-mcp.sh`
4. Restart the server

## 🤝 Support

For issues with:
- **a2ajava framework**: https://github.com/vishalmysore/a2ajava
- **MCP protocol**: https://modelcontextprotocol.io/
- **This implementation**: Check logs and configuration