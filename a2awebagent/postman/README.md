# a2aTravelAgent Postman Collection

This directory contains a comprehensive Postman collection for testing all web automation tools in the a2aTravelAgent system.

## 📁 Files

- **`a2awebagent-tools.postman_collection.json`** - Main test collection
- **`local-environment.postman_environment.json`** - Local development environment
- **`docker-environment.postman_environment.json`** - Docker deployment environment
- **`README.md`** - This documentation file

## 🚀 Quick Start

### 1. Import into Postman

1. Open Postman
2. Click "Import" button
3. Upload `a2awebagent-tools.postman_collection.json`
4. Upload your preferred environment file:
   - `local-environment.postman_environment.json` for local development
   - `docker-environment.postman_environment.json` for Docker testing

### 2. Start the Server

#### Local Development
```bash
cd /path/to/a2awebagent
mvn spring-boot:run
```

#### Docker Deployment
```bash
cd /path/to/a2awebagent
docker-compose up -d
```

### 3. Run Tests

1. Select the appropriate environment in Postman
2. Start with "System Tools" → "Health Check" to verify server is running
3. Run "System Tools" → "List Available Tools" to see all available tools
4. Test individual tools or run the entire collection

## 🔧 Available Tools (7 Total)

### 🏢 Resume & Portfolio Tools
- **`getWingstonsProjectsExpertiseResume`** - Professional portfolio and expertise showcase
  - Complete resume with ASCII art
  - AI/ML expertise focus
  - Audio technology expertise

### 🥗 Food Safety Tools
- **`askTasteBeforeYouWaste`** - Food safety guidance and recommendations
- **`getTasteBeforeYouWasteScreenshot`** - Visual food safety guide from website

### 💼 LinkedIn Tools
- **`searchLinkedInProfile`** - LinkedIn profile search and demonstration
  - Wingston Sharon profile showcase
  - General LinkedIn search capabilities

### 🌐 Web Automation Tools
- **`browseWebAndReturnText`** - Web automation with text extraction
- **`browseWebAndReturnImage`** - Web automation with screenshot capture
- **`takeCurrentPageScreenshot`** - Simple screenshot functionality

### 🧪 Demo & Test Tools
- **`searchHelloWorld`** - Demo web automation capabilities

## 📋 Test Categories

### 1. **System Validation**
- Health checks and server status
- Tool discovery and availability
- Basic connectivity testing

### 2. **Core Web Automation**
- Text extraction from web pages
- Screenshot capture functionality
- Complex navigation and interaction

### 3. **Specialized Tools**
- Professional portfolio presentation
- Food safety information retrieval
- LinkedIn profile demonstrations

### 4. **Error Handling**
- Timeout management
- Fallback behavior testing
- Service unavailability scenarios

## 🧪 Test Features

### Automatic Validation
Each request includes automatic tests for:
- ✅ HTTP 200 status codes
- ✅ JSON-RPC 2.0 structure validation
- ✅ Response time monitoring (max 3 minutes)
- ✅ Content-specific validation for each tool

### Screenshot Extraction
For tools that return images:
- Automatic base64 detection
- Environment variable storage for further processing
- Visual validation logging

### Error Detection
- Service availability checks
- Timeout handling
- Fallback response validation

## 🔍 Usage Examples

### Test a Specific Tool
1. Navigate to the relevant folder (e.g., "Resume & Portfolio Tools")
2. Select "Get Complete Resume"
3. Click "Send"
4. Review the response and test results

### Test LinkedIn Tool (Potential Screenshot Issues)
Based on your mention of LinkedIn screenshot problems:
1. Go to "LinkedIn Tools" → "Search LinkedIn - Wingston Profile"
2. Run the test
3. Check the test results for screenshot detection
4. Use the console logs to debug screenshot capture issues

### Extract Screenshots
If a test captures a screenshot:
1. Look for base64 data in the response
2. Check environment variables for stored screenshot data
3. Use online base64 decoder or save to file:

```bash
# Extract screenshot from response
echo "BASE64_DATA_HERE" | base64 -d > screenshot.png
```

## 🐛 Troubleshooting

### Common Issues

#### 1. **Connection Refused**
- **Issue**: Server not running on port 7860
- **Solution**: Start the application with `mvn spring-boot:run` or `docker-compose up -d`

#### 2. **Timeout Errors**
- **Issue**: Web automation taking too long
- **Solution**: Increase timeout in environment variables or simplify test queries

#### 3. **LinkedIn Screenshot Empty**
- **Issue**: LinkedIn tool returns empty screenshots
- **Possible Causes**: 
  - LinkedIn anti-automation measures
  - Playwright browser not properly initialized
  - Network connectivity issues
- **Debugging**: Check the text response for error messages

#### 4. **Tool Not Found**
- **Issue**: JSON-RPC returns "tool not found" error
- **Solution**: 
  - Verify tool name spelling
  - Run "List Available Tools" to see actual tool names
  - Check that @EnableAgent annotation is uncommented in Application.java

### Environment Variables

The collection uses these variables (configurable in environments):
- **`base_url`**: Server URL (default: http://localhost:7860)
- **`timeout_seconds`**: Request timeout (120s local, 180s Docker)
- **`max_response_time`**: Test validation timeout (3-5 minutes)

### Response Structure

All tools follow JSON-RPC 2.0 format:
```json
{
    "jsonrpc": "2.0",
    "id": 1234567890,
    "result": {
        "content": [
            {
                "type": "text",
                "text": "Response content here..."
            }
        ]
    }
}
```

For screenshot tools, the response includes:
```json
{
    "jsonrpc": "2.0",
    "id": 1234567890,
    "result": {
        "content": [
            {
                "type": "image",
                "data": "base64_encoded_image_data..."
            }
        ]
    }
}
```

## 📊 Performance Expectations

- **Simple Text Tools**: 5-15 seconds
- **Web Automation**: 30-120 seconds
- **Screenshot Capture**: 45-180 seconds
- **LinkedIn Tool**: 60-180 seconds (may have limitations)

## 🔄 Continuous Testing

### Run Collection Automatically
1. Install Newman (Postman CLI): `npm install -g newman`
2. Run collection:
```bash
newman run a2awebagent-tools.postman_collection.json \
  -e local-environment.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export results.html
```

### CI/CD Integration
Add to your build pipeline:
```bash
# Start application
mvn spring-boot:run &
sleep 30

# Run tests
newman run postman/a2awebagent-tools.postman_collection.json \
  -e postman/local-environment.postman_environment.json \
  --reporters junit \
  --reporter-junit-export test-results.xml

# Stop application
pkill -f "spring-boot:run"
```

## 🤝 Contributing

When adding new tools to the system:
1. Add corresponding test requests to the collection
2. Include proper validation tests
3. Update this README with new tool descriptions
4. Test both text and image response types if applicable

## 📞 Support

- **Issue**: Empty screenshots or tool failures
- **Solution**: Check application logs and verify Playwright is properly initialized
- **Contact**: Review the main project documentation for troubleshooting steps

---

*This collection tests the complete Playwright-based web automation system with all Selenium dependencies removed.*