ALTER TABLE monitoring_page
    ADD COLUMN IF NOT EXISTS telemetry_rack_id VARCHAR;

CREATE INDEX IF NOT EXISTS ix_monitoring_page_telemetry_rack_id
    ON monitoring_page (telemetry_rack_id);
