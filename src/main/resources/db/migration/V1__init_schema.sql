-- V1: Initial schema — matches JPA entities created by ddl-auto:update
-- Flyway baselines existing databases at this version via baseline-on-migrate=true

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(255) DEFAULT 'USER',
    created_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS events (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255),
    venue         VARCHAR(255),
    venue_address VARCHAR(255),
    event_time    TIMESTAMP(6),
    expiry_date   TIMESTAMP(6),
    price         FLOAT8,
    image_url     VARCHAR(255),
    contact_info  VARCHAR(255),
    description   VARCHAR(255),
    category      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS seat (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT REFERENCES events(id),
    seat_number VARCHAR(255),
    status      VARCHAR(255),
    version     BIGINT
);

CREATE TABLE IF NOT EXISTS bookings (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT REFERENCES users(id),
    buyer_email       VARCHAR(255),
    buyer_name        VARCHAR(255),
    seat_id           BIGINT REFERENCES seat(id),
    event_id          BIGINT REFERENCES events(id),
    booking_reference VARCHAR(255),
    booked_at         TIMESTAMP(6),
    status            VARCHAR(255),
    qr_code_base64    TEXT
);
