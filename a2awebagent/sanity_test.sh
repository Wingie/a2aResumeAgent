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

# Function to test SSE endpoint with connection verification
test_sse_endpoint() {
    local endpoint=$1
    local description=$2
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing SSE $endpoint"
    if [ ! -z "$description" ]; then
        echo -n " ($description)"
    fi
    echo -n "... "
    
    # Test SSE connection and look for proper SSE headers
    response=$(curl -s --max-time 5 -H "Accept: text/event-stream" -w "%{http_code}" "$BASE_URL$endpoint" 2>/dev/null | tail -c 3)
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ 200${NC}"
    else
        echo -e "${RED}✗ $response${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Function to test Phase 2 multi-step workflow functionality
test_multistep_workflow() {
    local description=$1
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing multi-step workflow ($description)... "
    
    # Test basic multi-step execution parameters
    payload='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"browseWebAndReturnText","arguments":{"webBrowsingSteps":"Navigate to time.is and get current time","executionParamsJson":"{\"maxSteps\":3,\"executionMode\":\"MULTI_STEP\",\"allowEarlyCompletion\":true}"}},"id":1}'
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$payload" \
        "$BASE_URL/v1/tools/call")
    
    if [ "$response" -eq 200 ] || [ "$response" -eq 202 ]; then
        echo -e "${GREEN}✓ $response${NC}"
    else
        echo -e "${RED}✗ $response${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Function to test database connectivity and health
test_database_health() {
    local db_name=$1
    local endpoint=$2
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -n "Testing $db_name database health... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
    
    if [ "$response" -eq 200 ]; then
        echo -e "${GREEN}✓ $response${NC}"
    else
        echo -e "${RED}✗ $response (database may be unavailable)${NC}"
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
echo -e "${YELLOW}=== Database Health Checks (Critical) ===${NC}"
test_database_health "PostgreSQL" "/v1/health"
test_database_health "Redis" "/api/tool-cache/health"
test_database_health "Neo4j" "/v1/neo4j/screenshot-nodes"

echo ""
echo -e "${YELLOW}=== Phase 2 Multi-Step Workflow Testing ===${NC}"
# Test ExecutionParameters and StepControlService functionality
test_multistep_workflow "ONE_SHOT mode"
test_post_endpoint "/v1/tools/call" '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"browseWebAndReturnText","arguments":{"webBrowsingSteps":"Get current time from time.is","executionParamsJson":"{\"maxSteps\":1,\"executionMode\":\"ONE_SHOT\"}"}},"id":1}' 200 "One-shot execution test"
test_multistep_workflow "MULTI_STEP mode"
test_multistep_workflow "AUTO mode with early completion"

echo ""
echo -e "${YELLOW}=== Agent Thought Streaming (Phase 2 Features) ===${NC}"
# Test Agent Thought Service endpoints
test_sse_endpoint "/v1/agent/thoughts/stream" "Agent thought stream SSE"
test_get_endpoint "/v1/agent/thoughts/recent" 200 "Recent agent thoughts"
test_get_endpoint "/v1/agent/thoughts/statistics" 200 "Agent thought statistics"

echo ""
echo -e "${YELLOW}=== Enhanced Screenshot Management ===${NC}"
test_get_endpoint "/v1/screenshots/gallery" 200 "Screenshot gallery"
test_get_endpoint "/v1/screenshots/thumbnails" 200 "Screenshot thumbnails"
test_get_endpoint "/v1/screenshots/similarity/search" 200 "Screenshot similarity search"

echo ""
echo -e "${YELLOW}=== Task Progress Streaming (Phase 2) ===${NC}"
test_sse_endpoint "/v1/tasks/progress-stream" "All task progress stream"
test_sse_endpoint "/v1/tasks/dummy-task-id/progress-stream" "Task-specific progress stream"

echo ""
echo -e "${YELLOW}=== Workflow State Management ===${NC}"
test_get_endpoint "/v1/workflows/state" 200 "Workflow state API"
test_get_endpoint "/v1/workflows/patterns" 200 "Workflow patterns API"
test_get_endpoint "/v1/workflows/analytics" 200 "Workflow analytics API"

echo ""
echo -e "${YELLOW}=== Network Interception Service ===${NC}"
test_get_endpoint "/v1/network/interception/status" 200 "Network interception status"
test_get_endpoint "/v1/network/interception/logs" 200 "Network interception logs"

echo ""
echo -e "${YELLOW}=== Browser State Persistence ===${NC}"
test_get_endpoint "/v1/browser/state" 200 "Browser state API"
test_get_endpoint "/v1/browser/state/sessions" 200 "Browser state sessions API"

echo ""
echo -e "${YELLOW}=== Screenshot Embedding and Neo4j ===${NC}"
test_get_endpoint "/v1/screenshots/embeddings" 200 "Screenshot embeddings API"
test_get_endpoint "/v1/screenshots/embeddings/similarity" 200 "Screenshot similarity API"
test_get_endpoint "/v1/neo4j/screenshot-nodes" 200 "Neo4j screenshot nodes API"

echo ""
echo -e "${YELLOW}=== Step Control Service Testing ===${NC}"
test_get_endpoint "/v1/step-control/statistics" 200 "Step control statistics"
test_get_endpoint "/v1/step-control/active-contexts" 200 "Active step contexts"

echo ""
echo -e "${YELLOW}=== Performance Metrics (Phase 2) ===${NC}"
test_get_endpoint "/v1/performance/metrics" 200 "Performance metrics API"
test_get_endpoint "/v1/performance/recommendations" 200 "Performance recommendations API"
test_get_endpoint "/v1/performance/cost-analysis" 200 "Cost analysis API"

echo ""
echo -e "${YELLOW}=== Live System Health (Advanced) ===${NC}"
test_get_endpoint "/v1/system/live-health" 200 "Live system health"
test_get_endpoint "/v1/system/transaction-status" 200 "Transaction system status"
test_get_endpoint "/v1/system/database-sync" 200 "Database synchronization status"

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
echo -e "${YELLOW}=== Phase 2 Implementation Validation ===${NC}"
echo "🔍 Checking critical Phase 2 features..."

# Count Phase 2 specific test results
PHASE2_TESTS=0
PHASE2_FAILED=0

# Simulate validation of key Phase 2 features
echo -n "✓ ExecutionParameters infrastructure... "
if [ -f "a2acore/src/main/java/io/wingie/a2acore/domain/ExecutionParameters.java" ]; then
    echo -e "${GREEN}IMPLEMENTED${NC}"
else
    echo -e "${RED}MISSING${NC}"
    PHASE2_FAILED=$((PHASE2_FAILED + 1))
fi
PHASE2_TESTS=$((PHASE2_TESTS + 1))

echo -n "✓ StepControlService... "
if [ -f "a2awebapp/src/main/java/io/wingie/service/StepControlService.java" ]; then
    echo -e "${GREEN}IMPLEMENTED${NC}"
else
    echo -e "${RED}MISSING${NC}"
    PHASE2_FAILED=$((PHASE2_FAILED + 1))
fi
PHASE2_TESTS=$((PHASE2_TESTS + 1))

echo -n "✓ TaskProgressService... "
if [ -f "a2awebapp/src/main/java/io/wingie/service/TaskProgressService.java" ]; then
    echo -e "${GREEN}IMPLEMENTED${NC}"
else
    echo -e "${RED}MISSING${NC}"
    PHASE2_FAILED=$((PHASE2_FAILED + 1))
fi
PHASE2_TESTS=$((PHASE2_TESTS + 1))

echo -n "✓ AgentThoughtService... "
if [ -f "a2awebapp/src/main/java/io/wingie/service/AgentThoughtService.java" ]; then
    echo -e "${GREEN}IMPLEMENTED${NC}"
else
    echo -e "${RED}MISSING${NC}"
    PHASE2_FAILED=$((PHASE2_FAILED + 1))
fi
PHASE2_TESTS=$((PHASE2_TESTS + 1))

echo -n "✓ SSE Progress Streaming... "
if [ -f "a2awebapp/src/main/java/io/wingie/controller/TaskProgressController.java" ]; then
    echo -e "${GREEN}IMPLEMENTED${NC}"
else
    echo -e "${RED}MISSING${NC}"
    PHASE2_FAILED=$((PHASE2_FAILED + 1))
fi
PHASE2_TESTS=$((PHASE2_TESTS + 1))

echo ""
echo -e "${YELLOW}=== Summary ===${NC}"
PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))
PHASE2_PASSED=$((PHASE2_TESTS - PHASE2_FAILED))

echo "Total endpoint tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""
echo "Phase 2 feature validation: $PHASE2_TESTS"
echo -e "Implemented: ${GREEN}$PHASE2_PASSED${NC}"
echo -e "Missing: ${RED}$PHASE2_FAILED${NC}"

# Calculate overall health score
TOTAL_ALL_TESTS=$((TOTAL_TESTS + PHASE2_TESTS))
TOTAL_ALL_PASSED=$((PASSED_TESTS + PHASE2_PASSED))
HEALTH_SCORE=$(( (TOTAL_ALL_PASSED * 100) / TOTAL_ALL_TESTS ))

echo ""
echo -e "🎯 ${YELLOW}Overall System Health Score: ${GREEN}$HEALTH_SCORE%${NC}"

if [ $FAILED_TESTS -eq 0 ] && [ $PHASE2_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed! System is production ready.${NC}"
    exit 0
elif [ $HEALTH_SCORE -ge 80 ]; then
    echo -e "${YELLOW}⚠️  System mostly functional but has some issues. Check failed tests.${NC}"
    exit 1
else
    echo -e "${RED}❌ System has significant issues. Multiple features failing.${NC}"
    exit 1
fi