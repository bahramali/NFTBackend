DO $$
BEGIN
    IF to_regclass('public.store_orders') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'store_orders'
              AND column_name = 'total_amount_cents'
        ) THEN
            ALTER TABLE store_orders
                ADD COLUMN total_amount_cents BIGINT;
        END IF;

        UPDATE store_orders
        SET total_amount_cents = total_cents
        WHERE total_amount_cents IS NULL;

        UPDATE store_orders
        SET total_amount_cents = 0
        WHERE total_amount_cents IS NULL;

        ALTER TABLE store_orders
            ALTER COLUMN total_amount_cents SET NOT NULL;
    END IF;
END $$;
