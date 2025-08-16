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
