#!/bin/bash

# Wingston Travel Agent MCP Build Script
# Builds Java application and Node.js MCP server

set -e  # Exit on any error

PROJECT_DIR="/Users/wingston/code/a2aTravelAgent/a2awebagent"
RESOURCES_DIR="$PROJECT_DIR/src/main/resources"
TARGET_DIR="$PROJECT_DIR/target"

echo "🚀 Building Wingston Travel Agent MCP Server..."
echo "================================================"

# Check if we're in the right directory
if [ ! -f "$PROJECT_DIR/pom.xml" ]; then
    echo "❌ Error: pom.xml not found in $PROJECT_DIR"
    echo "Please run this script from the correct directory"
    exit 1
fi

# Step 1: Clean and build Java application
echo "📦 Step 1: Building Java application..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Java build successful"
else
    echo "❌ Java build failed"
    exit 1
fi

# Step 2: Install Node.js dependencies
echo "📦 Step 2: Installing Node.js dependencies..."
cd "$RESOURCES_DIR"

if [ ! -f "package.json" ]; then
    echo "❌ Error: package.json not found in $RESOURCES_DIR"
    exit 1
fi

npm install

if [ $? -eq 0 ]; then
    echo "✅ Node.js dependencies installed"
else
    echo "❌ Node.js installation failed"
    exit 1
fi

# Step 3: Verify MCP server script
echo "🔍 Step 3: Verifying MCP server script..."
if [ ! -f "$RESOURCES_DIR/mcpserver.js" ]; then
    echo "❌ Error: mcpserver.js not found in $RESOURCES_DIR"
    exit 1
fi

echo "✅ MCP server script found"

# Step 4: Check for API keys configuration
echo "🔐 Step 4: Checking API key configuration..."
if grep -q "geminiKey=" "$RESOURCES_DIR/tools4ai.properties" && grep -q "geminiKey=$" "$RESOURCES_DIR/tools4ai.properties"; then
    echo "⚠️  Warning: Gemini API key is empty in tools4ai.properties"
    echo "   Set it via: export GEMINI_API_KEY='your_key'"
    echo "   Or use: -DgeminiKey=your_key when running"
fi

# Step 5: Display runtime information
echo ""
echo "🎉 Build Complete!"
echo "=================="
echo ""
echo "📋 Runtime Instructions:"
echo "1. Start Java application:"
echo "   java -jar $TARGET_DIR/a2awebagent-0.0.1.jar"
echo ""
echo "2. Or with Gemini key:"
echo "   java -DgeminiKey=your_key -jar $TARGET_DIR/a2awebagent-0.0.1.jar"
echo ""
echo "3. Claude Desktop Configuration:"
echo '   "wingston-travel-agent": {'
echo '     "command": "node",'
echo '     "args": ['
echo "       \"$RESOURCES_DIR/mcpserver.js\""
echo '     ]'
echo '   }'
echo ""
echo "4. Test MCP endpoints:"
echo "   curl http://localhost:7860/v1/tools"
echo "   curl http://localhost:7860/v1/config"
echo ""
echo "🔧 Available Tools:"
java -cp "$TARGET_DIR/a2awebagent-0.0.1.jar" -Dspring.main.web-application-type=none -Dspring.application.name=tools-list org.springframework.boot.loader.launch.JarLauncher --help 2>/dev/null || echo "   - browseWebAndReturnText"
echo "   - browseWebAndReturnImage"
echo "   - searchLinkedInProfile"
echo "   - getWingstonsProjectsExpertiseResume"
echo "   - askTasteBeforeYouWaste"
echo ""
echo "✅ Ready to use with Claude Desktop!"