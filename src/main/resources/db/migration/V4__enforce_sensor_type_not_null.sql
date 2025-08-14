-- Ensure sensor_type is always provided.
ALTER TABLE sensor_data
    ALTER COLUMN sensor_type SET NOT NULL;
