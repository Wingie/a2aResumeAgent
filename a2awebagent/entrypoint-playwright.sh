#!/bin/sh

# Playwright-optimized entrypoint script
# No need for X11/Xvfb since Playwright runs browsers in headless mode

# Function for graceful shutdown
cleanup() {
    echo "Shutting down Playwright-based a2awebagent..."
    exit 0
}

# Set up signal handlers
trap cleanup TERM INT EXIT

echo "=========================================="
echo "Starting Playwright-based a2awebagent"
echo "=========================================="

# Environment information
echo "Environment:"
echo "  User: $(id)"
echo "  Working Directory: $(pwd)"
echo "  Java Version: $(java -version 2>&1 | head -1)"
echo "  Available Memory: $(free -h | grep Mem)"

# Verify Playwright installation
echo "Playwright Environment:"
echo "  Playwright browsers path: ${PLAYWRIGHT_BROWSERS_PATH:-/ms-playwright}"
if [ -d "${PLAYWRIGHT_BROWSERS_PATH:-/ms-playwright}" ]; then
    echo "  Playwright browsers found: $(ls ${PLAYWRIGHT_BROWSERS_PATH:-/ms-playwright} 2>/dev/null | wc -l) browser(s)"
else
    echo "  Playwright browsers directory not found, using system browsers"
fi

# Test Playwright browser availability
echo "Testing Playwright browser capabilities..."
java -cp /app/target/a2awebagent-0.0.1.jar \
    -Dloader.main=com.microsoft.playwright.CLI \
    org.springframework.boot.loader.launch.PropertiesLauncher \
    --version 2>/dev/null && echo "  Playwright CLI: AVAILABLE" || echo "  Playwright CLI: NOT AVAILABLE"

# Create required directories
echo "Setting up application directories..."
mkdir -p /app/screenshots /app/logs /app/playwrightCache
ls -la /app/screenshots /app/logs /app/playwrightCache

# Set Playwright-specific environment variables
export PLAYWRIGHT_BROWSERS_PATH=${PLAYWRIGHT_BROWSERS_PATH:-/ms-playwright}
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Application startup
echo "=========================================="
echo "Starting Spring Boot application..."
echo "JAR file: /app/target/a2awebagent-0.0.1.jar"
echo "JVM Options: $JAVA_OPTS"
echo "=========================================="

# Start the Spring Boot application
exec java \
  $JAVA_OPTS \
  -Djava.security.egd=file:/dev/./urandom \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens java.desktop/java.awt=ALL-UNNAMED \
  --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-exports java.base/sun.security.util=ALL-UNNAMED \
  -jar /app/target/a2awebagent-0.0.1.jar