CREATE TABLE IF NOT EXISTS latest_sensor_value (
    id BIGSERIAL PRIMARY KEY,
    composite_id VARCHAR(128) NOT NULL,
    sensor_type VARCHAR(64) NOT NULL,
    sensor_value DOUBLE PRECISION,
    unit VARCHAR(32),
    value_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lsv_device FOREIGN KEY (composite_id) REFERENCES device(composite_id),
    CONSTRAINT ux_lsv_device_sensor UNIQUE (composite_id, sensor_type)
);

CREATE INDEX IF NOT EXISTS ix_lsv_sensor_device ON latest_sensor_value (sensor_type, composite_id);

-- Maintain latest_sensor_value whenever sensor_data is inserted or updated
CREATE OR REPLACE FUNCTION upsert_latest_sensor_value()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO latest_sensor_value (composite_id, sensor_type, sensor_value, unit, value_time)
    SELECT sr.device_composite_id, NEW.sensor_type, NEW.sensor_value, NEW.unit, sr.record_time
    FROM sensor_record sr
    WHERE sr.id = NEW.record_id
    ON CONFLICT (composite_id, sensor_type) DO UPDATE
        SET sensor_value = EXCLUDED.sensor_value,
            unit = EXCLUDED.unit,
            value_time = EXCLUDED.value_time;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_upsert_latest_sensor_value
AFTER INSERT OR UPDATE ON sensor_data
FOR EACH ROW EXECUTE FUNCTION upsert_latest_sensor_value();
