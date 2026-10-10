CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    price_snapshot DECIMAL(12,2) NOT NULL CHECK (price_snapshot >= 0),
    status VARCHAR(30) NOT NULL,
    locker_id BIGINT,
    compartment_id BIGINT,
    pickup_code_hash VARCHAR(255),
    pickup_deadline TIMESTAMP,
    pickup_attempts INT NOT NULL DEFAULT 0,
    deposit_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    rejection_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_order_locker FOREIGN KEY (locker_id) REFERENCES lockers(id),
    CONSTRAINT fk_order_compartment FOREIGN KEY (compartment_id) REFERENCES compartments(id)
);

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_seller ON orders(seller_id);
CREATE INDEX idx_orders_product ON orders(product_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

-- Partial unique index to guarantee at most one active order per product
CREATE UNIQUE INDEX uk_active_product_order ON orders(product_id) 
WHERE status IN ('PENDING_SELLER', 'AWAITING_DEPOSIT', 'READY_FOR_PICKUP');

-- Partial unique index to guarantee at most one active order per compartment
CREATE UNIQUE INDEX uk_active_compartment_order ON orders(compartment_id) 
WHERE status IN ('AWAITING_DEPOSIT', 'READY_FOR_PICKUP');
