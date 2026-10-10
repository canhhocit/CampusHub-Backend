CREATE TABLE lockers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE compartments (
    id BIGSERIAL PRIMARY KEY,
    locker_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_compartment_locker FOREIGN KEY (locker_id) REFERENCES lockers(id) ON DELETE RESTRICT,
    CONSTRAINT uk_locker_compartment_code UNIQUE (locker_id, code)
);

CREATE INDEX idx_lockers_status ON lockers(status);
CREATE INDEX idx_compartments_locker_id ON compartments(locker_id);
CREATE INDEX idx_compartments_status ON compartments(status);

-- Seed initial lockers for dev/demo
INSERT INTO lockers (code, name, location, status, created_at, updated_at) VALUES
('LK-A1', 'Tủ Khu A - Tầng 1', 'Sảnh chính Tòa nhà A, Tầng 1', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LK-B2', 'Tủ Thư Viện - Tầng 2', 'Khu tự học Thư viện Trung tâm, Tầng 2', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LK-C1', 'Tủ Ký Túc Xá C1', 'Sảnh Ký túc xá Khu B, Tòa C1', 'MAINTENANCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Seed compartments for LK-A1
INSERT INTO compartments (locker_id, code, status, created_at, updated_at)
SELECT l.id, c.code, c.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lockers l
CROSS JOIN (
    VALUES 
        ('C01', 'AVAILABLE'),
        ('C02', 'AVAILABLE'),
        ('C03', 'OCCUPIED'),
        ('C04', 'OUT_OF_SERVICE')
) AS c(code, status)
WHERE l.code = 'LK-A1'
ON CONFLICT (locker_id, code) DO NOTHING;

-- Seed compartments for LK-B2
INSERT INTO compartments (locker_id, code, status, created_at, updated_at)
SELECT l.id, c.code, c.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lockers l
CROSS JOIN (
    VALUES 
        ('C01', 'AVAILABLE'),
        ('C02', 'AVAILABLE'),
        ('C03', 'AVAILABLE')
) AS c(code, status)
WHERE l.code = 'LK-B2'
ON CONFLICT (locker_id, code) DO NOTHING;
