#!/bin/bash

# Test script for Live Agent Reasoning Display
echo "🧠 Testing Live Agent Reasoning Display"

BASE_URL="http://localhost:7860"

echo "1. Testing agent thoughts SSE endpoint..."
curl -s ${BASE_URL}/api/agent-thoughts/stream --max-time 3 &
SSE_PID=$!

sleep 1

echo "2. Broadcasting test thought..."
curl -X POST "${BASE_URL}/api/agent-thoughts/test-thought?taskId=test-$(date +%s)"

sleep 1

echo "3. Checking agent thoughts metrics..."
curl -s ${BASE_URL}/api/agent-thoughts/metrics | jq .

echo "4. Getting recent thoughts..."
curl -s ${BASE_URL}/api/agent-thoughts/recent | jq .[0]

echo "5. Testing MCP tool to generate real agent thoughts..."
curl -X POST ${BASE_URL}/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "getMoodGuide",
      "arguments": {}
    },
    "id": "frontend-test"
  }' | jq .

# Clean up background process
kill $SSE_PID 2>/dev/null || true

echo ""
echo "✅ Agent thoughts testing completed!"
echo "🌐 Open http://localhost:7860/agents to see the Live Agent Reasoning Display"