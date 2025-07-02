#!/bin/bash

# Sanity Test Script for a2awebagent Spring Boot Application
# Tests all exposed endpoints to verify they return appropriate HTTP status codes

BASE_URL="http://localhost:7860"
FAILED_TESTS=0
TOTAL_TESTS=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test GET endpoint
test_get_endpoint() {
    local endpoint=$1
    local expected_status=${2:-200}
    local description=$3
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing GET $endpoint"
    if [ ! -z "$description" ]; then
        echo -n " ($description)"
    fi
    echo -n "... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ $response${NC}"
    else
        echo -e "${RED}✗ $response (expected $expected_status)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Function to test POST endpoint with JSON payload
test_post_endpoint() {
    local endpoint=$1
    local payload=$2
    local expected_status=${3:-200}
    local description=$4
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing POST $endpoint"
    if [ ! -z "$description" ]; then
        echo -n " ($description)"
    fi
    echo -n "... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$payload" \
        "$BASE_URL$endpoint")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ $response${NC}"
    else
        echo -e "${RED}✗ $response (expected $expected_status)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Function to test DELETE endpoint
test_delete_endpoint() {
    local endpoint=$1
    local expected_status=${2:-404}
    local description=$3
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing DELETE $endpoint"
    if [ ! -z "$description" ]; then
        echo -n " ($description)"
    fi
    echo -n "... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL$endpoint")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ $response${NC}"
    else
        echo -e "${RED}✗ $response (expected $expected_status)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo -e "${YELLOW}Starting sanity test for a2awebagent endpoints...${NC}"
echo "Base URL: $BASE_URL"
echo ""

# Check if server is running
echo -n "Checking if server is running... "
if curl -s --connect-timeout 5 "$BASE_URL" > /dev/null; then
    echo -e "${GREEN}✓ Server is responding${NC}"
else
    echo -e "${RED}✗ Server is not responding. Please start the application first.${NC}"
    echo "Run: mvn spring-boot:run or java -jar target/a2awebagent-0.0.1.jar"
    exit 1
fi

echo ""
echo -e "${YELLOW}=== Core A2A/MCP Endpoints ===${NC}"
test_get_endpoint "/v1/tools" 200 "List available tools"
test_get_endpoint "/v1/health" 200 "Health check"
test_get_endpoint "/v1/metrics" 200 "Performance metrics"

# Test POST endpoints with sample JSON-RPC payload
echo ""
echo -e "${YELLOW}=== MCP Tool Calling (POST) ===${NC}"
test_post_endpoint "/v1/tools/call" '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"browseWebAndReturnText","arguments":{"provideAllValuesInPlainEnglish":"test"}},"id":1}' 400 "Tool call (expects validation error)"
test_post_endpoint "/v1" '{"jsonrpc":"2.0","method":"tools/list","id":1}' 200 "Generic MCP endpoint"

echo ""
echo -e "${YELLOW}=== Dashboard Endpoints ===${NC}"
test_get_endpoint "/" 200 "Homepage/Resume"
test_get_endpoint "/startup" 200 "Startup dashboard"
test_get_endpoint "/cache" 200 "Cache dashboard"
test_get_endpoint "/agents" 200 "Agents dashboard"
test_get_endpoint "/evaluations" 200 "Evaluation dashboard"

echo ""
echo -e "${YELLOW}=== API Endpoints ===${NC}"
test_get_endpoint "/agents/api/stats" 200 "Agent stats API"
test_get_endpoint "/agents/api/active" 200 "Active tasks API"
test_get_endpoint "/agents/api/recent" 200 "Recent tasks API"
test_get_endpoint "/evaluations/api/stats" 200 "Evaluation stats API"
test_get_endpoint "/evaluations/api/active" 200 "Active evaluations API"
test_get_endpoint "/evaluations/api/recent" 200 "Recent evaluations API"
test_get_endpoint "/evaluations/api/benchmarks" 200 "Available benchmarks API"
test_get_endpoint "/evaluations/api/models" 200 "Available models API"

echo ""
echo -e "${YELLOW}=== Cache Management APIs ===${NC}"
test_get_endpoint "/api/cache/descriptions" 200 "Cached descriptions"
test_get_endpoint "/api/cache/stats" 200 "Cache statistics"
test_get_endpoint "/api/cache/report" 200 "Cache report"
test_get_endpoint "/api/tool-cache/report" 200 "Tool cache report"
test_get_endpoint "/api/tool-cache/stats" 200 "Tool cache stats"
test_get_endpoint "/api/tool-cache/config" 200 "Tool cache config"
test_get_endpoint "/api/tool-cache/health" 200 "Tool cache health"

echo ""
echo -e "${YELLOW}=== Task Management APIs ===${NC}"
test_get_endpoint "/v1/tasks/active" 200 "Active tasks"
test_get_endpoint "/v1/tasks/history" 200 "Task history"
test_get_endpoint "/v1/tasks/stats" 200 "Task stats"
test_get_endpoint "/v1/tasks/health" 200 "Task health check"

echo ""
echo -e "${YELLOW}=== Agent Discovery ===${NC}"
test_get_endpoint "/.well-known/agent" 200 "Agent card for MCP discovery"

echo ""
echo -e "${YELLOW}=== Server-Sent Events (SSE) ===${NC}"
echo -n "Testing SSE /agents/events... "
# Use curl with max-time instead of timeout command for macOS compatibility
response=$(curl -s --max-time 3 -w "%{http_code}" "$BASE_URL/agents/events" 2>/dev/null | tail -c 3)
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ 200${NC}"
else
    echo -e "${RED}✗ $response${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo -n "Testing SSE /evaluations/events... "
# Use curl with max-time instead of timeout command for macOS compatibility
response=$(curl -s --max-time 3 -w "%{http_code}" "$BASE_URL/evaluations/events" 2>/dev/null | tail -c 3)
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ 200${NC}"
else
    echo -e "${RED}✗ $response${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo -e "${YELLOW}=== Testing Non-existent Endpoints (Should Return 404) ===${NC}"
test_get_endpoint "/nonexistent" 404 "Non-existent endpoint"
test_get_endpoint "/v1/invalid" 404 "Invalid API endpoint"

echo ""
echo -e "${YELLOW}=== Testing ID-based Endpoints (Should Return 404 for non-existent IDs) ===${NC}"
test_get_endpoint "/agents/task/nonexistent" 404 "Non-existent task"
test_get_endpoint "/evaluations/nonexistent" 404 "Non-existent evaluation"
test_get_endpoint "/v1/tasks/nonexistent/status" 404 "Non-existent task status"

echo ""
echo -e "${YELLOW}=== Summary ===${NC}"
PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))
echo "Total tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed. Check the application logs for details.${NC}"
    exit 1
fi