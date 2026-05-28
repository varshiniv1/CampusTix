# CampusTix

A production-deployed, full-stack event ticketing platform built with **Spring Boot 4** — featuring real-time WebSocket seat booking, an AI-powered event chatbot, QR code + PDF ticket generation, automated email reminders, waitlist management, Redis caching, and a Ticketmaster-inspired UI.

🚀 **Live:** [campustix-production.up.railway.app](https://campustix-production.up.railway.app)
📖 **API Docs:** [/swagger-ui.html](https://campustix-production.up.railway.app/swagger-ui.html)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 23 |
| Database | PostgreSQL 18 (Railway) |
| Cache / Rate Limiting | Redis 7 (Railway) |
| ORM | Hibernate 7 / Spring Data JPA |
| DB Migrations | Flyway |
| Real-time | WebSocket — STOMP over SockJS |
| AI | Groq API — LLaMA 3.1 8B Instant |
| QR Codes | ZXing (Google) |
| PDF Tickets | Apache PDFBox 3 |
| Email | Gmail SMTP (Spring Mail) |
| Security | Spring Security 7 (BCrypt, session-based admin auth) |
| Frontend | Thymeleaf + Tailwind CSS |
| Observability | Spring Actuator + Micrometer + Prometheus |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Deployment | Railway (Dockerfile) |

---

## Features

- **Ticketmaster-style UI** — event grid with search, category filters, and price sorting
- **Real-time seat booking** — WebSocket pushes booking result directly to user's browser
- **Optimistic locking** — prevents double-booking the same seat under concurrent requests
- **Redis rate limiting** — 1 booking attempt per 5 seconds per user
- **QR code tickets** — embedded in confirmation email and downloadable as PDF
- **PDF ticket download** — styled ticket with event details, seat, attendee info, and QR code
- **AI event chatbot** — answers user questions about a specific event via Groq LLaMA 3.1
- **AI description generator** — auto-generates compelling event descriptions for admins
- **Circuit breaker** — AI calls protected by Resilience4j (opens after 50% failures, resets after 30s)
- **Waitlist** — users join a queue when sold out; first in queue notified by email + WebSocket on cancellation
- **Automated reminders** — scheduled job sends 24-hour and 1-hour email reminders before events
- **Duplicate booking prevention** — users cannot book the same event twice
- **Admin dashboard** — create events, auto-generate seats, view analytics (bookings, revenue, waitlist)
- **Flyway migrations** — versioned schema (V1–V4)

---

## Running Locally

### Prerequisites
- Java 23+
- Maven
- Docker Desktop (for PostgreSQL + Redis)

### 1. Start infrastructure

```bash
docker run -d --name campustix-postgres \
  -e POSTGRES_DB=campustix -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password \
  -p 5433:5432 postgres:15

docker run -d --name campustix-redis \
  -p 6379:6379 redis:7
```

### 2. Set environment variables (optional — all have dev defaults)

```bash
export MAIL_USERNAME=your@gmail.com
export MAIL_PASSWORD=your-16-char-app-password
export AI_API_KEY=your-groq-api-key
```

### 3. Run

```bash
./mvnw spring-boot:run
```

App: **http://localhost:8080** | Swagger: **http://localhost:8080/swagger-ui.html**

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/campustix` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `user` | PostgreSQL username |
| `DB_PASSWORD` | `password` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password |
| `MAIL_USERNAME` | — | Gmail sender address |
| `MAIL_PASSWORD` | — | Gmail app password (16 chars) |
| `AI_BASE_URL` | `https://api.groq.com/openai/v1` | LLM API base URL |
| `AI_API_KEY` | — | Groq API key |
| `AI_MODEL` | `llama-3.1-8b-instant` | LLM model name |
| `ADMIN_USERNAME` | `username` | Admin login |
| `ADMIN_PASSWORD` | `password` | Admin password |

---

## Pages

| Route | Description |
|---|---|
| `GET /` | Home — Ticketmaster-style event grid |
| `GET /booking/{eventId}` | Seat selector + live booking + AI chat |
| `GET /login` | Login / Register |
| `GET /my-tickets` | View bookings, download PDF ticket |
| `GET /admin` | Admin panel — create events, analytics |

---

## REST API

Full interactive docs at `/swagger-ui.html`.

### Auth — `/api/v1/auth`
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/register` | Register a new account |
| `POST` | `/login` | Login |

### Events — `/api/v1/events`
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/search` | Search + filter (q, category, minPrice, maxPrice, sort) |

### Admin — `/api/v1/admin` *(requires admin session)*
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/login` | Admin login |
| `GET` | `/events/all` | List all events |
| `GET` | `/events/{id}` | Get event by ID (Redis cached) |
| `POST` | `/events?seatCount=N` | Create event + generate N seats |
| `GET` | `/analytics` | Bookings, revenue, waitlist, top events |

### Tickets — `/api/v1/tickets`
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/seats?eventId=N` | Get seats for an event |
| `POST` | `/claim` | Claim a seat (rate-limited, async, WebSocket result) |

### Bookings — `/api/v1/bookings`
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/by-email?email=` | Get all bookings for an email |
| `POST` | `/cancel/{id}` | Cancel booking (frees seat, triggers waitlist) |

### Waitlist — `/api/v1/waitlist`
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/join` | Join waitlist |
| `GET` | `/position` | Check queue position |

### AI — `/api/v1/ai`
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/describe` | Generate event description |
| `POST` | `/chat` | Ask AI about an event |

### PDF Ticket — `/api/v1/ticket`
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/download/{bookingRef}` | Download styled PDF ticket |

---

## Booking Flow

```
User selects seat → POST /api/v1/tickets/claim
        │
        ├─ Redis rate limit (5s window per email)
        ├─ Duplicate booking check
        ├─ Mark seat SOLD (optimistic locking)
        ├─ Generate QR code (ZXing 300×300 PNG → base64)
        ├─ Save Booking (ref: CT-XXXXXXXX)
        ├─ Send confirmation email (HTML + embedded QR)
        └─ WebSocket push → /topic/status/{base64(email)}
                    + broadcast /topic/seats-update
```

---

## Waitlist Flow

```
Sold out → User joins waitlist
Someone cancels → notifyNext(eventId)
        └─ First in queue: email notification + WebSocket refresh
```

---

## Reminder Scheduler

Every 30 minutes:
- Events in 23–25 hours → **24-hour reminder** email
- Events in 45–75 minutes → **1-hour reminder** email

---

## WebSocket

Connect to `/ws-tickets` (SockJS + STOMP).

| Topic | Description |
|---|---|
| `/topic/status/{base64(email)}` | Personal booking result |
| `/topic/seats-update` | Broadcast seat refresh |

---

## Database Schema

```
users     — id, name, email, password, role, created_at
events    — id, name, venue, venue_address, event_time, expiry_date,
            price, image_url, contact_info, description, category
seat      — id, event_id, seat_number, status, version
bookings  — id, user_id, buyer_email, buyer_name, seat_id, event_id,
            booking_reference, booked_at, status, qr_code_base64,
            payment_intent_id, reminded_24h, reminded_1h
waitlist  — id, event_id, buyer_email, buyer_name, added_at, notified
```

---

## Project Structure

```
src/main/java/com/university/campustix/
├── config/       SecurityConfig, WebSocketConfig, CacheConfig, OpenApiConfig
├── controller/   Auth, Booking, Event, Ticket, Payment, Waitlist, AI, Admin, View
├── dto/          Request/Response DTOs
├── model/        Event, Seat, Booking, User, Waitlist
├── repository/   JPA repositories
└── service/      Booking, Email, QRCode, PdfTicket, AI, Waitlist,
                  Reminder, RateLimiting, Payment, User
```

---

## Deployment

Hosted on **Railway** — three services: CampusTix app, PostgreSQL, Redis.

Multi-stage Dockerfile: Maven build → `eclipse-temurin:23-jre-alpine` runtime.
JVM: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75`

Actuator endpoints: `health`, `info`, `prometheus`, `metrics` — tagged `application: campustix`
