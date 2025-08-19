CREATE TABLE IF NOT EXISTS sensor_value (
    ts TIMESTAMPTZ NOT NULL,
    composite_id VARCHAR(128) NOT NULL,
    sensor_name VARCHAR(64) NOT NULL,
    value DOUBLE PRECISION,
    unit VARCHAR(32),
    health_ok BOOLEAN,
    meta JSONB,
    CONSTRAINT fk_sv_device FOREIGN KEY (composite_id) REFERENCES device(composite_id)
);

CREATE INDEX IF NOT EXISTS idx_sv_device_sensor_ts ON sensor_value (composite_id, sensor_name, ts DESC);

-- Optional index to filter by parts of the composite_id (e.g., system and layer)
CREATE INDEX IF NOT EXISTS idx_sv_system_layer ON sensor_value (
    split_part(composite_id, '-', 1),
    split_part(composite_id, '-', 2)
);
