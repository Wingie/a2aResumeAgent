#!/bin/bash

# Multi-step workflow test script
# Tests various ExecutionParameters configurations and workflow scenarios

set -e

SERVER_URL="http://localhost:7860"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
LOG_DIR="/Users/wingston/code/a2aTravelAgent/a2awebagent/test_logs"
LOG_FILE="$LOG_DIR/multistep_test_$TIMESTAMP.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create log directory
mkdir -p "$LOG_DIR"

# Logging function
log() {
    echo -e "$1" | tee -a "$LOG_FILE"
}

test_header() {
    log "\n${BLUE}================================================${NC}"
    log "${BLUE}TEST: $1${NC}"
    log "${BLUE}================================================${NC}"
}

test_result() {
    if [ "$1" -eq 0 ]; then
        log "${GREEN}✅ PASSED: $2${NC}"
    else
        log "${RED}❌ FAILED: $2${NC}"
    fi
}

# Function to call browseWebAndReturnText with ExecutionParameters
call_browse_web() {
    local description="$1"
    local execution_params="$2"
    local expected_pattern="$3"
    local test_id=$(date +%s)
    
    log "\n${YELLOW}Calling browseWebAndReturnText with:${NC}"
    log "Description: $description"
    log "Execution Parameters: $execution_params"
    
    # Escape quotes in parameters for JSON
    local escaped_description=$(echo "$description" | sed 's/"/\\"/g')
    local escaped_params=$(echo "$execution_params" | sed 's/"/\\"/g')
    
    # Make the API call using JSON-RPC format
    response=$(curl -s -X POST "$SERVER_URL/v1" \
        -H "Content-Type: application/json" \
        -d "{
            \"jsonrpc\": \"2.0\",
            \"method\": \"tools/call\",
            \"params\": {
                \"name\": \"browseWebAndReturnText\",
                \"arguments\": {
                    \"webBrowsingSteps\": \"$escaped_description\",
                    \"executionParamsJson\": \"$escaped_params\"
                }
            },
            \"id\": $test_id
        }")
    
    # Log the response
    log "Response: $response"
    
    # Check if response contains expected pattern and is successful
    if echo "$response" | grep -q "\"success\":true" && echo "$response" | grep -q "$expected_pattern"; then
        return 0
    else
        return 1
    fi
}

# Check if server is running
log "${BLUE}Starting Multi-Step Workflow Tests - $TIMESTAMP${NC}"
log "Server URL: $SERVER_URL"
log "Log file: $LOG_FILE"

if ! curl -s "$SERVER_URL/" > /dev/null; then
    log "${RED}❌ Server not running at $SERVER_URL${NC}"
    exit 1
fi

log "${GREEN}✅ Server is running${NC}"

# Test 1: ONE_SHOT mode with maxSteps=1
test_header "ONE_SHOT Execution Mode"
call_browse_web \
    "Navigate to google.com and take a screenshot" \
    "{\"maxSteps\": 1, \"executionMode\": \"ONE_SHOT\", \"allowEarlyCompletion\": false}" \
    "Screenshot"
test_result $? "ONE_SHOT mode execution"

# Test 2: MULTI_STEP mode with step breakdown
test_header "MULTI_STEP Mode with Step Breakdown"
call_browse_web \
    "Search for hotels in Paris on booking.com and check availability" \
    "{\"maxSteps\": 5, \"executionMode\": \"MULTI_STEP\", \"allowEarlyCompletion\": true, \"captureStepScreenshots\": true}" \
    "step"
test_result $? "MULTI_STEP mode with step breakdown"

# Test 3: AUTO mode with early completion
test_header "AUTO Mode with Early Completion"
call_browse_web \
    "Find information about machine learning on Wikipedia" \
    "{\"maxSteps\": 8, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true, \"earlyCompletionThreshold\": 0.6}" \
    "completed"
test_result $? "AUTO mode with early completion"

# Test 4: Travel booking workflow
test_header "Travel Booking Workflow"
call_browse_web \
    "Book a flight from NYC to Paris on expedia.com" \
    "{\"maxSteps\": 7, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true, \"stepTimeoutSeconds\": 30}" \
    "Navigate to"
test_result $? "Travel booking workflow"

# Test 5: LinkedIn search workflow  
test_header "LinkedIn Search Workflow"
call_browse_web \
    "Search for software engineers in San Francisco on LinkedIn" \
    "{\"maxSteps\": 6, \"executionMode\": \"MULTI_STEP\", \"allowEarlyCompletion\": false, \"captureStepScreenshots\": true}" \
    "LinkedIn"
test_result $? "LinkedIn search workflow"

# Test 6: Network monitoring test
test_header "Network Monitoring Test"
call_browse_web \
    "Search for news on reddit.com and check API responses" \
    "{\"maxSteps\": 4, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true}" \
    "network activity"
test_result $? "Network monitoring functionality"

# Test 7: Step timeout test
test_header "Step Timeout Configuration"
call_browse_web \
    "Navigate to a slow loading website and wait" \
    "{\"maxSteps\": 3, \"executionMode\": \"MULTI_STEP\", \"stepTimeoutSeconds\": 15}" \
    "timeout\\|completed"
test_result $? "Step timeout configuration"

# Test 8: Screenshot capture test
test_header "Screenshot Capture Test"
call_browse_web \
    "Visit github.com and capture screenshots of the homepage" \
    "{\"maxSteps\": 2, \"executionMode\": \"MULTI_STEP\", \"captureStepScreenshots\": true}" \
    "screenshot"
test_result $? "Screenshot capture functionality"

# Test 9: Complex workflow with multiple steps
test_header "Complex Multi-Step Workflow"
call_browse_web \
    "Navigate to amazon.com
Search for wireless headphones
Compare prices of top 3 results
Check customer reviews
Take screenshots for reference" \
    "{\"maxSteps\": 10, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true, \"captureStepScreenshots\": true}" \
    "Step.*completed"
test_result $? "Complex multi-step workflow"

# Test 10: Early completion threshold test
test_header "Early Completion Threshold Test"
call_browse_web \
    "Quick search on google.com for 'weather'" \
    "{\"maxSteps\": 5, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true, \"earlyCompletionThreshold\": 0.8}" \
    "Early completion\\|completed early"
test_result $? "Early completion threshold"

# Test 11: Error handling test
test_header "Error Handling Test"
call_browse_web \
    "Navigate to invalid-website-12345.com" \
    "{\"maxSteps\": 3, \"executionMode\": \"MULTI_STEP\", \"allowEarlyCompletion\": false}" \
    "Error\\|failed"
test_result $? "Error handling in workflow"

# Test 12: State transition test
test_header "State Transition Test"
call_browse_web \
    "Go to stackoverflow.com and search for Java questions" \
    "{\"maxSteps\": 4, \"executionMode\": \"AUTO\", \"allowEarlyCompletion\": true}" \
    "step.*completed"
test_result $? "State transition tracking"

log "\n${BLUE}================================================${NC}"
log "${BLUE}TEST SUMMARY${NC}"
log "${BLUE}================================================${NC}"
log "Test execution completed at $(date)"
log "Full log available at: $LOG_FILE"

# Count results
passed=$(grep -c "✅ PASSED" "$LOG_FILE" || echo "0")
failed=$(grep -c "❌ FAILED" "$LOG_FILE" || echo "0")

log "${GREEN}Passed: $passed${NC}"
log "${RED}Failed: $failed${NC}"

if [ "$failed" -eq 0 ]; then
    log "${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    log "${RED}💥 Some tests failed. Check the log for details.${NC}"
    exit 1
fi