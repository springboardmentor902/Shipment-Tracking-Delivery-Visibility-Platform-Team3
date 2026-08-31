-- ShipTrack Pro — Milestone 5: alerts and per-user alert settings
--
-- Hibernate (ddl-auto=update) creates these tables automatically; this script is
-- here so the schema can also be applied by hand:
--
--   psql -U postgres -d shiptrack_db -f shiptrack-pro/db/migration/V5__notifications.sql

CREATE TABLE IF NOT EXISTS notifications (
    id            BIGSERIAL PRIMARY KEY,
    recipient_id  BIGINT        NOT NULL,
    shipment_id   BIGINT,
    type          VARCHAR(32)   NOT NULL,
    severity      VARCHAR(16)   NOT NULL DEFAULT 'INFO',
    title         VARCHAR(200)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    dedupe_key    VARCHAR(200),
    read_flag     BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMP,
    email_sent_at TIMESTAMP,
    sms_sent_at   TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_shipment  FOREIGN KEY (shipment_id)  REFERENCES shipments (id) ON DELETE CASCADE
);

-- the bell reads the newest first, per user
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notifications (recipient_id, created_at DESC);
-- the cooldown check looks up recipient + event key
CREATE INDEX IF NOT EXISTS idx_notification_dedupe ON notifications (recipient_id, dedupe_key);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT      NOT NULL,
    in_app_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    email_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    -- SMS costs money and interrupts people, so it stays opt-in
    sms_enabled          BOOLEAN     NOT NULL DEFAULT FALSE,
    notify_status_change BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_delay_risk    BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_delivery      BOOLEAN     NOT NULL DEFAULT TRUE,
    min_risk_level       VARCHAR(16) NOT NULL DEFAULT 'HIGH',
    updated_at           TIMESTAMP,
    CONSTRAINT uk_notification_pref_user UNIQUE (user_id),
    CONSTRAINT fk_notification_pref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
