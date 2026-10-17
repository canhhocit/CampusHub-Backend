-- Migration V11: System settings for fees and rental support

-- 1. Create system_settings table for admin fee configurations
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_settings (setting_key, setting_value, description, updated_at) VALUES
('SALE_COMMISSION_PERCENT', '5.0', 'Phần trăm hoa hồng đơn bán (%)', CURRENT_TIMESTAMP),
('RENTAL_FEE_MODE', 'PERCENT', 'Cách tính phí thuê (PERCENT hoặc PER_DAY)', CURRENT_TIMESTAMP),
('RENTAL_FEE_VALUE', '10.0', 'Giá trị phí thuê (10.0 là 10% nếu PERCENT, hoặc số tiền/ngày nếu PER_DAY)', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;

-- 2. Add rental columns to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS listing_type VARCHAR(20) NOT NULL DEFAULT 'SALE';
ALTER TABLE products ADD COLUMN IF NOT EXISTS rent_price_per_day DECIMAL(12, 2) CHECK (rent_price_per_day >= 0);
ALTER TABLE products ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(12, 2) CHECK (deposit_amount >= 0);

-- 3. Add rental & fee tracking columns to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) NOT NULL DEFAULT 'PURCHASE';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS rental_duration_days INT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS due_return_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS platform_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS seller_payout DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- 4. Return locker compartment & pickup tracking for rental return
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_compartment_id BIGINT REFERENCES compartments(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_pickup_code_hash VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS encrypted_return_pickup_code VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_deposit_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_picked_up_at TIMESTAMP;

-- 5. Update partial unique index on orders for product reservation
DROP INDEX IF EXISTS uk_active_product_order;
CREATE UNIQUE INDEX uk_active_product_order ON orders(product_id) 
WHERE status IN ('PENDING_SELLER', 'AWAITING_DEPOSIT', 'READY_FOR_PICKUP', 'RENTING', 'AWAITING_RETURN_DEPOSIT', 'READY_FOR_RETURN_PICKUP') 
   OR (status = 'EXPIRED' AND cleared_at IS NULL);

DROP INDEX IF EXISTS uk_active_compartment_order;
CREATE UNIQUE INDEX uk_active_compartment_order ON orders(compartment_id) 
WHERE status IN ('AWAITING_DEPOSIT', 'READY_FOR_PICKUP', 'AWAITING_RETURN_DEPOSIT', 'READY_FOR_RETURN_PICKUP') 
   OR (status = 'EXPIRED' AND cleared_at IS NULL);
