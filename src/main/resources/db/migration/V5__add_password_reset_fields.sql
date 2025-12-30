ALTER TABLE app_user
    ADD COLUMN password_reset_token_hash VARCHAR(255),
    ADD COLUMN password_reset_expires_at TIMESTAMP,
    ADD COLUMN password_reset_used_at TIMESTAMP;
