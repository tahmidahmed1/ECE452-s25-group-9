# ECE452-s25-group-9

## Quick Start

- Windows: Run `run-dev.bat`
- Mac/Linux: Run `./run-dev.sh`

This script will:

- Start backend services (PostgreSQL, MinIO, Adminer) using Docker Compose
- Run the Python FastAPI server with hot reload
- Initialize the database with migrations

**Android App:**
- Open in Android Studio
- Run the `android` module on an emulator or device

---

## Accessing Services

- **FastAPI Backend:** [http://localhost:9000](http://localhost:9000)
  - API Docs: [http://localhost:9000/docs](http://localhost:9000/docs)
  - Auth: `/api/register`, `/api/token`, `/api/users/me`
  - Onboarding: `/api/onboarding/step-one`, `/api/onboarding/complete`
  - Upload: `/api/upload-profile-picture`

- **Database Viewer (Adminer):** [http://localhost:8082](http://localhost:8082)
  - System: PostgreSQL
  - Server: db
  - Username: postgres / Password: postgres / DB: mydb

- **MinIO Object Storage Console:** [http://localhost:9002](http://localhost:9002)
  - Username: minioadmin / Password: minioadmin123
  - API Endpoint: [http://localhost:9001](http://localhost:9001)

---

## Project Structure

```
.
├── android/                # Android app (Jetpack Compose, MVVM, Hilt)
│   └── src/main/kotlin/com/example/gooddeedfeed/
│       ├── core/          # Shared utilities
│       ├── data/          # Data layer (API services, repositories)
│       ├── domain/        # Business logic (models, use cases)
│       ├── di/            # Dependency injection
│       └── presentation/  # UI, navigation, viewmodels, screens
├── server/                # Python FastAPI backend
│   ├── app/               # FastAPI application
│   │   ├── main.py        # FastAPI entry point
│   │   ├── models.py      # SQLAlchemy models
│   │   ├── schemas.py     # Pydantic schemas
│   │   ├── routes.py      # API endpoints
│   │   ├── auth.py        # JWT authentication
│   │   ├── storage.py     # MinIO object storage
│   │   └── database.py    # Database configuration
│   ├── alembic/           # Database migrations
│   ├── requirements.txt   # Python dependencies
│   └── Dockerfile         # Backend Docker build
├── docker-compose.yml     # Orchestrates backend, DB, MinIO, Adminer
└── run-dev.sh / run-dev.bat # Development setup scripts
```

---

## Features

- **Authentication:** JWT-based user registration and login
- **Onboarding:** Multi-step user type selection and profile completion
- **Object Storage:** Profile picture uploads with MinIO S3-compatible storage
- **Database:** PostgreSQL with Alembic migrations
- **Hot Reload:** Development environment with automatic code reloading

---

## Contribution Rules

- **No direct commits to `main`**
- Use feature branches: `<issue#>-description` (e.g., `12-signup-endpoint`)
- Open PRs for review before merging

---

**Tech Stack:**
- Backend: Python FastAPI, SQLAlchemy, PostgreSQL, MinIO, JWT auth
- Frontend: Android (Jetpack Compose, MVVM, Hilt, Ktor client)
- Infrastructure: Docker Compose, Alembic migrations
