ALTER TABLE sensor_data ADD COLUMN sensor_type VARCHAR(64);

-- Populate the new column for existing records.
UPDATE sensor_data SET sensor_type = 'unknown';

ALTER TABLE sensor_data ALTER COLUMN sensor_type SET NOT NULL;
