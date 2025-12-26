CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(128) NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    invited BOOLEAN NOT NULL DEFAULT false,
    invited_at TIMESTAMP,
    invite_token_hash VARCHAR(128),
    invite_expires_at TIMESTAMP,
    invite_used_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_app_user_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS app_user_permissions (
    user_id BIGINT NOT NULL,
    permission VARCHAR(64)
);
