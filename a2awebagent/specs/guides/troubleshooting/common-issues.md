# 🔧 Troubleshooting Guide

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Audience:** Developers, System Administrators

## 🚨 Quick Diagnostics

### Health Check Script
```bash
#!/bin/bash
echo "=== A2A Travel Agent Diagnostics ==="

# Check Docker services
echo "1. Docker Services Status:"
docker-compose ps

# Check application health
echo -e "\n2. Application Health:"
curl -s http://localhost:7860/v1/health | jq .

# Check MCP tools
echo -e "\n3. MCP Tools Status:"
curl -s http://localhost:7860/v1/tools | jq '.tools | length'

# Check database connections
echo -e "\n4. Database Connections:"
docker exec a2a-postgres pg_isready -U agent -d a2awebagent
docker exec a2a-redis redis-cli ping
docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "RETURN 1"

# Check logs for errors
echo -e "\n5. Recent Error Logs:"
docker-compose logs a2awebagent | grep -i error | tail -5
```

## 🐳 Docker & Container Issues

### Problem: Container Won't Start
```bash
# Symptoms
docker-compose ps  # Shows container as "Exited"

# Diagnosis
docker-compose logs a2awebagent

# Common Solutions
1. Check environment variables in .env file
2. Verify port 7860 is not in use: netstat -tulpn | grep 7860
3. Increase Docker memory allocation (4GB minimum)
4. Check for missing dependencies: docker-compose build --no-cache
```

### Problem: Out of Memory Errors
```bash
# Symptoms
- Container restarts frequently
- Logs show "OutOfMemoryError"

# Solutions
1. Increase Docker memory limit:
   Docker Desktop > Settings > Resources > Memory (8GB+)

2. Add JVM memory settings to docker-compose.yml:
   environment:
     - JAVA_OPTS=-Xmx4g -Xms2g

3. Monitor memory usage:
   docker stats
```

### Problem: Database Connection Failures
```bash
# Symptoms
- "Connection refused" errors
- Application health check fails

# Diagnosis Steps
1. Check if databases are running:
   docker-compose ps postgres redis neo4j

2. Test connections manually:
   docker exec a2a-postgres psql -U agent -d a2awebagent -c "SELECT 1;"
   docker exec a2a-redis redis-cli ping
   docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "RETURN 1;"

# Solutions
1. Wait for services to fully start (can take 2-3 minutes)
2. Check if ports are blocked by firewall
3. Verify database passwords in .env file
4. Restart database services:
   docker-compose restart postgres redis neo4j
```

## 🔑 API Key & Authentication Issues

### Problem: API Keys Not Working
```bash
# Symptoms
- "Invalid API key" errors
- AI features not responding

# Diagnosis
1. Check if keys are set:
   docker exec a2awebagent env | grep -E "(OPENAI|CLAUDE|GEMINI)_"

2. Verify key format:
   - OpenAI: sk-...
   - Claude: sk-ant-...
   - Gemini: AIza... or similar

# Solutions
1. Update .env file with correct keys
2. Restart application: docker-compose restart a2awebagent
3. Test keys manually:
   curl -H "Authorization: Bearer $OPENAI_API_KEY" https://api.openai.com/v1/models
```

### Problem: MCP Authentication Errors
```bash
# Symptoms
- Claude Desktop can't connect
- MCP tools not appearing

# Solutions
1. Rebuild MCP connector:
   cd a2awebagent && ./build-mcp.sh

2. Check Claude Desktop config:
   ~/Library/Application Support/Claude/claude_desktop_config.json

3. Verify MCP server is running:
   curl http://localhost:7860/v1/tools

4. Test MCP endpoint directly:
   curl -X POST http://localhost:7860/v1 \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}'
```

## 🌐 Network & Connectivity Issues

### Problem: Application Not Accessible
```bash
# Symptoms
- Browser shows "connection refused"
- curl commands fail

# Diagnosis
1. Check if application is listening:
   netstat -tulpn | grep 7860
   docker-compose ps a2awebagent

2. Test from inside container:
   docker exec a2awebagent curl http://localhost:7860/v1/health

# Solutions
1. Check firewall settings:
   sudo ufw status
   sudo ufw allow 7860

2. Verify Docker port mapping:
   docker-compose.yml should have "7860:7860"

3. Check if another service is using port 7860:
   sudo lsof -i :7860
```

### Problem: Slow Response Times
```bash
# Symptoms
- API calls take >10 seconds
- Web interface is sluggish

# Diagnosis
1. Check system resources:
   docker stats
   htop or top

2. Monitor database performance:
   docker exec a2a-postgres psql -U agent -d a2awebagent -c "
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_exec_time DESC LIMIT 10;"

# Solutions
1. Increase system resources
2. Optimize database queries
3. Clear Redis cache:
   docker exec a2a-redis redis-cli FLUSHALL
4. Restart Neo4j to clear memory:
   docker-compose restart neo4j
```

## 🤖 AI & Tool Execution Issues

### Problem: Tools Not Executing
```bash
# Symptoms
- MCP calls return errors
- Screenshots not generating

# Diagnosis
1. Check tool availability:
   curl http://localhost:7860/v1/tools | jq '.tools[].name'

2. Test specific tool:
   curl -X POST http://localhost:7860/v1 \
     -H "Content-Type: application/json" \
     -d '{
       "jsonrpc": "2.0",
       "method": "tools/call",
       "params": {
         "name": "browseWebAndReturnText",
         "arguments": {"url": "https://example.com"}
       },
       "id": 1
     }'

# Solutions
1. Check Playwright dependencies:
   docker exec a2awebagent npx playwright install

2. Verify Chrome/Chromium is available:
   docker exec a2awebagent which google-chrome

3. Check screenshot directory permissions:
   docker exec a2awebagent ls -la /app/screenshots/
```

### Problem: AI Responses Are Slow/Invalid
```bash
# Symptoms
- Long delays in AI responses
- Error messages in responses

# Solutions
1. Check AI provider status pages
2. Verify API key quota/limits
3. Try different AI provider:
   - Set agent.provider=gemini in config
4. Monitor rate limiting
5. Check for network proxy issues
```

## 📊 Performance & Monitoring Issues

### Problem: High Memory Usage
```bash
# Diagnosis
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Solutions
1. Tune JVM garbage collection:
   JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

2. Optimize database connections:
   - Reduce connection pool size
   - Enable connection pooling

3. Clear Neo4j cache:
   docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "
   CALL db.clearQueryCaches();"
```

### Problem: Database Performance Issues
```bash
# PostgreSQL Optimization
1. Analyze slow queries:
   docker exec a2a-postgres psql -U agent -d a2awebagent -c "
   SELECT query, total_exec_time, mean_exec_time, calls
   FROM pg_stat_statements
   ORDER BY total_exec_time DESC LIMIT 10;"

2. Update statistics:
   docker exec a2a-postgres psql -U agent -d a2awebagent -c "ANALYZE;"

3. Check cache hit ratio:
   docker exec a2a-postgres psql -U agent -d a2awebagent -c "
   SELECT 
     sum(heap_blks_read) as heap_read,
     sum(heap_blks_hit) as heap_hit,
     sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as ratio
   FROM pg_statio_user_tables;"
```

## 🔍 Debugging Tools

### Enable Debug Logging
```bash
# Add to docker-compose.yml environment:
- LOG_LEVEL=DEBUG
- SPRING_PROFILES_ACTIVE=docker,debug

# View debug logs:
docker-compose logs -f a2awebagent | grep DEBUG
```

### Database Query Debugging
```bash
# Enable PostgreSQL query logging
docker exec a2a-postgres psql -U agent -d a2awebagent -c "
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_min_duration_statement = 1000;
SELECT pg_reload_conf();"

# View query logs:
docker exec a2a-postgres tail -f /var/lib/postgresql/data/log/postgresql-*.log
```

### JVM Monitoring
```bash
# Enable JMX monitoring
JAVA_OPTS="-Dcom.sun.management.jmxremote 
           -Dcom.sun.management.jmxremote.port=9999
           -Dcom.sun.management.jmxremote.authenticate=false"

# Connect with JConsole or VisualVM
```

## 🆘 Emergency Procedures

### Complete System Reset
```bash
# WARNING: This will delete all data!
docker-compose down -v
docker system prune -a
docker-compose up -d
```

### Backup Before Emergency Recovery
```bash
# Quick backup
mkdir emergency-backup-$(date +%Y%m%d)
docker exec a2a-postgres pg_dump -U agent a2awebagent > emergency-backup-$(date +%Y%m%d)/postgres.sql
docker cp a2a-redis:/data/dump.rdb emergency-backup-$(date +%Y%m%d)/redis.rdb
```

### Factory Reset with Data Preservation
```bash
# 1. Backup data
./backup.sh

# 2. Reset application only
docker-compose stop a2awebagent
docker-compose rm -f a2awebagent
docker rmi a2awebagent_a2awebagent
docker-compose up -d a2awebagent

# 3. Verify functionality
curl http://localhost:7860/v1/health
```

## 📞 Getting Help

### Log Collection for Support
```bash
# Collect all relevant logs
mkdir support-logs-$(date +%Y%m%d)
docker-compose logs > support-logs-$(date +%Y%m%d)/docker-compose.log
docker-compose ps > support-logs-$(date +%Y%m%d)/services-status.txt
curl -s http://localhost:7860/v1/health > support-logs-$(date +%Y%m%d)/health-check.json
docker stats --no-stream > support-logs-$(date +%Y%m%d)/resource-usage.txt

# Create support archive
tar -czf support-logs-$(date +%Y%m%d).tar.gz support-logs-$(date +%Y%m%d)/
```

### Useful Commands Reference
```bash
# System information
docker --version
docker-compose --version
uname -a
free -h
df -h

# Application status
docker-compose ps
curl http://localhost:7860/v1/health
curl http://localhost:7860/v1/tools | jq '.tools | length'

# Resource monitoring
docker stats --no-stream
docker system df
docker-compose top
```

---

**🔧 This troubleshooting guide covers the most common issues encountered with the A2A Travel Agent v1.0 system. For additional support, consult the research archive in `specs/reference/research-archive/`.**