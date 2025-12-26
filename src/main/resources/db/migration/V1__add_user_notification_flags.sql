ALTER TABLE app_user
    ADD COLUMN order_confirmation_emails boolean NOT NULL DEFAULT true;
ALTER TABLE app_user
    ADD COLUMN pickup_ready_notification boolean NOT NULL DEFAULT true;

ALTER TABLE app_user
    ALTER COLUMN order_confirmation_emails DROP DEFAULT;
ALTER TABLE app_user
    ALTER COLUMN pickup_ready_notification DROP DEFAULT;
