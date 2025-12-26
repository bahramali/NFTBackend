-- Minimal legacy schema needed so Flyway migrations that ALTER app_user can run
-- Keep this intentionally minimal: only columns needed to create a legacy row with id=1.

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY
);

-- Seed an existing user so the migration can backfill flags for "existing users"
INSERT INTO app_user (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;
