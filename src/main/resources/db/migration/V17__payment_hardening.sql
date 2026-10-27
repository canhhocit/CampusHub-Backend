-- V17: Payment hardening, PayOS transaction reference, and checkout details
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_transaction_reference VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_checkout_url TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_qr_code TEXT;

CREATE INDEX IF NOT EXISTS idx_orders_payos_transaction_ref ON orders(payos_transaction_reference);
