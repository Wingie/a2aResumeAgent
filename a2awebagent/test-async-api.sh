#!/bin/bash

# Test script for a2aTravelAgent Async API
# This script tests both current synchronous functionality and new async endpoints

set -e

BASE_URL="http://localhost:7860"
ASYNC_BASE_URL="$BASE_URL/v1/tasks"

echo "🚀 a2aTravelAgent API Testing Script"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if server is running
check_server() {
    print_status "Checking if server is running at $BASE_URL..."
    
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_success "Server is running!"
        return 0
    else
        print_error "Server is not responding at $BASE_URL"
        print_warning "Please start the server with: docker-compose up -d"
        return 1
    fi
}

# Test current synchronous endpoints
test_current_sync_api() {
    echo ""
    echo "==================="
    echo "Testing Current Sync API"
    echo "==================="
    
    print_status "Testing current MCP endpoint..."
    
    # Test current web browsing endpoint
    SYNC_PAYLOAD='{
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "browseWebAndReturnText",
            "arguments": {
                "provideAllValuesInPlainEnglish": "Quick test: what is the weather like today? Just visit weather.com briefly"
            }
        },
        "id": 1
    }'
    
    print_status "Sending synchronous request..."
    SYNC_RESPONSE=$(curl -s -X POST "$BASE_URL/v1/tools/call" \
        -H "Content-Type: application/json" \
        -d "$SYNC_PAYLOAD" \
        --max-time 60)
    
    if [ $? -eq 0 ]; then
        print_success "Synchronous API responded"
        echo "Response preview: $(echo "$SYNC_RESPONSE" | jq -r '.result // .error // "No result field"' | head -c 200)..."
    else
        print_error "Synchronous API test failed"
    fi
}

# Test new async endpoints
test_async_api() {
    echo ""
    echo "==================="
    echo "Testing New Async API"
    echo "==================="
    
    # Test async task submission
    print_status "Testing async task submission..."
    
    ASYNC_PAYLOAD='{
        "query": "What are the prices of flights from Amsterdam to London next weekend?",
        "taskType": "travel_search",
        "requesterId": "test-script",
        "timeoutSeconds": 300
    }'
    
    print_status "Submitting async task..."
    SUBMIT_RESPONSE=$(curl -s -X POST "$ASYNC_BASE_URL/submit" \
        -H "Content-Type: application/json" \
        -d "$ASYNC_PAYLOAD")
    
    if [ $? -eq 0 ]; then
        TASK_ID=$(echo "$SUBMIT_RESPONSE" | jq -r '.taskId // empty')
        
        if [ -n "$TASK_ID" ]; then
            print_success "Task submitted successfully! Task ID: $TASK_ID"
            
            # Monitor task progress
            monitor_task_progress "$TASK_ID"
            
        else
            print_error "Task submission failed"
            echo "Response: $SUBMIT_RESPONSE"
        fi
    else
        print_error "Failed to submit async task"
    fi
}

# Monitor task progress
monitor_task_progress() {
    local task_id=$1
    local max_attempts=30
    local attempt=0
    
    print_status "Monitoring task progress for: $task_id"
    
    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))
        
        STATUS_RESPONSE=$(curl -s "$ASYNC_BASE_URL/$task_id/status")
        
        if [ $? -eq 0 ]; then
            STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status // "unknown"')
            PROGRESS=$(echo "$STATUS_RESPONSE" | jq -r '.progressPercent // 0')
            MESSAGE=$(echo "$STATUS_RESPONSE" | jq -r '.progressMessage // "No message"')
            
            print_status "[$attempt/$max_attempts] Status: $STATUS, Progress: $PROGRESS%, Message: $MESSAGE"
            
            # Check if task is complete
            if [ "$STATUS" = "COMPLETED" ]; then
                print_success "Task completed successfully!"
                
                # Get final results
                RESULTS_RESPONSE=$(curl -s "$ASYNC_BASE_URL/$task_id/results")
                print_status "Final results preview:"
                echo "$RESULTS_RESPONSE" | jq -r '.results // "No results"' | head -c 300
                echo "..."
                break
                
            elif [ "$STATUS" = "FAILED" ]; then
                print_error "Task failed!"
                ERROR=$(echo "$STATUS_RESPONSE" | jq -r '.errorDetails // "No error details"')
                echo "Error: $ERROR"
                break
                
            elif [ "$STATUS" = "CANCELLED" ] || [ "$STATUS" = "TIMEOUT" ]; then
                print_warning "Task terminated with status: $STATUS"
                break
            fi
            
        else
            print_error "Failed to get task status"
        fi
        
        # Wait before next check
        sleep 10
    done
    
    if [ $attempt -eq $max_attempts ]; then
        print_warning "Stopped monitoring after $max_attempts attempts"
    fi
}

# Test system endpoints
test_system_endpoints() {
    echo ""
    echo "==================="
    echo "Testing System Endpoints"
    echo "==================="
    
    # Test health endpoint
    print_status "Testing health endpoint..."
    HEALTH_RESPONSE=$(curl -s "$ASYNC_BASE_URL/health")
    if [ $? -eq 0 ]; then
        STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status // "unknown"')
        print_success "Health check: $STATUS"
    else
        print_error "Health check failed"
    fi
    
    # Test stats endpoint
    print_status "Testing stats endpoint..."
    STATS_RESPONSE=$(curl -s "$ASYNC_BASE_URL/stats")
    if [ $? -eq 0 ]; then
        print_success "Stats endpoint working"
        echo "Stats: $STATS_RESPONSE"
    else
        print_error "Stats endpoint failed"
    fi
    
    # Test active tasks endpoint
    print_status "Testing active tasks endpoint..."
    ACTIVE_RESPONSE=$(curl -s "$ASYNC_BASE_URL/active")
    if [ $? -eq 0 ]; then
        ACTIVE_COUNT=$(echo "$ACTIVE_RESPONSE" | jq '. | length')
        print_success "Active tasks endpoint working. Active tasks: $ACTIVE_COUNT"
    else
        print_error "Active tasks endpoint failed"
    fi
}

# Test UI dashboard access
test_ui_dashboard() {
    echo ""
    echo "==================="
    echo "Testing UI Dashboard"
    echo "==================="
    
    print_status "Testing /agents dashboard..."
    
    if curl -s -f "$BASE_URL/agents" > /dev/null 2>&1; then
        print_success "Dashboard is accessible at $BASE_URL/agents"
    else
        print_warning "Dashboard not yet available (this is expected if UI is not implemented yet)"
    fi
}

# Main execution
main() {
    echo "Starting comprehensive API tests..."
    echo "Timestamp: $(date)"
    echo ""
    
    # Check if jq is available
    if ! command -v jq &> /dev/null; then
        print_error "jq is required for this script. Please install it:"
        echo "  - macOS: brew install jq"
        echo "  - Ubuntu: sudo apt-get install jq"
        exit 1
    fi
    
    # Check server
    if ! check_server; then
        exit 1
    fi
    
    # Run tests
    test_current_sync_api
    test_async_api
    test_system_endpoints
    test_ui_dashboard
    
    echo ""
    echo "==================="
    echo "Test Summary"
    echo "==================="
    print_success "Testing completed!"
    
    echo ""
    echo "📊 Useful URLs:"
    echo "  - Server Health: $BASE_URL/actuator/health"
    echo "  - API Documentation: $BASE_URL/swagger-ui.html"
    echo "  - Task Stats: $ASYNC_BASE_URL/stats"
    echo "  - Active Tasks: $ASYNC_BASE_URL/active"
    echo "  - Agent Dashboard: $BASE_URL/agents (coming soon)"
    echo ""
    echo "🧪 Test with Claude:"
    echo "Ask Claude to submit a travel search task to: $ASYNC_BASE_URL/submit"
    echo ""
}

# Handle script arguments
case "${1:-all}" in
    "sync")
        check_server && test_current_sync_api
        ;;
    "async")
        check_server && test_async_api
        ;;
    "system")
        check_server && test_system_endpoints
        ;;
    "ui")
        check_server && test_ui_dashboard
        ;;
    "all"|*)
        main
        ;;
esac