CREATE INDEX IF NOT EXISTS idx_svh_device_time ON sensor_value_history(composite_id, value_time);
