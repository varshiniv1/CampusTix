# CampusTix

A full-stack event ticketing platform built with Spring Boot 4, featuring real-time seat booking, AI-generated event descriptions, QR code tickets, and a Ticketmaster-style UI.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 17 |
| Database | PostgreSQL 15 (Docker) |
| Cache | Redis 7 (Docker) |
| Messaging | Apache Kafka 3.7 KRaft (Docker) |
| ORM | Hibernate / Spring Data JPA |
| Real-time | WebSocket (STOMP over SockJS) |
| AI | Unsloth Studio — Qwen2.5-7B-Instruct (local LLM, OpenAI-compatible API) |
| QR Codes | ZXing |
| Email | Gmail SMTP |
| Frontend | Thymeleaf + Tailwind CSS |
| DB Admin | pgAdmin 4 |

---

## Running Locally

### Prerequisites
- Docker Desktop
- Java 17+
- Maven
- Unsloth Studio running on port 8888 (for AI features — optional, degrades gracefully)

### 1. Start infrastructure

```bash
docker-compose up -d
```

| Service | URL |
|---|---|
| PostgreSQL | localhost:5433 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |
| Kafka UI | http://localhost:8081 |
| pgAdmin | http://localhost:5050 |

pgAdmin login: `admin@admin.com` / `password`
Connect to DB with host `postgres`, port `5432`, user `user`, password `password`.

### 2. Start the application

```bash
./mvnw spring-boot:run
```

App runs at **http://localhost:8080**

---

## Environment Variables

All variables have dev-safe defaults so the app runs without any extra config.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/campustix` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `user` | PostgreSQL username |
| `DB_PASSWORD` | `password` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `MAIL_USERNAME` | `pcmbwow@gmail.com` | Gmail address for sending tickets |
| `MAIL_PASSWORD` | *(set this)* | Gmail app password (16 chars) |
| `AI_BASE_URL` | `http://127.0.0.1:8888` | Unsloth Studio base URL |
| `AI_API_KEY` | `sk-unsloth-...` | Unsloth Studio API key |
| `AI_MODEL` | `unsloth/Qwen2.5-7B-Instruct-GGUF` | Model name |

---

## Pages

| Route | Description |
|---|---|
| `GET /` | Home — Ticketmaster-style event grid with search and category filters |
| `GET /events` | Events listing page |
| `GET /booking/{eventId}` | Seat selector + real-time booking + AI chat widget |
| `GET /login` | Login / Register (any email) |
| `GET /my-tickets` | View tickets by email, download QR code |
| `GET /admin` | Create events, generate AI descriptions, manage seats |

---

## REST API

### Auth — `/api/v1/auth`

| Method | Endpoint | Body | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `{ name, email, password }` | Register a new account |
| `POST` | `/api/v1/auth/login` | `{ email, password }` | Login, returns `{ name, email }` |

### Events — `/api/v1/admin`

| Method | Endpoint | Params | Description |
|---|---|---|---|
| `GET` | `/api/v1/admin/events/all` | — | List all events |
| `GET` | `/api/v1/admin/events/{id}` | — | Get a single event by ID |
| `POST` | `/api/v1/admin/events?seatCount=N` | Body: Event JSON | Create event + auto-generate N seats |

**Event JSON body:**
```json
{
  "name": "Spring Concert",
  "venue": "Main Auditorium",
  "eventTime": "2025-09-01T19:00:00",
  "description": "An evening of live music.",
  "imageUrl": "https://...",
  "price": 15.00,
  "category": "MUSIC"
}
```
Categories: `MUSIC`, `SPORTS`, `COMEDY`, `ARTS`, `FAMILY`, `OTHER`

### Tickets — `/api/v1/tickets`

| Method | Endpoint | Params | Description |
|---|---|---|---|
| `GET` | `/api/v1/tickets/seats` | `eventId` | Get all seats for an event |
| `POST` | `/api/v1/tickets/claim` | `studentId` (email), `seatId`, `studentName` | Claim a seat — queued via Kafka |

### Bookings — `/api/v1/bookings`

| Method | Endpoint | Params | Description |
|---|---|---|---|
| `GET` | `/api/v1/bookings/by-email` | `email` | Get all bookings for an email address |

### AI — `/api/v1/ai`

| Method | Endpoint | Body | Description |
|---|---|---|---|
| `POST` | `/api/v1/ai/describe` | `{ eventName, venue, category, price }` | Generate a 2–3 sentence event description |
| `POST` | `/api/v1/ai/chat` | `{ eventId, message }` | Ask the AI assistant about a specific event |

Requires Unsloth Studio running locally. Returns a fallback message if unavailable.

---

## WebSocket

Connect to `/ws-tickets` via SockJS/STOMP.

| Topic | Description |
|---|---|
| `/topic/status/{base64(email)}` | Booking result for a specific user — `SUCCESS: ...` or `FAILED: ...` |
| `/topic/seats-update` | Broadcast trigger to refresh seat availability |

The channel suffix is the URL-safe Base64 encoding of the user's email (no padding). Both server and client compute the same value.

---

## Booking Flow

1. User selects a seat on `/booking/{eventId}`
2. Frontend calls `POST /api/v1/tickets/claim`
3. Request is rate-limited via Redis, then published to Kafka
4. Kafka consumer processes the message:
   - Locks the seat with optimistic locking
   - Creates a `Booking` record
   - Generates a QR code (ZXing, base64 PNG)
   - Sends a confirmation email with the embedded QR code
   - Pushes a WebSocket message to the user's personal channel
5. Frontend receives the WebSocket message and shows the result in real time

---

## Project Structure

```
src/main/java/com/university/campustix/
├── config/          # SecurityConfig, WebSocketConfig, KafkaConfig
├── controller/      # REST controllers + Thymeleaf view controllers
├── dto/             # Request/Response DTOs
├── model/           # JPA entities: Event, Seat, Booking, User
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic: Kafka, Email, QR, AI, Rate limiting
```
