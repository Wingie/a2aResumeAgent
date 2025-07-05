# Tutorial 9: Kubernetes Deployment for a2aTravelAgent

## Overview

This comprehensive tutorial demonstrates how to deploy the a2aTravelAgent application - a multi-module Spring Boot system with a2acore MCP framework and Playwright browser automation - to Kubernetes in production. The architecture includes PostgreSQL and Redis for data persistence, Neo4j for graph storage, and horizontal autoscaling based on MCP tool usage.

## Architecture Overview

### Multi-Module Structure
```
a2awebagent/
├── a2acore/           # MCP framework library
├── a2awebapp/         # Spring Boot application  
├── Dockerfile         # Multi-stage build
└── docker-compose.yml # Local development stack
```

### Key Components
- **a2acore**: MCP (Model Context Protocol) framework with tool descriptions
- **a2awebapp**: Spring Boot 3.2.4 application with Playwright automation
- **PostgreSQL**: Primary database with tool description caching
- **Redis**: Session storage and pub/sub messaging
- **Neo4j**: Graph database for relationship mapping
- **Playwright**: Headless browser automation in containers

---

## 1. Prerequisites and Environment Setup

### 1.1 Install Required Tools
```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install Docker (for building images)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

### 1.2 Verify Kubernetes Cluster
```bash
# Check cluster connection
kubectl cluster-info

# Verify node readiness
kubectl get nodes

# Check available storage classes
kubectl get storageclass
```

### 1.3 Create Namespace
```bash
kubectl create namespace a2a-travel-agent
kubectl config set-context --current --namespace=a2a-travel-agent
```

---

## 2. Building and Pushing Container Images

### 2.1 Multi-Stage Dockerfile Optimization
Create an optimized `Dockerfile.prod` for production:

```dockerfile
# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble AS builder

# Install build tools
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Maven files for dependency caching
COPY pom.xml .
COPY a2acore/pom.xml ./a2acore/
COPY a2awebapp/pom.xml ./a2awebapp/

# Download dependencies with cache mount
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Copy source code
COPY a2acore/src/ ./a2acore/src/
COPY a2awebapp/src/ ./a2awebapp/src/

# Build application
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# Production stage
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

# Create application user
RUN groupadd -r a2auser && useradd -r -g a2auser a2auser

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built application
COPY --from=builder /app/a2awebapp/target/a2awebapp-0.0.1.jar app.jar

# Create required directories
RUN mkdir -p /app/screenshots /app/logs /app/uploads /app/temp && \
    chown -R a2auser:a2auser /app

# Set JVM options for production
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:7860/actuator/health || exit 1

USER a2auser

EXPOSE 7860

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 2.2 Build and Push Images
```bash
# Build production image
docker build -f Dockerfile.prod -t your-registry.com/a2a-travel-agent:v1.0.0 .

# Push to registry
docker push your-registry.com/a2a-travel-agent:v1.0.0

# Tag as latest
docker tag your-registry.com/a2a-travel-agent:v1.0.0 your-registry.com/a2a-travel-agent:latest
docker push your-registry.com/a2a-travel-agent:latest
```

---

## 3. PostgreSQL Database Deployment

### 3.1 Create PostgreSQL Secret
```yaml
# postgresql-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
  namespace: a2a-travel-agent
type: Opaque
data:
  # Base64 encoded values
  postgres-user: YWdlbnQ=          # agent
  postgres-password: YWdlbnQxMjM=  # agent123
  postgres-db: YTJhd2ViYWdlbnQ=    # a2awebagent
```

```bash
kubectl apply -f postgresql-secret.yaml
```

### 3.2 PostgreSQL PersistentVolumeClaim
```yaml
# postgresql-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgresql-pvc
  namespace: a2a-travel-agent
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
  storageClassName: gp2  # AWS EBS, adjust for your cloud provider
```

```bash
kubectl apply -f postgresql-pvc.yaml
```

### 3.3 PostgreSQL Deployment
```yaml
# postgresql-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql
  namespace: a2a-travel-agent
  labels:
    app: postgresql
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
      - name: postgresql
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: postgres-user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: postgres-password
        - name: POSTGRES_DB
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: postgres-db
        - name: POSTGRES_INITDB_ARGS
          value: "--encoding=UTF-8"
        volumeMounts:
        - name: postgresql-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - agent
            - -d
            - a2awebagent
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - agent
            - -d
            - a2awebagent
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgresql-storage
        persistentVolumeClaim:
          claimName: postgresql-pvc
```

### 3.4 PostgreSQL Service
```yaml
# postgresql-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: a2a-travel-agent
  labels:
    app: postgresql
spec:
  selector:
    app: postgresql
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
```

```bash
kubectl apply -f postgresql-deployment.yaml
kubectl apply -f postgresql-service.yaml
```

---

## 4. Redis Cache Deployment

### 4.1 Redis ConfigMap
```yaml
# redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
  namespace: a2a-travel-agent
data:
  redis.conf: |
    maxmemory 512mb
    maxmemory-policy allkeys-lru
    appendonly yes
    appendfsync everysec
    save 900 1
    save 300 10
    save 60 10000
```

### 4.2 Redis PVC and Deployment
```yaml
# redis-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: a2a-travel-agent
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: gp2

---
# redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: a2a-travel-agent
  labels:
    app: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        command:
        - redis-server
        - /etc/redis/redis.conf
        volumeMounts:
        - name: redis-storage
          mountPath: /data
        - name: redis-config
          mountPath: /etc/redis
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "200m"
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc
      - name: redis-config
        configMap:
          name: redis-config

---
# redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: a2a-travel-agent
  labels:
    app: redis
spec:
  selector:
    app: redis
  type: ClusterIP
  ports:
  - port: 6379
    targetPort: 6379
```

```bash
kubectl apply -f redis-config.yaml
kubectl apply -f redis-pvc.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f redis-service.yaml
```

---

## 5. Neo4j Graph Database Deployment

### 5.1 Neo4j Secret and PVC
```yaml
# neo4j-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: neo4j-secret
  namespace: a2a-travel-agent
type: Opaque
data:
  neo4j-password: cGFzc3dvcmQxMjM=  # password123

---
# neo4j-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: neo4j-pvc
  namespace: a2a-travel-agent
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: gp2
```

### 5.2 Neo4j Deployment
```yaml
# neo4j-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: neo4j
  namespace: a2a-travel-agent
  labels:
    app: neo4j
spec:
  replicas: 1
  selector:
    matchLabels:
      app: neo4j
  template:
    metadata:
      labels:
        app: neo4j
    spec:
      containers:
      - name: neo4j
        image: neo4j:5-community
        ports:
        - containerPort: 7474  # HTTP
        - containerPort: 7687  # Bolt
        env:
        - name: NEO4J_AUTH
          value: "neo4j/password123"
        - name: NEO4J_PLUGINS
          value: '["apoc"]'
        - name: NEO4J_dbms_security_procedures_unrestricted
          value: "apoc.*"
        - name: NEO4J_dbms_memory_heap_initial__size
          value: "512m"
        - name: NEO4J_dbms_memory_heap_max__size
          value: "1g"
        volumeMounts:
        - name: neo4j-storage
          mountPath: /data
        resources:
          requests:
            memory: "1Gi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - cypher-shell
            - -u
            - neo4j
            - -p
            - password123
            - "RETURN 1"
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /
            port: 7474
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: neo4j-storage
        persistentVolumeClaim:
          claimName: neo4j-pvc

---
# neo4j-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: neo4j
  namespace: a2a-travel-agent
  labels:
    app: neo4j
spec:
  selector:
    app: neo4j
  type: ClusterIP
  ports:
  - name: http
    port: 7474
    targetPort: 7474
  - name: bolt
    port: 7687
    targetPort: 7687
```

```bash
kubectl apply -f neo4j-secret.yaml
kubectl apply -f neo4j-pvc.yaml
kubectl apply -f neo4j-deployment.yaml
kubectl apply -f neo4j-service.yaml
```

---

## 6. Application Configuration with ConfigMaps and Secrets

### 6.1 Application ConfigMap
```yaml
# app-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: a2a-travel-agent
data:
  application-k8s.yml: |
    spring:
      profiles:
        active: k8s
      
      datasource:
        url: jdbc:postgresql://postgresql:5432/a2awebagent
        driver-class-name: org.postgresql.Driver
        hikari:
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          maximum-pool-size: 20
          minimum-idle: 5
      
      jpa:
        hibernate:
          ddl-auto: update
        show-sql: false
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect
            format_sql: true
            use_sql_comments: false
      
      data:
        redis:
          host: redis
          port: 6379
          timeout: 5000ms
          lettuce:
            pool:
              max-active: 8
              max-wait: -1ms
              max-idle: 8
              min-idle: 0
      
      neo4j:
        uri: bolt://neo4j:7687
        authentication:
          username: neo4j
          password: password123
      
      task:
        execution:
          pool:
            core-size: 8
            max-size: 20
            queue-capacity: 200
            thread-name-prefix: "k8s-task-"
            keep-alive: 60s
          timeout: 600s
          cleanup-interval: 3600s
    
    server:
      port: 7860
      servlet:
        context-path: /
      tomcat:
        max-threads: 400
        min-spare-threads: 20
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
      metrics:
        export:
          prometheus:
            enabled: true
    
    logging:
      level:
        root: INFO
        io.wingie: DEBUG
        org.hibernate.SQL: WARN
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
      file:
        name: /app/logs/a2awebagent.log
        max-size: 100MB
        max-history: 10
    
    app:
      storage:
        screenshots: /app/screenshots
        uploads: /app/uploads
        cleanup-older-than: 3d
      
      playwright:
        timeout: 45000
        headless: true
        viewport:
          width: 1920
          height: 1080
        cache-dir: /app/playwright-cache
      
      async:
        enabled: true
        max-concurrent-tasks: 10
        progress-update-interval: 5s
      
      mcp:
        custom:
          enabled: true
        cache:
          enabled: true
        fallback:
          enabled: true
```

### 6.2 Application Secrets
```yaml
# app-secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: a2a-travel-agent
type: Opaque
data:
  # Base64 encoded API keys
  gemini-api-key: <BASE64_ENCODED_GEMINI_KEY>
  openai-api-key: <BASE64_ENCODED_OPENAI_KEY>
  mistral-api-key: <BASE64_ENCODED_MISTRAL_KEY>
  db-user: YWdlbnQ=
  db-password: YWdlbnQxMjM=
```

```bash
# Create secrets with actual API keys
kubectl create secret generic app-secrets \
  --from-literal=gemini-api-key="YOUR_GEMINI_KEY" \
  --from-literal=openai-api-key="YOUR_OPENAI_KEY" \
  --from-literal=mistral-api-key="YOUR_MISTRAL_KEY" \
  --from-literal=db-user="agent" \
  --from-literal=db-password="agent123" \
  -n a2a-travel-agent

kubectl apply -f app-config.yaml
```

---

## 7. Main Application Deployment

### 7.1 Application Deployment
```yaml
# a2a-travel-agent-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: a2a-travel-agent
  namespace: a2a-travel-agent
  labels:
    app: a2a-travel-agent
    version: v1.0.0
spec:
  replicas: 3
  selector:
    matchLabels:
      app: a2a-travel-agent
  template:
    metadata:
      labels:
        app: a2a-travel-agent
        version: v1.0.0
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: a2a-travel-agent
        image: your-registry.com/a2a-travel-agent:v1.0.0
        ports:
        - containerPort: 7860
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: JAVA_OPTS
          value: "-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
        - name: GEMINI_API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: gemini-api-key
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: openai-api-key
        - name: MISTRAL_API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: mistral-api-key
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: db-user
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: db-password
        volumeMounts:
        - name: app-config
          mountPath: /app/config
          readOnly: true
        - name: screenshots-storage
          mountPath: /app/screenshots
        - name: uploads-storage
          mountPath: /app/uploads
        - name: logs-storage
          mountPath: /app/logs
        - name: playwright-cache
          mountPath: /app/playwright-cache
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 7860
          initialDelaySeconds: 120
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 7860
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 7860
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 12
      volumes:
      - name: app-config
        configMap:
          name: app-config
      - name: screenshots-storage
        emptyDir:
          sizeLimit: 5Gi
      - name: uploads-storage
        emptyDir:
          sizeLimit: 2Gi
      - name: logs-storage
        emptyDir:
          sizeLimit: 1Gi
      - name: playwright-cache
        emptyDir:
          sizeLimit: 1Gi
      restartPolicy: Always
      terminationGracePeriodSeconds: 60
```

### 7.2 Application Service
```yaml
# a2a-travel-agent-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: a2a-travel-agent
  namespace: a2a-travel-agent
  labels:
    app: a2a-travel-agent
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "7860"
    prometheus.io/path: "/actuator/prometheus"
spec:
  selector:
    app: a2a-travel-agent
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 7860
    protocol: TCP
    name: http
```

```bash
kubectl apply -f a2a-travel-agent-deployment.yaml
kubectl apply -f a2a-travel-agent-service.yaml
```

---

## 8. Horizontal Pod Autoscaler (HPA)

### 8.1 Install Metrics Server (if not present)
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 8.2 HPA Configuration
```yaml
# a2a-travel-agent-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: a2a-travel-agent-hpa
  namespace: a2a-travel-agent
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: a2a-travel-agent
  minReplicas: 3
  maxReplicas: 15
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Max
```

```bash
kubectl apply -f a2a-travel-agent-hpa.yaml
```

---

## 9. Ingress Configuration for MCP Endpoints

### 9.1 Install NGINX Ingress Controller
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

### 9.2 TLS Certificate (Let's Encrypt)
```yaml
# certificate.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: a2a-travel-agent-tls
  namespace: a2a-travel-agent
spec:
  secretName: a2a-travel-agent-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - a2a-travel-agent.yourdomain.com
  - api.a2a-travel-agent.yourdomain.com
```

### 9.3 Ingress Resource
```yaml
# a2a-travel-agent-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: a2a-travel-agent-ingress
  namespace: a2a-travel-agent
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - a2a-travel-agent.yourdomain.com
    - api.a2a-travel-agent.yourdomain.com
    secretName: a2a-travel-agent-tls
  rules:
  - host: a2a-travel-agent.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: a2a-travel-agent
            port:
              number: 80
  - host: api.a2a-travel-agent.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: a2a-travel-agent
            port:
              number: 80
```

```bash
kubectl apply -f a2a-travel-agent-ingress.yaml
```

---

## 10. Monitoring and Observability

### 10.1 Prometheus ServiceMonitor
```yaml
# prometheus-servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: a2a-travel-agent-monitor
  namespace: a2a-travel-agent
  labels:
    app: a2a-travel-agent
spec:
  selector:
    matchLabels:
      app: a2a-travel-agent
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

### 10.2 Grafana Dashboard ConfigMap
```yaml
# grafana-dashboard.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: a2a-travel-agent-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  a2a-travel-agent.json: |
    {
      "dashboard": {
        "id": null,
        "title": "A2A Travel Agent Monitoring",
        "tags": ["a2a", "travel", "mcp"],
        "panels": [
          {
            "title": "HTTP Requests per Second",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(http_server_requests_seconds_count{service=\"a2a-travel-agent\"}[5m])",
                "legendFormat": "{{method}} {{uri}}"
              }
            ]
          },
          {
            "title": "JVM Memory Usage",
            "type": "graph",
            "targets": [
              {
                "expr": "jvm_memory_used_bytes{service=\"a2a-travel-agent\"}",
                "legendFormat": "{{area}}"
              }
            ]
          },
          {
            "title": "MCP Tool Executions",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(mcp_tool_executions_total{service=\"a2a-travel-agent\"}[5m])",
                "legendFormat": "{{tool_name}}"
              }
            ]
          },
          {
            "title": "Playwright Browser Sessions",
            "type": "singlestat",
            "targets": [
              {
                "expr": "playwright_active_sessions{service=\"a2a-travel-agent\"}"
              }
            ]
          }
        ]
      }
    }
```

```bash
kubectl apply -f prometheus-servicemonitor.yaml
kubectl apply -f grafana-dashboard.yaml
```

---

## 11. Security Policies and Network Policies

### 11.1 Pod Security Policy
```yaml
# pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: a2a-travel-agent-psp
  namespace: a2a-travel-agent
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### 11.2 Network Policy
```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: a2a-travel-agent-netpol
  namespace: a2a-travel-agent
spec:
  podSelector:
    matchLabels:
      app: a2a-travel-agent
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 7860
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 7860
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgresql
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  - to:
    - podSelector:
        matchLabels:
          app: neo4j
    ports:
    - protocol: TCP
      port: 7687
  - to: []  # Allow outbound internet for web automation
    ports:
    - protocol: TCP
      port: 80
    - protocol: TCP
      port: 443
```

```bash
kubectl apply -f pod-security-policy.yaml
kubectl apply -f network-policy.yaml
```

---

## 12. Helm Chart for Simplified Deployment

### 12.1 Create Helm Chart Structure
```bash
helm create a2a-travel-agent-chart
cd a2a-travel-agent-chart
```

### 12.2 Values.yaml
```yaml
# values.yaml
replicaCount: 3

image:
  repository: your-registry.com/a2a-travel-agent
  tag: "v1.0.0"
  pullPolicy: IfNotPresent

nameOverride: ""
fullnameOverride: ""

service:
  type: ClusterIP
  port: 80
  targetPort: 7860

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
  hosts:
    - host: a2a-travel-agent.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: a2a-travel-agent-tls
      hosts:
        - a2a-travel-agent.yourdomain.com

resources:
  limits:
    cpu: 2000m
    memory: 4Gi
  requests:
    cpu: 500m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 15
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

postgresql:
  enabled: true
  auth:
    username: agent
    password: agent123
    database: a2awebagent
  primary:
    persistence:
      enabled: true
      size: 20Gi
    resources:
      requests:
        memory: 512Mi
        cpu: 250m
      limits:
        memory: 1Gi
        cpu: 500m

redis:
  enabled: true
  auth:
    enabled: false
  master:
    persistence:
      enabled: true
      size: 5Gi
    resources:
      requests:
        memory: 256Mi
        cpu: 100m
      limits:
        memory: 512Mi
        cpu: 200m

neo4j:
  enabled: true
  auth:
    password: password123
  persistence:
    size: 10Gi
  resources:
    requests:
      memory: 1Gi
      cpu: 250m
    limits:
      memory: 2Gi
      cpu: 500m

secrets:
  geminiApiKey: ""
  openaiApiKey: ""
  mistralApiKey: ""

config:
  app:
    playwright:
      timeout: 45000
      headless: true
    async:
      maxConcurrentTasks: 10
    mcp:
      cache:
        enabled: true

monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
```

### 12.3 Deploy with Helm
```bash
# Install the chart
helm install a2a-travel-agent ./a2a-travel-agent-chart \
  --namespace a2a-travel-agent \
  --create-namespace \
  --set secrets.geminiApiKey="YOUR_GEMINI_KEY" \
  --set secrets.openaiApiKey="YOUR_OPENAI_KEY" \
  --set secrets.mistralApiKey="YOUR_MISTRAL_KEY" \
  --set ingress.hosts[0].host="a2a-travel-agent.yourdomain.com"

# Upgrade deployment
helm upgrade a2a-travel-agent ./a2a-travel-agent-chart \
  --namespace a2a-travel-agent \
  --set image.tag="v1.1.0"

# Rollback if needed
helm rollback a2a-travel-agent 1 --namespace a2a-travel-agent
```

---

## 13. Production Deployment Strategies

### 13.1 Blue-Green Deployment
```yaml
# blue-green-deployment.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: a2a-travel-agent-rollout
  namespace: a2a-travel-agent
spec:
  replicas: 5
  strategy:
    blueGreen:
      activeService: a2a-travel-agent-active
      previewService: a2a-travel-agent-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: a2a-travel-agent-preview.a2a-travel-agent.svc.cluster.local
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: a2a-travel-agent-active.a2a-travel-agent.svc.cluster.local
  selector:
    matchLabels:
      app: a2a-travel-agent
  template:
    metadata:
      labels:
        app: a2a-travel-agent
    spec:
      containers:
      - name: a2a-travel-agent
        image: your-registry.com/a2a-travel-agent:v1.0.0
        ports:
        - containerPort: 7860
```

### 13.2 Canary Deployment
```yaml
# canary-deployment.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: a2a-travel-agent-canary
  namespace: a2a-travel-agent
spec:
  replicas: 10
  strategy:
    canary:
      steps:
      - setWeight: 10
      - pause: {duration: 5m}
      - setWeight: 25
      - pause: {duration: 5m}
      - setWeight: 50
      - pause: {duration: 5m}
      - setWeight: 75
      - pause: {duration: 5m}
      canaryService: a2a-travel-agent-canary
      stableService: a2a-travel-agent-stable
  selector:
    matchLabels:
      app: a2a-travel-agent
  template:
    metadata:
      labels:
        app: a2a-travel-agent
    spec:
      containers:
      - name: a2a-travel-agent
        image: your-registry.com/a2a-travel-agent:v1.0.0
```

---

## 14. Backup and Disaster Recovery

### 14.1 Database Backup CronJob
```yaml
# postgres-backup-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: a2a-travel-agent
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: postgres-backup
            image: postgres:15-alpine
            command:
            - /bin/bash
            - -c
            - |
              pg_dump -h postgresql -U agent -d a2awebagent > /backup/backup-$(date +%Y%m%d-%H%M%S).sql
              # Upload to S3 or persistent storage
              aws s3 cp /backup/backup-$(date +%Y%m%d-%H%M%S).sql s3://your-backup-bucket/postgres/
              # Clean up old backups (keep last 7 days)
              find /backup -name "backup-*.sql" -mtime +7 -delete
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgresql-secret
                  key: postgres-password
            volumeMounts:
            - name: backup-storage
              mountPath: /backup
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
          restartPolicy: OnFailure
```

### 14.2 Disaster Recovery Script
```bash
# disaster-recovery.sh
#!/bin/bash

NAMESPACE="a2a-travel-agent"
BACKUP_BUCKET="your-backup-bucket"

echo "Starting disaster recovery process..."

# Restore PostgreSQL
echo "Restoring PostgreSQL database..."
kubectl exec -n $NAMESPACE deployment/postgresql -- \
  psql -U agent -d a2awebagent -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

aws s3 cp s3://$BACKUP_BUCKET/postgres/latest-backup.sql /tmp/
kubectl cp /tmp/latest-backup.sql $NAMESPACE/postgresql-pod:/tmp/
kubectl exec -n $NAMESPACE deployment/postgresql -- \
  psql -U agent -d a2awebagent -f /tmp/latest-backup.sql

# Restore Redis (if needed)
echo "Restoring Redis data..."
kubectl exec -n $NAMESPACE deployment/redis -- \
  redis-cli FLUSHALL

# Restart application pods
echo "Restarting application pods..."
kubectl rollout restart deployment/a2a-travel-agent -n $NAMESPACE

echo "Disaster recovery completed successfully!"
```

---

## 15. Troubleshooting and Maintenance

### 15.1 Common Issues and Solutions

#### Pod Startup Issues
```bash
# Check pod status
kubectl get pods -n a2a-travel-agent

# Describe pod for events
kubectl describe pod <pod-name> -n a2a-travel-agent

# Check logs
kubectl logs -f <pod-name> -n a2a-travel-agent

# Check resource constraints
kubectl top pods -n a2a-travel-agent
```

#### Database Connection Issues
```bash
# Test PostgreSQL connection
kubectl exec -it deployment/postgresql -n a2a-travel-agent -- \
  psql -U agent -d a2awebagent -c "SELECT version();"

# Check Redis connection
kubectl exec -it deployment/redis -n a2a-travel-agent -- \
  redis-cli ping

# Test Neo4j connection
kubectl exec -it deployment/neo4j -n a2a-travel-agent -- \
  cypher-shell -u neo4j -p password123 "RETURN 1;"
```

#### Performance Issues
```bash
# Check HPA status
kubectl get hpa -n a2a-travel-agent

# Monitor resource usage
kubectl top pods -n a2a-travel-agent --sort-by=cpu
kubectl top pods -n a2a-travel-agent --sort-by=memory

# Check Playwright browser processes
kubectl exec -it <app-pod> -n a2a-travel-agent -- \
  ps aux | grep chromium
```

### 15.2 Maintenance Tasks

#### Rolling Updates
```bash
# Update application image
kubectl set image deployment/a2a-travel-agent \
  a2a-travel-agent=your-registry.com/a2a-travel-agent:v1.1.0 \
  -n a2a-travel-agent

# Monitor rollout status
kubectl rollout status deployment/a2a-travel-agent -n a2a-travel-agent

# Rollback if needed
kubectl rollout undo deployment/a2a-travel-agent -n a2a-travel-agent
```

#### Resource Cleanup
```bash
# Clean up old screenshots and logs
kubectl exec -it <app-pod> -n a2a-travel-agent -- \
  find /app/screenshots -mtime +3 -delete

# Clean up completed jobs
kubectl delete jobs --field-selector status.successful=1 -n a2a-travel-agent

# Clean up unused PVCs
kubectl get pvc -n a2a-travel-agent | grep "Available\|Released"
```

### 15.3 Health Monitoring Script
```bash
#!/bin/bash
# health-check.sh

NAMESPACE="a2a-travel-agent"

echo "=== A2A Travel Agent Health Check ==="

# Check all pods
echo "Pod Status:"
kubectl get pods -n $NAMESPACE -o wide

# Check services
echo -e "\nService Status:"
kubectl get svc -n $NAMESPACE

# Check ingress
echo -e "\nIngress Status:"
kubectl get ingress -n $NAMESPACE

# Check HPA
echo -e "\nAutoscaler Status:"
kubectl get hpa -n $NAMESPACE

# Health endpoint check
echo -e "\nApplication Health:"
APP_POD=$(kubectl get pods -n $NAMESPACE -l app=a2a-travel-agent -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n $NAMESPACE $APP_POD -- \
  curl -s http://localhost:7860/actuator/health | jq .

# Database connectivity
echo -e "\nDatabase Connectivity:"
kubectl exec -n $NAMESPACE deployment/postgresql -- \
  pg_isready -U agent -d a2awebagent

echo -e "\nRedis Connectivity:"
kubectl exec -n $NAMESPACE deployment/redis -- \
  redis-cli ping

echo -e "\nNeo4j Connectivity:"
kubectl exec -n $NAMESPACE deployment/neo4j -- \
  cypher-shell -u neo4j -p password123 "RETURN 1;" 2>/dev/null && echo "OK" || echo "FAILED"

echo "=== Health Check Complete ==="
```

---

## Conclusion

This comprehensive Kubernetes deployment guide provides a production-ready setup for the a2aTravelAgent application with:

- **Multi-module architecture** supporting a2acore MCP framework and a2awebapp Spring Boot application
- **High availability** with PostgreSQL, Redis, and Neo4j persistence layers
- **Auto-scaling** based on CPU and memory utilization
- **Security** with pod security policies and network policies
- **Monitoring** with Prometheus and Grafana integration
- **Backup and disaster recovery** procedures
- **Production deployment strategies** including blue-green and canary deployments

The deployment supports the full MCP protocol stack with Playwright browser automation in containerized environments, making it suitable for enterprise-scale travel automation workloads.

For additional customization and advanced configurations, refer to the individual component documentation and adjust the resource limits, scaling parameters, and security policies according to your specific requirements.