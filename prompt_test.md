# a2aTravelAgent Phase 1 Testing Protocol

## Overview
This document provides comprehensive testing instructions for validating the current a2aTravelAgent system before implementing async features. Use this as a focused test of the software capabilities.

## Prerequisites

### System Requirements
- Java 17+ installed
- Maven 3.1+ installed  
- Chrome/Chromium browser available
- Redis server (for future async testing)
- 8GB+ RAM recommended

### Environment Setup
```bash
# Navigate to project directory
cd /Users/wingston/code/a2aTravelAgent

# Verify Java version
java -version

# Check available ports
lsof -i :7860
lsof -i :7861
```

## Test Suite 1: System Verification

### 1.1 Application Startup Test
**Objective**: Verify both applications start successfully

#### Test a2aTravelAgent (Playwright)
```bash
# Terminal 1: Start main application
cd /Users/wingston/code/a2aTravelAgent
mvn spring-boot:run

# Expected output should include:
# - Spring Boot banner
# - Port 7860 initialization
# - "Started Application in X seconds"
```

#### Test a2awebagent (Selenium)  
```bash
# Terminal 2: Start with different port
cd /Users/wingston/code/a2aTravelAgent/a2awebagent
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=7861

# Expected output should include:
# - Spring Boot banner  
# - Port 7861 initialization
# - Selenium WebDriver initialization
# - "Started Application in X seconds"
```

### 1.2 Web Interface Test
**Objective**: Verify web interfaces are accessible

```bash
# Test main application UI
curl -s http://localhost:7860 | grep -o "<title>.*</title>"
# Expected: <title>A2A MCP Playwright Web Automation Agent</title>

# Test a2awebagent UI
curl -s http://localhost:7861 | grep -o "<title>.*</title>"
# Expected: <title>A2A Selenium Agent</title>

# Open in browser for visual verification
open http://localhost:7860
open http://localhost:7861
```

### 1.3 API Discovery Test
**Objective**: Verify A2A protocol endpoints

```bash
# Test tools discovery on main app
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{"jsonrpc": "2.0", "method": "tools/list", "params": {}, "id": 1}' | jq .

# Test tools discovery on a2awebagent
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{"jsonrpc": "2.0", "method": "tools/list", "params": {}, "id": 1}' | jq .

# Test agent card
curl http://localhost:7861/.well-known/agent.json | jq .
```

**Success Criteria**:
- Both applications start without errors
- Web interfaces load correctly
- API endpoints return valid JSON responses
- Agent card contains proper tool definitions

## Test Suite 2: Core Functionality

### 2.1 Basic Web Navigation Test
**Objective**: Test simple web automation

```bash
# Test 1: Simple Google search (a2awebagent)
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Go to https://www.google.com and search for a2ajava"
    }
  },
  "id": 1
}' > test_google_search.json

# Measure response time
time curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText", 
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://example.com and extract the main heading"
    }
  },
  "id": 2
}'
```

### 2.2 Screenshot Capture Test
**Objective**: Verify screenshot functionality

```bash
# Test screenshot capture
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnImage",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://dev.to and take a screenshot"
    }
  },
  "id": 3
}' > test_screenshot.json

# Verify base64 image in response
cat test_screenshot.json | jq -r '.result' | head -c 100
```

### 2.3 Error Handling Test
**Objective**: Test error recovery and handling

```bash
# Test invalid URL
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://invalid-domain-that-does-not-exist.com"
    }
  },
  "id": 4
}'

# Test malformed request
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "invalidParameter": "This should fail"
    }
  },
  "id": 5
}'
```

## Test Suite 3: Travel Research Automation

### 3.1 Booking.com Web.action Test
**Objective**: Test the travel research automation script

```bash
# Verify web.action file exists and has travel content
cat /Users/wingston/code/a2aTravelAgent/a2awebagent/src/main/resources/web.action

# Test travel search functionality
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Search for flights from Amsterdam to Palma on July 6th 2025 using booking.com"
    }
  },
  "id": 6
}' > test_travel_search.json

# Monitor execution time (should be longer for complex automation)
time curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnImage",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Go to booking.com, search for hotels in Ibiza for July 6-7 2025, and capture screenshot"
    }
  },
  "id": 7
}'
```

### 3.2 Multi-step Travel Planning Test
**Objective**: Test complex multi-step automation

```bash
# Test comprehensive travel research
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Research complete travel options: 1) Flights from Amsterdam to Palma on July 6th 2025, 2) Hotels in Palma for July 6-7, 3) Attractions in Palma. Provide summary with prices."
    }
  },
  "id": 8
}' > test_comprehensive_research.json
```

## Test Suite 4: MCP Integration

### 4.1 Claude Desktop Integration Test
**Objective**: Test MCP bridge functionality

```bash
# Check if mcp-connector is available
ls -la /work/a2a-mcp-bridge/target/mcp-connector-full.jar

# Test MCP bridge connection (if available)
java -jar /work/a2a-mcp-bridge/target/mcp-connector-full.jar http://localhost:7861 &

# Use Claude Desktop to test:
# "Use the webbrowsingagent to search for flights to Spain"
```

### 4.2 Protocol Compliance Test
**Objective**: Verify A2A and MCP protocol compliance

```bash
# Test A2A agent card format
curl http://localhost:7861/.well-known/agent.json | jq '
{
  "name": .name,
  "capabilities": .capabilities,
  "skills": (.skills | length),
  "authentication": .authentication.valid
}'

# Test MCP-style resource listing
curl -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "params": {},
  "id": 9
}' | jq '.result.tools[] | {name: .name, description: .description}'
```

## Test Suite 5: Performance Baseline

### 5.1 Response Time Measurement
**Objective**: Establish performance baselines

```bash
# Create performance test script
cat > performance_test.sh << 'EOF'
#!/bin/bash
echo "=== Performance Baseline Test ==="
echo "Date: $(date)"
echo "System: $(uname -a)"
echo ""

# Test 1: Simple navigation
echo "Test 1: Simple Navigation"
time curl -s -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://example.com"
    }
  },
  "id": 1
}' > /dev/null

# Test 2: Screenshot capture
echo "Test 2: Screenshot Capture"
time curl -s -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnImage",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Navigate to https://httpbin.org/html and take screenshot"
    }
  },
  "id": 2
}' > /dev/null

# Test 3: Complex automation
echo "Test 3: Complex Automation"
time curl -s -X POST http://localhost:7861 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "browseWebAndReturnText",
    "arguments": {
      "provideAllValuesInPlainEnglish": "Go to booking.com and search for any hotel in Paris"
    }
  },
  "id": 3
}' > /dev/null

echo "=== Performance Test Complete ==="
EOF

chmod +x performance_test.sh
./performance_test.sh > performance_baseline.txt
```

### 5.2 Memory Usage Monitoring
**Objective**: Track resource consumption

```bash
# Monitor memory usage during tests
ps aux | grep java | grep -v grep
top -pid $(pgrep -f "a2awebagent") -l 5 > memory_usage.txt &

# Run memory stress test
for i in {1..5}; do
  echo "Concurrent request $i"
  curl -X POST http://localhost:7861 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "browseWebAndReturnText",
      "arguments": {
        "provideAllValuesInPlainEnglish": "Navigate to https://httpbin.org/delay/2"
      }
    },
    "id": '$i'
  }' &
done

wait
```

## Test Suite 6: UI/UX Assessment

### 6.1 Current Interface Evaluation
**Objective**: Document existing UI capabilities

#### a2aTravelAgent Interface (localhost:7860)
- [ ] Modern Tailwind CSS design
- [ ] Clear API examples with curl commands
- [ ] Protocol explanation (A2A/MCP)
- [ ] Links to live demo
- [ ] **Missing**: Job tracking, progress monitoring, screenshot gallery

#### a2awebagent Interface (localhost:7861)
- [ ] Bootstrap-based responsive design
- [ ] Interactive workflow carousel
- [ ] Comprehensive documentation
- [ ] Code examples with syntax highlighting
- [ ] **Missing**: Real-time job status, screenshot management

### 6.2 Missing Features Assessment
**Objective**: Identify gaps for Phase 2

#### Critical Missing Features:
1. **Job Queue Visualization**: No way to see pending/active jobs
2. **Progress Tracking**: No real-time updates during automation
3. **Screenshot Gallery**: No organized view of captured screenshots
4. **Job History**: No persistence of previous executions
5. **Error Logging**: No detailed error tracking interface
6. **Performance Metrics**: No built-in monitoring dashboard

#### Desired Enhancements:
1. **WebSocket Integration**: Real-time progress updates
2. **Job Templates**: Saved automation sequences
3. **Batch Operations**: Multiple job submission
4. **Export Functionality**: Download results/screenshots
5. **Search/Filter**: Find specific jobs or screenshots
6. **User Preferences**: Saved settings and configurations

## Success Criteria

### ✅ System Health
- [ ] Both applications start without errors
- [ ] All API endpoints respond correctly
- [ ] Web interfaces load and display properly
- [ ] Memory usage remains stable during tests

### ✅ Functionality 
- [ ] Text extraction works on various websites
- [ ] Screenshot capture produces valid images
- [ ] Error handling provides meaningful responses
- [ ] Travel automation completes successfully

### ✅ Performance
- [ ] Simple navigation completes in <30 seconds
- [ ] Screenshot capture completes in <45 seconds
- [ ] Complex automation completes in <2 minutes
- [ ] System handles 3+ concurrent requests

### ✅ Integration
- [ ] A2A protocol compliance verified
- [ ] MCP bridge connection works (if available)
- [ ] Agent cards contain proper metadata
- [ ] Claude Desktop integration functional

## Phase 2 Requirements

Based on testing results, Phase 2 should implement:

1. **Async Job Management**
   - Job queue with status tracking
   - Real-time progress updates via WebSocket
   - Job persistence and history

2. **Enhanced UI**
   - Job monitoring dashboard
   - Screenshot gallery with metadata
   - Performance metrics visualization

3. **API Enhancements**
   - RESTful job management endpoints
   - Batch operation support
   - Advanced filtering and search

4. **System Improvements**
   - Error recovery mechanisms
   - Resource pooling and optimization
   - Comprehensive logging and monitoring

## Test Execution Log

Use this section to record test results:

```
Test Date: ____________________
Tester: ______________________
Environment: __________________

System Verification: ✅/❌
Core Functionality: ✅/❌  
Travel Automation: ✅/❌
MCP Integration: ✅/❌
Performance: ✅/❌
UI Assessment: ✅/❌

Notes:
_________________________________
_________________________________
_________________________________

Issues Found:
_________________________________
_________________________________
_________________________________

Recommendations:
_________________________________
_________________________________
_________________________________
```

## Next Steps

After completing Phase 1 testing:

1. **Document Results**: Record all findings in test execution log
2. **Prioritize Issues**: Create bug/enhancement backlog
3. **Performance Baseline**: Save metrics for comparison
4. **Plan Phase 2**: Use findings to refine async implementation plan
5. **Backup Working State**: Create git tag before major changes

This comprehensive testing will ensure a solid foundation for the async transformation and help identify exactly what UI/UX enhancements are needed for the job tracking interface.