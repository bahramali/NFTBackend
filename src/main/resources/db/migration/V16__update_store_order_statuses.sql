DO $$
BEGIN
    IF to_regclass('public.store_orders') IS NOT NULL THEN
        UPDATE store_orders
        SET status = 'OPEN'
        WHERE status IN ('PENDING_PAYMENT', 'FAILED');

        UPDATE store_orders
        SET status = 'PROCESSING'
        WHERE status = 'PAID';

        UPDATE store_orders
        SET status = 'CANCELLED'
        WHERE status = 'CANCELED';
    END IF;
END $$;
