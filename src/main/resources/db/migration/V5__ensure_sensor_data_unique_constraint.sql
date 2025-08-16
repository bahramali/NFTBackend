-- Remove duplicates before adding the constraint
DELETE FROM sensor_data a
USING sensor_data b
WHERE a.record_id = b.record_id
  AND a.sensor_type = b.sensor_type
  AND a.ctid < b.ctid;

-- Ensure unique (record_id, sensor_type) pairs in sensor_data
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ux_data_record_type'
    ) THEN
        ALTER TABLE sensor_data
            ADD CONSTRAINT ux_data_record_type UNIQUE (record_id, sensor_type);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_sensor_type_not_empty'
    ) THEN
        ALTER TABLE sensor_data
            ADD CONSTRAINT ck_sensor_type_not_empty CHECK (sensor_type <> '');
    END IF;
END$$;
