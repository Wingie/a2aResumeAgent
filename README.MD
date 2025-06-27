# A2A MCP Playwright Web Automation Agent

A Spring Boot-based web automation agent that provides both A2A (Agent-to-Agent) and MCP (Model Context Protocol) support using Playwright for browser automation and natural language processing.

## 🚀 Features

- **Web Automation**: Full Playwright integration for browser control
- **Multi-Protocol Support**: A2A and MCP protocol compatibility  
- **AI Integration**: Support for OpenAI, Google Gemini, and Anthropic models
- **Natural Language Processing**: Convert plain English commands to web actions
- **Safety Features**: Built-in URL validation and error handling
- **Screenshot & Text Extraction**: Capture page content and images
- **Docker Ready**: Containerized deployment with pre-built images

## 📋 Requirements

- Java 17+
- Maven 3.1+
- Docker (optional)

## 🛠️ Building & Running

### Local Development

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/a2aPlaywright-0.2.3.jar

# With custom configuration
java -Dloader.path=/path/to/config -jar target/a2aPlaywright-0.2.3.jar
```

### Docker Deployment

```bash
# Build Docker image
docker build -t a2a-playwright .

# Run container
docker run -p 7860:7860 a2a-playwright
```

## ⚙️ Configuration

### Application Settings

Edit `src/main/resources/application.properties`:
```properties
server.port=7860
a2a.persistence=cache
```

### AI Provider Configuration

Configure AI models in `src/main/resources/tools4ai.properties`:

```properties
# OpenAI
openAiKey=your_openai_api_key
openAiModelName=gpt-4o-mini
openAiBaseURL=https://api.openai.com/v1

# Google Gemini
gemini.modelName=gemini-2.0-flash-001
gemini.projectId=your_project_id
gemini.location=us-central1

# Anthropic Claude
claudeKey=your_claude_api_key
anthropic.modelName=claude-3-haiku-20240307

# Default provider
agent.provider=openai
```

### Runtime Configuration

You can also set API keys via system properties:
```bash
java -DopenAiKey=sk-xxx -DclaudeKey=sk-ant-xxx -jar target/a2aPlaywright-0.2.3.jar
```

## 🔧 API Usage

The server exposes JSON-RPC endpoints on port 7860.

### Text Extraction Example

```bash
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Go to google.com and search for a2ajava"
        }
    },
    "id": 1
}'
```

### Screenshot Capture Example

```bash
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
    "jsonrpc": "2.0",
    "method": "tools/call", 
    "params": {
        "name": "browseWebAndReturnImage",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to https://example.com and take a screenshot"
        }
    },
    "id": 2
}'
```

## 🌐 Web Interface

- **Application**: http://localhost:7860
- **API Documentation**: http://localhost:7860/swagger-ui.html  
- **Health Check**: http://localhost:7860/actuator/health

## 🎯 Supported Actions

The agent can perform the following web automation tasks:

- **Navigation**: Navigate to URLs with safety validation
- **Interaction**: Click elements, fill forms, type text
- **Data Extraction**: Extract text content from elements
- **Visual Capture**: Take full-page or element screenshots
- **Waiting**: Wait for elements or page states
- **Scrolling**: Scroll to specific elements

## 🔐 Security Features

- **URL Safety Validation**: Prevents navigation to malicious sites
- **Error Handling**: Robust exception management and retry logic
- **Sandbox Environment**: Isolated browser contexts
- **Input Validation**: AI-powered command validation

## 🏗️ Architecture

### Core Components

- **Spring Boot Application**: Main application framework
- **Playwright Integration**: Browser automation engine
- **A2A Framework**: Agent-to-agent communication
- **Tools4AI**: AI model integration and prompt processing
- **Callback System**: Extensible action processing pipeline

### Protocol Support

#### A2A Protocol
- Agent registration via a2ajava framework
- JSON-RPC tool calling capabilities  
- Natural language command processing
- Response handling through callbacks

#### MCP Protocol  
- Direct integration with MCP network
- Standardized message format
- Tool registration and discovery
- Asynchronous operation support

## 🐳 Docker Configuration

The application includes a multi-stage Dockerfile:

```dockerfile
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

# Install dependencies
RUN apt-get update && apt-get install -y curl

# Set version and download JAR
ARG VERSION=0.2.3
RUN curl -L -o /ai/mcpdemo.jar \
    https://github.com/vishalmysore/a2aPlaywright/releases/download/hugginface/a2aPlaywright-${VERSION}.jar

# Configure and run
EXPOSE 7860
ENTRYPOINT ["/entrypoint.sh"]
```

## 📝 Development

### Project Structure

```
src/main/java/io/vishalmysore/
├── Application.java                    # Spring Boot main class
├── PlaywrightProcessor.java           # Core automation interface  
├── PlaywrightActions.java             # Action data model
├── PlaywrightOpenAIProcessor.java     # OpenAI integration
├── PlayWrightGeminiProcessor.java     # Gemini integration
├── PlaywrightScriptProcessor.java     # Script execution engine
├── PWScreenShotAndTextCallback.java   # Screenshot/text callback
├── URLSafety.java                     # URL validation model
└── WebBrowsingAction.java             # Web action definitions
```

### Adding New AI Providers

1. Extend `PlaywrightProcessor` interface
2. Implement required methods for browser interaction
3. Add configuration properties to `tools4ai.properties`
4. Register the processor in the callback system

### Extending Automation Capabilities

1. Add new action types to `PlaywrightActions`
2. Implement action handlers in `PlaywrightProcessor`
3. Update prompt descriptions for AI understanding
4. Test with natural language commands

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

## 📄 License

This project is proprietary software. All rights reserved.

## 🐛 Troubleshooting

### Common Issues

**Build failures**: Ensure Java 17+ and Maven 3.1+ are installed
**Browser issues**: Playwright automatically downloads browser binaries
**API key errors**: Verify AI provider API keys are correctly configured
**Port conflicts**: Change server.port in application.properties

### Logs and Debugging

The application uses standard Spring Boot logging. Enable debug mode:

```bash
java -jar target/a2aPlaywright-0.2.3.jar --logging.level.io.vishalmysore=DEBUG
```

## 📞 Support

For issues and feature requests, please use the project's issue tracker.