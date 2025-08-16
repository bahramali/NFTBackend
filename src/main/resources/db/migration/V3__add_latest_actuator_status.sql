CREATE TABLE latest_actuator_status (
    id BIGSERIAL PRIMARY KEY,
    composite_id VARCHAR(128) NOT NULL,
    actuator_type VARCHAR(64) NOT NULL,
    state BOOLEAN NOT NULL,
    status_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_las_device FOREIGN KEY (composite_id) REFERENCES device(composite_id),
    CONSTRAINT ux_las_device_actuator UNIQUE (composite_id, actuator_type)
);

CREATE INDEX ix_las_actuator_device ON latest_actuator_status (actuator_type, composite_id);
