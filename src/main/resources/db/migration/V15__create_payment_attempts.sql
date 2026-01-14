CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    stripe_session_id VARCHAR(255) NOT NULL,
    cart_id UUID NOT NULL,
    user_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_payment_attempts_stripe_session UNIQUE (stripe_session_id)
);

CREATE INDEX idx_payment_attempts_cart_id ON payment_attempts(cart_id);
CREATE INDEX idx_payment_attempts_user_id ON payment_attempts(user_id);

ALTER TABLE payment_attempts
    ADD CONSTRAINT payment_attempts_status_check
    CHECK (status IN ('CREATED'));
