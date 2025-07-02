# CLAUDE.md - a2aTravelAgent Web Automation Agent

## Quick Start

This is a Spring Boot web automation agent with MCP (Model Context Protocol) support using Playwright for intelligent web interactions.

### Architecture
```
a2awebagent/
├── a2acore/          # 🔧 Internal MCP Framework  
└── a2awebapp/        # 🌐 Spring Boot Application
```

**Dependencies**: a2awebapp uses internal a2acore framework (NOT external a2ajava)

## Development Commands

### Build & Run
```bash
# Working directory
cd /Users/wingston/code/a2aTravelAgent/a2awebagent

# Build and run
mvn clean compile
mvn spring-boot:run -pl a2awebapp

# Docker (preferred)
docker-compose down a2awebagent && docker-compose up --build a2awebagent -d
```

### Database Setup
- **PostgreSQL**: Primary data storage (port 5432)
- **Redis**: Caching and pub/sub (port 6379)  
- **Neo4j**: Knowledge graph analytics (ports 7474/7687)

## Frontend Development

### Key URLs
- **Main App**: http://localhost:7860
- **Agents Dashboard**: http://localhost:7860/agents
- **Tools Testing**: http://localhost:7860/tools-test
- **Graph Analytics**: http://localhost:7860/api/graph/overview

### Database Admin
- **PostgreSQL**: http://localhost:8080 (pgAdmin)
- **Neo4j Browser**: http://localhost:7474

## MCP Protocol Integration

### Working Endpoint
```bash
# List tools
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'

# Execute tool  
curl -X POST http://localhost:7860/v1 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "generateMeme", "arguments": {"template": "success", "topText": "Test", "bottomText": "Working"}}, "id": 2}'
```

### Available Tools (9 total)
- `generateMeme` / `generateMoodMeme` - Meme creation
- `browseWebAndReturnText` / `browseWebAndReturnImage` - Web automation
- `searchLinkedInProfile` - Professional profile search
- `getMoodGuide` - Meme template guide
- `askTasteBeforeYouWaste` / `getTasteBeforeYouWasteScreenshot` - Food safety
- `getWingstonsProjectsExpertiseResume` - Professional info

## Configuration

### Environment Variables
```bash
# AI Provider Keys
export OPENAI_KEY=your_key
export CLAUDE_KEY=your_key
export GEMINI_API_KEY=your_key

# JVM Properties (alternative)
-DopenAiKey=your_key -DclaudeKey=your_key
```

### Docker Environment
- **Port**: 7860
- **Profiles**: application-docker.yml
- **Neo4j Auth**: neo4j/password123
- **PostgreSQL**: agent/agent123

## Troubleshooting

### Common Issues

**1. Frontend Pages Error**
- Check logs: `docker logs a2awebagent`
- Verify database connections
- Restart: `docker-compose restart a2awebagent`

**2. Neo4j Warnings (Normal)**
- `Unknown LabelWarning` - Expected for new deployments
- Warnings disappear as data is created

**3. Database Health Check**
```bash
# PostgreSQL
docker exec a2a-postgres psql -U agent -d a2awebagent -c "SELECT 1;"

# Redis
docker exec a2a-redis redis-cli ping

# Neo4j  
docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "RETURN 1;"
```

### Testing
```bash
# Quick sanity test
./quick_test.sh

# MCP health check
curl -s http://localhost:7860/v1/health | jq .

# Tool count verification
curl -s http://localhost:7860/v1/tools | jq '.toolCount'
```

## Development Notes

### Frontend Focus
- Main UI components in `src/main/resources/templates/`
- Static assets in `src/main/resources/static/`
- Real-time updates via Server-Sent Events (SSE)
- Task execution tracking with progress bars

### Database Architecture
- **PostgreSQL**: Task executions, tool descriptions, caching
- **Redis**: Session management, real-time pub/sub
- **Neo4j**: Screenshot embeddings, knowledge graph analytics

### Code Modification Pattern
Always use Task() agents for analysis and changes:
```bash
Task(
  description="Debug frontend issue",
  prompt="Use read_file to analyze template errors and MCP tool integration"
)
```

### Performance Targets
- Tool Discovery: <100ms
- Web Automation: 2-5 seconds
- MCP Tool Execution: 1-3 seconds
- Neo4j Async Logging: Non-blocking