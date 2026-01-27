CREATE TABLE IF NOT EXISTS device_status_history (
    id BIGSERIAL PRIMARY KEY,
    composite_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_time TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_device_status_history_device
        FOREIGN KEY (composite_id) REFERENCES device (composite_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_device_status_history_device_time
    ON device_status_history (composite_id, status_time DESC);

CREATE TABLE IF NOT EXISTS device_event (
    id BIGSERIAL PRIMARY KEY,
    composite_id VARCHAR(128) NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    level VARCHAR(16),
    code VARCHAR(64),
    msg TEXT,
    raw JSONB,
    CONSTRAINT fk_device_event_device
        FOREIGN KEY (composite_id) REFERENCES device (composite_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_device_event_device_time
    ON device_event (composite_id, event_time DESC);
