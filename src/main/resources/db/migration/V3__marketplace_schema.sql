CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Indexes for performance
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_owner ON products(owner_id);
CREATE INDEX idx_products_status_created ON products(status, created_at DESC);

-- Seed Categories
INSERT INTO categories (name, slug, is_active, created_at, updated_at) VALUES 
('Electronics', 'electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Books', 'books', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Clothing', 'clothing', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Furniture', 'furniture', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Others', 'others', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;
