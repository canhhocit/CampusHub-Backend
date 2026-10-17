-- V14: Seed sample accounts (user01, user02, user03) and 10 realistic campus products

-- 1. Insert 3 user accounts with password 'admin123'
-- BCrypt hash for 'admin123': $2a$10$c7TjWP1/jqL3d.sQbNi/0unarUE15/78gJTZCZOC4x2bysBn6Mz7u
INSERT INTO users (
    username, email, password_hash, full_name, role, status,
    date_of_birth, identity_verified, citizen_id, identity_verified_at, identity_reviewed_at, created_at, updated_at
) VALUES 
(
    'user01', 'user01@campushub.com',
    '$2a$10$c7TjWP1/jqL3d.sQbNi/0unarUE15/78gJTZCZOC4x2bysBn6Mz7u',
    'Nguyễn Văn An (K64)', 'STUDENT', 'ACTIVE',
    '2003-05-15', TRUE, '001201000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'user02', 'user02@campushub.com',
    '$2a$10$c7TjWP1/jqL3d.sQbNi/0unarUE15/78gJTZCZOC4x2bysBn6Mz7u',
    'Trần Thị Mai (K65)', 'STUDENT', 'ACTIVE',
    '2004-08-20', TRUE, '001201000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'user03', 'user03@campushub.com',
    '$2a$10$c7TjWP1/jqL3d.sQbNi/0unarUE15/78gJTZCZOC4x2bysBn6Mz7u',
    'Lê Hoàng Nam (K63)', 'STUDENT', 'ACTIVE',
    '2002-11-10', TRUE, '001201000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    identity_verified = TRUE,
    identity_reviewed_at = CURRENT_TIMESTAMP,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 2. Insert 10 realistic products for user01, user02, user03
INSERT INTO products (
    title, description, price, image_url, status, category_id, owner_id,
    listing_type, rent_price_per_day, deposit_amount, created_at, updated_at, version
) VALUES
-- Product 1: Sách Giải Tích (user01)
(
    'Giáo trình Giải tích 1 & 2 (Đại học Bách Khoa)',
    'Sách còn mới 90%, đã bọc bìa nylon cẩn thận, có note bài tập thi cuối kỳ các năm. Giao nhận qua tủ Smart Locker KTX B5.',
    45000.00,
    'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'books' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user01' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 2: Sách Lập trình Java (user02)
(
    'Giáo trình Lập trình Hướng đối tượng Java & C++',
    'Giáo trình chuẩn khoa CNTT, đầy đủ ví dụ minh họa và đề cương ôn tập môn OOP. Nhận đồ nhanh qua Locker cổng 1.',
    55000.00,
    'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'books' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user02' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 3: Bộ sách IELTS (user03 - Hỗ trợ cả bán và thuê)
(
    'Bộ sách IELTS Cambridge 15 - 18 (Kèm Audio QR Code)',
    'Bộ đề ôn luyện IELTS band 6.5+, giấy in rõ nét, chưa viết bút mực. Hỗ trợ cho thuê 5k/ngày hoặc mua đứt.',
    85000.00,
    'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'books' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user03' LIMIT 1),
    'BOTH', 5000.00, 80000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 4: Casio FX-580VN X (user01)
(
    'Máy tính cầm tay Casio fx-580VN X (Chính hãng Bitex)',
    'Máy tính bỏ túi quốc dân cho sinh viên kỹ thuật/kinh tế. Máy còn nguyên tem Bitex, phím bấm nảy nhạy, pin trâu.',
    380000.00,
    'https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'electronics' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user01' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 5: Tai nghe Soundpeats (user02)
(
    'Tai nghe không dây Bluetooth chống ồn Soundpeats Life',
    'Tai nghe chống ồn chủ động ANC học bài thư viện cực êm, pin dùng 5-6 tiếng, dock sạc Type-C. Đầy đủ núm tai thay thế.',
    320000.00,
    'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'electronics' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user02' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 6: Bàn phím cơ RK68 (user03 - Hỗ trợ cả bán và thuê)
(
    'Bàn phím cơ không dây Bluetooth RK68 (Gateron Red Switch)',
    'Bàn phím cơ gọn nhẹ gõ êm không ồn phòng KTX, kết nối 3 thiết bị cùng lúc, LED trắng thanh lịch. Có thể thuê ngắn ngày 15k/ngày.',
    490000.00,
    'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'electronics' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user03' LIMIT 1),
    'BOTH', 15000.00, 450000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 7: Bàn học gấp gọn (user01)
(
    'Bàn học gấp gọn chân chữ U mặt gỗ chống trượt',
    'Bàn học gấp gọn tiện lợi để trên giường tầng KTX, có khe cắm iPad và khay đựng cốc nước, chân đế bọc cao su chống trượt.',
    75000.00,
    'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'furniture' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user01' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 8: Đèn học LED Rạng Đông (user02)
(
    'Đèn học LED chống cận bảo vệ mắt Rạng Đông (Pin sạc)',
    'Đèn 3 chế độ sáng (trắng, vàng, trung tính), có pin sạc dự phòng dùng được 4 tiếng khi mất điện KTX.',
    110000.00,
    'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'furniture' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user02' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 9: Áo Hoodie Unisex (user03)
(
    'Áo Hoodie Unisex form rộng chất nỉ bông dày dặn (Size L)',
    'Mua về mặc đúng 2 lần mùa đông năm ngoái, áo màu xám tiêu dễ phối đồ, không xù lông.',
    120000.00,
    'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'clothing' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user03' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
),
-- Product 10: Bình giữ nhiệt Lock&Lock (user01)
(
    'Bình giữ nhiệt Lock&Lock 500ml Inox 304 giữ nhiệt 24h',
    'Bình nước giữ lạnh giữ nóng cả ngày mang đi giảng đường, lòng bình inox 304 kháng khuẩn sạch sẽ, có lưới lọc trà.',
    95000.00,
    'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80',
    'ACTIVE',
    (SELECT id FROM categories WHERE slug = 'others' LIMIT 1),
    (SELECT id FROM users WHERE username = 'user01' LIMIT 1),
    'SALE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
);

-- 3. Insert secondary gallery images for richer product detail view
INSERT INTO product_images (product_id, image_url, sort_order, created_at)
SELECT p.id, p.image_url, 0, CURRENT_TIMESTAMP
FROM products p
WHERE p.title IN (
    'Giáo trình Giải tích 1 & 2 (Đại học Bách Khoa)',
    'Giáo trình Lập trình Hướng đối tượng Java & C++',
    'Bộ sách IELTS Cambridge 15 - 18 (Kèm Audio QR Code)',
    'Máy tính cầm tay Casio fx-580VN X (Chính hãng Bitex)',
    'Tai nghe không dây Bluetooth chống ồn Soundpeats Life',
    'Bàn phím cơ không dây Bluetooth RK68 (Gateron Red Switch)',
    'Bàn học gấp gọn chân chữ U mặt gỗ chống trượt',
    'Đèn học LED chống cận bảo vệ mắt Rạng Đông (Pin sạc)',
    'Áo Hoodie Unisex form rộng chất nỉ bông dày dặn (Size L)',
    'Bình giữ nhiệt Lock&Lock 500ml Inox 304 giữ nhiệt 24h'
)
ON CONFLICT DO NOTHING;
