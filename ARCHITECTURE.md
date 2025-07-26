# GoodDeedFeed – Architecture Guide

> **Version:** 2024-04-28 • _Keep this file up-to-date when you add or refactor major components._

---

## 1. Overview
GoodDeedFeed is a **volunteering & community engagement platform** consisting of:

1. **Android mobile app** – Built with Jetpack Compose, MVVM & Hilt.
2. **Backend API** – Python FastAPI service exposing REST + WebSocket endpoints.
3. **Data layer** – PostgreSQL for relational data, MinIO (S3-compatible) for object storage, Firebase Cloud Messaging for push notifications, and optional OpenAI integration.
4. **DevOps** – Docker Compose for local orchestration and GitHub Actions for CI/CD.

The system enables organizers to publish volunteering opportunities, volunteers to discover & join events, chat, earn karma & badges, receive notifications, and much more.

---

## 2. High-Level Architecture
```mermaid
graph TD;
  subgraph Mobile
    A[Android App (Compose)]
  end
  subgraph Backend
    B[FastAPI Service]
    C[PostgreSQL]
    D[MinIO]
  end
  E[Firebase&nbsp;Cloud&nbsp;Messaging]
  F[OpenAI API]
  A <--> |REST+WebSocket| B
  B <-->|SQLAlchemy| C
  B <-->|S3-API| D
  B --> |Push Tokens| E
  B --> |Optional| F
```
> **Note:** For production deployments MinIO can be swapped for AWS S3 and FCM for APNS where needed.

---

## 3. Android Application
### 3.1 Module / Layer Diagram
```mermaid
graph LR;
  Presentation(UI) --> Domain
  Domain --> Data
  Data --> Remote[Ktor Client]
  Data --> Local[DataStore]
```

• **presentation/** – Jetpack Compose screens, `ViewModel`s, navigation & UI theme.  
• **domain/** – Pure Kotlin business logic: models + `usecase/` orchestrating operations.  
• **data/** – Repository pattern. Remote calls via Ktor, mapping DTO ⇄ domain models, caching user/session state in DataStore.  
• **di/** – Hilt modules wiring dependencies.  

### 3.2 Navigation
`AppNavHost.kt` registers top-level routes (Home, Chat, Map, Leaderboard, etc.). Deep-links from notifications use the same destinations.

### 3.3 Realtime Chat Flow
1. Client resolves `roomId` (sorted pair of user IDs).  
2. Opens WebSocket `/api/ws/chat/{roomId}`.  
3. Messages are optimistically added to local list, then persisted after server ACK.  
4. Push payload triggers navigation when user is outside chat.

### 3.4 Offline & Caching
Minimal local caching (DataStore) for user/session tokens & conversation list. Further Room integration is planned (see Roadmap).

### 3.5 Build Variants
* **debug** – Developer Mode, verbose logging, staging endpoints.  
* **release** – Proguard/R8, Developer Mode disabled.

---

## 4. Backend Service
### 4.1 Project Layout
```
server/app/
 ├── main.py          # FastAPI bootstrap & middleware
 ├── routes.py        # 3 700 + lines of grouped endpoints
 ├── models.py        # SQLAlchemy models
 ├── schemas.py       # Pydantic I/O schemas
 ├── storage.py       # MinIO wrapper
 ├── firebase_service.py
 └── session_auth.py  # Cookie-based auth helpers
```

### 4.2 Key Concerns
| Area | Implementation |
|------|----------------|
| **Auth** | Secure sessions stored in Redis-like in-memory dict (swap for Redis in prod). |
| **Events** | CRUD, image carousel, volunteer relation table, attendance & karma calculation. |
| **Chat** | `/ws/chat/{room_id}` with in-memory connection registry. |
| **Notifications** | FCM push + in-app table; OpenAPI `send_*` helpers coexist. |
| **Leaderboard** | Window functions to rank volunteers by karma. |
| **Badges** | Rule-based evaluation in `/users/me/check-badges`. |
| **Lost & Found** | Separate table + images, auto cleanup via background task. |

### 4.3 Database Entities (excerpt)
```mermaid
erDiagram
  User ||--o{ Event : organizes
  User ||--o{ Message : "sends"
  User ||--o{ VolunteerEvents : "joins"
  User ||--o{ UserBadges : awards
  Event ||--o{ EventImage : has
  User ||--o{ Subscription : follows
  LostFoundItem ||--o{ LostFoundImage : has
```
_See `alembic/versions/` for full DDL history._

### 4.4 Storage Service
`storage_service` streams uploads directly to MinIO; presigned URLs returned to clients. Buckets:
* `profile-pictures`
* `event-images`
* `lost-found`

### 4.5 Background Jobs
Currently scheduled via `asyncio.create_task` (see `routes.schedule_lost_found_cleanup`). In production migrate to Celery or APScheduler.

---

## 5. API Surface (selected)
| Method & Path | Purpose |
|---------------|---------|
| `POST /api/register` | Register new account |
| `POST /api/login` | Session login (cookie) |
| `POST /api/events` | Create volunteering opportunity |
| `POST /api/events/{id}/join` | Volunteer joins event |
| `POST /api/ws/chat/{room_id}` | WebSocket chat |
| `GET  /api/leaderboard` | Paginated karma leaderboard |
| `POST /api/lost-found` | Create lost/found item |

_For full list start the server and visit `/docs`._

---

## 6. Deployment & Ops

1. **Local:** `./run-dev.sh` → spins up Postgres 15, MinIO, Adminer & API (with hot-reload) in Docker.
2. **CI:** GitHub Actions workflow builds Debug APK and runs backend unit tests on every push & PR.
3. **Production (suggested):**
   * AWS RDS Postgres
   * S3 bucket w/ lifecycle policies
   * Redis for sessions & WebSocket broadcasting
   * Gunicorn/Uvicorn workers behind Traefik/NGINX

---

## 7. Security Hardening Checklist
- [ ] Enforce HTTPS – terminate TLS at load-balancer or Traefik.
- [ ] Store sessions in Redis with expiry instead of memory.
- [ ] Rate-limit auth endpoints.
- [ ] Sanitize user uploads (MIME & size limits already in place).
- [ ] Rotate MinIO/S3 credentials via IAM role.

---

## 8. Roadmap
* _Q2:_ Refactor chat to use Redis pub/sub for horizontal scalability.  
* _Q2:_ Implement server-side pagination & search across all list endpoints.  
* _Q3:_ iOS client built with Kotlin Multiplatform.

---

## 9. Contributing
1. Pick or create an issue, assign yourself, branch from `main`.
2. Follow **Conventional Commits** (`feat:`, `fix:`…).
3. Write/maintain unit tests; run `black` + `isort` + `ktlint`.
4. Open PR → GitHub Actions must pass before review.

---

_Questions? ping **@ECE452-s25-group-9** on Teams._ 
