-- ShipTrack Pro — Milestone 2: record where route metrics came from
--
-- LIVE_MAPS      distance/duration from the Google Directions API
-- STRAIGHT_LINE  Haversine estimate used because Maps was unavailable
-- MANUAL         typed in by an operator
--
-- Hibernate (ddl-auto=update) adds this column automatically; this script is
-- here so the schema can also be applied by hand:
--
--   psql -U postgres -d shiptrack_db -f shiptrack-pro/db/migration/V3__route_metrics_source.sql

ALTER TABLE routes ADD COLUMN IF NOT EXISTS metrics_source VARCHAR(20);

-- existing rows were entered by hand before Maps integration existed
UPDATE routes SET metrics_source = 'MANUAL' WHERE metrics_source IS NULL;
