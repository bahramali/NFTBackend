-- Ensure unique (record_id, sensor_type) pairs in sensor_data
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ux_data_record_type'
    ) THEN
        ALTER TABLE sensor_data
            ADD CONSTRAINT ux_data_record_type UNIQUE (record_id, sensor_type);
    END IF;
END$$;
