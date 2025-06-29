#!/bin/bash

echo "=== Testing Screenshot Fixes ==="
echo "This script will test the enhanced screenshot functionality"

# Start the application in background
echo "Starting a2awebagent..."
mvn spring-boot:run &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start (30 seconds)..."
sleep 30

echo "Testing screenshot functionality..."

# Test 1: Basic screenshot test
echo "Test 1: Taking a basic screenshot of Google homepage"
curl -X POST -H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnImage",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://www.google.com and take a screenshot"
    }
  },
  "id": 1
}' \
http://localhost:7860 > test_result_1.json

echo "Test 1 result saved to test_result_1.json"

# Test 2: Test current page screenshot
echo "Test 2: Taking current page screenshot"
curl -X POST -H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "takeCurrentPageScreenshot",
    "arguments": {}
  },
  "id": 2
}' \
http://localhost:7860 > test_result_2.json

echo "Test 2 result saved to test_result_2.json"

# Test 3: Complex page test
echo "Test 3: Testing with a more complex page"
curl -X POST -H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnImage",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://example.com and wait 3 seconds then take a screenshot"
    }
  },
  "id": 3
}' \
http://localhost:7860 > test_result_3.json

echo "Test 3 result saved to test_result_3.json"

# Wait a bit for all requests to complete
sleep 10

# Stop the application
echo "Stopping application..."
kill $APP_PID

# Display results
echo "=== Test Results ==="
echo "Test 1 (Google homepage):"
cat test_result_1.json | python3 -m json.tool 2>/dev/null || cat test_result_1.json
echo ""

echo "Test 2 (Current page screenshot):"
cat test_result_2.json | python3 -m json.tool 2>/dev/null || cat test_result_2.json
echo ""

echo "Test 3 (Example.com):"
cat test_result_3.json | python3 -m json.tool 2>/dev/null || cat test_result_3.json
echo ""

echo "=== Screenshot files should be in the screenshots/ directory ==="
ls -la screenshots/ 2>/dev/null || echo "No screenshots directory found"

echo "=== Test completed ==="