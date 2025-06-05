@echo off
REM Install git hooks
IF EXIST .githooks (
  echo Installing git hooks...
  copy /Y .githooks\* .git\hooks\
)

REM Build and start backend (Ktor server + Postgres)
echo Starting backend services with Docker Compose...
docker-compose up --build -d

echo Backend services are running.

echo Please open Android Studio and run the android module on an emulator or device. 