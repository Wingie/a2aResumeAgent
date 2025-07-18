# CLAUDE.md - a2aTravelAgent Web Automation Agent

## Quick Start

This is a Spring Boot web automation agent with MCP (Model Context Protocol) support using Playwright for intelligent web interactions. The project includes a comprehensive evaluation system for testing AI models and a connection pool monitoring system for database health.

### Architecture
```
a2awebagent/
├── a2acore/          # 🔧 Internal MCP Framework  
└── a2awebapp/        # 🌐 Spring Boot Application
    ├── evaluation/   # 📊 Model evaluation system (Phase 2 ready)
    ├── monitoring/   # 📈 Connection pool & system health
    └── web/         # 🌐 Web automation tools
```

**Dependencies**: a2awebapp uses internal a2acore framework (NOT external a2ajava)

## Phase 2 Readiness Status ✅

The project is **Phase 2 ready** with:
- ✅ **Evaluation System**: Full model testing framework with UI
- ✅ **Connection Pool Monitoring**: Real-time database health tracking
- ✅ **Async+Transactional Fixes**: Anti-pattern resolved across all services
- ⚠️ **Queue Processor**: Missing component needs implementation (see Known Issues)

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
- **Evaluation Dashboard**: http://localhost:7860/evaluations
- **Evaluation API**: http://localhost:7860/evaluations/api/stats
- **Health Monitoring**: http://localhost:7860/actuator/health

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

## Model Evaluation System 📊

### Current Status
The evaluation system is **fully operational** and Phase 2 ready with:
- ✅ Complete evaluation framework with UI dashboard
- ✅ Real-time progress tracking via Server-Sent Events
- ✅ Multi-model support (OpenAI, Anthropic, Google, Mistral)
- ✅ Benchmark-based testing with customizable tasks
- ✅ Screenshot capture and storage for visual verification
- ✅ Async+Transactional fixes applied (no more anti-patterns)

### Evaluation Endpoints

#### Start Evaluation
```bash
curl -X POST http://localhost:7860/evaluations/start \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "modelName=gpt-4o-mini&modelProvider=openai&benchmarkName=travel_search&initiatedBy=admin"
```

#### Get Evaluation Progress
```bash
curl http://localhost:7860/evaluations/{evaluationId}/progress
```

#### Get Evaluation Results
```bash
curl http://localhost:7860/evaluations/{evaluationId}/results
```

#### List Active Evaluations
```bash
curl http://localhost:7860/evaluations/api/active
```

#### Get System Stats
```bash
curl http://localhost:7860/evaluations/api/stats
```

### Available Models
- **gpt-4o-mini** (OpenAI) - Fast and efficient
- **claude-3-haiku** (Anthropic) - High quality reasoning
- **gemini-2.0-flash** (Google) - Latest Gemini model
- **gemma-3b** (OpenRouter) - Open source alternative
- **mistral-large** (Mistral) - European AI model

### Testing Procedures

#### 1. Quick Evaluation Test
```bash
# Start a simple evaluation
curl -X POST http://localhost:7860/evaluations/start \
  -d "modelName=gpt-4o-mini&modelProvider=openai&benchmarkName=travel_search&initiatedBy=test"

# Monitor progress (replace {id} with returned evaluationId)
curl http://localhost:7860/evaluations/{id}/progress
```

#### 2. Dashboard Monitoring
- Visit: http://localhost:7860/evaluations
- Real-time updates via SSE
- Progress bars and status indicators
- Task-by-task breakdown with screenshots

#### 3. Evaluation Health Check
```bash
# Check if evaluation system is responsive
curl http://localhost:7860/evaluations/api/stats

# Verify database connections
curl http://localhost:7860/actuator/health
```

## Configuration


### Docker Environment
- **Port**: 7860
- **Profiles**: application-docker.yml
- **Neo4j Auth**: neo4j/password123
- **PostgreSQL**: agent/agent123

## Database Connection Pool Monitoring 📈

### Connection Pool Health
The system includes comprehensive HikariCP connection pool monitoring:

#### Real-time Monitoring
- **Automatic Logging**: Every 60 seconds in application logs
- **Health Endpoint**: `/actuator/health` includes pool status
- **Utilization Alerts**: Warnings at 80%+ usage
- **Connection Waiting**: Alerts when threads wait for connections

#### Pool Statistics
```bash
# Check pool health via actuator
curl http://localhost:7860/actuator/health | jq '.components.connectionPool'

# Monitor logs for pool stats
docker logs a2awebagent | grep "HikariCP Pool Stats"
```

#### Pool Configuration
- **Maximum Pool Size**: Configured per environment
- **Connection Timeout**: Prevents hanging operations
- **Idle Timeout**: Efficient resource management
- **Leak Detection**: Identifies connection leaks

### Recent Fixes Applied ✅

#### Async+Transactional Anti-Pattern Resolution
Fixed in these services:
- **ToolDescriptionCacheService**: Separated async wrapper from transactional method
- **TaskGraphService**: Split graph logging into async + transactional methods  
- **ScreenshotEmbeddingService**: Isolated embedding processing transactions

**Pattern Applied**: 
1. Async method as public entry point
2. Protected/private transactional method for database operations
3. Proper exception handling maintained in both layers

## Known Issues ⚠️

### Queue Processor Missing
The evaluation system references `WebBrowsingTaskProcessor` but lacks a dedicated queue processing component:
- **Current**: Direct method calls in TaskExecutorService
- **Needed**: Queue-based task processing for scalability
- **Impact**: Evaluation tasks may not scale under load
- **Location**: Referenced in ModelEvaluationService and TaskExecutorService

**Recommendation**: Implement a proper queue processor (Redis-based) for async task handling.

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

# Connection Pool Health
curl http://localhost:7860/actuator/health
```

### Evaluation System Issues

**4. Evaluation Not Starting**
```bash
# Check if benchmark exists
curl http://localhost:7860/evaluations/api/benchmarks

# Verify model configuration
curl http://localhost:7860/evaluations/api/models

# Check evaluation service logs
docker logs a2awebagent | grep "ModelEvaluationService"
```

**5. Evaluation Stuck/Hanging**
```bash
# Check active evaluations
curl http://localhost:7860/evaluations/api/active

# Monitor connection pool
curl http://localhost:7860/actuator/health | jq '.components.connectionPool'

# Cancel stuck evaluation
curl -X DELETE http://localhost:7860/evaluations/{evaluationId}
```

**6. WebBrowsingTaskProcessor Issues**
- WebBrowsingTaskProcessor exists but may need queue integration
- Check logs for task processing errors
- Verify Playwright browser setup

### Testing

#### System Health Tests
```bash
# Quick sanity test
./sanity_test.sh

# MCP health check
curl -s http://localhost:7860/v1/health | jq .

# Tool count verification
curl -s http://localhost:7860/v1/tools | jq '.toolCount'

# Connection pool health
curl -s http://localhost:7860/actuator/health | jq '.components.connectionPool'
```

#### Evaluation System Tests
```bash
# Test evaluation API
curl http://localhost:7860/evaluations/api/stats

# Start test evaluation
curl -X POST http://localhost:7860/evaluations/start \
  -d "modelName=gpt-4o-mini&modelProvider=openai&benchmarkName=travel_search&initiatedBy=test"

# Monitor evaluation dashboard
open http://localhost:7860/evaluations
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

### Development Rules
**IMPORTANT**: Never create fallback content. If data is missing, investigate the root cause rather than creating workarounds. Missing data typically indicates incomplete backend data mapping or missing field assignments.

### Current Development Workflow

#### For Evaluation System Changes:
1. **Test evaluation endpoints** before and after changes
2. **Monitor connection pool** during development
3. **Check async transaction separation** in modified services
4. **Verify evaluation progress tracking** works properly
5. **Test both UI dashboard and API endpoints**

#### For Core System Changes:
1. **Run sanity_test.sh** before making changes
2. **Test MCP tool endpoints** after modifications  
3. **Monitor database connections** during development
4. **Verify async logging** doesn't block main operations

### Performance Targets
- Tool Discovery: <100ms
- Web Automation: 2-5 seconds
- MCP Tool Execution: 1-3 seconds
- Neo4j Async Logging: Non-blocking
- Evaluation Start: <2 seconds
- Connection Pool Health Check: <500ms

DEV WORKFLOW

to restart and vrify that changes work,
docker-compose up --build a2awebagent -d && docker-compose restart a2awebagent

to see logs use a vairant of,
docker-compose logs -f a2awebagent