# Tutorial 02: Docker Setup and Multi-Service Configuration

## **What You'll Learn**
Master the Docker configuration for a2aTravelAgent, including PostgreSQL, Redis, Neo4j, and the Playwright-enabled Spring Boot application.

## **Learning Objectives**
- ✅ Understand the multi-service Docker architecture
- ✅ Configure PostgreSQL for tool description caching  
- ✅ Set up Redis for real-time messaging
- ✅ Deploy Playwright in a Docker container
- ✅ Master Docker Compose service orchestration

## **Prerequisites**
- ✅ Completed Tutorial 01: Project Overview
- ✅ Docker and Docker Compose installed
- ✅ 8GB+ RAM available for containers
- ✅ Understanding of basic Docker concepts

## **Step 1: Understanding the Service Architecture**

### **🐳 Container Overview**
```yaml
services:
  a2awebagent:      # Main Spring Boot application
  postgres:         # PostgreSQL database
  redis:           # Redis cache/messaging  
  neo4j:           # Neo4j graph database
  openserp:        # Search API service
```

### **📊 Resource Requirements**
```bash
# Memory allocation per service
a2awebagent: 2GB (Playwright + JVM)
postgres:    512MB
redis:       256MB  
neo4j:       1GB
openserp:    256MB
Total:       ~4GB minimum
```

## **Step 2: Docker Compose Configuration Deep Dive**

### **🗄️ PostgreSQL Service**
```yaml
postgres:
  image: postgres:15-alpine
  environment:
    POSTGRES_DB: a2awebagent
    POSTGRES_USER: agent
    POSTGRES_PASSWORD: agent123
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./init-scripts:/docker-entrypoint-initdb.d
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U agent"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**💡 PostgreSQL Learning Points:**
- **Alpine Image**: Smaller footprint for faster deployment
- **Health Checks**: Ensures database is ready before app starts
- **Volume Persistence**: Data survives container restarts
- **Initialization Scripts**: Automatic schema setup

### **🚀 Redis Service**
```yaml
redis:
  image: redis:7-alpine
  command: redis-server --appendonly yes
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 3s
    retries: 5
```

**💡 Redis Learning Points:**
- **Persistence Enabled**: `--appendonly yes` for data durability
- **Health Monitoring**: Redis ping for service verification
- **Volume Mount**: Persistent storage for cache data

### **🕸️ Neo4j Service**
```yaml
neo4j:
  image: neo4j:5-community
  environment:
    NEO4J_AUTH: neo4j/password123
    NEO4J_PLUGINS: '["apoc"]'
    NEO4J_apoc_export_file_enabled: true
  ports:
    - "7474:7474"  # HTTP
    - "7687:7687"  # Bolt
  volumes:
    - neo4j_data:/data
    - neo4j_logs:/logs
```

**💡 Neo4j Learning Points:**
- **APOC Plugin**: Advanced procedures for graph operations
- **Dual Ports**: HTTP interface (7474) and Bolt protocol (7687)
- **Future Usage**: Travel knowledge graphs and recommendations

## **Step 3: Main Application Container**

### **🎭 Playwright-Enabled Spring Boot**
```dockerfile
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy and install dependencies (cached layer)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source and build
COPY src/ ./src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# Runtime configuration
EXPOSE 7860
ENTRYPOINT ["/app/entrypoint.sh"]
```

**💡 Dockerfile Learning Points:**
- **Playwright Base**: Pre-installed browsers and dependencies
- **Multi-Stage Caching**: Maven dependencies cached for faster builds
- **Layer Optimization**: Dependencies cached separately from source code
- **Health Checks**: Proper container health monitoring

### **⚙️ Environment Configuration**
```yaml
a2awebagent:
  build: .
  ports:
    - "7860:7860"
  environment:
    # Database connections
    DATABASE_URL: jdbc:postgresql://postgres:5432/a2awebagent
    REDIS_URL: redis://redis:6379
    NEO4J_URI: bolt://neo4j:7687
    
    # AI API Keys (from .env file)
    openAiKey: ${OPENAI_KEY}
    claudeKey: ${CLAUDE_KEY}
    geminiProjectId: ${GEMINI_PROJECT_ID}
    
    # Spring profiles
    SPRING_PROFILES_ACTIVE: docker
    
    # JVM optimization
    JAVA_OPTS: "-Xmx2g -XX:+UseG1GC"
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
    neo4j:
      condition: service_started
```

## **Step 4: Hands-On Setup**

### **📋 Prerequisites Check**
```bash
# Verify Docker installation
docker --version
docker-compose --version

# Check available memory
docker system info | grep "Total Memory"

# Verify port availability  
netstat -ln | grep -E "(5432|6379|7474|7687|7860)"
```

### **🔐 Environment Setup**
```bash
# 1. Create environment file
cp .env.template .env

# 2. Edit .env with your API keys
nano .env
```

**Required in .env:**
```bash
# OpenRouter/OpenAI API Key
OPENAI_KEY=your_openrouter_key_here

# Google Gemini (optional)
GEMINI_PROJECT_ID=your_project_id

# Anthropic Claude (optional)  
CLAUDE_KEY=your_claude_key_here

# Serper for Google Search
SERPER_KEY=your_serper_key_here
```

### **🚀 First Time Launch**
```bash
# 1. Clean start (removes old data)
docker-compose down -v
docker system prune -f

# 2. Build and start all services
docker-compose up --build -d

# 3. Monitor startup logs
docker-compose logs -f a2awebagent
```

### **📊 Health Verification**
```bash
# Check all services are running
docker-compose ps

# Test database connection
docker-compose exec postgres psql -U agent -d a2awebagent -c "\\dt"

# Test Redis
docker-compose exec redis redis-cli ping

# Test main application
curl http://localhost:7860/v1/tasks/health
```

## **Step 5: Service Integration Testing**

### **🧪 PostgreSQL Cache Test**
```bash
# Check tool descriptions table
docker-compose exec postgres psql -U agent -d a2awebagent -c "
SELECT COUNT(*) as cached_tools FROM tool_descriptions;
"

# View cached tools
docker-compose exec postgres psql -U agent -d a2awebagent -c "
SELECT tool_name, provider_model, generation_time_ms 
FROM tool_descriptions 
ORDER BY created_at DESC LIMIT 5;
"
```

### **🔄 Redis Pub/Sub Test**
```bash
# Terminal 1: Subscribe to task updates
docker-compose exec redis redis-cli SUBSCRIBE task_updates

# Terminal 2: Publish test message  
docker-compose exec redis redis-cli PUBLISH task_updates "Test message"
```

### **🌐 Web Automation Test**
```bash
# Test Playwright browser automation
curl -X POST -H "Content-Type: application/json" \
-d '{
  "name": "browseWebAndReturnText",
  "arguments": {
    "provideAllValuesInPlainEnglish": "Go to example.com and get the page title"
  }
}' \
http://localhost:7860/v1/tools/call
```

## **Step 6: Advanced Configuration**

### **🔧 Performance Tuning**
```yaml
# docker-compose.override.yml for development
version: '3.8'
services:
  a2awebagent:
    environment:
      # JVM tuning
      JAVA_OPTS: "-Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
      
      # Playwright optimization
      PLAYWRIGHT_BROWSERS_PATH: /app/playwrightCache
      
      # Spring Boot dev tools
      SPRING_DEVTOOLS_RESTART_ENABLED: true
    volumes:
      # Hot reload source code
      - ./src:/app/src
      - playwright_cache:/app/playwrightCache
```

### **🗄️ Database Optimization**
```sql
-- Connect to PostgreSQL and optimize
docker-compose exec postgres psql -U agent -d a2awebagent

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tool_descriptions_provider_tool 
ON tool_descriptions(provider_model, tool_name);

CREATE INDEX IF NOT EXISTS idx_tool_descriptions_created_at 
ON tool_descriptions(created_at);

-- View current cache statistics
SELECT 
    provider_model,
    COUNT(*) as tool_count,
    AVG(generation_time_ms) as avg_generation_time,
    SUM(usage_count) as total_usage
FROM tool_descriptions 
GROUP BY provider_model;
```

## **Step 7: Troubleshooting Common Issues**

### **🚨 Container Won't Start**
```bash
# Check Docker resources
docker system df
docker system info

# View detailed logs
docker-compose logs --tail=100 a2awebagent

# Check port conflicts
netstat -tulpn | grep -E "(5432|6379|7474|7860)"
```

### **💾 Database Connection Issues**
```bash
# Test database connectivity
docker-compose exec postgres pg_isready -U agent

# Check network connectivity
docker-compose exec a2awebagent ping postgres

# Verify environment variables
docker-compose exec a2awebagent env | grep DATABASE
```

### **🎭 Playwright Browser Issues**
```bash
# Check browser installation
docker-compose exec a2awebagent playwright --version

# Test browser launch
docker-compose exec a2awebagent bash -c "
cd /app && java -cp target/classes:target/dependency/* \
io.wingie.playwright.PlaywrightHealthCheck
"

# Check display/graphics support
docker-compose exec a2awebagent bash -c "ls -la /tmp/.X11-unix"
```

### **🔧 Memory and Performance Issues**
```bash
# Monitor container resource usage
docker stats

# Check Java heap usage
docker-compose exec a2awebagent bash -c "
jcmd \$(pgrep java) GC.run_finalization
jcmd \$(pgrep java) VM.memory_summary
"

# Optimize garbage collection
# Add to docker-compose.yml environment:
JAVA_OPTS: "-Xmx2g -XX:+UseG1GC -XX:+PrintGC"
```

## **Step 8: Production Deployment Considerations**

### **🔒 Security Hardening**
```yaml
# Production docker-compose.yml changes
services:
  postgres:
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_password
    secrets:
      - postgres_password
      
  a2awebagent:
    environment:
      # Remove default passwords
      # Use Docker secrets or external secret management
      openAiKey: /run/secrets/openai_key
    secrets:
      - openai_key

secrets:
  postgres_password:
    external: true
  openai_key:
    external: true
```

### **📊 Monitoring and Logging**
```yaml
# Add monitoring stack
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
      
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
```

## **Step 9: Backup and Recovery**

### **💾 Database Backup**
```bash
# Create backup
docker-compose exec postgres pg_dump -U agent a2awebagent > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U agent a2awebagent < backup.sql
```

### **🔄 Volume Management**
```bash
# List volumes
docker volume ls | grep a2awebagent

# Backup volumes
docker run --rm -v a2awebagent_postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_backup.tar.gz -C /data .

# Restore volumes
docker run --rm -v a2awebagent_postgres_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_backup.tar.gz -C /data
```

## **Key Takeaways**

✅ **Multi-service orchestration** with proper dependency management  
✅ **Health checks and monitoring** ensure reliable startup  
✅ **Volume persistence** protects data across container restarts  
✅ **Environment-based configuration** supports different deployment scenarios  
✅ **Performance optimization** through proper resource allocation  
✅ **Troubleshooting skills** for common Docker and service issues  

## **What's Next?**

Continue to **Tutorial 03: MCP Protocol Setup** to understand how the Model Context Protocol enables AI agent communication.

---

**Prerequisites for Next Tutorial**: Running Docker environment  
**Estimated Time**: 30 minutes  
**Difficulty**: Intermediate