#!/bin/bash

# a2aTravelAgent MCP Server Test Script
# Tests web automation functionality with proper timeouts

set -e

MCP_SERVER_URL="http://localhost:7860"
TIMEOUT=300  # 5 minutes for complex operations
TEST_DIR="test_results_$(date +%Y%m%d_%H%M%S)"

echo "🚀 Starting a2aTravelAgent MCP Server Tests"
echo "📁 Test results will be saved to: $TEST_DIR"
mkdir -p "$TEST_DIR"

# Function to test MCP endpoint with timeout
test_mcp_call() {
    local test_name="$1"
    local tool_name="$2"
    local request_body="$3"
    local timeout="${4:-$TIMEOUT}"
    
    echo "⏳ Testing: $test_name (timeout: ${timeout}s)"
    
    local start_time=$(date +%s)
    
    curl -X POST "$MCP_SERVER_URL" \
        -H "Content-Type: application/json" \
        -d "$request_body" \
        --max-time "$timeout" \
        --connect-timeout 10 \
        -o "$TEST_DIR/${test_name}.json" \
        -w "HTTP Status: %{http_code}, Total Time: %{time_total}s\n" 2>/dev/null
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ -f "$TEST_DIR/${test_name}.json" ]; then
        local response_size=$(wc -c < "$TEST_DIR/${test_name}.json")
        echo "✅ $test_name completed in ${duration}s (Response: ${response_size} bytes)"
        
        # Check if it's a screenshot response and extract base64 data
        if [[ "$tool_name" == "browseWebAndReturnImage" ]]; then
            echo "🖼️  Extracting screenshot from response..."
            extract_screenshot "$test_name"
        fi
        
        # Show first 200 characters of text responses
        if [[ "$tool_name" == "browseWebAndReturnText" ]]; then
            echo "📄 Response preview:"
            jq -r '.result.content[0].text' "$TEST_DIR/${test_name}.json" 2>/dev/null | head -c 200 | tr '\n' ' '
            echo "..."
        fi
    else
        echo "❌ $test_name failed or timed out"
    fi
    echo ""
}

# Function to extract screenshot from JSON response
extract_screenshot() {
    local test_name="$1"
    local json_file="$TEST_DIR/${test_name}.json"
    
    # Try to extract base64 image data
    local base64_data=$(jq -r '.result.content[0].text' "$json_file" 2>/dev/null)
    
    if [[ "$base64_data" != "null" && "$base64_data" =~ ^[A-Za-z0-9+/].*=*$ ]]; then
        echo "$base64_data" | base64 -d > "$TEST_DIR/${test_name}.png" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "🖼️  Screenshot saved as: $TEST_DIR/${test_name}.png"
            # Get image info if 'file' command is available
            if command -v file &> /dev/null; then
                file "$TEST_DIR/${test_name}.png"
            fi
        else
            echo "⚠️  Failed to decode base64 screenshot data"
        fi
    else
        echo "⚠️  No valid base64 image data found in response"
    fi
}

# Test 1: Server Health Check
echo "🔍 Test 1: Server Health Check"
curl -s "$MCP_SERVER_URL/.well-known/agent.json" > "$TEST_DIR/agent_card.json"
if [ $? -eq 0 ]; then
    echo "✅ Agent card retrieved"
    jq '.name, .capabilities' "$TEST_DIR/agent_card.json"
else
    echo "❌ Server not responding"
    exit 1
fi
echo ""

# Test 2: List Available Tools
echo "🔍 Test 2: List Available Tools"
test_mcp_call "tools_list" "tools/list" '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 1
}' 30

echo "Available tools:"
jq -r '.result.tools[].name' "$TEST_DIR/tools_list.json" 2>/dev/null | while read tool; do
    echo "  - $tool"
done
echo ""

# Test 3: Simple Web Navigation (Fast)
echo "🔍 Test 3: Simple Web Navigation"
test_mcp_call "simple_navigation" "browseWebAndReturnText" '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to https://httpbin.org/html and extract the page title"
        }
    },
    "id": 2
}' 60

# Test 4: Screenshot Test (Medium)
echo "🔍 Test 4: Screenshot Test"
test_mcp_call "screenshot_test" "browseWebAndReturnImage" '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnImage",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to https://example.com and take a screenshot"
        }
    },
    "id": 3
}' 120

# Test 5: Google Search Test (Medium)
echo "🔍 Test 5: Google Search Test"
test_mcp_call "google_search" "browseWebAndReturnText" '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Go to google.com, search for \"selenium automation\", and extract the first 3 search result titles"
        }
    },
    "id": 4
}' 180

# Test 6: Google Search Screenshot (Medium)
echo "🔍 Test 6: Google Search Screenshot"
test_mcp_call "google_screenshot" "browseWebAndReturnImage" '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnImage",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to google.com, search for \"web automation\", and take a screenshot of the results page"
        }
    },
    "id": 5
}' 180

# Test 7: Simple Travel Search (Long)
echo "🔍 Test 7: Simple Travel Search"
test_mcp_call "travel_search" "browseWebAndReturnText" '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Go to booking.com and search for hotels in Amsterdam for next month. Extract the first hotel name and price."
        }
    },
    "id": 6
}' 400

# Generate Test Report
echo "📊 Generating Test Report"
cat > "$TEST_DIR/test_report.md" << EOF
# a2aTravelAgent MCP Server Test Report

Generated: $(date)
Test Directory: $TEST_DIR

## Test Results

$(for file in "$TEST_DIR"/*.json; do
    if [ -f "$file" ]; then
        test_name=$(basename "$file" .json)
        echo "### $test_name"
        echo "- File: $file"
        echo "- Size: $(wc -c < "$file") bytes"
        if [[ "$test_name" == *"screenshot"* ]] || [[ "$test_name" == *"image"* ]]; then
            png_file="$TEST_DIR/${test_name}.png"
            if [ -f "$png_file" ]; then
                echo "- Screenshot: $png_file ($(wc -c < "$png_file") bytes)"
            fi
        fi
        echo ""
    fi
done)

## Screenshots

All extracted screenshots are saved as PNG files in the test directory.
To view them:
\`\`\`bash
open $TEST_DIR/*.png  # On macOS
# or
eog $TEST_DIR/*.png   # On Linux
\`\`\`

## Raw Response Data

All JSON responses are saved in the test directory for detailed analysis.

EOF

echo "✅ Test report saved to: $TEST_DIR/test_report.md"
echo ""
echo "🎉 All tests completed!"
echo "📁 Results location: $TEST_DIR"
echo "🖼️  Screenshots saved as PNG files"
echo "📄 View report: cat $TEST_DIR/test_report.md"

# List all generated files
echo ""
echo "Generated files:"
ls -la "$TEST_DIR"