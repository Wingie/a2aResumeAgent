# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

# Install build tools (Maven is needed for compilation)
RUN apt-get update && apt-get install -y \
    curl maven \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven configuration first (for better layer caching)
COPY pom.xml .

# Download dependencies with BuildKit cache mount for faster builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# Create required directories (remove seleniumCache, add playwrightCache)
RUN mkdir -p /app/screenshots /app/logs /app/playwrightCache /app/resolutionCache /app/uploads && \
    chmod -R 777 /app

# Verify the JAR was built
RUN ls -la target/ && \
    test -f target/a2awebagent-0.0.1.jar

# Check what user exists in Playwright image and use it
RUN id && ls -la /home/ && \
    chown -R $(id -u):$(id -g) /app 2>/dev/null || true

# Use the existing pwuser that comes with Playwright image
USER pwuser

# Expose port
EXPOSE 7860

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:7860/actuator/health || exit 1

# Set JVM options for container environment with module system compatibility
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED \
--add-opens java.base/java.text=ALL-UNNAMED \
--add-opens java.desktop/java.awt=ALL-UNNAMED \
--add-opens java.desktop/java.awt.image=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
--add-exports java.base/sun.net.util=ALL-UNNAMED \
--add-exports java.desktop/sun.awt=ALL-UNNAMED"

# Copy Playwright-optimized entrypoint script
COPY entrypoint-playwright.sh /app/entrypoint.sh

# Make entrypoint executable
USER root
RUN chmod +x /app/entrypoint.sh && \
    mkdir -p /tmp/.X11-unix && \
    chmod 1777 /tmp/.X11-unix && \
    chown -R pwuser:pwuser /app 2>/dev/null || chown -R $(id -u pwuser):$(id -g pwuser) /app

# Switch back to non-root user
USER pwuser

# Start the application
ENTRYPOINT ["/app/entrypoint.sh"]