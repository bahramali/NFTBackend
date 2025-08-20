DROP INDEX IF EXISTS idx_svh_system_layer;

ALTER TABLE sensor_value_history
    ADD COLUMN IF NOT EXISTS system_part VARCHAR(64) GENERATED ALWAYS AS (split_part(composite_id, '-', 1)) STORED;

ALTER TABLE sensor_value_history
    ADD COLUMN IF NOT EXISTS layer_part VARCHAR(64) GENERATED ALWAYS AS (split_part(composite_id, '-', 2)) STORED;

CREATE INDEX IF NOT EXISTS idx_svh_system_layer
    ON sensor_value_history(system_part, layer_part);
