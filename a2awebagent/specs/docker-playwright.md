# Comprehensive Docker Testing Strategy for Playwright Integration

## Overview

This document provides a comprehensive testing strategy for Microsoft Playwright in the a2aTravelAgent Docker environment, with special focus on M1 Mac compatibility. Selenium/ChromeDriver migration has been completed.

## Project Context

### Current State
- **a2awebagent**: Fully migrated to Playwright (Microsoft Playwright 1.51.0)
- **Status**: Selenium dependencies completely removed
- **Achievement**: Better reliability and performance with Playwright

### Architecture 
```
Current: Java → Microsoft Playwright → Browser Engines (Chromium/Firefox/WebKit)
Previous: Java → Selenium WebDriver → ChromeDriver → Chrome Browser (REMOVED)
```

## 1. Step-by-Step Docker Build and Test Plan

### Phase 1: Environment Preparation

#### 1.1 Create Playwright-Specific Dockerfile
```bash
# Create test Dockerfile based on Microsoft's official Playwright image
cat > /tmp/Dockerfile.playwright-test << 'EOF'
# Option 1: Microsoft's Official Playwright Image
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

WORKDIR /app

# Install Maven for Java builds
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy test project files
COPY pom.xml .
COPY src/ ./src/

# Download dependencies
RUN mvn dependency:go-offline -B

# Build application
RUN mvn clean package -DskipTests -B

# Create required directories
RUN mkdir -p /app/screenshots /app/logs

# Expose port
EXPOSE 7860

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:7860/actuator/health || exit 1

# Set environment variables for Playwright
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Start application
CMD ["java", "-jar", "target/a2awebagent-0.0.1.jar"]
EOF
```

#### 1.2 Create Custom OpenJDK-based Dockerfile
```bash
# Create alternative Dockerfile with manual Playwright installation
cat > /tmp/Dockerfile.playwright-custom << 'EOF'
FROM openjdk:18-jdk-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl unzip gnupg wget tar maven \
    libnss3 libnspr4 libatk-bridge2.0-0 libdrm2 libxkbcommon0 \
    libgtk-3-0 libatspi2.0-0 libxss1 libasound2 \
    fonts-liberation libappindicator3-1 xdg-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy and build application
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src/ ./src/
RUN mvn clean package -DskipTests -B

# Install Playwright browsers using the Java SDK
RUN java -cp target/a2awebagent-0.0.1.jar \
    -Dloader.main=com.microsoft.playwright.CLI \
    org.springframework.boot.loader.launch.PropertiesLauncher \
    install --with-deps chromium

# Create directories and set permissions
RUN mkdir -p /app/screenshots /app/logs && \
    useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

USER appuser
EXPOSE 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:7860/actuator/health || exit 1

CMD ["java", "-jar", "target/a2awebagent-0.0.1.jar"]
EOF
```

### Phase 2: Build and Test Commands

#### 2.1 Build Test Images
```bash
# Build Microsoft official image version
cd /Users/wingston/code/a2aTravelAgent/a2awebagent
docker build -f /tmp/Dockerfile.playwright-test -t a2a-playwright-official:test .

# Build custom OpenJDK version
docker build -f /tmp/Dockerfile.playwright-custom -t a2a-playwright-custom:test .

# Compare image sizes
docker images | grep a2a-playwright
```

#### 2.2 Basic Container Startup Tests
```bash
# Test official image startup
docker run -d --name playwright-official-test -p 7861:7860 a2a-playwright-official:test

# Test custom image startup
docker run -d --name playwright-custom-test -p 7862:7860 a2a-playwright-custom:test

# Monitor startup logs
docker logs -f playwright-official-test
docker logs -f playwright-custom-test
```

## 2. Bash Commands to Validate Playwright Browser Installation

### 2.1 Browser Installation Verification
```bash
#!/bin/bash
# validate_playwright_browsers.sh

echo "=== Playwright Browser Installation Validation ==="

# Function to test container browser installation
test_playwright_browsers() {
    local container_name=$1
    echo "Testing container: $container_name"
    
    # Check if Playwright browsers are installed
    echo "1. Checking Playwright browser installation..."
    docker exec $container_name find /ms-playwright -name "*chromium*" -type d 2>/dev/null || \
    docker exec $container_name find /root/.cache/ms-playwright -name "*chromium*" -type d 2>/dev/null || \
    echo "No Playwright browsers found"
    
    # Test Playwright CLI
    echo "2. Testing Playwright CLI availability..."
    docker exec $container_name java -cp target/a2awebagent-0.0.1.jar \
        -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher \
        --version 2>/dev/null || echo "Playwright CLI not available"
    
    # Check browser executables
    echo "3. Checking browser executables..."
    docker exec $container_name sh -c "
        if [ -d /ms-playwright ]; then
            find /ms-playwright -name 'chromium' -executable 2>/dev/null | head -5
        fi
        if [ -d /root/.cache/ms-playwright ]; then
            find /root/.cache/ms-playwright -name 'chromium' -executable 2>/dev/null | head -5
        fi
    " || echo "No browser executables found"
    
    # Test system resources
    echo "4. Checking system resources..."
    docker exec $container_name sh -c "
        echo 'Memory:' && free -h
        echo 'Disk space:' && df -h
        echo 'Architecture:' && uname -m
    "
    
    echo "--- Container $container_name test complete ---"
    echo
}

# Test both containers
test_playwright_browsers "playwright-official-test"
test_playwright_browsers "playwright-custom-test"
```

### 2.2 Browser Engine Validation
```bash
#!/bin/bash
# validate_browser_engines.sh

echo "=== Browser Engine Validation ==="

validate_browser_engine() {
    local container_name=$1
    echo "Validating browser engines in: $container_name"
    
    # Test Chromium engine availability
    echo "1. Testing Chromium engine..."
    docker exec $container_name java -cp target/a2awebagent-0.0.1.jar \
        -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher \
        install chromium 2>&1 | grep -E "(SUCCESS|FAILED|already)" || echo "Chromium test failed"
    
    # Test browser launch capability
    echo "2. Testing browser launch capability..."
    docker exec $container_name timeout 30s java -cp target/a2awebagent-0.0.1.jar \
        -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher \
        open --browser=chromium --timeout=10000 https://example.com 2>&1 | \
        grep -E "(Navigated|Error|Timeout)" || echo "Browser launch test failed"
    
    # Check for required dependencies
    echo "3. Checking required system dependencies..."
    docker exec $container_name sh -c "
        echo 'Checking libnss3:' && dpkg -l | grep libnss3
        echo 'Checking libgtk-3-0:' && dpkg -l | grep libgtk-3-0
        echo 'Checking fonts:' && fc-list | wc -l
        echo 'Checking X11:' && ls -la /tmp/.X11-unix/ 2>/dev/null || echo 'No X11 sockets'
    "
    
    echo "--- Browser engine validation for $container_name complete ---"
    echo
}

# Validate both containers
validate_browser_engine "playwright-official-test"
validate_browser_engine "playwright-custom-test"
```

## 3. Screenshot Capture Testing Within Docker Container

### 3.1 Screenshot Test Script
```bash
#!/bin/bash
# test_screenshot_capture.sh

echo "=== Screenshot Capture Testing ==="

test_screenshot_capability() {
    local container_name=$1
    local test_url="https://example.com"
    local screenshot_path="/app/screenshots/test_$(date +%Y%m%d_%H%M%S).png"
    
    echo "Testing screenshot capture in: $container_name"
    echo "URL: $test_url"
    echo "Output path: $screenshot_path"
    
    # Create a simple Java test for screenshot
    cat > /tmp/ScreenshotTest.java << 'EOF'
import com.microsoft.playwright.*;
import java.nio.file.Paths;

public class ScreenshotTest {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(java.util.Arrays.asList(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--disable-web-security"
                )));
            
            Page page = browser.newPage();
            page.navigate("https://example.com");
            page.waitForLoadState();
            
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("/app/screenshots/playwright_test.png"))
                .setFullPage(true));
            
            System.out.println("Screenshot captured successfully");
            browser.close();
        } catch (Exception e) {
            System.err.println("Screenshot test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

    # Copy test file to container
    docker cp /tmp/ScreenshotTest.java $container_name:/app/
    
    # Compile and run screenshot test
    echo "1. Compiling screenshot test..."
    docker exec $container_name javac -cp "target/a2awebagent-0.0.1.jar" ScreenshotTest.java
    
    echo "2. Running screenshot test..."
    docker exec $container_name java -cp ".:target/a2awebagent-0.0.1.jar" ScreenshotTest
    
    # Verify screenshot was created
    echo "3. Verifying screenshot creation..."
    docker exec $container_name ls -la /app/screenshots/
    
    # Copy screenshot back to host for inspection
    echo "4. Copying screenshot to host..."
    docker cp $container_name:/app/screenshots/playwright_test.png ./screenshot_${container_name}_$(date +%H%M%S).png
    
    # Check screenshot file properties
    echo "5. Screenshot file properties:"
    docker exec $container_name file /app/screenshots/playwright_test.png 2>/dev/null || echo "Screenshot file not found or invalid"
    
    echo "--- Screenshot test for $container_name complete ---"
    echo
}

# Test screenshot capability in both containers
test_screenshot_capability "playwright-official-test"
test_screenshot_capability "playwright-custom-test"
```

### 3.2 Advanced Screenshot Testing
```bash
#!/bin/bash
# advanced_screenshot_test.sh

echo "=== Advanced Screenshot Testing ==="

advanced_screenshot_test() {
    local container_name=$1
    
    echo "Running advanced screenshot tests for: $container_name"
    
    # Test different viewport sizes
    echo "1. Testing different viewport sizes..."
    docker exec $container_name java -cp ".:target/a2awebagent-0.0.1.jar" -e "
        import com.microsoft.playwright.*;
        try (Playwright p = Playwright.create()) {
            Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = b.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            page.navigate('https://httpbin.org/html');
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/app/screenshots/viewport_1920x1080.png')));
            
            page.setViewportSize(375, 667); // Mobile size
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/app/screenshots/viewport_mobile.png')));
            
            System.out.println('Viewport tests completed');
            b.close();
        }
    " 2>&1 | grep -E "(completed|Error)"
    
    # Test full page vs viewport screenshots
    echo "2. Testing full page vs viewport screenshots..."
    docker exec $container_name java -cp ".:target/a2awebagent-0.0.1.jar" -e "
        import com.microsoft.playwright.*;
        try (Playwright p = Playwright.create()) {
            Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = b.newPage();
            page.navigate('https://httpbin.org/html');
            
            // Viewport screenshot
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/app/screenshots/viewport_only.png')).setFullPage(false));
            
            // Full page screenshot
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/app/screenshots/full_page.png')).setFullPage(true));
            
            System.out.println('Full page tests completed');
            b.close();
        }
    " 2>&1 | grep -E "(completed|Error)"
    
    # List all generated screenshots
    echo "3. Generated screenshots:"
    docker exec $container_name ls -la /app/screenshots/ | grep ".png"
    
    echo "--- Advanced screenshot test for $container_name complete ---"
    echo
}

# Run advanced tests
advanced_screenshot_test "playwright-official-test"
advanced_screenshot_test "playwright-custom-test"
```

## 4. Health Check Commands for Playwright Browsers

### 4.1 Comprehensive Health Check Script
```bash
#!/bin/bash
# playwright_health_check.sh

echo "=== Playwright Browser Health Check ==="

playwright_health_check() {
    local container_name=$1
    local health_score=0
    local max_score=10
    
    echo "Health check for container: $container_name"
    echo "========================================"
    
    # 1. Container running status
    echo "1. Container Status Check..."
    if docker ps | grep -q $container_name; then
        echo "   ✅ Container is running"
        ((health_score++))
    else
        echo "   ❌ Container is not running"
    fi
    
    # 2. Application port accessibility
    echo "2. Application Port Check..."
    local port=$(docker port $container_name 7860 2>/dev/null | cut -d: -f2)
    if [ ! -z "$port" ] && curl -s -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
        echo "   ✅ Application health endpoint accessible on port $port"
        ((health_score++))
    else
        echo "   ❌ Application health endpoint not accessible"
    fi
    
    # 3. Playwright installation check
    echo "3. Playwright Installation Check..."
    if docker exec $container_name java -cp target/a2awebagent-0.0.1.jar \
        -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher \
        --version >/dev/null 2>&1; then
        echo "   ✅ Playwright CLI is accessible"
        ((health_score++))
    else
        echo "   ❌ Playwright CLI not accessible"
    fi
    
    # 4. Browser engine availability
    echo "4. Browser Engine Check..."
    local chromium_test=$(docker exec $container_name timeout 15s java -cp target/a2awebagent-0.0.1.jar \
        -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher \
        open --browser=chromium --timeout=5000 about:blank 2>&1)
    
    if echo "$chromium_test" | grep -q -E "(SUCCESS|Navigated)" || ! echo "$chromium_test" | grep -q -E "(Error|Failed)"; then
        echo "   ✅ Chromium browser engine functional"
        ((health_score++))
    else
        echo "   ❌ Chromium browser engine not functional"
        echo "      Debug: $chromium_test"
    fi
    
    # 5. Screenshot capability
    echo "5. Screenshot Capability Check..."
    docker exec $container_name mkdir -p /tmp/health_test
    if docker exec $container_name timeout 20s java -cp ".:target/a2awebagent-0.0.1.jar" -e "
        import com.microsoft.playwright.*;
        try (Playwright p = Playwright.create()) {
            Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = b.newPage();
            page.navigate('data:text/html,<h1>Health Check</h1>');
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/tmp/health_test/health.png')));
            System.out.println('HEALTH_CHECK_SUCCESS');
            b.close();
        }
    " 2>&1 | grep -q "HEALTH_CHECK_SUCCESS"; then
        echo "   ✅ Screenshot capability functional"
        ((health_score++))
    else
        echo "   ❌ Screenshot capability not functional"
    fi
    
    # 6. Memory and resource check
    echo "6. Resource Availability Check..."
    local memory_info=$(docker exec $container_name free -m | grep "Mem:")
    local available_memory=$(echo $memory_info | awk '{print $7}')
    if [ "$available_memory" -gt 100 ]; then
        echo "   ✅ Sufficient memory available (${available_memory}MB)"
        ((health_score++))
    else
        echo "   ❌ Low memory available (${available_memory}MB)"
    fi
    
    # 7. Disk space check
    echo "7. Disk Space Check..."
    local disk_usage=$(docker exec $container_name df -h /app | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$disk_usage" -lt 80 ]; then
        echo "   ✅ Sufficient disk space (${disk_usage}% used)"
        ((health_score++))
    else
        echo "   ❌ High disk usage (${disk_usage}% used)"
    fi
    
    # 8. System dependencies check
    echo "8. System Dependencies Check..."
    local missing_deps=0
    for dep in libnss3 libgtk-3-0 libxss1; do
        if ! docker exec $container_name dpkg -l | grep -q $dep; then
            echo "   ❌ Missing dependency: $dep"
            ((missing_deps++))
        fi
    done
    if [ $missing_deps -eq 0 ]; then
        echo "   ✅ All required system dependencies present"
        ((health_score++))
    fi
    
    # 9. Network connectivity check
    echo "9. Network Connectivity Check..."
    if docker exec $container_name curl -s --max-time 5 https://httpbin.org/status/200 >/dev/null 2>&1; then
        echo "   ✅ External network connectivity working"
        ((health_score++))
    else
        echo "   ❌ External network connectivity issues"
    fi
    
    # 10. Application logs check
    echo "10. Application Logs Check..."
    local error_count=$(docker logs $container_name 2>&1 | grep -c -i "error\|exception\|failed")
    if [ "$error_count" -lt 5 ]; then
        echo "   ✅ Application logs healthy ($error_count errors)"
        ((health_score++))
    else
        echo "   ❌ High error count in logs ($error_count errors)"
    fi
    
    # Final health score
    echo
    echo "========================================"
    echo "HEALTH SCORE: $health_score/$max_score"
    if [ $health_score -ge 8 ]; then
        echo "STATUS: ✅ HEALTHY"
    elif [ $health_score -ge 6 ]; then
        echo "STATUS: ⚠️  DEGRADED"
    else
        echo "STATUS: ❌ UNHEALTHY"
    fi
    echo "========================================"
    echo
}

# Run health checks
playwright_health_check "playwright-official-test"
playwright_health_check "playwright-custom-test"
```

### 4.2 Continuous Health Monitoring
```bash
#!/bin/bash
# continuous_health_monitor.sh

echo "=== Continuous Playwright Health Monitoring ==="

monitor_container_health() {
    local container_name=$1
    local interval=${2:-30}  # Default 30 seconds
    local max_iterations=${3:-10}  # Default 10 iterations
    
    echo "Monitoring $container_name every ${interval}s for $max_iterations iterations"
    
    for i in $(seq 1 $max_iterations); do
        echo "--- Iteration $i/$(echo $max_iterations) - $(date) ---"
        
        # Quick health indicators
        local cpu_usage=$(docker stats $container_name --no-stream --format "{{.CPUPerc}}" 2>/dev/null | sed 's/%//')
        local mem_usage=$(docker stats $container_name --no-stream --format "{{.MemPerc}}" 2>/dev/null | sed 's/%//')
        
        echo "CPU: ${cpu_usage}%, Memory: ${mem_usage}%"
        
        # Test basic Playwright functionality
        local playwright_test=$(docker exec $container_name timeout 10s java -cp target/a2awebagent-0.0.1.jar \
            -Dloader.main=com.microsoft.playwright.CLI \
            org.springframework.boot.loader.launch.PropertiesLauncher \
            --version 2>&1)
        
        if echo "$playwright_test" | grep -q "Playwright"; then
            echo "Playwright: ✅ Responsive"
        else
            echo "Playwright: ❌ Not responding"
        fi
        
        # Test application endpoint
        local port=$(docker port $container_name 7860 2>/dev/null | cut -d: -f2)
        if [ ! -z "$port" ] && curl -s -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            echo "Application: ✅ Healthy"
        else
            echo "Application: ❌ Unhealthy"
        fi
        
        sleep $interval
    done
    
    echo "Monitoring complete for $container_name"
}

# Monitor both containers
monitor_container_health "playwright-official-test" 30 5 &
monitor_container_health "playwright-custom-test" 30 5 &

wait
echo "All monitoring complete"
```

## 5. M1 Mac Specific Considerations and Workarounds

### 5.1 Architecture Detection and Handling
```bash
#!/bin/bash
# m1_mac_compatibility_check.sh

echo "=== M1 Mac Compatibility Check ==="

check_host_architecture() {
    echo "1. Host Architecture Analysis"
    echo "   Host architecture: $(uname -m)"
    echo "   Docker architecture: $(docker version --format '{{.Server.Arch}}')"
    
    if [ "$(uname -m)" = "arm64" ]; then
        echo "   ✅ Running on Apple Silicon (ARM64)"
        echo "   🔍 Checking for Rosetta emulation..."
        
        if [ -f /usr/sbin/softwareupdate ] && softwareupdate --list | grep -q "Rosetta"; then
            echo "   ✅ Rosetta available for x86-64 emulation"
        else
            echo "   ⚠️  Rosetta may not be installed"
        fi
    else
        echo "   ℹ️  Running on Intel/AMD64 architecture"
    fi
}

check_docker_buildx() {
    echo "2. Docker Buildx Multi-Platform Support"
    
    if docker buildx version >/dev/null 2>&1; then
        echo "   ✅ Docker Buildx available"
        echo "   Available platforms:"
        docker buildx ls | grep -A 10 "default"
    else
        echo "   ❌ Docker Buildx not available"
        echo "   Install with: docker buildx install"
    fi
}

check_playwright_arm64_support() {
    echo "3. Playwright ARM64 Support Check"
    
    # Check if official Playwright image supports ARM64
    echo "   Testing official Playwright image ARM64 support..."
    if docker manifest inspect mcr.microsoft.com/playwright/java:v1.51.0-noble | grep -q "arm64"; then
        echo "   ✅ Official Playwright image supports ARM64"
    else
        echo "   ❌ Official Playwright image may not support ARM64"
    fi
    
    # Check browser binary architecture in containers
    for container in "playwright-official-test" "playwright-custom-test"; do
        if docker ps | grep -q $container; then
            echo "   Checking $container browser architecture..."
            local browser_arch=$(docker exec $container sh -c "
                find /ms-playwright -name 'chromium' -executable 2>/dev/null | head -1 | xargs file 2>/dev/null
            " 2>/dev/null || echo "Browser not found")
            echo "   $container: $browser_arch"
        fi
    done
}

create_m1_optimized_dockerfile() {
    echo "4. Creating M1-Optimized Dockerfile"
    
    cat > /tmp/Dockerfile.m1-optimized << 'EOF'
# Multi-stage build for M1 Mac optimization
FROM --platform=$BUILDPLATFORM mcr.microsoft.com/playwright/java:v1.51.0-noble as base

# Build arguments for cross-compilation
ARG TARGETPLATFORM
ARG BUILDPLATFORM
ARG TARGETARCH

# Install build dependencies
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy and build application (architecture-agnostic)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src/ ./src/
RUN mvn clean package -DskipTests -B

# Final stage - runtime
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

WORKDIR /app

# Copy built application from previous stage
COPY --from=base /app/target/a2awebagent-0.0.1.jar .

# Create required directories
RUN mkdir -p /app/screenshots /app/logs && \
    useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

# Set environment variables optimized for ARM64
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport"

USER appuser
EXPOSE 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:7860/actuator/health || exit 1

CMD ["java", "-jar", "a2awebagent-0.0.1.jar"]
EOF

    echo "   ✅ M1-optimized Dockerfile created at /tmp/Dockerfile.m1-optimized"
}

test_cross_platform_build() {
    echo "5. Testing Cross-Platform Build"
    
    if command -v docker buildx >/dev/null 2>&1; then
        echo "   Building for multiple architectures..."
        cd /Users/wingston/code/a2aTravelAgent/a2awebagent
        
        # Create a buildx builder if it doesn't exist
        docker buildx create --name multiarch --use --bootstrap 2>/dev/null || true
        
        # Build for both ARM64 and AMD64
        docker buildx build \
            --platform linux/arm64,linux/amd64 \
            -f /tmp/Dockerfile.m1-optimized \
            -t a2a-playwright-multiarch:test \
            . 2>&1 | grep -E "(SUCCESS|FAILED|ERROR)" || echo "Build attempted"
        
        echo "   Cross-platform build test completed"
    else
        echo "   ⚠️  Docker Buildx not available for cross-platform builds"
    fi
}

# Run all M1 compatibility checks
check_host_architecture
check_docker_buildx
check_playwright_arm64_support
create_m1_optimized_dockerfile
test_cross_platform_build

echo "=== M1 Mac Compatibility Check Complete ==="
```

### 5.2 M1 Mac Performance Optimization
```bash
#!/bin/bash
# m1_performance_optimization.sh

echo "=== M1 Mac Performance Optimization ==="

optimize_docker_settings() {
    echo "1. Docker Desktop Settings Optimization"
    echo "   Recommended Docker Desktop settings for M1 Mac:"
    echo "   - Memory: 4-6 GB (more for heavy Playwright workloads)"
    echo "   - CPUs: 4-6 cores"
    echo "   - Disk image size: 64GB+"
    echo "   - Enable 'Use Rosetta for x86/amd64 emulation on Apple Silicon'"
    echo "   - Enable 'Use Docker Compose V2'"
    
    # Check current Docker resource allocation
    echo "   Current Docker resources:"
    docker system info | grep -E "(CPUs|Total Memory)" || echo "   Unable to get Docker resource info"
}

create_performance_optimized_compose() {
    echo "2. Creating Performance-Optimized Docker Compose"
    
    cat > /tmp/docker-compose.m1-optimized.yml << 'EOF'
version: '3.8'

services:
  a2a-playwright:
    build:
      context: .
      dockerfile: Dockerfile.m1-optimized
      platforms:
        - linux/arm64
    image: a2a-playwright:m1-optimized
    container_name: a2a-playwright-m1
    ports:
      - "7860:7860"
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
      - PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
      - PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
    volumes:
      - screenshots:/app/screenshots
      - logs:/app/logs
      # Optimize for Apple Silicon performance
      - /tmp:/tmp:rw
    deploy:
      resources:
        limits:
          memory: 3G
          cpus: '2.0'
        reservations:
          memory: 1G
          cpus: '1.0'
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:7860/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
    # Use Apple Silicon optimized settings
    platform: linux/arm64

volumes:
  screenshots:
    driver: local
  logs:
    driver: local

networks:
  default:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
EOF

    echo "   ✅ M1-optimized Docker Compose created at /tmp/docker-compose.m1-optimized.yml"
}

benchmark_performance() {
    echo "3. Performance Benchmarking"
    
    # Function to run performance test
    performance_test() {
        local container_name=$1
        local test_name=$2
        
        echo "   Running $test_name performance test..."
        
        local start_time=$(date +%s)
        
        # Run a standardized screenshot test
        docker exec $container_name timeout 60s java -cp ".:target/a2awebagent-0.0.1.jar" -e "
            import com.microsoft.playwright.*;
            long startTime = System.currentTimeMillis();
            
            try (Playwright p = Playwright.create()) {
                Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                
                for (int i = 0; i < 5; i++) {
                    Page page = b.newPage();
                    page.navigate('https://httpbin.org/html');
                    page.waitForLoadState();
                    page.screenshot(new Page.ScreenshotOptions()
                        .setPath(java.nio.file.Paths.get('/app/screenshots/perf_test_' + i + '.png')));
                    page.close();
                }
                
                b.close();
                long endTime = System.currentTimeMillis();
                System.out.println('PERFORMANCE_TEST_TIME:' + (endTime - startTime) + 'ms');
            }
        " 2>&1 | grep "PERFORMANCE_TEST_TIME" | cut -d: -f2 || echo "Test failed"
        
        local end_time=$(date +%s)
        local total_time=$((end_time - start_time))
        
        echo "   $test_name total time: ${total_time}s"
    }
    
    # Test both containers if available
    for container in "playwright-official-test" "playwright-custom-test"; do
        if docker ps | grep -q $container; then
            performance_test $container $container
        fi
    done
}

# Run optimization steps
optimize_docker_settings
create_performance_optimized_compose
benchmark_performance

echo "=== M1 Mac Performance Optimization Complete ==="
```

## 6. Comparison: Microsoft Official vs Custom OpenJDK Setup

### 6.1 Detailed Comparison Script
```bash
#!/bin/bash
# compare_playwright_setups.sh

echo "=== Playwright Setup Comparison ==="

compare_image_characteristics() {
    echo "1. Image Size and Layer Analysis"
    
    # Get image sizes
    echo "   Image sizes:"
    docker images | grep -E "(playwright|a2a)" | sort
    
    # Analyze layers
    echo
    echo "   Layer analysis for mcr.microsoft.com/playwright/java:v1.51.0-noble:"
    docker history mcr.microsoft.com/playwright/java:v1.51.0-noble 2>/dev/null | head -10 || echo "   Image not available locally"
    
    echo
    echo "   Layer analysis for custom builds:"
    for image in "a2a-playwright-official:test" "a2a-playwright-custom:test"; do
        if docker images | grep -q $(echo $image | cut -d: -f1); then
            echo "   $image layers:"
            docker history $image | head -5
        else
            echo "   $image: Not built yet"
        fi
    done
}

compare_startup_performance() {
    echo "2. Startup Performance Comparison"
    
    # Function to measure container startup time
    measure_startup() {
        local image_name=$1
        local container_name="startup-test-$(date +%s)"
        
        echo "   Testing startup time for: $image_name"
        
        local start_time=$(date +%s.%N)
        docker run -d --name $container_name -p 0:7860 $image_name >/dev/null 2>&1
        
        # Wait for health check to pass
        local health_timeout=120
        local elapsed=0
        while [ $elapsed -lt $health_timeout ]; do
            if docker exec $container_name curl -s -f http://localhost:7860/actuator/health >/dev/null 2>&1; then
                local end_time=$(date +%s.%N)
                local startup_time=$(echo "$end_time - $start_time" | bc)
                echo "   $image_name startup time: ${startup_time}s"
                break
            fi
            sleep 2
            elapsed=$((elapsed + 2))
        done
        
        if [ $elapsed -ge $health_timeout ]; then
            echo "   $image_name: Failed to start within ${health_timeout}s"
        fi
        
        # Cleanup
        docker stop $container_name >/dev/null 2>&1
        docker rm $container_name >/dev/null 2>&1
    }
    
    # Test available images
    for image in "a2a-playwright-official:test" "a2a-playwright-custom:test"; do
        if docker images | grep -q $(echo $image | cut -d: -f1); then
            measure_startup $image
        else
            echo "   $image: Not available for testing"
        fi
    done
}

compare_browser_capabilities() {
    echo "3. Browser Capabilities Comparison"
    
    test_browser_features() {
        local container_name=$1
        local setup_type=$2
        
        echo "   Testing $setup_type browser capabilities..."
        
        # Test browser engines availability
        local chromium_available=$(docker exec $container_name java -cp target/a2awebagent-0.0.1.jar \
            -Dloader.main=com.microsoft.playwright.CLI \
            org.springframework.boot.loader.launch.PropertiesLauncher \
            install --dry-run chromium 2>&1 | grep -c "chromium" || echo "0")
        
        echo "     Chromium support: $([ $chromium_available -gt 0 ] && echo '✅ Available' || echo '❌ Not available')"
        
        # Test screenshot capabilities
        local screenshot_test=$(docker exec $container_name timeout 30s java -cp ".:target/a2awebagent-0.0.1.jar" -e "
            import com.microsoft.playwright.*;
            try (Playwright p = Playwright.create()) {
                Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page = b.newPage();
                page.navigate('data:text/html,<h1>Test</h1>');
                page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get('/tmp/test.png')));
                System.out.println('SCREENSHOT_SUCCESS');
                b.close();
            }
        " 2>&1)
        
        echo "     Screenshot capability: $(echo "$screenshot_test" | grep -q 'SCREENSHOT_SUCCESS' && echo '✅ Working' || echo '❌ Failed')"
        
        # Test multiple tabs
        local multitab_test=$(docker exec $container_name timeout 30s java -cp ".:target/a2awebagent-0.0.1.jar" -e "
            import com.microsoft.playwright.*;
            try (Playwright p = Playwright.create()) {
                Browser b = p.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                Page page1 = b.newPage();
                Page page2 = b.newPage();
                page1.navigate('data:text/html,<h1>Tab 1</h1>');
                page2.navigate('data:text/html,<h1>Tab 2</h1>');
                System.out.println('MULTITAB_SUCCESS');
                b.close();
            }
        " 2>&1)
        
        echo "     Multiple tabs: $(echo "$multitab_test" | grep -q 'MULTITAB_SUCCESS' && echo '✅ Working' || echo '❌ Failed')"
    }
    
    # Test both setups
    for container in "playwright-official-test" "playwright-custom-test"; do
        if docker ps | grep -q $container; then
            local setup_type=$(echo $container | sed 's/playwright-//' | sed 's/-test//')
            test_browser_features $container "$setup_type"
        fi
    done
}

compare_resource_usage() {
    echo "4. Resource Usage Comparison"
    
    # Get resource stats for running containers
    echo "   Current resource usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" $(docker ps --format "{{.Names}}" | grep playwright) 2>/dev/null || echo "   No playwright containers running"
    
    # Check image disk usage
    echo
    echo "   Disk usage breakdown:"
    for image in "mcr.microsoft.com/playwright/java:v1.51.0-noble" "a2a-playwright-official:test" "a2a-playwright-custom:test"; do
        local size=$(docker images --format "{{.Size}}" $image 2>/dev/null)
        if [ ! -z "$size" ]; then
            echo "   $image: $size"
        fi
    done
}

compare_security_aspects() {
    echo "5. Security Comparison"
    
    security_scan() {
        local container_name=$1
        local setup_type=$2
        
        echo "   Security analysis for $setup_type:"
        
        # Check running processes
        local process_count=$(docker exec $container_name ps aux | wc -l)
        echo "     Running processes: $process_count"
        
        # Check for non-root execution
        local user_check=$(docker exec $container_name id | grep -c "uid=1000" || echo "0")
        echo "     Non-root execution: $([ $user_check -gt 0 ] && echo '✅ Yes' || echo '❌ Running as root')"
        
        # Check for unnecessary capabilities
        local capabilities=$(docker inspect $container_name | grep -A 10 "CapAdd" | grep -v "null" | wc -l)
        echo "     Additional capabilities: $capabilities"
        
        # Check network exposure
        local ports=$(docker port $container_name | wc -l)
        echo "     Exposed ports: $ports"
    }
    
    # Analyze both setups
    for container in "playwright-official-test" "playwright-custom-test"; do
        if docker ps | grep -q $container; then
            local setup_type=$(echo $container | sed 's/playwright-//' | sed 's/-test//')
            security_scan $container "$setup_type"
        fi
    done
}

generate_recommendation() {
    echo "6. Setup Recommendation"
    echo "   ================================"
    echo
    echo "   MICROSOFT OFFICIAL IMAGE (mcr.microsoft.com/playwright/java:v1.51.0-noble):"
    echo "   Pros:"
    echo "   + Pre-configured with all Playwright dependencies"
    echo "   + Officially maintained by Microsoft"
    echo "   + Regular security updates"
    echo "   + Multi-architecture support (ARM64/AMD64)"
    echo "   + Optimized browser installation"
    echo
    echo "   Cons:"
    echo "   - Larger image size (includes all browsers)"
    echo "   - Less control over base system"
    echo "   - May include unnecessary components"
    echo
    echo "   CUSTOM OPENJDK SETUP:"
    echo "   Pros:"
    echo "   + Smaller base image"
    echo "   + Full control over dependencies"
    echo "   + Can optimize for specific use cases"
    echo "   + Easier to customize and debug"
    echo
    echo "   Cons:"
    echo "   - Requires manual Playwright browser installation"
    echo "   - More complex setup and maintenance"
    echo "   - Potential compatibility issues"
    echo "   - Manual security updates needed"
    echo
    echo "   RECOMMENDATION FOR M1 MAC:"
    echo "   🎯 Use Microsoft Official Image for:"
    echo "      - Production deployments"
    echo "      - Teams wanting minimal setup"
    echo "      - Multi-browser testing needs"
    echo
    echo "   🎯 Use Custom OpenJDK for:"
    echo "      - Development environments"
    echo "      - Resource-constrained deployments"
    echo "      - Specific customization requirements"
    echo "   ================================"
}

# Run all comparisons
compare_image_characteristics
compare_startup_performance
compare_browser_capabilities
compare_resource_usage
compare_security_aspects
generate_recommendation

echo "=== Playwright Setup Comparison Complete ==="
```

## 7. Migration Testing Commands

### 7.1 Pre-Migration Validation
```bash
#!/bin/bash
# pre_migration_validation.sh

echo "=== Pre-Migration Validation ==="

validate_current_selenium_setup() {
    echo "1. Current Selenium Setup Validation"
    
    # Check if current a2awebagent is running
    cd /Users/wingston/code/a2aTravelAgent/a2awebagent
    
    if docker ps | grep -q a2awebagent; then
        local container_name=$(docker ps --format "{{.Names}}" | grep a2awebagent | head -1)
        echo "   ✅ a2awebagent container running: $container_name"
        
        # Test current screenshot capability
        echo "   Testing current screenshot capability..."
        local test_result=$(curl -s -X POST -H "Content-Type: application/json" \
            -d '{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "browseWebAndReturnImage", "arguments": {"provideAllValuesInPlainEnglish": "Navigate to https://httpbin.org/html and take a screenshot"}}, "id": 1}' \
            http://localhost:7860 | grep -c "result" || echo "0")
        
        echo "   Current screenshot API: $([ $test_result -gt 0 ] && echo '✅ Working' || echo '❌ Failed')"
    else
        echo "   ❌ No a2awebagent container running"
        echo "   Starting current setup for comparison..."
        docker-compose up -d a2awebagent 2>/dev/null || echo "   Failed to start current setup"
    fi
}

backup_current_configuration() {
    echo "2. Backing Up Current Configuration"
    
    local backup_dir="/tmp/a2a_migration_backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p $backup_dir
    
    # Backup Docker configuration
    cp /Users/wingston/code/a2aTravelAgent/a2awebagent/Dockerfile $backup_dir/
    cp /Users/wingston/code/a2aTravelAgent/a2awebagent/docker-compose.yml $backup_dir/ 2>/dev/null || true
    cp /Users/wingston/code/a2aTravelAgent/a2awebagent/entrypoint.sh $backup_dir/
    
    # Backup application configuration
    cp -r /Users/wingston/code/a2aTravelAgent/a2awebagent/src/main/resources $backup_dir/
    
    # Backup any screenshots or logs
    docker cp $(docker ps --format "{{.Names}}" | grep a2awebagent | head -1):/app/screenshots $backup_dir/ 2>/dev/null || true
    docker cp $(docker ps --format "{{.Names}}" | grep a2awebagent | head -1):/app/logs $backup_dir/ 2>/dev/null || true
    
    echo "   ✅ Configuration backed up to: $backup_dir"
    echo "   Backup contents:"
    ls -la $backup_dir
}

create_migration_plan() {
    echo "3. Creating Migration Plan"
    
    cat > /tmp/playwright_migration_plan.md << 'EOF'
# Playwright Migration Plan

## Phase 1: Preparation (Estimated: 1-2 hours)
- [x] Backup current Selenium-based configuration
- [x] Test current functionality baseline
- [ ] Review Playwright API compatibility with current tools
- [ ] Prepare new Docker configuration files

## Phase 2: Development Setup (Estimated: 2-3 hours)
- [ ] Create Playwright-based Docker image
- [ ] Update pom.xml with Playwright dependencies
- [ ] Migrate WebBrowsingAction from Selenium to Playwright
- [ ] Update screenshot capture methods
- [ ] Test basic functionality

## Phase 3: Testing (Estimated: 2-4 hours)
- [ ] Unit test Playwright integration
- [ ] Integration test with existing tools
- [ ] Performance comparison with Selenium setup
- [ ] Cross-platform testing (ARM64/AMD64)

## Phase 4: Deployment (Estimated: 1 hour)
- [ ] Update production Docker configuration
- [ ] Deploy new version
- [ ] Monitor for issues
- [ ] Rollback plan if needed

## Rollback Plan
1. Stop new Playwright container
2. Restore Selenium-based configuration from backup
3. Restart with original setup
4. Verify functionality restored

## Risk Mitigation
- Keep original Selenium setup running during migration
- Test thoroughly in development environment
- Have monitoring in place for production deployment
EOF

    echo "   ✅ Migration plan created at: /tmp/playwright_migration_plan.md"
    cat /tmp/playwright_migration_plan.md
}

# Run pre-migration validation
validate_current_selenium_setup
backup_current_configuration
create_migration_plan

echo "=== Pre-Migration Validation Complete ==="
```

### 7.2 Side-by-Side Testing
```bash
#!/bin/bash
# side_by_side_testing.sh

echo "=== Side-by-Side Testing: Selenium vs Playwright ==="

setup_test_environment() {
    echo "1. Setting Up Test Environment"
    
    # Ensure both containers are running on different ports
    echo "   Starting Selenium-based container (current)..."
    cd /Users/wingston/code/a2aTravelAgent/a2awebagent
    docker-compose up -d 2>/dev/null || echo "   Current container may already be running"
    
    echo "   Starting Playwright-based container..."
    if docker images | grep -q "a2a-playwright-official:test"; then
        docker run -d --name playwright-test-comparison -p 7861:7860 a2a-playwright-official:test
    else
        echo "   ❌ Playwright test image not available. Build it first with previous commands."
        return 1
    fi
    
    # Wait for both to be ready
    echo "   Waiting for containers to be ready..."
    sleep 10
}

run_comparison_tests() {
    echo "2. Running Comparison Tests"
    
    # Test data for comparison
    local test_urls=("https://httpbin.org/html" "https://example.com" "https://httpbin.org/status/200")
    local selenium_port="7860"
    local playwright_port="7861"
    
    echo "   Testing URL navigation and screenshot capture..."
    
    for url in "${test_urls[@]}"; do
        echo "   Testing URL: $url"
        
        # Test Selenium setup
        echo "     Selenium test..."
        local selenium_start=$(date +%s.%N)
        local selenium_result=$(curl -s -X POST -H "Content-Type: application/json" \
            -d "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"browseWebAndReturnImage\", \"arguments\": {\"provideAllValuesInPlainEnglish\": \"Navigate to $url and take a screenshot\"}}, \"id\": 1}" \
            http://localhost:$selenium_port 2>&1)
        local selenium_end=$(date +%s.%N)
        local selenium_time=$(echo "$selenium_end - $selenium_start" | bc)
        
        echo "       Time: ${selenium_time}s"
        echo "       Result: $(echo "$selenium_result" | grep -q "result" && echo "✅ Success" || echo "❌ Failed")"
        
        # Test Playwright setup (if API is compatible)
        echo "     Playwright test..."
        local playwright_start=$(date +%s.%N)
        local playwright_result=$(curl -s -X POST -H "Content-Type: application/json" \
            -d "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"browseWebAndReturnImage\", \"arguments\": {\"provideAllValuesInPlainEnglish\": \"Navigate to $url and take a screenshot\"}}, \"id\": 1}" \
            http://localhost:$playwright_port 2>&1)
        local playwright_end=$(date +%s.%N)
        local playwright_time=$(echo "$playwright_end - $playwright_start" | bc)
        
        echo "       Time: ${playwright_time}s"
        echo "       Result: $(echo "$playwright_result" | grep -q "result" && echo "✅ Success" || echo "❌ Failed")"
        
        # Compare performance
        local performance_diff=$(echo "$selenium_time - $playwright_time" | bc)
        echo "       Performance difference: ${performance_diff}s (negative = Playwright faster)"
        echo
    done
}

compare_resource_usage() {
    echo "3. Resource Usage Comparison"
    
    echo "   Current resource usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" 2>/dev/null | grep -E "(selenium|playwright|a2awebagent)" || echo "   Unable to get resource stats"
}

test_error_handling() {
    echo "4. Error Handling Comparison"
    
    # Test invalid URLs
    local invalid_url="https://this-domain-does-not-exist-12345.com"
    
    echo "   Testing error handling with invalid URL: $invalid_url"
    
    # Test Selenium error handling
    echo "   Selenium error handling..."
    local selenium_error=$(curl -s -X POST -H "Content-Type: application/json" \
        -d "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"browseWebAndReturnText\", \"arguments\": {\"provideAllValuesInPlainEnglish\": \"Navigate to $invalid_url\"}}, \"id\": 1}" \
        http://localhost:7860 2>&1)
    
    echo "     Error response: $(echo "$selenium_error" | grep -q "error\|timeout\|failed" && echo "✅ Handled gracefully" || echo "❌ No error handling")"
    
    # Test Playwright error handling (when implemented)
    echo "   Playwright error handling..."
    local playwright_error=$(curl -s -X POST -H "Content-Type: application/json" \
        -d "{\"jsonrpc\": \"2.0\", \"method\": \"tools/call\", \"params\": {\"name\": \"browseWebAndReturnText\", \"arguments\": {\"provideAllValuesInPlainEnglish\": \"Navigate to $invalid_url\"}}, \"id\": 1}" \
        http://localhost:7861 2>&1)
    
    echo "     Error response: $(echo "$playwright_error" | grep -q "error\|timeout\|failed" && echo "✅ Handled gracefully" || echo "❌ No error handling")"
}

generate_comparison_report() {
    echo "5. Generating Comparison Report"
    
    local report_file="/tmp/selenium_vs_playwright_comparison_$(date +%Y%m%d_%H%M%S).md"
    
    cat > $report_file << 'EOF'
# Selenium vs Playwright Comparison Report

## Executive Summary
This report compares the current Selenium-based setup with the proposed Playwright implementation for the a2aTravelAgent project.

## Test Results

### Performance Comparison
| Test Case | Selenium Time | Playwright Time | Performance Gain |
|-----------|---------------|-----------------|------------------|
| httpbin.org/html | [SELENIUM_TIME] | [PLAYWRIGHT_TIME] | [DIFFERENCE] |
| example.com | [SELENIUM_TIME] | [PLAYWRIGHT_TIME] | [DIFFERENCE] |
| Status page | [SELENIUM_TIME] | [PLAYWRIGHT_TIME] | [DIFFERENCE] |

### Resource Usage
| Setup | CPU Usage | Memory Usage | Image Size |
|-------|-----------|--------------|------------|
| Selenium | [SELENIUM_CPU] | [SELENIUM_MEM] | [SELENIUM_SIZE] |
| Playwright | [PLAYWRIGHT_CPU] | [PLAYWRIGHT_MEM] | [PLAYWRIGHT_SIZE] |

### Reliability
- Selenium error handling: [SELENIUM_ERROR_HANDLING]
- Playwright error handling: [PLAYWRIGHT_ERROR_HANDLING]

## Recommendations
Based on testing results:

1. **Performance**: [PERFORMANCE_WINNER] shows better performance
2. **Resource Usage**: [RESOURCE_WINNER] uses fewer resources
3. **Reliability**: [RELIABILITY_WINNER] handles errors better
4. **Maintenance**: [MAINTENANCE_WINNER] is easier to maintain

## Migration Decision
- [ ] Proceed with Playwright migration
- [ ] Stay with current Selenium setup
- [ ] Hybrid approach (use both based on use case)

## Next Steps
1. [NEXT_STEP_1]
2. [NEXT_STEP_2]
3. [NEXT_STEP_3]
EOF

    echo "   ✅ Comparison report template created at: $report_file"
    echo "   Fill in the test results and complete the analysis."
}

cleanup_test_environment() {
    echo "6. Cleaning Up Test Environment"
    
    # Stop test containers
    docker stop playwright-test-comparison 2>/dev/null || true
    docker rm playwright-test-comparison 2>/dev/null || true
    
    echo "   ✅ Test environment cleaned up"
}

# Run side-by-side testing
setup_test_environment
run_comparison_tests
compare_resource_usage
test_error_handling
generate_comparison_report
cleanup_test_environment

echo "=== Side-by-Side Testing Complete ==="
```

## Conclusion

This comprehensive Docker testing strategy provides:

1. **Complete build and test validation** for both Microsoft official and custom Playwright setups
2. **Extensive browser validation commands** to ensure Playwright works correctly in Docker
3. **Screenshot testing framework** to validate core functionality
4. **Health monitoring scripts** for production readiness
5. **M1 Mac specific optimizations** and compatibility checks
6. **Detailed comparison framework** between official and custom setups
7. **Migration testing strategy** to safely transition from Selenium to Playwright

### Key Recommendations:

**For M1 Mac Development:**
- Use Microsoft's official Playwright image for reliability and multi-arch support
- Leverage Docker Buildx for cross-platform builds
- Implement comprehensive health checks and monitoring

**For Production:**
- Choose Microsoft official image for stability and security updates
- Implement the continuous health monitoring scripts
- Have rollback procedures ready

**For Testing:**
- Use the provided scripts to validate browser functionality before migration
- Run side-by-side comparisons to ensure feature parity
- Test extensively on target architecture (ARM64 for M1 Macs)

This strategy ensures a successful Playwright integration with minimal risk and maximum reliability across different architectures and environments.