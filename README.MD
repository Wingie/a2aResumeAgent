# a2aTravelAgent - JAVA  enterprise based AI-Powered Travel Research Agent & MCP Demo

This repository contains A SAMPLE MCP that demonstrate advanced AI integration and web automation capabilities:

## 📁 Project Structure

### 2. **a2awebagent** (Web Automation Engine) 
- **Location**: `a2awebagent/` subdirectory
- **Purpose**: Selenium-based web automation and travel research
- **Key Features**: Web browsing, screenshot capture, data extraction, travel planning

## 🎯 Project Purpose

This project serves as:
- A technical portfolio demonstration of **AI-powered travel research**
- An educational example of **MCP protocol implementation**  
- A showcase of **advanced web automation capabilities**
- A **personal resume tool** with interactive AI features

**⚠️ Important**: For the resume example and testing, make sure you're running the **a2awebagent** project (not the main directory). 

![Agent Interface](agent.png)

## Features

- Web automation using Selenium WebDriver
- Flexible deployment modes:
  - Local A2A task execution
  - Remote 8-Way server integration
- Screenshot capture and validation
- Multi-agent workflow support
- Integration with other agents (e.g., Kafka, lock capture)

## Architecture

The system is built using Spring Boot and follows a modular architecture:

![Multiple Agent Architecture](multiple.png)

### Components

1. **Web Browsing Agent**: Handles Selenium-based web automation
2. **Agent Card Controller**: Manages agent capabilities and real-time communication
3. **Selenium A2A Controller**: Processes JSON-RPC requests for web automation
4. **Task Management**: Supports both local and remote task execution

## Getting Started

### Prerequisites

- Java 18 or higher
- Maven
- Chrome/Chromium browser

### Installation

1. Clone the repository
2. Install dependencies:
```bash
mvn install
```

### ✅ Technical Implementation Validation

This project demonstrates **production-ready** technical implementation:
- ✅ **Advanced Spring Boot architecture** 
- ✅ **MCP protocol implementation**
- ✅ **AI integration capabilities**
- ✅ **Web automation framework**
- ✅ **Professional error handling**
- ✅ **Proper dependency management**

### 🔑 API Key Configuration (Required)

**For anyone wanting to test this resume showcase project**, you'll need to provide your own API keys:

#### Method 1: Environment Variables
```bash
# Standard OpenAI
export OPENAI_API_KEY=sk-your-actual-key-here
export CLAUDE_KEY=sk-ant-your-key-here  
export GEMINI_KEY=your-gemini-key-here
java -jar target/a2awebagent-0.0.1.jar

# Mistral AI (Free Alternative) # But it cannot use a browser via Selenium
export OPENAI_API_KEY=your-mistral-api-key
export OPENAI_BASE_URL=https://api.mistral.ai/v1
export OPENAI_MODEL_NAME=mistral-large-latest
java -jar target/a2awebagent-0.0.1.jar
```

#### Method 2: JVM Arguments  
```bash
# Standard OpenAI
java -DopenAiKey=sk-your-actual-key-here \
     -DclaudeKey=sk-ant-your-key-here \
     -DgeminiKey=your-gemini-key-here \
     -jar target/a2awebagent-0.0.1.jar

# Mistral AI (Free Alternative via Le Chat)
java -DopenAiKey=your-mistral-api-key \
     -DopenAiBaseURL=https://api.mistral.ai/v1 \
     -DopenAiModelName=mistral-large-latest \
     -Dagent.provider=openai \
     -jar target/a2awebagent-0.0.1.jar
```

#### Method 3: Configuration Files (Recommended for Local Development)

**📁 Project-Specific Configuration**: Both projects have their own `tools4ai.properties` files:

- **Main Project**: `src/main/resources/tools4ai.properties`
- **Web Agent**: `a2awebagent/src/main/resources/tools4ai.properties`

Create or edit these files with your API keys:

```properties
##Gemini related settings (Recommended - Better AI understanding)
gemini.modelName=gemini-2.0-flash-001
gemini.location=us-central1
gemini.projectId=your-google-project-id
geminiKey=your-gemini-api-key-here
agent.provider=gemini

##Anthropic related settings
anthropic.modelName=claude-3-haiku-20240307
claudeKey=sk-ant-your-claude-key-here

##OpenAI Configuration
openAiKey=sk-your-openai-key-here
openAiModelName=gpt-4o-mini

##Mistral AI Configuration (Free Alternative)
openAiKey=your-mistral-api-key-here
openAiBaseURL=https://api.mistral.ai/v1
openAiModelName=mistral-large-latest
agent.provider=openai

##Serper Key for google search
serperKey=your-serper-key-here
```

**🔐 Security Note**: These files are automatically ignored by git to protect your API keys.

### 🎯 Why API Keys Are Required

This showcases **real AI integration** capabilities:
- **Natural language processing** for web automation commands
- **Multi-provider AI support** (OpenAI, Gemini, Anthropic, Mistral)
- **Intelligent command interpretation** and execution
- **Context-aware automation** decision making

### 🆓 Recommended: Mistral AI (Free Alternative) # But it cannot use a browser via Selenium

**Best option for testing**: Use Mistral AI via Le Chat platform:
- ✅ **Free tier available** through Le Chat foundation
- ✅ **High-quality large language model** (mistral-large-latest)
- ✅ **Compatible with OpenAI API format** (drop-in replacement)
- ✅ **Perfect for demonstration purposes**

Get your free Mistral API key at: https://console.mistral.ai/

## 🤖 MCP (Model Context Protocol) Setup with Gemini

This project provides a complete **MCP server implementation** that integrates with Claude Desktop for advanced AI-powered travel automation.

### Prerequisites for MCP Setup

1. **Google Cloud CLI** (for Gemini Vertex AI):
   ```bash
   # Install on macOS
   brew install --cask google-cloud-sdk
   ```

2. **Node.js** (for MCP proxy server):
   ```bash
   # Install via Homebrew
   brew install node
   ```

### Step-by-Step MCP Setup

#### 1. Google Cloud Authentication
```bash
# Authenticate with Google Cloud
gcloud auth application-default login

# Set your project (if needed)
gcloud config set project your-google-project-id
```

#### 2. Build the Complete MCP System
```bash
# Navigate to the web agent project
cd a2awebagent

# Run the automated build script
./build-mcp.sh
```

#### 3. Configure Vertex AI Gemini
The `tools4ai.properties` file should be configured for Vertex AI:
```properties
##Gemini related settings (Vertex AI)
gemini.modelName=gemini-2.0-flash-001
gemini.location=us-central1
gemini.projectId=your-google-project-id
geminiKey=
agent.provider=gemini
```

#### 4. Start the Travel Agent Server
```bash
# Start the Java application
java -jar target/a2awebagent-0.0.1.jar

# OR use the run script
./run-mcp.sh
```

#### 5. Configure Claude Desktop
Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "wingston-demo-agent": {
      "command": "node",
      "args": [
        "/Users/your-username/code/a2aTravelAgent/a2awebagent/src/main/resources/mcpserver.js"
      ]
    }
  }
}
```

#### 6. Restart Claude Desktop
Close and reopen Claude Desktop to load the MCP server.

### 🎯 Available MCP Tools

Once configured, Claude Desktop will have access to these tools:

- **`browseWebAndReturnText`** - Web automation with text extraction
- **`browseWebAndReturnImage`** - Web automation with screenshot capture  
- **`searchLinkedInProfile`** - LinkedIn profile search
- **`getWingstonsProjectsExpertiseResume`** - Personal expertise and resume showcase
- **`askTasteBeforeYouWaste`** - Food research and recommendations

### 🧪 Testing Your MCP Setup

1. **Verify server is running**:
   ```bash
   curl http://localhost:7860/v1/tools
   ```

2. **Test in Claude Desktop**:
   - Open Claude Desktop
   - Look for "wingston-demo-agent" in MCP settings
   - Try asking: "Can you help me research travel options from New York to Paris?"

### 🔧 Troubleshooting MCP Setup

**Common Issues:**

1. **Authentication errors**: Ensure `gcloud auth application-default login` was successful
2. **Port conflicts**: Check that port 7860 is available
3. **Node.js errors**: Verify Node.js dependencies with `npm install` in resources directory
4. **Claude Desktop not detecting**: Restart Claude Desktop after config changes

### 📖 Complete MCP Documentation

For detailed setup instructions and troubleshooting, see: `a2awebagent/MCP-SETUP.md`

### 🚀 Running the Projects

#### For Resume Example & Web Automation Testing:
```bash
# Navigate to the web agent project
cd a2awebagent

# Ensure your API keys are configured in tools4ai.properties
# Then build and run
mvn clean compile
mvn spring-boot:run
```

#### For Main Orchestration Server:
```bash
# In the root directory
mvn clean compile  
mvn spring-boot:run
```

**⚠️ Important for Resume Demo**: Always run the **`a2awebagent`** project (not the main directory) to access the interactive resume features and web automation capabilities.

### 🚀 Successful Startup Indicators

When properly configured, you'll see:
```
🎯 Testing Wingston's New Expertise Tools
📋 Available Tools:
  ✅ getWingstonsProjectsExpertiseResume
  ✅ searchLinkedInProfile  
  ✅ askTasteBeforeYouWaste
  ✅ browseWebAndReturnText
  ✅ browseWebAndReturnImage
  ✅ webPageAction

Server started on port 7860
```

### 🌐 Access Points

Once the server is running, you can access:

- **Main Application**: http://localhost:7860 (or 7861 if using custom port)
- **API Documentation**: http://localhost:7860/swagger-ui/index.html
- **MCP Tools List**: `curl -X POST http://localhost:7860 -H "Content-Type: application/json" -d '{"jsonrpc": "2.0", "method": "tools/list", "params": {}, "id": 1}'`

### 💡 "API Key Required" = Successful Technical Demonstration

If you see this error when starting without API keys:
```
OpenAiHttpException: You have exceeded your daily limit of free tokens for demonstration purposes. 
If you wish to continue, please use your own OpenAI API key.
```

## Usage

### Local Task Execution

Create a web action file (`web.action`):

```action
go to website https://the-internet.herokuapp.com
click on A/B Testing
save screenshot as "the-internet.herokuapp.com.png"
```

Run the agent:

```java
LocalA2ATaskClient client = context.getBean(LocalA2ATaskClient.class);
FilePart filePart = new FilePart();
// ... setup file content
Task t = client.sendFileTask(filePart);
```

### Task Management

Monitor task execution through the web interface:

![Task List](taskList.png)

### Real-time Communication

The system supports real-time updates through WebSocket:

![WebChat Interface](webchat.png)

## Event Flow

The following diagram illustrates the event flow in the system:

![Event Flow](event.png)

## Local Task Execution

Example of local task execution:

![Local Task](LocalTask.png)

## Sample Test Results

Here are sample test execution results:

![Test Result 1](the-internet.herokuapp.com.png)
![Test Result 2](the-internet.herokuapp.com_2.png)


