# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is Wingston Sharon's **a2aTravelAgent** - an enterprise-grade AI-powered web automation agent that demonstrates the convergence of modern AI protocols (A2A & MCP), async task management, and intelligent web automation. Built with Spring Boot 3.2.4, this system showcases production-ready patterns for AI-agent integration in enterprise environments.

## Project Philosophy & Vision

### Core Innovation Areas
- **AI-Human Collaboration**: Natural language interface to complex web automation tasks
- **Real-time Interactive Systems**: Async task processing with live progress tracking
- **Protocol Standardization**: Pioneer implementation of A2A and MCP protocols
- **Enterprise-Grade Architecture**: Production-ready scalability and monitoring

### Technical Philosophy
- Real-time interactive systems over batch processing
- Human-AI collaboration rather than full automation
- Educational value and community contribution
- Open source contribution to creative coding ecosystems

## Architecture Overview

### Multi-Protocol Support
- **A2A (Agent-to-Agent)**: Google's standard for agent interoperability
- **MCP (Model Context Protocol)**: Anthropic's standard for AI-application integration
- **JSON-RPC 2.0**: Bidirectional communication protocol
- **WebSocket**: Real-time progress updates via Server-Sent Events

### Technology Stack

#### Backend Infrastructure
- **Spring Boot 3.2.4**: Enterprise application framework
- **Java 18**: Modern JDK with enhanced performance
- **Maven 3.1+**: Dependency management and build automation
- **Lombok**: Code generation for POJOs

#### AI Integration Layer
- **tools4ai 1.1.6.2**: AI tool integration framework with prompt annotations
- **a2ajava 0.1.9.6**: Agent-to-agent communication framework
- **Multi-Provider AI**: OpenAI GPT-4, Google Gemini, Anthropic Claude

#### Web Automation Engine
- **Microsoft Playwright 1.51.0**: Advanced web automation
- **Selenium WebDriver 4.32.0**: Cross-browser automation
- **Chrome DevTools Protocol**: Deep browser integration

#### Data Persistence
- **PostgreSQL 15**: Primary database with ACID compliance
- **Redis 7**: Real-time caching and pub/sub messaging
- **Neo4j 5**: Graph database for knowledge representation
- **H2**: In-memory database for testing

#### Real-time Communication
- **Server-Sent Events (SSE)**: Live progress updates
- **WebSocket**: Bidirectional real-time communication
- **Spring WebSocket**: Reactive messaging framework

#### Monitoring & Observability
- **Spring Actuator**: Health checks and metrics
- **Micrometer Prometheus**: Metrics collection
- **Structured Logging**: Debug and production monitoring

## Core Components Deep Dive

### 1. Task Execution Engine (`TaskExecutorService`)
```java
@Service
@Async
public class TaskExecutorService {
    // Async task processing with real-time progress tracking
    // Supports parallel execution and timeout handling
    // Integrates with multiple AI providers
}
```

#### Key Features
- **Async Processing**: Non-blocking task execution with Spring @Async
- **Progress Tracking**: Real-time updates via database and Redis
- **Timeout Management**: Configurable task timeouts with cleanup
- **Error Recovery**: Retry mechanisms and graceful failure handling

### 2. Web Automation Layer (`WebBrowsingTaskProcessor`)
```java
@Component
public class WebBrowsingTaskProcessor {
    // Natural language to web action translation
    // Screenshot capture and analysis
    // Multi-step automation workflows
}
```

#### Capabilities
- **Natural Language Processing**: Convert user queries to web actions
- **Screenshot Intelligence**: Automated visual documentation
- **Multi-site Orchestration**: Parallel automation across platforms
- **Dynamic Content Handling**: Adaptive automation for modern web apps

### 3. Protocol Integration (`MainEntryPoint`)
```java
@Component
@Agent(groupName = "travelResearch", 
       groupDescription = "Intelligent travel research automation")
public class MainEntryPoint extends SpringAwareJSONRpcController {
    // A2A and MCP protocol endpoint
    // Tool registration and discovery
    // Multi-modal AI integration
}
```

#### Protocol Features
- **Tool Discovery**: Automatic capability detection and registration
- **Security Integration**: RBAC with Spring Security
- **Multi-Protocol**: Simultaneous A2A and MCP support
- **Real-time Bidirectional**: WebSocket and HTTP+SSE transport

### 4. Database Architecture

#### Primary Entities
```java
@Entity
@Table(name = "task_executions")
public class TaskExecution {
    // Task lifecycle management
    // Progress tracking with timestamps
    // Screenshot relationship mapping
    // Error details and retry logic
}
```

#### Graph Knowledge Representation
- **Neo4j Integration**: Travel knowledge graphs
- **Relationship Mapping**: Location, service, and preference connections
- **Query Optimization**: Cypher for complex travel queries

## Development Workflow

### 1. Local Development Setup

#### Prerequisites
```bash
# Required versions
Java 18+
Maven 3.1+
Docker & Docker Compose
Node.js 18+ (for MCP client testing)
```

#### Quick Start
```bash
# Clone and setup
cd a2awebagent
mvn clean compile

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/a2awebagent-0.0.1.jar
```

### 2. Docker Deployment

#### Production Setup
```bash
# Setup Google Cloud credentials (required for Gemini)
gcloud auth application-default login
# OR copy existing credentials
cp ~/.config/gcloud/application_default_credentials.json ./

# Configure environment
echo "GEMINI_API_KEY=your_key_here" > .env

# Deploy with Docker Compose
docker-compose up -d
```

#### Multi-Service Architecture
```yaml
services:
  a2awebagent:      # Main application
  postgres:         # Primary database
  redis:            # Real-time caching
  neo4j:            # Graph database
  pgadmin:          # Database management (optional)
  redis-commander:  # Redis management (optional)
```

### 3. Testing Strategy

#### Integration Testing
```bash
# Run comprehensive test suite
mvn clean package test

# Run specific test categories
mvn test -Dtest=AsyncTaskIntegrationTest
mvn test -Dtest=WebAutomationTest
```

#### Test Categories
- **Flight Search**: Real-time automation with tabular output
- **LinkedIn Search**: Screenshot processing and data extraction
- **Concurrent Processing**: Multi-task parallel execution
- **System Health**: Endpoint and database connectivity

## AI Integration Best Practices

### 1. Multi-Provider Architecture

#### Configuration Pattern
```properties
# tools4ai.properties
agent.provider=gemini
gemini.modelName=gemini-2.0-flash-001
gemini.projectId=your-project-id
openAiModelName=gpt-4o-mini
anthropic.modelName=claude-3-haiku-20240307
```

#### Provider Fallback Strategy
1. **Primary**: Google Gemini (cost-effective, fast)
2. **Secondary**: OpenAI GPT-4 (advanced reasoning)
3. **Tertiary**: Anthropic Claude (creative tasks)

### 2. Prompt Engineering

#### Action Annotation Pattern
```java
@Action(description = "Research comprehensive travel options including flights, hotels, and attractions")
public String browseWebAndReturnText(
    @Parameter(description = "Natural language travel research query")
    String webBrowsingSteps
) {
    // AI-guided web automation implementation
}
```

#### Context-Aware Processing
- **Task Decomposition**: Break complex queries into atomic actions
- **Progress Communication**: Real-time status updates to user
- **Error Explanation**: Human-readable failure descriptions
- **Result Synthesis**: Structured output with visual documentation

### 3. Real-time Collaboration

#### Progress Tracking Pattern
```java
// Update progress with context
taskProgressService.updateProgress(
    taskId, 
    75, 
    "Processing flight search results - found 15 options"
);

// Add screenshot with metadata
taskProgressService.addScreenshot(
    taskId, 
    screenshotPath, 
    "Flight search results page"
);
```

## Security & Production Considerations

### 1. Authentication & Authorization

#### Google Cloud Integration
```bash
# Service account setup
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/credentials.json"

# Container mounting
volumes:
  - "./application_default_credentials.json:/app/credentials/application_default_credentials.json:ro"
```

#### Spring Security Configuration
- **RBAC**: Role-based access control for different agent capabilities
- **JWT Integration**: Stateless authentication for API access
- **CORS Configuration**: Secure cross-origin resource sharing

### 2. Production Deployment

#### Environment Configuration
```yaml
# application-docker.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://postgres:5432/a2awebagent}
    username: ${DB_USER:agent}
    password: ${DB_PASSWORD:agent123}
  
  data:
    redis:
      url: ${REDIS_URL:redis://redis:6379}
  
  neo4j:
    uri: ${NEO4J_URI:bolt://neo4j:7687}
```

#### Monitoring & Observability
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3. Error Handling & Recovery

#### Graceful Degradation
- **Circuit Breaker**: Fail fast for external service issues
- **Retry Logic**: Exponential backoff for transient failures
- **Fallback Strategies**: Alternative data sources and methods
- **User Communication**: Clear error messages and recovery suggestions

## Performance Optimization

### 1. Database Optimization

#### Query Performance
```sql
-- Optimized indexes for common queries
CREATE INDEX idx_task_status ON task_executions (status);
CREATE INDEX idx_task_type ON task_executions (task_type);
CREATE INDEX idx_created ON task_executions (created);
```

#### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### 2. Caching Strategy

#### Redis Integration
- **Session Storage**: User preferences and context
- **Result Caching**: Expensive computation results
- **Real-time Updates**: Progress and status broadcasting
- **Rate Limiting**: API request throttling

### 3. Async Processing

#### Task Executor Configuration
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        return executor;
    }
}
```

## API Documentation

### 1. REST Endpoints

#### Task Management
```http
POST /v1/tasks/submit
GET  /v1/tasks/{taskId}/status
GET  /v1/tasks/{taskId}/results
GET  /v1/tasks/active
GET  /v1/tasks/health
GET  /v1/tasks/stats
```

#### Real-time Updates
```http
GET /v1/tasks/{taskId}/progress-stream  # Server-Sent Events
GET /agents                             # Dashboard UI
```

### 2. Protocol Endpoints

#### A2A Integration
```http
POST /                    # JSON-RPC 2.0 endpoint
GET  /.well-known/agent   # Agent discovery
```

#### MCP Integration
```http
POST /mcp/tools/call      # Tool execution
GET  /mcp/tools/list      # Tool discovery
```

### 3. Example Usage

#### Travel Research Request
```bash
curl -X POST http://localhost:7860 \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "browseWebAndReturnText",
      "arguments": {
        "provideAllValuesInPlainEnglish": "Research flights from Amsterdam to London next weekend, find budget hotels, and recommend attractions. Create a comprehensive travel plan with screenshots."
      }
    },
    "id": 1
  }'
```

## Troubleshooting Guide

### 1. Common Issues

#### Google Cloud Authentication
```bash
# Issue: "Your default credentials were not found"
# Solution: Setup Application Default Credentials
gcloud auth application-default login

# Verify credentials
ls ~/.config/gcloud/application_default_credentials.json
```

#### Docker Build Performance
```bash
# Issue: Slow Maven dependency downloads
# Solution: Enable BuildKit cache mounts
DOCKER_BUILDKIT=1 docker-compose build
```

#### Test Failures
```bash
# Issue: Hibernate LazyInitializationException
# Solution: Use API endpoints instead of direct entity access
# Fixed in: AsyncTaskIntegrationTest.verifyLinkedInSearchResults()
```

### 2. Debugging

#### Application Logs
```bash
# View real-time logs
docker-compose logs -f a2awebagent

# Check specific service
docker-compose logs postgres
docker-compose logs redis
```

#### Database Access
```bash
# PostgreSQL via pgAdmin
http://localhost:8080
# Default: admin@a2awebagent.local / admin123

# Redis via Redis Commander
http://localhost:8081

# Neo4j Browser
http://localhost:7474
# Default: neo4j / password123
```

## Future Development Roadmap

### 1. Enhanced AI Capabilities
- **Multi-modal Integration**: Vision + Language models for screenshot analysis
- **Knowledge Graph Enhancement**: Auto-building travel knowledge from successful searches
- **Personalization Engine**: Learning user preferences and search patterns
- **Voice Interface**: Natural language voice commands for hands-free operation

### 2. Enterprise Features
- **SSO Integration**: SAML/OAuth2 for enterprise authentication
- **Audit Logging**: Comprehensive trail for compliance requirements
- **Role-Based Permissions**: Fine-grained access control for different user types
- **API Rate Limiting**: Advanced throttling and quota management

### 3. Performance Scaling
- **Horizontal Scaling**: Kubernetes deployment patterns
- **Database Sharding**: Multi-tenant data partitioning
- **CDN Integration**: Global content delivery for screenshots
- **Edge Computing**: Regional deployment for latency optimization

## Contributing Guidelines

### 1. Code Standards
- **Java Conventions**: Google Java Style Guide
- **Documentation**: Comprehensive JavaDoc for public APIs
- **Testing**: Minimum 80% code coverage
- **Security**: OWASP secure coding practices

### 2. Development Process
- **Feature Branches**: Git flow for new development
- **Code Review**: Mandatory peer review for all changes
- **Integration Testing**: Automated testing in CI/CD pipeline
- **Performance Testing**: Load testing for critical paths

### 3. Community Engagement
- **Open Source**: MIT license for community contribution
- **Documentation**: Maintain comprehensive README and guides
- **Issue Tracking**: GitHub issues for bug reports and feature requests
- **Knowledge Sharing**: Regular blog posts and conference presentations

---

## About the Author

**Wingston Sharon** is an Engineering Manager at Booking.com with 7+ years of experience driving technical innovation at massive scale. He's a pioneer in AI-agent integration, real-time audio processing, and creative technology applications.

### Key Achievements
- **100+ Open Source Projects**: Comprehensive portfolio spanning AI, audio, and automation
- **MCP Protocol Pioneer**: Early adopter and contributor to emerging AI standards
- **Production Impact**: Systems serving millions of users daily at Booking.com
- **Innovation Leadership**: Breakthrough work in neural audio synthesis and AI automation

### Professional Links
- **LinkedIn**: https://www.linkedin.com/in/wingstonsharon/
- **GitHub**: Arctic Code Vault Contributor with extensive portfolio
- **Location**: Amsterdam, Netherlands
- **Expertise**: AI/ML Engineering Leadership, Real-time Systems, Creative Technology

### Technical Philosophy
Specializes in bridging artificial intelligence with creative applications, emphasizing real-time interactive systems over batch processing, human-AI collaboration rather than full automation, and strong commitment to educational value and community contribution.

---

*This documentation represents the current state of the a2aTravelAgent project, demonstrating enterprise-grade AI-agent integration patterns and best practices for modern web automation systems.*