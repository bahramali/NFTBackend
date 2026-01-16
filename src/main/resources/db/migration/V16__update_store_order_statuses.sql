DO $$
BEGIN
    IF to_regclass('public.store_orders') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_name = 'store_orders'
              AND constraint_name = 'store_orders_status_check'
        ) THEN
            ALTER TABLE store_orders
                DROP CONSTRAINT store_orders_status_check;
        END IF;

        UPDATE store_orders
        SET status = 'OPEN'
        WHERE status IN ('PENDING_PAYMENT', 'FAILED');

        UPDATE store_orders
        SET status = 'PROCESSING'
        WHERE status = 'PAID';

        UPDATE store_orders
        SET status = 'CANCELLED'
        WHERE status = 'CANCELED';

        ALTER TABLE store_orders
            ADD CONSTRAINT store_orders_status_check
            CHECK (status IN ('OPEN', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));
    END IF;
END $$;
