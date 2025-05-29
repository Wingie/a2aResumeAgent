# A2A MCP Playwright Web Automation Agent

A web automation agent that supports both A2A and MCP protocols using the a2ajava framework and Playwright for web interactions.

## Features

- Web automation using Playwright
- Support for A2A and MCP protocols
- Tool calling capabilities
- Screenshot and text extraction
- Natural language command processing

## Usage Examples

### Text Extraction

To browse web pages and extract text:

```bash
curl -X POST \
-H "Content-Type: application/json" \
-d '{
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnText",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Go to Google.com, search for \"a2ajava\"give me result"
        }
    },
    "jsonrpc": "2.0",
    "id": 17
}' \
http://localhost:7860
```

## Screenshot Capture
To capture screenshots of web pages:

``` 
curl -X POST \
-H "Content-Type: application/json" \
-d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "browseWebAndReturnImage",
        "arguments": {
            "provideAllValuesInPlainEnglish": "Navigate to https://dev.to and take a screenshot of the homepage"
        }
    },
    "id": 17
}' \
http://localhost:7860
```

## Protocol Support
- A2A Protocol
Register agent using the a2ajava framework
Tool calling capabilities via JSON-RPC
Natural language command processing
Response handling through callbacks
- MCP Protocol
Direct integration with MCP network
Standardized message format
Tool registration and discovery
Asynchronous operation support