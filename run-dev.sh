#!/bin/bash
set -e

echo "Starting development environment..."

# 1. Install git hooks
if [ -d .githooks ]; then
    echo "Installing git hooks..."
    cp .githooks/* .git/hooks/
    chmod +x .git/hooks/*
fi

echo ""
echo "Starting services with Docker Compose..."
docker-compose up --build -d
echo ""
echo "Services are starting..."
echo "API endpoint: http://localhost:9000"
echo "API documentation: http://localhost:9000/docs"
echo "Adminer: http://localhost:8082"
echo "  - System: PostgreSQL"
echo "  - Server: db"
echo "  - Username: postgres"
echo "  - Password: postgres"
echo "  - Database: mydb"

echo ""
echo "Waiting a moment for services to start..."
sleep 5

echo ""
echo "Checking API status..."
curl -f http://localhost:9000/ || echo "API not ready yet, please wait a moment..."

echo ""
echo "To view logs: docker-compose logs -f api"
echo "To stop: docker-compose down"
echo "Once the server is running, open Android Studio and run the android module on an emulator or device."

echo "Development environment is running!"
echo "API is available at: http://localhost:9000"
echo "Database UI (Adminer) is available at: http://localhost:8082"
echo "  - System: PostgreSQL"
echo "  - Server: db"
echo "  - Username: postgres"
echo "  - Password: postgres"
echo "  - Database: mydb"

echo ""
echo "To view logs: docker-compose logs -f api"
echo "To stop: docker-compose down" 