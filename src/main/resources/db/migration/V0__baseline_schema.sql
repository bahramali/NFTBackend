CREATE TABLE IF NOT EXISTS device (
    composite_id VARCHAR(128) PRIMARY KEY,
    system VARCHAR(16) NOT NULL,
    rack VARCHAR(16) NOT NULL,
    layer VARCHAR(16) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    name VARCHAR(128),
    owner_user_id BIGINT,
    topic VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_device_system_rack_layer ON device (system, rack, layer);
CREATE INDEX IF NOT EXISTS ix_device_device_id ON device (device_id);

CREATE TABLE IF NOT EXISTS latest_sensor_value (
    id BIGSERIAL PRIMARY KEY,
    composite_id VARCHAR(128) NOT NULL,
    sensor_type VARCHAR(64) NOT NULL,
    sensor_value DOUBLE PRECISION,
    unit VARCHAR(32),
    value_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_latest_sensor_value_device
        FOREIGN KEY (composite_id) REFERENCES device (composite_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lsv_device_sensor ON latest_sensor_value (composite_id, sensor_type);

CREATE TABLE IF NOT EXISTS actuator_status (
    id BIGSERIAL PRIMARY KEY,
    status_time TIMESTAMP NOT NULL,
    actuator_type VARCHAR(255) NOT NULL,
    state BOOLEAN NOT NULL,
    composite_id VARCHAR(128) NOT NULL,
    CONSTRAINT fk_actuator_status_device
        FOREIGN KEY (composite_id) REFERENCES device (composite_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_as_device_time ON actuator_status (composite_id, status_time DESC);

CREATE TABLE IF NOT EXISTS germination_cycle (
    composite_id VARCHAR(128) PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_germination_cycle_device
        FOREIGN KEY (composite_id) REFERENCES device (composite_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sensor_value_history (
    value_time TIMESTAMP WITH TIME ZONE NOT NULL,
    system_part VARCHAR(64),
    layer_part VARCHAR(64),
    composite_id VARCHAR(128) NOT NULL,
    sensor_type VARCHAR(64) NOT NULL,
    sensor_value DOUBLE PRECISION,
    PRIMARY KEY (value_time, composite_id, sensor_type)
);

CREATE INDEX IF NOT EXISTS idx_svh_device_sensor_time
    ON sensor_value_history (composite_id, sensor_type, value_time DESC);
CREATE INDEX IF NOT EXISTS idx_svh_device_time
    ON sensor_value_history (composite_id, value_time);
CREATE INDEX IF NOT EXISTS idx_svh_system_layer
    ON sensor_value_history (system_part, layer_part);

CREATE TABLE IF NOT EXISTS sensor_config (
    id BIGSERIAL PRIMARY KEY,
    sensor_type VARCHAR(64) NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    description VARCHAR(512),
    CONSTRAINT ux_sensor_config_sensor_type UNIQUE (sensor_type)
);

CREATE TABLE IF NOT EXISTS water_flow_status (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    status_time TIMESTAMP NOT NULL,
    source VARCHAR(255),
    sensor_type VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS note (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    date TIMESTAMP NOT NULL,
    content TEXT NOT NULL
);
