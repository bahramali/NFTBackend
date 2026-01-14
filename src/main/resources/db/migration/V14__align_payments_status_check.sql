ALTER TABLE payments
  DROP CONSTRAINT IF EXISTS payments_status_check;

UPDATE payments
SET status = 'CANCELLED'
WHERE status = 'CANCELED';

ALTER TABLE payments
  ADD CONSTRAINT payments_status_check
  CHECK (status IN ('CREATED', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED'));
