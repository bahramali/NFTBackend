CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    label VARCHAR(255) NOT NULL,
    weight_grams INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    stock_quantity INTEGER NOT NULL,
    sku VARCHAR(255) UNIQUE,
    ean VARCHAR(255),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_product_variant_weight UNIQUE (product_id, weight_grams)
);

ALTER TABLE cart_items
    ADD COLUMN variant_id UUID;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id);

ALTER TABLE order_items
    ADD COLUMN variant_id UUID;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id);
