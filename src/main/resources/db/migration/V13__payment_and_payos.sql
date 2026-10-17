-- V13: Payment status, Escrow holding, and PayOS integration
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) DEFAULT 'UNPAID';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_order_code BIGINT UNIQUE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_payment_link_id VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_paid_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_orders_payos_order_code ON orders(payos_order_code);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
