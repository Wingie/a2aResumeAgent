#!/bin/bash

# Screenshot Debug Script for a2aTravelAgent
# This script tests screenshot functionality step by step

MCP_SERVER_URL="http://localhost:7860"
DEBUG_DIR="screenshot_debug_$(date +%Y%m%d_%H%M%S)"

echo "🔍 Screenshot Debug Session Started"
echo "📁 Debug files will be saved to: $DEBUG_DIR"
mkdir -p "$DEBUG_DIR"

# Function to take a screenshot and analyze it
debug_screenshot() {
    local test_name="$1"
    local url="$2"
    local description="$3"
    
    echo ""
    echo "📸 Testing screenshot: $test_name"
    echo "🌐 URL: $url"
    echo "📝 Description: $description"
    
    local request_body='{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "browseWebAndReturnImage",
            "arguments": {
                "provideAllValuesInPlainEnglish": "'"$description"'"
            }
        },
        "id": 1
    }'
    
    echo "⏳ Making MCP call..."
    curl -X POST "$MCP_SERVER_URL" \
        -H "Content-Type: application/json" \
        -d "$request_body" \
        --max-time 120 \
        -o "$DEBUG_DIR/${test_name}_response.json" \
        -w "HTTP Status: %{http_code}, Time: %{time_total}s\n"
    
    if [ -f "$DEBUG_DIR/${test_name}_response.json" ]; then
        echo "✅ Response received"
        
        # Check response structure
        echo "📊 Response analysis:"
        echo "  - Size: $(wc -c < "$DEBUG_DIR/${test_name}_response.json") bytes"
        
        # Extract result field
        local result_field=$(jq -r '.result' "$DEBUG_DIR/${test_name}_response.json" 2>/dev/null)
        if [[ "$result_field" == "null" ]]; then
            echo "  - ❌ No result field found"
            echo "  - 🔍 Full response:"
            cat "$DEBUG_DIR/${test_name}_response.json" | jq '.' 2>/dev/null || cat "$DEBUG_DIR/${test_name}_response.json"
            return
        fi
        
        # Extract base64 data (try both .data and .text fields)
        local base64_data=$(jq -r '.result.content[0].data // .result.content[0].text' "$DEBUG_DIR/${test_name}_response.json" 2>/dev/null)
        
        if [[ "$base64_data" == "null" || -z "$base64_data" ]]; then
            echo "  - ❌ No base64 image data found"
            echo "  - 🔍 Content structure:"
            jq '.result.content' "$DEBUG_DIR/${test_name}_response.json" 2>/dev/null || echo "Invalid JSON"
        else
            echo "  - ✅ Base64 data found (${#base64_data} characters)"
            echo "  - First 50 chars: ${base64_data:0:50}..."
            
            # Try to decode base64
            echo "🖼️  Attempting to decode screenshot..."
            echo "$base64_data" > "$DEBUG_DIR/${test_name}_base64.txt"
            
            if echo "$base64_data" | base64 -d > "$DEBUG_DIR/${test_name}.png" 2>/dev/null; then
                local png_size=$(wc -c < "$DEBUG_DIR/${test_name}.png")
                echo "  - ✅ PNG file created: ${png_size} bytes"
                
                # Check if it's a valid PNG
                if command -v file &> /dev/null; then
                    local file_info=$(file "$DEBUG_DIR/${test_name}.png")
                    echo "  - 📋 File info: $file_info"
                    
                    if [[ "$file_info" == *"PNG image data"* ]]; then
                        echo "  - ✅ Valid PNG image detected"
                        
                        # Try to get image dimensions if possible
                        if command -v identify &> /dev/null; then
                            local img_info=$(identify "$DEBUG_DIR/${test_name}.png" 2>/dev/null)
                            echo "  - 📐 Image info: $img_info"
                        fi
                        
                        # Check for "white" image by file size heuristics
                        if [ "$png_size" -lt 1000 ]; then
                            echo "  - ⚠️  Very small PNG - might be blank/white"
                        elif [ "$png_size" -lt 5000 ]; then
                            echo "  - ⚠️  Small PNG - might be mostly white/simple"
                        else
                            echo "  - ✅ Normal size PNG - likely has content"
                        fi
                    else
                        echo "  - ❌ Not a valid PNG file"
                    fi
                fi
            else
                echo "  - ❌ Failed to decode base64 data"
                echo "  - 🔍 Base64 validation:"
                
                # Check if it looks like base64
                if [[ "$base64_data" =~ ^[A-Za-z0-9+/]*=*$ ]]; then
                    echo "    - Format looks like base64"
                else
                    echo "    - Format doesn't look like base64"
                fi
                
                # Check length
                local len=${#base64_data}
                local remainder=$((len % 4))
                echo "    - Length: $len chars (remainder: $remainder)"
            fi
        fi
    else
        echo "❌ No response received"
    fi
}

echo "🚀 Starting screenshot debug tests..."

# Test 1: Very simple page
debug_screenshot "test1_simple" "https://httpbin.org/html" "Navigate to https://httpbin.org/html and take a screenshot"

# Test 2: Even simpler page
debug_screenshot "test2_example" "https://example.com" "Navigate to https://example.com and take a screenshot"

# Test 3: With explicit wait
debug_screenshot "test3_wait" "https://httpbin.org/delay/2" "Navigate to https://httpbin.org/delay/2, wait for page to fully load, then take a screenshot"

# Test 4: Test Chrome headless behavior
debug_screenshot "test4_chrome" "data:text/html,<h1>Test Page</h1><p>This is a test</p>" "Navigate to a simple HTML page and take a screenshot"

echo ""
echo "🎉 Screenshot debug tests completed!"
echo "📁 Check results in: $DEBUG_DIR"
echo ""
echo "💡 Troubleshooting tips:"
echo "  1. Small PNG files (< 5KB) often indicate blank/white screenshots"
echo "  2. Check if Chrome is running in proper headless mode"
echo "  3. Verify that web pages are fully loading before screenshot"
echo "  4. Some sites may block headless browsers"
echo ""
echo "🔍 To view screenshots:"
echo "  open $DEBUG_DIR/*.png  # (macOS)"
echo "  eog $DEBUG_DIR/*.png   # (Linux)"
echo ""
echo "📋 Generated files:"
ls -la "$DEBUG_DIR"