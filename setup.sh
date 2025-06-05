#!/bin/bash
set -e

# Install git hooks
if [ -d .githooks ]; then
  echo "Installing git hooks..."
  cp .githooks/* .git/hooks/
  chmod +x .git/hooks/*
fi

# Build and start backend (Ktor server + Postgres)
echo "Starting backend services with Docker Compose..."
docker-compose up --build -d

echo "Backend services are running."

echo "Please open Android Studio and run the android module on an emulator or device." 