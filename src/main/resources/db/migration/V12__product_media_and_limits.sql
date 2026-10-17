-- V12: Product media (video & gallery images) and Admin Media Limit Settings

ALTER TABLE products ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);

-- Seed System Settings for Media Limits
INSERT INTO system_settings (key, value, description, updated_at) VALUES
('MAX_PRODUCT_IMAGES', '5', 'Số lượng ảnh mô tả tối đa cho mỗi sản phẩm', CURRENT_TIMESTAMP),
('MAX_PRODUCT_VIDEOS', '1', 'Số lượng video tối đa cho mỗi sản phẩm', CURRENT_TIMESTAMP),
('ALLOW_PRODUCT_VIDEO', 'true', 'Cho phép người bán upload video sản phẩm', CURRENT_TIMESTAMP),
('MAX_VIDEO_SIZE_MB', '50', 'Dung lượng tối đa của video sản phẩm (MB)', CURRENT_TIMESTAMP)
ON CONFLICT (key) DO NOTHING;
