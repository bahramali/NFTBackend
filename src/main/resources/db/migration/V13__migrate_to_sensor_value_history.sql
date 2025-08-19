-- Migrate existing sensor data and drop legacy tables

-- Populate sensor_value_history from old sensor_record/sensor_reading tables
INSERT INTO sensor_value_history (composite_id, sensor_type, sensor_value, value_time)
SELECT sr.device_composite_id, sd.sensor_type, sd.sensor_value, sr.record_time
FROM sensor_record sr
JOIN sensor_reading sd ON sd.record_id = sr.id;

-- Drop legacy columns no longer needed
ALTER TABLE sensor_value_history DROP COLUMN IF EXISTS unit;
ALTER TABLE sensor_value_history DROP COLUMN IF EXISTS health_ok;
ALTER TABLE sensor_value_history DROP COLUMN IF EXISTS meta;

-- Remove obsolete tables
DROP TABLE IF EXISTS sensor_health_item;
DROP TABLE IF EXISTS sensor_reading;
DROP TABLE IF EXISTS sensor_record;
