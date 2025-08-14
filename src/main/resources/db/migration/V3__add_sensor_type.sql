-- Populate missing sensor types for existing records before
-- enforcing the NOT NULL constraint in a later migration.
UPDATE sensor_data
SET sensor_type = 'unknown'
WHERE sensor_type IS NULL;
