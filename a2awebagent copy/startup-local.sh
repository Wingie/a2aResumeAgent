#!/bin/bash

# Local startup script for a2aTravelAgent
# This script sets up and runs the application locally for development

set -e

echo "🚀 Starting a2aTravelAgent Local Development"
echo "============================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if .env file exists
if [ ! -f .env ]; then
    print_warning ".env file not found. Creating from template..."
    cp .env.example .env
    print_warning "Please edit .env file with your API keys before continuing"
    echo "Required: OPENAI_API_KEY"
    read -p "Press Enter to continue after editing .env..."
fi

# Source environment variables
if [ -f .env ]; then
    print_status "Loading environment variables from .env..."
    export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
fi

# Check required environment variables
if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "your_openai_api_key_here" ]; then
    print_warning "OPENAI_API_KEY not set properly"
    print_warning "Please set your OpenAI API key in .env file"
    exit 1
fi

# Create necessary directories
print_status "Creating required directories..."
mkdir -p screenshots logs data/uploads

# Clean and build the application
print_status "Building application..."
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    print_success "Build completed successfully"
else
    echo "Build failed. Please check the errors above."
    exit 1
fi

# Start the application
print_status "Starting application with local H2 database..."
print_status "Dashboard will be available at: http://localhost:7860/agents"
print_status "H2 Console will be available at: http://localhost:7860/h2-console"
print_status "API Documentation: http://localhost:7860/swagger-ui.html"

echo ""
print_success "Application starting..."
echo ""

# Run the application
java -jar target/a2awebagent-0.0.1.jar \
    --spring.profiles.active=default \
    --logging.level.org.springframework.web=INFO