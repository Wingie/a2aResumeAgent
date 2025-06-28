#!/bin/bash

# Docker startup script for a2aTravelAgent
# This script builds and runs the complete Docker infrastructure

set -e

echo "🐳 Starting a2aTravelAgent Docker Infrastructure"
echo "================================================"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
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

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Check if .env file exists
if [ ! -f .env ]; then
    print_warning ".env file not found. Creating from template..."
    cp .env.example .env
    print_warning "Please edit .env file with your API keys"
    echo ""
    print_error "Required environment variables:"
    echo "  - GEMINI_API_KEY: Your Gemini API key"
    echo "  - DB_USER: Database username (default: agent)"
    echo "  - DB_PASSWORD: Database password (default: agent123)"
    echo ""
    print_warning "Edit .env file and run this script again"
    exit 1
fi

# Source environment variables
print_status "Loading environment variables..."
export $(cat .env | grep -v '#' | grep -v '^$' | awk '/=/ {print $1}')

# Validate required environment variables
if [ -z "$GEMINI_API_KEY" ] || [ "$GEMINI_API_KEY" = "your_gemini_api_key_here" ]; then
    print_error "GEMINI_API_KEY not set properly in .env file"
    exit 1
fi

# Create required directories
print_status "Creating required directories..."
mkdir -p data/postgres data/redis data/neo4j data/neo4j-logs data/pgadmin screenshots logs init-scripts

# Set proper permissions
chmod -R 755 data/
chmod -R 777 screenshots/ logs/

# Build the application first (to catch any compilation errors)
print_status "Building application..."
mvn clean package -DskipTests -q

if [ $? -ne 0 ]; then
    print_error "Application build failed. Please fix compilation errors."
    exit 1
fi

print_success "Application built successfully"

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose down --remove-orphans > /dev/null 2>&1 || true

# Enable BuildKit for faster builds
export DOCKER_BUILDKIT=1
print_status "BuildKit enabled for faster Maven dependency caching"

# Build and start the infrastructure
print_status "Building and starting Docker infrastructure..."
print_status "This may take a few minutes on first run..."

# Start core services (without admin tools)
docker-compose up --build -d a2awebagent postgres redis neo4j

if [ $? -eq 0 ]; then
    print_success "Infrastructure started successfully!"
else
    print_error "Failed to start infrastructure"
    docker-compose logs a2awebagent
    exit 1
fi

# Wait for services to be healthy
print_status "Waiting for services to be ready..."
sleep 10

# Check service health
print_status "Checking service health..."

# Check PostgreSQL
if docker-compose exec -T postgres pg_isready -U agent -d a2awebagent > /dev/null 2>&1; then
    print_success "PostgreSQL is ready"
else
    print_warning "PostgreSQL is not ready yet"
fi

# Check Redis
if docker-compose exec -T redis redis-cli ping | grep -q "PONG"; then
    print_success "Redis is ready"
else
    print_warning "Redis is not ready yet"
fi

# Check application
sleep 5
if curl -s http://localhost:7860/actuator/health > /dev/null 2>&1; then
    print_success "Application is ready"
else
    print_warning "Application is still starting up..."
    print_status "Checking application logs..."
    docker-compose logs --tail=20 a2awebagent
fi

echo ""
print_success "🎉 a2aTravelAgent Infrastructure is running!"
echo ""
echo "📊 Service URLs:"
echo "  • Agent Dashboard:    http://localhost:7860/agents"
echo "  • API Documentation:  http://localhost:7860/swagger-ui.html"
echo "  • Health Check:       http://localhost:7860/actuator/health"
echo "  • Task API:           http://localhost:7860/v1/tasks/"
echo ""
echo "💾 Database Management:"
echo "  • PostgreSQL:         localhost:5432 (user: agent, password: agent123)"
echo "  • Redis:              localhost:6379"
echo "  • Neo4j Browser:      http://localhost:7474 (user: neo4j, password: password123)"
echo ""
echo "🔧 Admin Tools (optional):"
echo "  Start with: docker-compose --profile admin up -d"
echo "  • PgAdmin:            http://localhost:8080"
echo "  • Redis Commander:    http://localhost:8081"
echo ""
echo "📝 Useful Commands:"
echo "  • View logs:          docker-compose logs -f a2awebagent"
echo "  • Stop services:      docker-compose down"
echo "  • Restart app:        docker-compose restart a2awebagent"
echo "  • Test API:           ./test-async-api.sh"
echo ""

# Optional: Run tests
read -p "Run integration tests? (y/N): " run_tests
if [[ $run_tests =~ ^[Yy]$ ]]; then
    print_status "Running API tests..."
    sleep 5  # Give services more time
    ./test-async-api.sh
fi