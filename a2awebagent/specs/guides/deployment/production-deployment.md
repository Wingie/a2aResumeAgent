# 🚀 Production Deployment Guide

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Audience:** DevOps Engineers, System Administrators

## 📋 Prerequisites

### System Requirements
- **OS**: Linux (Ubuntu 20.04+ recommended) or macOS
- **Memory**: 8GB+ RAM (16GB recommended for full stack)
- **Storage**: 50GB+ free space
- **CPU**: 4+ cores recommended
- **Network**: Internet access for AI API calls

### Software Dependencies
- **Docker**: 24.0+ with Docker Compose
- **Git**: For source code management
- **curl**: For health checks and testing

## 🐳 Docker Production Deployment

### 1. Environment Setup

```bash
# Clone repository
git clone https://github.com/your-org/a2aTravelAgent.git
cd a2aTravelAgent/a2awebagent

# Create production environment file
cat > .env << EOF
# AI Provider API Keys
OPENAI_API_KEY=sk-your-production-openai-key
CLAUDE_KEY=sk-ant-your-production-claude-key
GEMINI_API_KEY=your-production-gemini-key
SERPER_KEY=your-production-serper-key

# Database Passwords (Change these!)
POSTGRES_PASSWORD=your-secure-postgres-password-here
REDIS_PASSWORD=your-secure-redis-password-here
NEO4J_PASSWORD=your-secure-neo4j-password-here

# Application Configuration
SPRING_PROFILES_ACTIVE=docker,production
LOG_LEVEL=INFO
SERVER_PORT=7860

# Security
JWT_SECRET=your-jwt-secret-here
ENCRYPTION_KEY=your-encryption-key-here
EOF
```

### 2. Docker Compose Configuration

Ensure your `docker-compose.yml` includes production optimizations:

```yaml
version: '3.8'
services:
  a2awebagent:
    build: 
      context: .
      dockerfile: Dockerfile
    ports:
      - "7860:7860"
    environment:
      - SPRING_PROFILES_ACTIVE=docker,production
      - DATABASE_URL=jdbc:postgresql://postgres:5432/a2awebagent
      - REDIS_URL=redis://redis:6379
      - NEO4J_URI=bolt://neo4j:7687
    env_file:
      - .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      neo4j:
        condition: service_healthy
    volumes:
      - ./screenshots:/app/screenshots
      - ./logs:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:7860/v1/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=a2awebagent
      - POSTGRES_USER=agent
    env_file:
      - .env
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U agent -d a2awebagent"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    env_file:
      - .env
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "--no-auth-warning", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  neo4j:
    image: neo4j:5
    environment:
      - NEO4J_PLUGINS=["graph-data-science"]
      - NEO4J_dbms_memory_heap_initial__size=1G
      - NEO4J_dbms_memory_heap_max__size=4G
      - NEO4J_dbms_memory_pagecache_size=2G
    env_file:
      - .env
    volumes:
      - neo4j_data:/data
      - neo4j_logs:/logs
    ports:
      - "7474:7474"
      - "7687:7687"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "cypher-shell", "-u", "neo4j", "-p", "${NEO4J_PASSWORD}", "RETURN 1"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  redis_data:
  neo4j_data:
  neo4j_logs:
```

### 3. Deployment Commands

```bash
# Build and start all services
docker-compose up -d

# Verify all services are healthy
docker-compose ps

# Check application logs
docker-compose logs -f a2awebagent

# Test application health
curl http://localhost:7860/v1/health
```

## 🔒 Security Configuration

### 1. Firewall Setup

```bash
# Ubuntu/Debian
sudo ufw enable
sudo ufw allow 22    # SSH
sudo ufw allow 7860  # Application
sudo ufw allow from trusted-ip to any port 5432  # PostgreSQL (restricted)
sudo ufw allow from trusted-ip to any port 6379  # Redis (restricted)
sudo ufw allow from trusted-ip to any port 7474  # Neo4j (restricted)
```

### 2. SSL/TLS Configuration

For production, use a reverse proxy like Nginx:

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /path/to/your/certificate.pem;
    ssl_certificate_key /path/to/your/private.key;
    
    location / {
        proxy_pass http://localhost:7860;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support for SSE
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 📊 Monitoring & Observability

### 1. Health Checks

```bash
#!/bin/bash
# health-check.sh

echo "=== A2A Travel Agent Health Check ==="

# Application health
echo -n "Application: "
if curl -s http://localhost:7860/v1/health | grep -q "UP"; then
    echo "✅ Healthy"
else
    echo "❌ Unhealthy"
fi

# Database connections
echo -n "PostgreSQL: "
if docker exec a2a-postgres pg_isready -U agent -d a2awebagent > /dev/null 2>&1; then
    echo "✅ Connected"
else
    echo "❌ Connection failed"
fi

echo -n "Redis: "
if docker exec a2a-redis redis-cli ping | grep -q "PONG"; then
    echo "✅ Connected"
else
    echo "❌ Connection failed"
fi

echo -n "Neo4j: "
if docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "RETURN 1" > /dev/null 2>&1; then
    echo "✅ Connected"
else
    echo "❌ Connection failed"
fi

# MCP Tools
echo -n "MCP Tools: "
TOOL_COUNT=$(curl -s http://localhost:7860/v1/tools | jq '.tools | length' 2>/dev/null)
if [ "$TOOL_COUNT" -gt 5 ]; then
    echo "✅ $TOOL_COUNT tools available"
else
    echo "❌ Tools unavailable"
fi
```

### 2. Log Management

```bash
# Set up log rotation
cat > /etc/logrotate.d/a2awebagent << EOF
/path/to/a2awebagent/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
}
EOF
```

## 💾 Backup Strategy

### 1. Database Backups

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backups/$(date +%Y-%m-%d)"
mkdir -p $BACKUP_DIR

# PostgreSQL backup
docker exec a2a-postgres pg_dump -U agent a2awebagent > $BACKUP_DIR/postgres-backup.sql

# Redis backup
docker exec a2a-redis redis-cli BGSAVE
docker cp a2a-redis:/data/dump.rdb $BACKUP_DIR/redis-backup.rdb

# Neo4j backup
docker exec a2a-neo4j neo4j-admin database dump --to-path=/backups neo4j
docker cp a2a-neo4j:/backups/neo4j.dump $BACKUP_DIR/neo4j-backup.dump

# Application screenshots
tar -czf $BACKUP_DIR/screenshots-backup.tar.gz ./screenshots/

echo "Backup completed: $BACKUP_DIR"
```

### 2. Automated Backups

Add to crontab:
```bash
# Daily backups at 2 AM
0 2 * * * /path/to/backup.sh
```

## 🔄 Updates & Maintenance

### 1. Application Updates

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose down a2awebagent
docker-compose build a2awebagent
docker-compose up -d a2awebagent

# Verify deployment
curl http://localhost:7860/v1/health
```

### 2. Database Maintenance

```bash
# PostgreSQL maintenance
docker exec a2a-postgres psql -U agent -d a2awebagent -c "VACUUM ANALYZE;"

# Redis maintenance
docker exec a2a-redis redis-cli FLUSHDB  # Careful: clears cache

# Neo4j maintenance
docker exec a2a-neo4j cypher-shell -u neo4j -p password123 "CALL db.stats.collect();"
```

## 🚨 Troubleshooting

### Common Issues

1. **Out of Memory**: Increase Docker memory limits
2. **Database Connection Failures**: Check network connectivity and credentials
3. **API Key Issues**: Verify environment variables are set correctly
4. **Performance Issues**: Monitor resource usage and scale accordingly

### Emergency Procedures

```bash
# Complete system restart
docker-compose down
docker-compose up -d

# Reset all data (DESTRUCTIVE)
docker-compose down -v
docker-compose up -d
```

## 📈 Performance Optimization

### Resource Allocation
- **Application**: 4GB RAM minimum
- **PostgreSQL**: 2GB RAM, SSD storage
- **Redis**: 1GB RAM
- **Neo4j**: 4GB RAM for large datasets

### Scaling Recommendations
- Use Docker Swarm or Kubernetes for horizontal scaling
- Consider read replicas for PostgreSQL
- Implement Redis clustering for high availability
- Monitor and tune JVM parameters

---

**🎯 This production deployment guide ensures reliable, secure, and scalable deployment of the A2A Travel Agent v1.0 system.**