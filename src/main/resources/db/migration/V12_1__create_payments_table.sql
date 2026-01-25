CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES store_orders(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    method VARCHAR(255),
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_payment_id VARCHAR(255) NOT NULL,
    provider_reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_payments_provider_payment_id UNIQUE (provider, provider_payment_id)
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
