-- ShipTrack Pro — Milestone 1 schema change: one shipment -> many route legs
--
-- Hibernate runs with ddl-auto=update, which ADDS new columns but never drops
-- the old one-to-one unique constraint on routes.shipment_id and never
-- backfills leg_number. Run this script once against shiptrack_db before
-- starting the application with the new code:
--
--   psql -U postgres -d shiptrack_db -f shiptrack-pro/db/migration/V2__multi_leg_routes.sql
--
-- It is written to be safe to run more than once.

BEGIN;

-- 1. drop the old one-shipment-one-route uniqueness (name varies by generator)
DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'routes'
          AND con.contype = 'u'
          AND con.conname <> 'uk_routes_shipment_leg'
    LOOP
        EXECUTE format('ALTER TABLE routes DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

-- 2. new columns (idempotent)
ALTER TABLE routes ADD COLUMN IF NOT EXISTS leg_number                   INTEGER;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS driver_id                    BIGINT;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS origin_latitude              NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS origin_longitude             NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS destination_latitude         NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS destination_longitude        NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS duration_in_traffic_minutes  INTEGER;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS traffic_condition            VARCHAR(64);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS last_known_latitude          NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS last_known_longitude         NUMERIC(9, 6);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS last_location_at             TIMESTAMP;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS status                       VARCHAR(16);
ALTER TABLE routes ADD COLUMN IF NOT EXISTS notes                        VARCHAR(500);

-- 3. backfill existing rows: every old route becomes leg 1
UPDATE routes SET leg_number = 1 WHERE leg_number IS NULL;
UPDATE routes SET status = 'PLANNED' WHERE status IS NULL;

ALTER TABLE routes ALTER COLUMN leg_number SET NOT NULL;
ALTER TABLE routes ALTER COLUMN status SET NOT NULL;

-- 4. distance and duration are now optional: they are filled by the
--    Google Maps directions lookup, which may not have run yet
ALTER TABLE routes ALTER COLUMN distance_km DROP NOT NULL;
ALTER TABLE routes ALTER COLUMN expected_duration_minutes DROP NOT NULL;

-- 5. new keys, indexes and the driver relation
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_routes_shipment_leg') THEN
        ALTER TABLE routes ADD CONSTRAINT uk_routes_shipment_leg UNIQUE (shipment_id, leg_number);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_routes_driver') THEN
        ALTER TABLE routes ADD CONSTRAINT fk_routes_driver
            FOREIGN KEY (driver_id) REFERENCES users (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_routes_shipment ON routes (shipment_id);
CREATE INDEX IF NOT EXISTS idx_routes_driver   ON routes (driver_id);

COMMIT;
