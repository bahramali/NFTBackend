-- Index to support latest sensor average queries by sensor type and record
CREATE INDEX IF NOT EXISTS idx_sreading_type_record ON sensor_reading (sensor_type, record_id);
