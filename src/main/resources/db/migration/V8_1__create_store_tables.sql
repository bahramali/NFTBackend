CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    inventory_qty INTEGER NOT NULL,
    image_url VARCHAR(255),
    category VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE carts (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE store_orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    shipping_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    total_amount_cents BIGINT,
    currency VARCHAR(3) NOT NULL,
    ship_name VARCHAR(255) NOT NULL,
    ship_line1 VARCHAR(255) NOT NULL,
    ship_line2 VARCHAR(255),
    ship_city VARCHAR(255) NOT NULL,
    ship_state VARCHAR(255),
    ship_postal VARCHAR(255) NOT NULL,
    ship_country VARCHAR(255) NOT NULL,
    ship_phone VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    qty INTEGER NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    line_total_cents BIGINT NOT NULL
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES store_orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    name_snapshot VARCHAR(255) NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    qty INTEGER NOT NULL,
    line_total_cents BIGINT NOT NULL
);
