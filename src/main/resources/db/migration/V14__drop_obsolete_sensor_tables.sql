-- Drop obsolete sensor tables no longer used after migration to sensor_value_history
DROP TABLE IF EXISTS latest_sensor_health;
DROP TABLE IF EXISTS sensor_value;
