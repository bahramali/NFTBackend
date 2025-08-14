ALTER TABLE sensor_data
    DROP CONSTRAINT IF EXISTS ux_data_record_sensor;
ALTER TABLE sensor_data
    RENAME COLUMN value_type TO sensor_type;
ALTER TABLE sensor_data
    DROP COLUMN IF EXISTS sensor_name;
ALTER TABLE sensor_data
    ADD CONSTRAINT ux_data_record_type UNIQUE (record_id, sensor_type);
