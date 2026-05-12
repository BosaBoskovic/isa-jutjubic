# JutJubic

A full-stack video sharing platform built with Spring Boot and React. Users can upload and stream videos over HLS, schedule releases, interact through live chat and watch parties, and explore content on an interactive map.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [API Overview](#api-overview)
- [Configuration](#configuration)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [Frontend Routes](#frontend-routes)
- [Cache Strategy](#cache-strategy)
- [Notes](#notes)

---

## Tech Stack

### Backend

| Layer | Technology |
|-------|-----------|
| Framework | Java 17, Spring Boot 3.2 |
| Security | Spring Security, JWT (stateless) |
| Database | Spring Data JPA, PostgreSQL |
| Messaging | RabbitMQ — JSON and Protobuf |
| Cache / Rate limiting | Redis |
| Video processing | FFmpeg (HLS conversion, 720p transcoding) |
| Image compression | Thumbnailator |
| Real-time | WebSocket / STOMP |
| Observability | Micrometer, Prometheus |
| Utilities | Lombok, Jackson, jjwt 0.11 |

### Frontend

| Layer | Technology |
|-------|-----------|
| Framework | React 19, Vite 7 |
| Routing | React Router DOM 7 |
| HTTP | Axios |
| Video streaming | hls.js |
| Maps | Leaflet, React Leaflet |
| WebSocket | STOMP.js, SockJS |

---

## Features

### Authentication

- Register with email and email verification (24-hour token link)
- JWT-based stateless login; token stored in localStorage and attached to every request via an Axios interceptor
- Brute-force protection — an IP is blocked for one minute after five failed login attempts
- BCrypt password hashing
- Protected routes on the frontend via a `PrivateRoute` wrapper

### Video Upload

- Upload MP4 files up to 200 MB with a thumbnail, tags, and an optional geolocation
- Client-side validation covers file type, file size, maximum video duration (30 seconds), and coordinate ranges
- Upload progress bar powered by Axios `onUploadProgress`
- Location can be picked directly on an embedded Leaflet map, searched by city or country name through the Nominatim geocoding API, or detected automatically from the browser
- After upload, HLS conversion runs asynchronously via FFmpeg producing four-second segments; the raw MP4 is accessible immediately while conversion is in progress
- A 720p transcoded copy is produced asynchronously through a RabbitMQ consumer

### Video Scheduling and Synchronized Streaming

- When uploading, a video can be scheduled for a future date and time
- Scheduled videos are hidden from all users until the configured time; the owner can preview them beforehand
- At the scheduled time every viewer watches from the same position — the player calculates the correct offset from the start timestamp and re-syncs every three seconds
- Manual seeking is blocked during an active stream
- Once the stream duration has elapsed, the player reloads in standard on-demand mode so the video can be replayed freely

### HLS Player

- HLS playback via hls.js with a native HLS fallback for Safari
- A live indicator is shown during scheduled streams
- A countdown timer is displayed to viewers waiting for a stream to start
- A replay badge appears once the stream ends

### Live Chat

- Real-time per-video chat available throughout the lifecycle of a scheduled stream — while waiting and while live
- Built on WebSocket and STOMP, subscribing to `/topic/chat.{videoId}`
- Messages are capped at 500 characters and echoed locally before the server confirms delivery
- The chat component tracks the stream phase internally and appears or disappears without a page reload
- Up to 200 messages are kept in memory; there is no persistent message history

### Watch Party

- Authenticated users can create named rooms with unique invite codes
- The room creator picks a video to play; the command is broadcast via WebSocket to all members subscribed to `/topic/watchparty/{roomId}`
- The WebSocket handshake is authenticated by a JWT interceptor

### Map Explorer

- Videos that have a geolocation are shown as thumbnail markers on a Leaflet map
- At lower zoom levels the backend groups nearby videos into cluster markers and returns only the most-viewed representative alongside a count
- Map queries are tile-based: the frontend calculates which OSM tiles cover the current viewport, fetches each in parallel, and merges the results into a deduplicated set
- Requests are debounced — a new fetch fires 800 ms after the user stops panning; any in-flight request is cancelled via `AbortController` when the viewport changes
- A time period filter supports All Time, Last 30 Days, and This Year

### Popular Videos

- A daily ETL pipeline runs at midnight via `@Scheduled`
- Popularity is scored by weighting view counts from each of the last seven days, with more recent days carrying higher weight (7 down to 1)
- The top three scored videos are saved to the `popular_videos` table and displayed on the home screen as trending content
- Pipeline records older than 30 days are cleaned up automatically

### Comments

- Paginated and cached in Redis with a 10-minute TTL; the cache entry is evicted whenever a new comment is posted
- Per-user rate limit of 60 comments per hour, enforced with a Redis counter and sliding window

### Thumbnail Compression

- A scheduled job runs at 2:00 AM daily
- It finds all video posts older than 30 days whose thumbnails have not yet been compressed and re-encodes them at 60% quality using Thumbnailator
- Compressed paths are stored separately so the original file is preserved
- The job can also be triggered manually via `POST /api/admin/compress-thumbnails`

### User Profiles

- Public profile page at `/users/:username` showing the user's display name and all their uploaded videos

### Observability

- Prometheus metrics exposed at `/actuator/prometheus`
- Active user tracking with two Micrometer Gauges: currently active users and active users in the last 24 hours
- Inactive users are cleaned up every 15 minutes by a scheduled task
- A benchmark endpoint at `/debug/mq/benchmark?count=N` publishes N upload events in both JSON and Protobuf and prints average serialization and deserialization times to the console

---

## Project Structure

```
jutjubic/
├── jutjubic-backend/
│   └── src/main/java/com/jutjubic/jutjubic_backend/
│       ├── config/         # Security, CORS, Redis, RabbitMQ, WebSocket, async thread pool,
│       │                   # JWT filters, metrics, scheduling
│       ├── controller/     # REST controllers and WebSocket message handlers
│       ├── dto/            # Request and response DTOs
│       ├── exception/      # GlobalExceptionHandler, ApiException
│       ├── messaging/      # RabbitMQ publisher, consumers, Protobuf generated classes,
│       │                   # benchmark statistics
│       ├── model/          # JPA entities
│       ├── repository/     # Spring Data JPA repositories
│       └── service/        # Business logic
│           └── map/        # TileUtil — OSM tile coordinate conversions
│
└── jutjubic-frontend/
    └── src/
        ├── api/            # Axios instance and API helper modules
        ├── components/     # Navbar, VideoCard, HLSVideoPlayer, LiveChat,
        │                   # MapPicker, CommentsSection
        ├── context/        # AuthContext, WatchPartyContext
        ├── pages/          # All page-level components
        └── utils/          # date.js, tileUtils.js
```

---

## API Overview

| Method | Path | Auth | Description |
|--------|------|:----:|-------------|
| POST | `/api/auth/register` | | Register a new user |
| GET | `/api/auth/verify?token=` | | Verify email token |
| POST | `/api/auth/login` | | Login, returns JWT |
| GET | `/api/posts` | | List all public videos |
| POST | `/api/posts` | Yes | Upload a new video (multipart) |
| GET | `/api/posts/mine` | Yes | Videos uploaded by the current user |
| GET | `/api/posts/{id}` | | Get video by ID |
| GET | `/api/posts/{id}/thumbnail` | | Serve thumbnail image |
| GET | `/api/posts/{id}/hls/playlist.m3u8` | | HLS playlist |
| GET | `/api/posts/{id}/hls/{filename}` | | HLS segment (.ts file) |
| GET | `/api/posts/{id}/video` | | Raw MP4 stream |
| GET | `/api/posts/{id}/availability` | | Check scheduled status and timing |
| POST | `/api/posts/{id}/like` | Yes | Toggle like |
| GET | `/api/posts/{id}/comments` | | Paginated comments |
| POST | `/api/posts/{id}/comments` | Yes | Add a comment |
| GET | `/api/map/tile?z=&x=&y=&period=` | | Map tile — videos and clusters |
| GET | `/api/map/videos` | | Videos in a bounding box |
| GET | `/api/popular/top3` | | Top three popular videos from the latest ETL run |
| GET | `/api/users/{username}/public` | | Public user profile and their videos |
| POST | `/api/watchparty` | Yes | Create a watch party room |
| GET | `/api/watchparty` | | List active rooms |
| GET | `/api/watchparty/{inviteCode}` | | Room details |
| DELETE | `/api/watchparty/{inviteCode}` | Yes | Close a room (creator only) |
| GET | `/api/admin/compress-thumbnails` | | Manually trigger thumbnail compression |
| WS | `/ws?token=` | JWT | WebSocket endpoint for chat and watch party |

---

## Configuration

All sensitive values support environment variable overrides. The defaults below match a local development setup.

```properties
# Database
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/jutjubic}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}

# Redis
spring.redis.host=${SPRING_REDIS_HOST:localhost}
spring.redis.port=${SPRING_REDIS_PORT:6379}

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# JWT
jwt.secret=your-secret-key-minimum-32-characters
jwt.expiration=86400000

# FFmpeg — must be available on PATH or provide the full path
app.ffmpeg.path=ffmpeg
app.ffprobe.path=ffprobe

# File storage
app.uploads.root=uploads
app.uploads.timeout-ms=30000

# Multipart limits
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=210MB

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=your_app_password
```

The frontend communicates with `http://localhost:8080` by default. To change this, update `API_BASE_URL` in `src/api/api.js`.

---

## Prerequisites

- Java 17 or later
- Node.js 18 or later
- PostgreSQL
- Redis
- RabbitMQ
- FFmpeg installed and available on the system PATH

---

## Running Locally

**Start the required infrastructure:**

```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=jutjubic \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  postgres:15

docker run -d -p 6379:6379 redis:7

docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

**Start the backend:**

```bash
cd jutjubic-backend
./mvnw spring-boot:run
```

On first startup, a test user and five test videos are seeded automatically:

| Field | Value |
|-------|-------|
| Username | testuser |
| Password | test123 |
| Email | test@test.com |

**Start the frontend:**

```bash
cd jutjubic-frontend
npm install
npm run dev
```

The application is available at [http://localhost:5173](http://localhost:5173).

---

## Frontend Routes

| Path | Auth required | Description |
|------|:-------------:|-------------|
| `/` | | Home — trending videos when logged in, landing page otherwise |
| `/login` | | Login |
| `/register` | | Register |
| `/verify` | | Email verification |
| `/watch` | | Browse all videos |
| `/videos/:id` | | Video detail with HLS player, comments, and live chat |
| `/upload` | Yes | Upload a new video |
| `/my-videos` | Yes | Videos uploaded by the current user |
| `/my-videos/:id` | Yes | Owner view of a specific video |
| `/users/:username` | | Public user profile |
| `/map` | | Interactive map explorer |
| `/create-watch-party` | Yes | Create a watch party room |
| `/join-watch-party` | | Join a room by invite code |
| `/watch-party/:inviteCode` | Yes | Watch party room |

---

## Cache Strategy

| Cache name | TTL | Notes |
|------------|-----|-------|
| `thumbnails` | 60 minutes | Raw byte array, uses a separate byte-array serializer |
| `videoComments` | 10 minutes | Evicted when a new comment is posted to that video |
| `mapTiles` | 24 hours | Evicted per-tile when a new video is uploaded to that location, and fully evicted nightly at 3:00 AM |
| `mapClusters` | 15 minutes | Fully evicted whenever a new video is uploaded |
| Redis rate limiter | 1-hour window | Per-user comment counter; the key expires after the window |

---

## Notes

- The class name `ApiExcepiton` is a typo carried throughout the codebase from early development. Renaming it requires a project-wide refactor.
- `HLSService` and `TranscodingConsumer` both invoke FFmpeg for HLS-related processing. Consolidating the two is a reasonable future cleanup task.
- Live chat messages exist only in component state — there is no persistent history, and the chat resets on page reload.
- The 30-second maximum video duration is enforced client-side in `UploadVideo.jsx` via the `MAX_VIDEO_SECONDS` constant and can be adjusted there.
- The async thread pool for HLS conversion is configured with a core size of 4 and a maximum of 8 threads, with a queue capacity of 100 tasks.
