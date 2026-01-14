DO $$
BEGIN
    IF to_regclass('public.payments') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'payments'
              AND column_name = 'provider_reference'
        ) THEN
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = 'payments'
                  AND column_name = 'provider_ref'
            ) THEN
                EXECUTE 'UPDATE payments SET provider_reference = provider_ref WHERE provider_reference IS NULL';
            END IF;
        ELSIF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'payments'
              AND column_name = 'provider_ref'
        ) THEN
            ALTER TABLE payments
                RENAME COLUMN provider_ref TO provider_reference;
        ELSE
            ALTER TABLE payments
                ADD COLUMN provider_reference VARCHAR(255);
        END IF;

        UPDATE payments
        SET provider_reference = COALESCE(provider_reference, provider_payment_id, '')
        WHERE provider_reference IS NULL;

        ALTER TABLE payments
            ALTER COLUMN provider_reference SET NOT NULL;
    END IF;
END $$;
