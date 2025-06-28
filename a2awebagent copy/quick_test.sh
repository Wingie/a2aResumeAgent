#!/bin/bash

# Quick Test Script for MCP Server
# Usage: ./quick_test.sh [timeout_seconds]

TIMEOUT=${1:-180}  # Default 3 minutes, or use first argument
MCP_SERVER_URL="http://localhost:7860"

echo "🚀 Quick MCP Server Test (timeout: ${TIMEOUT}s)"
echo "🌐 Server: $MCP_SERVER_URL"
echo ""

# Quick screenshot test
echo "📸 Testing screenshot capture..."
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "browseWebAndReturnImage",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Navigate to https://google.com/flights and take a screenshot"
            }
        },
        "id": 1
    }' \
    --max-time "$TIMEOUT" \
    -o "quick_screenshot_$(date +%H%M%S).json" \
    -w "HTTP Status: %{http_code}, Time: %{time_total}s\n"

echo ""
echo "📄 Testing text extraction..."
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "browseWebAndReturnText",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Navigate to https://booking.com/flights and extract the page title and main heading"
            }
        },
        "id": 2
    }' \
    --max-time "$TIMEOUT" \
    -s | jq -r '.result.content[0].text' | head -10

echo ""
echo "✅ Quick test completed!"
echo "📁 Screenshot response saved to: quick_screenshot_*.json"
echo ""
echo "💡 To extract screenshot:"
echo "  jq -r '.result.content[0].data // .result.content[0].text' quick_screenshot_*.json | base64 -d > screenshot.png"