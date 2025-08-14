ALTER TABLE sensor_data
    RENAME COLUMN sensor_name TO sensor_type;
ALTER TABLE sensor_data
    ADD COLUMN sensor_name VARCHAR(64);
ALTER TABLE sensor_data
    RENAME CONSTRAINT ux_data_record_sensor TO ux_data_record_type;
