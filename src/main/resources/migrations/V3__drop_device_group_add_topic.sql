ALTER TABLE device
    DROP COLUMN IF EXISTS group_id,
    ADD COLUMN IF NOT EXISTS topic VARCHAR(64);

UPDATE device SET topic = 'growSensors' WHERE topic IS NULL;

ALTER TABLE device
    ALTER COLUMN topic SET NOT NULL;

DROP TABLE IF EXISTS device_group;
