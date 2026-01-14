DO $$
BEGIN
    IF to_regclass('public.payments') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'payments'
              AND column_name = 'provider_ref'
        ) THEN
            ALTER TABLE payments
                DROP COLUMN provider_ref;
        END IF;
    END IF;
END $$;
