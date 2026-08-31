-- ShipTrack Pro — Milestone 4: delivery forecast and delay risk
--
-- One row per shipment, overwritten on every recalculation.
--
-- Hibernate (ddl-auto=update) creates this table automatically; this script is
-- here so the schema can also be applied by hand:
--
--   psql -U postgres -d shiptrack_db -f shiptrack-pro/db/migration/V4__eta_predictions.sql

CREATE TABLE IF NOT EXISTS eta_predictions (
    id                     BIGSERIAL PRIMARY KEY,
    shipment_id            BIGINT       NOT NULL,
    predicted_delivery_at  TIMESTAMP,
    promised_delivery_date DATE,
    expected_delay_minutes INTEGER,
    delay_risk_score       INTEGER      NOT NULL DEFAULT 0,
    risk_level             VARCHAR(16)  NOT NULL DEFAULT 'LOW',
    confidence_score       INTEGER      NOT NULL DEFAULT 0,
    factors                VARCHAR(1000),
    source                 VARCHAR(32),
    calculated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_eta_shipment UNIQUE (shipment_id),
    CONSTRAINT fk_eta_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE
);

-- the at-risk screens read by score, newest risk first
CREATE INDEX IF NOT EXISTS idx_eta_risk_score ON eta_predictions (delay_risk_score DESC);
