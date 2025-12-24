DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'store_orders'
          AND column_name = 'total_cents'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'store_orders'
          AND column_name = 'total_amount_cents'
    ) THEN
        EXECUTE 'ALTER TABLE store_orders RENAME COLUMN total_cents TO total_amount_cents';
    END IF;
END $$;

ALTER TABLE store_orders
    ADD COLUMN IF NOT EXISTS total_amount_cents BIGINT;

ALTER TABLE store_orders
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'SEK';

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    provider VARCHAR NOT NULL,
    method VARCHAR,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'SEK',
    status VARCHAR NOT NULL,
    provider_payment_id VARCHAR NOT NULL,
    provider_reference VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES store_orders(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_provider_payment_id
    ON payments(provider, provider_payment_id);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments(order_id);
