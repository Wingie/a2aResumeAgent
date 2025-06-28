#!/bin/bash

# Test script for new Wingston expertise tools
# Run this after restarting the server with the new tools

MCP_SERVER_URL="http://localhost:7860"

echo "🎯 Testing Wingston's New Expertise Tools"
echo "========================================="
echo ""

# Test 1: Check available tools
echo "📋 Test 1: Available Tools"
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc": "2.0", "method": "tools/list", "params": {}, "id": 1}' \
    --max-time 10 | jq -r '.result.tools[].name' | while read tool; do
    echo "  ✅ $tool"
done
echo ""

# Test 2: Get Wingston's Complete Resume
echo "📄 Test 2: Wingston's Complete Resume with ASCII Art"
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "getWingstonsProjectsExpertiseResume",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Show me the complete resume with all sections"
            }
        },
        "id": 2
    }' \
    --max-time 30 > wingston_resume_test.json

if [ -f "wingston_resume_test.json" ]; then
    echo "✅ Resume generated successfully"
    echo "📊 Response size: $(wc -c < wingston_resume_test.json) bytes"
    echo "🎨 Checking for ASCII art..."
    if grep -q "██" wingston_resume_test.json; then
        echo "✅ ASCII art found in resume!"
    else
        echo "❌ ASCII art not found"
    fi
else
    echo "❌ Resume generation failed"
fi
echo ""

# Test 3: AI/ML Expertise Focus
echo "🤖 Test 3: AI/ML Expertise Focus"
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "getWingstonsProjectsExpertiseResume",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Focus on ai-ml expertise and show detailed AI/ML background"
            }
        },
        "id": 3
    }' \
    --max-time 20 | jq -r '.result.content[0].text' | head -20
echo ""

# Test 4: LinkedIn Search Tool
echo "🔍 Test 4: LinkedIn Search Tool"
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "searchLinkedInProfile",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Search for Wingston Sharon from Booking.com"
            }
        },
        "id": 4
    }' \
    --max-time 120 > linkedin_search_test.json

if [ -f "linkedin_search_test.json" ]; then
    echo "✅ LinkedIn search completed"
    echo "📊 Response size: $(wc -c < linkedin_search_test.json) bytes"
    echo "🖼️  Checking for screenshot..."
    if jq -r '.result.content[0].data // .result.content[0].text' linkedin_search_test.json | head -c 50 | grep -q "^[A-Za-z0-9+/]"; then
        echo "✅ Screenshot data found!"
        # Extract screenshot
        jq -r '.result.content[0].data // .result.content[0].text' linkedin_search_test.json | base64 -d > wingston_linkedin_screenshot.png
        echo "📸 Screenshot saved as: wingston_linkedin_screenshot.png"
        if command -v file &> /dev/null; then
            file wingston_linkedin_screenshot.png
        fi
    else
        echo "📄 Text response received"
    fi
else
    echo "❌ LinkedIn search failed"
fi
echo ""

# Test 5: Audio Expertise Focus
echo "🎵 Test 5: Audio Expertise Focus"
curl -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -d '{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "getWingstonsProjectsExpertiseResume",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Show audio synthesis and music technology expertise"
            }
        },
        "id": 5
    }' \
    --max-time 15 | jq -r '.result.content[0].text' | head -15
echo ""

echo "🎉 Tool Testing Complete!"
echo ""
echo "📁 Generated Files:"
ls -la *.json *.png 2>/dev/null | grep -E "(wingston|linkedin)" || echo "  No files generated"
echo ""
echo "💡 Usage Examples:"
echo "  - View complete resume: jq -r '.result.content[0].text' wingston_resume_test.json"
echo "  - Extract LinkedIn screenshot: jq -r '.result.content[0].data' linkedin_search_test.json | base64 -d > screenshot.png"
echo "  - Open screenshot: open wingston_linkedin_screenshot.png (macOS)"