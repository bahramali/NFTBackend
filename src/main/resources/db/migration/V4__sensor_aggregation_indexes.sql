-- Index supporting sensorType filtering and non-null values
CREATE INDEX IF NOT EXISTS ix_sensor_data_type_record_notnull
    ON sensor_data (sensor_type, record_id)
    WHERE sensor_value IS NOT NULL;

-- Expression index for common 1 minute bucket to speed up aggregation
-- Only create when TimescaleDB is available
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        CREATE INDEX IF NOT EXISTS ix_record_device_time_bucket_1m
            ON sensor_record (device_composite_id, time_bucket('1 minute', record_time));
    END IF;
END$$;
