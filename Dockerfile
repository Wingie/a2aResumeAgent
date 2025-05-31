FROM mcr.microsoft.com/playwright/java:v1.51.0-noble

# Install curl (not included by default)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set version
ARG VERSION=0.2.3

# Create working directory
WORKDIR /ai

# Download your JAR
RUN curl -L -o /ai/mcpdemo.jar \
    https://github.com/vishalmysore/a2aPlaywright/releases/download/hugginface/a2aPlaywright-${VERSION}.jar

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose application port
EXPOSE 7860

# Entrypoint
ENTRYPOINT ["/entrypoint.sh"]