# ECE452-s25-group-9

## Quick Start

**Windows:** Run `setup.bat`

**Mac/Linux:** Run `./setup.sh`

- Installs git hooks
- Starts backend (Ktor server, PostgreSQL, Adminer) with Docker Compose

**Android App:**
- Open in Android Studio
- Run the `android` module on an emulator or device

---

## Accessing the Backend & Database

- **Ktor API:** [http://localhost:8080/ping](http://localhost:8080/ping) (returns `hello world`)
  - Auth: `/auth/signup`, `/auth/signin`, `/auth/me`
- **Database Viewer (Adminer):** [http://localhost:8081](http://localhost:8081)
  - System: PostgreSQL
  - Server: db
  - Username/Password/DB: from `.env`

---

## Project Structure

```
.
├── android/                # Android app (Jetpack Compose, MVVM, Hilt)
│   └── src/main/kotlin/com/example/gooddeedfeed/
│       ├── core/          # Shared utilities
│       ├── data/          # Data layer
│       ├── domain/        # Business logic
│       ├── di/            # Dependency injection
│       └── presentation/  # UI, navigation, viewmodels, screens
├── server/                # Ktor backend
│   ├── src/main/kotlin/com/example/gooddeedfeed/
│   │   ├── Application.kt # Ktor entry
│   │   ├── AuthModule.kt  # JWT auth
│   │   └── auth/          # Auth logic/routes/models
│   └── Dockerfile         # Backend Docker build
├── docker-compose.yml     # Orchestrates backend, DB, Adminer
├── setup.sh / setup.bat   # One-step setup scripts
├── .env                   # Environment variables
└── .env.template          # Environment variables template

```

---

## Contribution Rules

- **No direct commits to `main`**
- Use feature branches: `<issue#>-description` (e.g., `12-signup-endpoint`)
- Open PRs for review before merging

---

- Backend: Ktor server (JWT auth), PostgreSQL, Adminer
- Frontend: Android (Jetpack Compose, MVVM, Hilt)
- Lint/format: Gradle tasks & git hooks
