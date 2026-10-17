-- V15: Add locker_id to products and activate lockers for filtering
ALTER TABLE products ADD COLUMN IF NOT EXISTS locker_id BIGINT REFERENCES lockers(id);
CREATE INDEX IF NOT EXISTS idx_products_locker ON products(locker_id);

UPDATE lockers SET status = 'ACTIVE' WHERE code = 'LK-C1';

-- Insert compartments for LK-B2 and LK-C1 if needed
INSERT INTO compartments (locker_id, code, status, created_at, updated_at)
SELECT l.id, c.code, c.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lockers l
CROSS JOIN (
    VALUES 
        ('B01', 'AVAILABLE'),
        ('B02', 'AVAILABLE'),
        ('B03', 'OCCUPIED'),
        ('B04', 'AVAILABLE')
) AS c(code, status)
WHERE l.code = 'LK-B2'
ON CONFLICT (locker_id, code) DO NOTHING;

INSERT INTO compartments (locker_id, code, status, created_at, updated_at)
SELECT l.id, c.code, c.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lockers l
CROSS JOIN (
    VALUES 
        ('C01', 'AVAILABLE'),
        ('C02', 'AVAILABLE'),
        ('C03', 'AVAILABLE'),
        ('C04', 'AVAILABLE')
) AS c(code, status)
WHERE l.code = 'LK-C1'
ON CONFLICT (locker_id, code) DO NOTHING;

-- Assign lockers to existing products if not assigned
UPDATE products SET locker_id = ((id % 3) + 1) WHERE locker_id IS NULL;
