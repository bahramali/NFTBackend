CREATE TABLE IF NOT EXISTS customer_address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(128),
    street1 VARCHAR(128) NOT NULL,
    street2 VARCHAR(128),
    postal_code VARCHAR(16) NOT NULL,
    city VARCHAR(64) NOT NULL,
    region VARCHAR(64),
    country_code VARCHAR(2) NOT NULL DEFAULT 'SE',
    phone_number VARCHAR(32),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_customer_address_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_address_user_id ON customer_address (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_address_default
    ON customer_address (user_id)
    WHERE is_default = true;
