ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS email_verified boolean default false not null;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS picture_url varchar(512);

CREATE TABLE IF NOT EXISTS user_identity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    provider varchar(32) NOT NULL,
    provider_subject varchar(255) NOT NULL,
    provider_email varchar(255),
    created_at timestamp not null,
    CONSTRAINT ux_user_identity_provider_subject UNIQUE (provider, provider_subject)
);
