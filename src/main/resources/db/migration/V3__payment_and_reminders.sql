-- V3: Stripe payment tracking + scheduled reminder flags on bookings

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS reminded_24h    BOOLEAN DEFAULT FALSE;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS reminded_1h     BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_bookings_event_reminder
    ON bookings(event_id, reminded_24h, reminded_1h, status);
