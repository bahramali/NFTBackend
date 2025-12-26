-- 1) add columns nullable
ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS order_confirmation_emails BOOLEAN;

ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS pickup_ready_notification BOOLEAN;

-- 2) backfill existing rows
UPDATE app_user
SET order_confirmation_emails = TRUE
WHERE order_confirmation_emails IS NULL;

UPDATE app_user
SET pickup_ready_notification = TRUE
WHERE pickup_ready_notification IS NULL;

-- 3) set defaults for new rows
ALTER TABLE app_user
  ALTER COLUMN order_confirmation_emails SET DEFAULT TRUE;

ALTER TABLE app_user
  ALTER COLUMN pickup_ready_notification SET DEFAULT TRUE;

-- 4) enforce NOT NULL
ALTER TABLE app_user
  ALTER COLUMN order_confirmation_emails SET NOT NULL;

ALTER TABLE app_user
  ALTER COLUMN pickup_ready_notification SET NOT NULL;
