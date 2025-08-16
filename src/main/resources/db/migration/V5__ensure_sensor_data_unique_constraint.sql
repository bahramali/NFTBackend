-- Remove duplicates before adding the constraint
WITH dup AS (
    SELECT ctid FROM (
        SELECT ctid,
               row_number() OVER (PARTITION BY record_id, sensor_type ORDER BY ctid) AS rn
        FROM sensor_data
    ) s WHERE s.rn > 1
)
DELETE FROM sensor_data WHERE ctid IN (SELECT ctid FROM dup);

-- Remove rows with empty sensor_type before enforcing constraint
DELETE FROM sensor_data WHERE sensor_type = '';

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
