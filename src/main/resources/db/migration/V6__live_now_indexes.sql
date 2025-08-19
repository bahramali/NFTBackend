-- Index supporting device and time lookups on sensor_record
CREATE INDEX IF NOT EXISTS idx_sr_device_ts ON sensor_record (device_composite_id, record_time DESC);

-- Index for efficient sensor_reading lookups by record and type
CREATE INDEX IF NOT EXISTS idx_sreading_record_type ON sensor_reading (record_id, sensor_type);

-- Index supporting device and actuator type queries ordered by timestamp
CREATE INDEX IF NOT EXISTS idx_act_device_type_ts ON actuator_status (composite_id, actuator_type, status_time DESC);

-- Index to speed up device queries by system and layer
CREATE INDEX IF NOT EXISTS idx_device_system_layer ON device (system, layer);
