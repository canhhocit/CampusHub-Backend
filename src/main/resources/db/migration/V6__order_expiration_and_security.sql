-- Migration V6: Support order expiration and secure encrypted pickup code persistence
ALTER TABLE orders ADD COLUMN IF NOT EXISTS encrypted_pickup_code VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cleared_at TIMESTAMP;

-- Drop previous partial unique indexes from V5 and update them to include EXPIRED status
-- This guarantees that while an order is EXPIRED and not yet physically cleared (cleared_at IS NULL),
-- neither the compartment nor the product can be concurrently allocated to another order.
DROP INDEX IF EXISTS uk_active_product_order;
CREATE UNIQUE INDEX uk_active_product_order ON orders(product_id) 
WHERE status IN ('PENDING_SELLER', 'AWAITING_DEPOSIT', 'READY_FOR_PICKUP') 
   OR (status = 'EXPIRED' AND cleared_at IS NULL);

DROP INDEX IF EXISTS uk_active_compartment_order;
CREATE UNIQUE INDEX uk_active_compartment_order ON orders(compartment_id) 
WHERE status IN ('AWAITING_DEPOSIT', 'READY_FOR_PICKUP') 
   OR (status = 'EXPIRED' AND cleared_at IS NULL);
