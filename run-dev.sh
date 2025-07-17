#!/bin/bash
set -e

echo "Starting development environment..."

# Install git hooks if directory exists
if [ -d .githooks ]; then
    echo "Installing git hooks..."
    cp .githooks/* .git/hooks/
    chmod +x .git/hooks/*
fi

# Ensure .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Please create a .env file in the project root with the following variables:"
    echo ""
    echo "DB_URL=jdbc:postgresql://localhost:5432/gooddeedfeed"
    echo "DB_USER=postgres"
    echo "DB_PASSWORD=your_actual_postgres_password"
    echo ""
    echo "IMPORTANT: Make sure your PostgreSQL database is running."
    echo "If using Docker, start the database with:"
    echo "docker-compose up -d db"
    read -p "Press Enter to exit..."
    exit 1
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
echo "To view logs: docker-compose logs -f api"
echo "To stop: docker-compose down"
echo "Once the server is running, open Android Studio and run the android module on an emulator or device." 
