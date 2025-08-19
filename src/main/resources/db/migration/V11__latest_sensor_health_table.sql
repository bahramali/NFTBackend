CREATE TABLE IF NOT EXISTS latest_sensor_health (
    composite_id VARCHAR(128) NOT NULL,
    sensor_name VARCHAR(64) NOT NULL,
    sensor_value DOUBLE PRECISION,
    unit VARCHAR(32),
    health_ok BOOLEAN,
    ts TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_latest_sensor_health PRIMARY KEY (composite_id, sensor_name),
    CONSTRAINT fk_lsh_device FOREIGN KEY (composite_id) REFERENCES device(composite_id)
);
