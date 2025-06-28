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

# Install Chromium instead of Chrome for better multi-arch support
RUN apt-get update && \
    apt-get install -y chromium chromium-driver && \
    rm -rf /var/lib/apt/lists/*

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

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/a2awebagent-0.0.1.jar"]