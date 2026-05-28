-- V2: Waitlist table for sold-out events
-- Users on the waitlist are notified via email + WebSocket when a seat opens up

CREATE TABLE IF NOT EXISTS waitlist (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES events(id),
    buyer_email VARCHAR(255) NOT NULL,
    buyer_name  VARCHAR(255),
    added_at    TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    notified    BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_waitlist_event_notified ON waitlist(event_id, notified);
