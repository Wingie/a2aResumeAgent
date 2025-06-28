#!/bin/bash

# Wingston Travel Agent MCP Runner Script
# Runs the travel agent with proper configuration

set -e

PROJECT_DIR="/Users/wingston/code/a2aTravelAgent/a2awebagent"
JAR_FILE="$PROJECT_DIR/target/a2awebagent-0.0.1.jar"

echo "🚀 Starting Wingston Travel Agent MCP Server..."
echo "============================================="

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: JAR file not found at $JAR_FILE"
    echo "Run ./build-mcp.sh first to build the project"
    exit 1
fi

# Check for API keys
JAVA_OPTS=""

if [ -n "$GEMINI_API_KEY" ]; then
    echo "✅ Using Gemini API key from environment"
    JAVA_OPTS="$JAVA_OPTS -DgeminiKey=$GEMINI_API_KEY"
fi

if [ -n "$CLAUDE_API_KEY" ]; then
    echo "✅ Using Claude API key from environment"
    JAVA_OPTS="$JAVA_OPTS -DclaudeKey=$CLAUDE_API_KEY"
fi

if [ -n "$OPENAI_API_KEY" ]; then
    echo "⚠️  OpenAI API key found but Gemini is preferred provider"
    # Don't set OpenAI key to avoid quota issues
    # JAVA_OPTS="$JAVA_OPTS -DopenAiKey=$OPENAI_API_KEY"
fi

if [ -n "$SERPER_API_KEY" ]; then
    echo "✅ Using Serper API key from environment"
    JAVA_OPTS="$JAVA_OPTS -DserperKey=$SERPER_API_KEY"
fi

# Set server port if provided
if [ -n "$SERVER_PORT" ]; then
    echo "🔌 Using custom port: $SERVER_PORT"
    JAVA_OPTS="$JAVA_OPTS -Dserver.port=$SERVER_PORT"
else
    echo "🔌 Using default port: 7860"
fi

echo ""
echo "🎯 Configuration:"
echo "   Java application: $JAR_FILE"
echo "   Java options: $JAVA_OPTS"
echo "   MCP server: $PROJECT_DIR/src/main/resources/mcpserver.js"
echo ""

# Start the application
echo "▶️  Starting application..."
java $JAVA_OPTS -jar "$JAR_FILE"