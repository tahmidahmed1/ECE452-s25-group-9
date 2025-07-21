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
│   │   ├── auth.py        # authentication
│   │   ├── storage.py     # MinIO object storage
│   │   └── database.py    # Database configuration
│   ├── alembic/           # Database migrations
│   ├── requirements.txt   # Python dependencies
│   └── Dockerfile         # Backend Docker build
├── docker-compose.yml     # Orchestrates backend, DB, MinIO, Adminer
└── run-dev.sh / run-dev.bat # Development setup scripts
```


## Features

- **Authentication:** session-based user registration and login
- **Onboarding:** Multi-step user type selection and profile completion
- **Object Storage:** Profile picture uploads with MinIO S3-compatible storage
- **Database:** PostgreSQL with Alembic migrations
- **Hot Reload:** Development environment with automatic code reloading


## Contribution Rules

- **No direct commits to `main`**
- Use feature branches: `<issue#>-description` (e.g., `12-signup-endpoint`)
- Open PRs for review before merging


**Tech Stack:**
- Backend: Python FastAPI, SQLAlchemy, PostgreSQL, MinIO, session auth
- Frontend: Android (Jetpack Compose, MVVM, Hilt, Ktor client)
- Infrastructure: Docker Compose, Alembic migrations

## Development Mode 🚀

For faster development and testing, the Android app includes a **Development Mode** that allows you to quickly sign in without creating accounts manually.

### Features

- **Quick Sign-in**: Auto-generates unique users with realistic data
- **Multiple User Types**: Create Volunteer or Organizer accounts instantly  
- **No Server Required**: Works even when the backend is unavailable
- **Unique Identifiers**: Each dev user gets a unique hash-based username and email

### How to Use

1. **Debug Builds Only**: Dev mode is automatically enabled in debug builds
2. **Sign-in Screen**: Look for the "🚀 Development Mode" card at the bottom
3. **Choose User Type**: Click on 👤 Volunteer or ⭐ Organizer
4. **Instant Access**: Get signed in immediately with a pre-configured account

### Generated Data

Each dev user gets:
- **Username**: `dev_{usertype}_{timestamp}`
- **Email**: `dev_{usertype}_{timestamp}@example.com`  
- **Full Name**: `Dev User {hash}`
- **Phone**: `+1-555-DEV-{hash}`
- **User Type**: Based on your selection
- **Onboarding**: Pre-completed for immediate access

### Configuration

The dev mode can be controlled via build configuration:
- **Debug builds**: `DEV_MODE = true` (enabled)
- **Release builds**: `DEV_MODE = false` (disabled)

This ensures dev mode is never accidentally included in production releases.

## Google Maps Setup

This app uses Google Maps for location-based features. To configure Google Maps:

1. **Get a Google Maps API Key:**
   - Go to the [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select an existing one
   - Enable the "Maps SDK for Android" API
   - Create credentials and generate an API key
   - Restrict the API key to Android apps (recommended for security)

2. **Configure the API Key:**
   - Copy your API key
   - Update the `.env` file in the project root:
   ```
   GOOGLE_MAPS_API_KEY=your_actual_api_key_here
   ```

3. **Environment Variables:**
   - The API key is loaded from the `.env` file during build time
   - Never commit your actual API key to version control
   - The `.env` file is already in `.gitignore` for security
   - Use `.env.example` as a template for required environment variables

4. **Build Configuration:**
   - The Android build system automatically reads the API key from `.env`
   - The key is injected into the Android manifest as a placeholder
   - No code changes are needed - the Maps SDK will automatically use the configured key

## Environment Variables

This project uses environment variables for configuration. The main configuration file is `.env` in the project root.

### Required Environment Variables:

- `GOOGLE_MAPS_API_KEY`: Google Maps API key for Android app
- `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`: Database configuration
- `MINIO_*`: Object storage configuration (handled by Docker Compose)

### Security Notes:

- **Never commit the `.env` file** - it's already in `.gitignore`
- Use `.env.example` as a template for required variables
- Generate strong, unique secrets for production
- The server automatically loads environment variables using `os.getenv()`

## Continuous Integration

We use a GitHub Actions workflow at  
`.github/workflows/android-ci.yml`  
to build the Android Debug APK on every push and pull request to `main`.  

**Required repository secrets** (Settings → Secrets → Actions):  
- `SHARED_DEBUG_KEYSTORE_BASE64` – Base64-encoded contents of `shared-debug.keystore`  
- `GOOGLE_MAPS_API_KEY` – Your Google Maps SDK API key  

Once these are set, the CI will:
1. Restore the debug keystore  
2. Inject your `GOOGLE_MAPS_API_KEY`  
3. Run `./gradlew assembleDebug`
