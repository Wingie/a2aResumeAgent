# syntax=docker/dockerfile:1
FROM openjdk:18-jdk-slim

# Install system dependencies for Chrome and build tools
RUN apt-get update && apt-get install -y \
    curl unzip gnupg wget tar maven \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libwayland-client0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxkbcommon0 \
    libxrandr2 \
    xdg-utils \
    libu2f-udev \
    libvulkan1 \
    && rm -rf /var/lib/apt/lists/*

# Install Chromium and xvfb for headless display (ARM64 compatible)
RUN apt-get update && \
    apt-get install -y chromium chromium-driver xvfb x11-utils \
    # Add additional browser dependencies
    ca-certificates \
    fontconfig \
    libappindicator3-1 \
    libgconf-2-4 \
    libxss1 \
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

# Create required directories
RUN mkdir -p /app/screenshots /app/logs /app/seleniumCache /app/resolutionCache /app/uploads && \
    chmod -R 777 /app

# Verify the JAR was built
RUN ls -la target/ && \
    test -f target/a2awebagent-0.0.1.jar

# Create a non-root user for security
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

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

# Copy entrypoint script
COPY entrypoint.sh /app/entrypoint.sh

# Make entrypoint executable
USER root
RUN chmod +x /app/entrypoint.sh && \
    mkdir -p /tmp/.X11-unix && \
    chmod 1777 /tmp/.X11-unix && \
    chown -R appuser:appuser /app

# Switch back to non-root user
USER appuser

# Start the application
ENTRYPOINT ["/app/entrypoint.sh"]