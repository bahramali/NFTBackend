-- Add rack column and backfill existing devices with a safe default.
-- Existing composite_id values are expanded to include rack (UNKNOWN when missing).

ALTER TABLE device ADD COLUMN IF NOT EXISTS rack VARCHAR(16);

UPDATE device
SET rack = split_part(composite_id, '-', 2)
WHERE rack IS NULL
  AND composite_id ~ '^[^-]+-[^-]+-[^-]+-[^-]+$';

UPDATE device
SET rack = 'UNKNOWN'
WHERE rack IS NULL;

UPDATE device
SET composite_id = split_part(composite_id, '-', 1)
    || '-' || rack
    || '-' || split_part(composite_id, '-', 2)
    || '-' || split_part(composite_id, '-', 3)
WHERE composite_id ~ '^[^-]+-[^-]+-[^-]+$';

UPDATE latest_sensor_value
SET composite_id = split_part(composite_id, '-', 1)
    || '-UNKNOWN-'
    || split_part(composite_id, '-', 2)
    || '-' || split_part(composite_id, '-', 3)
WHERE composite_id ~ '^[^-]+-[^-]+-[^-]+$';

UPDATE actuator_status
SET composite_id = split_part(composite_id, '-', 1)
    || '-UNKNOWN-'
    || split_part(composite_id, '-', 2)
    || '-' || split_part(composite_id, '-', 3)
WHERE composite_id ~ '^[^-]+-[^-]+-[^-]+$';

UPDATE germination_cycle
SET composite_id = split_part(composite_id, '-', 1)
    || '-UNKNOWN-'
    || split_part(composite_id, '-', 2)
    || '-' || split_part(composite_id, '-', 3)
WHERE composite_id ~ '^[^-]+-[^-]+-[^-]+$';

UPDATE sensor_value_history
SET composite_id = split_part(composite_id, '-', 1)
    || '-UNKNOWN-'
    || split_part(composite_id, '-', 2)
    || '-' || split_part(composite_id, '-', 3)
WHERE composite_id ~ '^[^-]+-[^-]+-[^-]+$';

UPDATE sensor_value_history
SET system_part = split_part(composite_id, '-', 1),
    layer_part = split_part(composite_id, '-', 3)
WHERE composite_id IS NOT NULL;

ALTER TABLE device ALTER COLUMN rack SET NOT NULL;

ALTER TABLE device DROP CONSTRAINT IF EXISTS ux_device_system_layer_deviceid;
ALTER TABLE device
    ADD CONSTRAINT ux_device_system_rack_layer_deviceid
    UNIQUE (system, rack, layer, device_id);

DROP INDEX IF EXISTS ix_device_system_layer;
CREATE INDEX IF NOT EXISTS ix_device_system_rack_layer ON device (system, rack, layer);
