-- Index to support latest sensor average queries by sensor type and record
CREATE INDEX IF NOT EXISTS idx_sd_type_record ON sensor_data (sensor_type, record_id);
