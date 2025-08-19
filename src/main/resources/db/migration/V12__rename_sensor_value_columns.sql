ALTER TABLE sensor_value RENAME TO sensor_value_history;
ALTER TABLE sensor_value_history RENAME COLUMN sensor_name TO sensor_type;
ALTER TABLE sensor_value_history RENAME COLUMN ts TO value_time;
ALTER TABLE sensor_value_history RENAME COLUMN value TO sensor_value;
ALTER TABLE sensor_value_history RENAME CONSTRAINT fk_sv_device TO fk_svh_device;
ALTER INDEX idx_sv_device_sensor_ts RENAME TO idx_svh_device_sensor_time;
ALTER INDEX idx_sv_system_layer RENAME TO idx_svh_system_layer;
