#!/bin/bash

# Connection Test Script for a2aTravelAgent MCP Server
# This script tests basic connectivity and server status

echo "🔍 Testing MCP Server Connection"
echo "================================="

# Test different possible ports
PORTS=(7860 7861 7862 8080)
FOUND_PORT=""

for port in "${PORTS[@]}"; do
    echo "Testing port $port..."
    if curl -s --connect-timeout 3 --max-time 5 "http://localhost:$port" > /dev/null 2>&1; then
        echo "✅ Server responding on port $port"
        FOUND_PORT=$port
        break
    else
        echo "❌ No response on port $port"
    fi
done

if [ -z "$FOUND_PORT" ]; then
    echo ""
    echo "🚨 No server found on any port!"
    echo "💡 Make sure you've started the server with:"
    echo "   java -jar target/a2awebagent-0.0.1.jar"
    echo ""
    echo "🔍 Checking if any Java processes are running:"
    ps aux | grep java | grep -v grep
    exit 1
fi

SERVER_URL="http://localhost:$FOUND_PORT"
echo ""
echo "🎯 Found server at: $SERVER_URL"
echo ""

# Test 1: Basic HTTP response
echo "📡 Test 1: Basic HTTP Response"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER_URL")
echo "   HTTP Status: $HTTP_STATUS"

if [ "$HTTP_STATUS" = "200" ]; then
    echo "   ✅ Server is responding"
else
    echo "   ⚠️  Unexpected HTTP status"
fi

# Test 2: Agent Card
echo ""
echo "🏷️  Test 2: Agent Card"
AGENT_RESPONSE=$(curl -s --max-time 10 "$SERVER_URL/.well-known/agent.json")
if [ $? -eq 0 ] && [ -n "$AGENT_RESPONSE" ]; then
    echo "   ✅ Agent card retrieved"
    echo "$AGENT_RESPONSE" | jq '.name, .capabilities' 2>/dev/null || echo "   Raw response: $AGENT_RESPONSE"
else
    echo "   ❌ Failed to get agent card"
fi

# Test 3: MCP Tools List
echo ""
echo "🛠️  Test 3: MCP Tools List"
TOOLS_REQUEST='{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 1
}'

TOOLS_RESPONSE=$(curl -s --max-time 10 -X POST "$SERVER_URL" \
    -H "Content-Type: application/json" \
    -d "$TOOLS_REQUEST")

if [ $? -eq 0 ] && [ -n "$TOOLS_RESPONSE" ]; then
    echo "   ✅ Tools list retrieved"
    echo "$TOOLS_RESPONSE" | jq -r '.result.tools[]?.name' 2>/dev/null | while read tool; do
        echo "     - $tool"
    done
else
    echo "   ❌ Failed to get tools list"
fi

# Test 4: Simple MCP Call
echo ""
echo "🧪 Test 4: Simple MCP Call (Quick Text Test)"
SIMPLE_REQUEST='{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to https://httpbin.org/html and get the page title"
        }
    },
    "id": 2
}'

echo "   ⏳ Making MCP call (30s timeout)..."
SIMPLE_RESPONSE=$(curl -s --max-time 30 -X POST "$SERVER_URL" \
    -H "Content-Type: application/json" \
    -d "$SIMPLE_REQUEST")

if [ $? -eq 0 ] && [ -n "$SIMPLE_RESPONSE" ]; then
    echo "   ✅ MCP call completed"
    
    # Check if we got an error
    ERROR_MSG=$(echo "$SIMPLE_RESPONSE" | jq -r '.error.message' 2>/dev/null)
    if [ "$ERROR_MSG" != "null" ] && [ -n "$ERROR_MSG" ]; then
        echo "   ⚠️  Error: $ERROR_MSG"
    else
        echo "   📄 Response preview:"
        echo "$SIMPLE_RESPONSE" | jq -r '.result.content[0].text' 2>/dev/null | head -c 200 | tr '\n' ' '
        echo "..."
    fi
else
    echo "   ❌ MCP call failed or timed out"
fi

# Summary
echo ""
echo "📊 Connection Test Summary"
echo "========================="
echo "Server URL: $SERVER_URL"
echo "Status: $([ -n "$FOUND_PORT" ] && echo "✅ Connected" || echo "❌ Failed")"
echo ""

if [ -n "$FOUND_PORT" ]; then
    echo "🎉 Server is working! You can now run:"
    echo "   ./test_mcp_server.sh      # Full test suite"
    echo "   ./debug_screenshot.sh     # Screenshot debugging"
    echo "   ./quick_test.sh 300       # Quick test with 5min timeout"
else
    echo "🔧 Troubleshooting:"
    echo "   1. Make sure server is started: java -jar target/a2awebagent-0.0.1.jar"
    echo "   2. Check for port conflicts: lsof -i :7860"
    echo "   3. Check server logs for errors"
fi