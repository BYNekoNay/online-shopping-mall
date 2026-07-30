-- ============================================================
-- Mock Behavior Data
-- 用于推荐算法联调与测试的模拟用户行为数据
-- 依赖：V1__init_schema.sql 已执行
-- 支持重复执行：先清理再插入
-- 用户 ID 约定：
--   user1~user8 = ID 2~9（M-36 修复：由本脚本第 1.5 节创建，
--     原注释称由 V1__init_schema.sql 插入，但 V1 实际只创建 admin，导致孤儿数据）
--   user9~user18 = ID 10~19（R__seed_recommend_data.sql 引用的补充用户）
--   merchant1=20, merchant2=21（本脚本第 1.5 节创建）
-- ============================================================

-- ============================================================
-- 1. 清理已有模拟数据（按实际存在的用户 ID）
-- 注意：M-36 修复——不再 DELETE user 表记录（演示用户为常驻引用数据，
-- 原实现删除 10/11 后从不重建，重复执行会使行为数据失去归属）
-- ============================================================

DELETE FROM user_behavior
WHERE user_id BETWEEN 2 AND 11;
DELETE FROM page_view_log
WHERE user_id BETWEEN 2 AND 11 OR user_id IS NULL;
DELETE FROM order_item
WHERE order_id IN (SELECT id FROM orders WHERE user_id BETWEEN 2 AND 11);
DELETE FROM orders
WHERE user_id BETWEEN 2 AND 11;
DELETE FROM cart WHERE user_id BETWEEN 2 AND 11;
DELETE FROM sku WHERE product_id BETWEEN 100 AND 112;
DELETE FROM product WHERE id BETWEEN 100 AND 112;
DELETE FROM shop WHERE id BETWEEN 1 AND 10;

-- ============================================================
-- 1.5 M-36 修复：创建脚本引用的演示用户（原缺失导致行为/店铺数据全部孤儿）
-- H-11 修复：密码由 user123 轮换为 Mall@2026（原哈希已随调试文件泄露作废）
-- INSERT IGNORE 保证重复执行幂等；显式 ID 使 AUTO_INCREMENT 顺延，
-- 不影响后续脚本（如 init-data.sql）按自增插入的账号
-- ============================================================

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`) VALUES
(2,  'user1',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户1', 1, 1),
(3,  'user2',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户2', 1, 1),
(4,  'user3',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户3', 1, 1),
(5,  'user4',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户4', 1, 1),
(6,  'user5',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户5', 1, 1),
(7,  'user6',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户6', 1, 1),
(8,  'user7',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户7', 1, 1),
(9,  'user8',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户8', 1, 1),
(10, 'user9',     '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户9', 1, 1),
(11, 'user10',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户10', 1, 1),
(12, 'user11',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户11', 1, 1),
(13, 'user12',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户12', 1, 1),
(14, 'user13',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户13', 1, 1),
(15, 'user14',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户14', 1, 1),
(16, 'user15',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户15', 1, 1),
(17, 'user16',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户16', 1, 1),
(18, 'user17',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户17', 1, 1),
(19, 'user18',    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示用户18', 1, 1),
(20, 'merchant1', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示商家1', 2, 1),
(21, 'merchant2', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '演示商家2', 2, 1);

-- ============================================================
-- 2. 插入商家店铺（使用实际 merchant_user_id=20, 21）
-- ============================================================

INSERT INTO `shop` (`merchant_user_id`, `name`, `logo`, `description`, `level`, `status`,
                    `contact_name`, `contact_phone`, `license_no`, `license_image`, `apply_reason`) VALUES
(20, 'DigitalStore', 'https://example.com/logo1.png', 'Digital electronics', 1, 1,
 'Boss1', '74955953457', 'LIC001', 'https://example.com/lic1.png', 'Quality merchant'),
(21, 'FashionStore', 'https://example.com/logo2.png', 'Fashion apparel', 1, 1,
 'Boss2', '74955953458', 'LIC002', 'https://example.com/lic2.png', 'Quality merchant');

-- ============================================================
-- 3. 模拟商品（上架状态，供浏览/收藏/购买/评价使用）
-- shop_id=1 (DigitalStore), shop_id=2 (FashionStore)
-- 商品 ID：100~111
-- ============================================================

INSERT INTO `product` (`id`, `shop_id`, `category_id`, `name`, `main_image`, `images`, `detail`,
                        `price`, `original_price`, `stock`, `sales`, `status`) VALUES
-- 电子产品 (shop_id=1, category_id=2=电脑 / 3=耳机 / 1=手机)
(100, 1, 2, 'Laptop Pro 15', 'https://example.com/p1.jpg', '["https://example.com/p1a.jpg"]', 'High performance laptop', 5999.00, 6999.00, 100, 50, 1),
(101, 1, 2, 'Laptop Air 14', 'https://example.com/p2.jpg', '["https://example.com/p2a.jpg"]', 'Lightweight office laptop', 4299.00, 4999.00, 80, 30, 1),
(103, 1, 3, 'Wireless ANC Headphones', 'https://example.com/p3.jpg', '["https://example.com/p3a.jpg"]', 'Active noise canceling', 899.00, 1299.00, 200, 120, 1),
(104, 1, 3, 'Smart Bluetooth Speaker', 'https://example.com/p4.jpg', '["https://example.com/p4a.jpg"]', 'Portable smart speaker', 299.00, 399.00, 150, 60, 1),
(105, 1, 1, 'Flagship Phone X1', 'https://example.com/p5.jpg', '["https://example.com/p5a.jpg"]', '5G flagship phone', 3999.00, 4599.00, 50, 80, 1),
(106, 1, 1, 'Budget Phone Lite', 'https://example.com/p6.jpg', '["https://example.com/p6a.jpg"]', 'Affordable 5G phone', 1999.00, 2299.00, 120, 150, 1),
-- 服饰类 (shop_id=2, category_id=6=男装 / 7=女装 / 8=运动)
(107, 2, 6, 'Classic Cotton T-Shirt', 'https://example.com/p7.jpg', '["https://example.com/p7a.jpg"]', 'Pure cotton comfortable', 99.00, 159.00, 500, 300, 1),
(108, 2, 6, 'Slim Fit Jeans', 'https://example.com/p8.jpg', '["https://example.com/p8a.jpg"]', 'Elastic slim fit', 199.00, 299.00, 200, 100, 1),
(109, 2, 7, 'Floral Dress', 'https://example.com/p9.jpg', '["https://example.com/p9a.jpg"]', 'Summer floral print', 259.00, 399.00, 80, 60, 1),
(110, 2, 7, 'Casual Sneakers', 'https://example.com/p10.jpg', '["https://example.com/p10a.jpg"]', 'Lightweight breathable', 359.00, 499.00, 100, 90, 1),
(111, 2, 6, 'Men Casual Jacket', 'https://example.com/p11.jpg', '["https://example.com/p11a.jpg"]', 'Spring autumn thin jacket', 299.00, 459.00, 60, 40, 1),
(112, 2, 8, 'Running Shoes', 'https://example.com/p12.jpg', '["https://example.com/p12a.jpg"]', 'Cushion running shoes', 499.00, 699.00, 70, 55, 1);

-- ============================================================
-- 4. SKU 规格（为部分商品补充 SKU）
-- ============================================================

INSERT INTO `sku` (`product_id`, `spec_json`, `price`, `stock`, `image`) VALUES
(100, '{"color":"Space Gray","storage":"256GB"}', 5999.00, 50, 'https://example.com/sku1a.jpg'),
(100, '{"color":"Silver","storage":"512GB"}', 6999.00, 30, 'https://example.com/sku1b.jpg'),
(101, '{"color":"Space Gray","storage":"512GB"}', 4799.00, 40, 'https://example.com/sku2a.jpg'),
(105, '{"color":"Black","storage":"128GB"}', 1999.00, 60, 'https://example.com/sku5a.jpg'),
(105, '{"color":"Blue","storage":"256GB"}', 2299.00, 40, 'https://example.com/sku5b.jpg'),
(106, '{"color":"White","storage":"128GB"}', 2199.00, 50, 'https://example.com/sku6a.jpg'),
(107, '{"size":"M","color":"White"}', 99.00, 200, 'https://example.com/sku7a.jpg'),
(107, '{"size":"L","color":"White"}', 99.00, 150, 'https://example.com/sku7b.jpg'),
(107, '{"size":"XL","color":"Black"}', 109.00, 100, 'https://example.com/sku7c.jpg');

-- ============================================================
-- 5. 用户行为数据（user_behavior）
-- behavior_type: 1=browse, 2=favorite, 3=purchase, 4=review
--
-- User ID mapping:
--   user1=2, user2=3, user3=4, user4=5, user5=6, user6=7, user7=8, user8=9
--
-- Product ID mapping:
--   100=LaptopPro15, 101=LaptopAir14, 103=Headphones, 104=Speaker,
--   105=PhoneX1, 106=PhoneLite, 107=TShirt, 108=Jeans,
--   109=Dress, 110=Sneakers, 111=Jacket, 112=RunningShoes
-- ============================================================

-- User 1 (user1, ID=2): Heavy digital product user with complete behavior chain
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(2, 100, 1, 1.00),
(2, 100, 2, 2.00),
(2, 100, 3, 3.00),
(2, 100, 4, 4.00),
(2, 101, 1, 1.00),
(2, 101, 2, 2.00),
(2, 103, 1, 1.00),
(2, 103, 2, 2.00),
(2, 105, 1, 1.00),
(2, 105, 3, 3.00),
(2, 105, 4, 4.00),
(2, 103, 3, 3.00),
(2, 104, 1, 1.00);

-- User 2 (user2, ID=3): Digital + Fashion cross-interest
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(3, 100, 1, 1.00),
(3, 100, 2, 2.00),
(3, 103, 1, 1.00),
(3, 103, 3, 3.00),
(3, 104, 1, 1.00),
(3, 104, 2, 2.00),
(3, 107, 1, 1.00),
(3, 107, 3, 3.00),
(3, 108, 1, 1.00),
(3, 109, 1, 1.00),
(3, 109, 2, 2.00),
(3, 105, 1, 1.00);

-- User 3 (user3, ID=4): Digital product buyer
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(4, 101, 1, 1.00),
(4, 101, 3, 3.00),
(4, 101, 4, 4.00),
(4, 106, 1, 1.00),
(4, 106, 2, 2.00),
(4, 106, 3, 3.00),
(4, 103, 1, 1.00),
(4, 100, 1, 1.00);

-- User 4 (user4, ID=5): Fashion buyer
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(5, 107, 1, 1.00),
(5, 107, 2, 2.00),
(5, 107, 3, 3.00),
(5, 108, 1, 1.00),
(5, 108, 2, 2.00),
(5, 108, 3, 3.00),
(5, 109, 1, 1.00),
(5, 109, 3, 3.00),
(5, 110, 1, 1.00),
(5, 110, 2, 2.00),
(5, 111, 1, 1.00);

-- User 5 (user5, ID=6): Mixed fashion + digital
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(6, 107, 1, 1.00),
(6, 107, 3, 3.00),
(6, 112, 1, 1.00),
(6, 112, 2, 2.00),
(6, 100, 1, 1.00),
(6, 105, 1, 1.00),
(6, 105, 2, 2.00);

-- User 6 (user6, ID=7): Digital buyer, highly similar to user1 (for UserCF)
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(7, 100, 1, 1.00),
(7, 100, 2, 2.00),
(7, 100, 3, 3.00),
(7, 105, 1, 1.00),
(7, 105, 2, 2.00),
(7, 103, 1, 1.00),
(7, 103, 3, 3.00),
(7, 101, 1, 1.00);

-- User 7 (user7, ID=8): Low-frequency browser (cold-start test)
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`) VALUES
(8, 100, 1, 1.00),
(8, 107, 1, 1.00);

-- User 8 (user8, ID=9): Brand new user, no behavior data (pure cold-start)
-- No user_behavior records inserted

-- ============================================================
-- 6. 模拟页面访问日志（page_view_log）
-- 覆盖主要页面路径，用于 PV/bounce rate/stay duration stats
-- ============================================================

INSERT INTO `page_view_log` (`user_id`, `session_id`, `page_path`, `referrer_page`, `enter_time`, `leave_time`, `stay_duration`) VALUES
-- User 1 (ID=2) visit paths
(2, 'sess_001_001', '/', '', '2024-01-15 09:00:00', '2024-01-15 09:02:30', 150),
(2, 'sess_001_001', '/product/100', '/', '2024-01-15 09:02:45', '2024-01-15 09:05:10', 145),
(2, 'sess_001_001', '/product/103', '/product/100', '2024-01-15 09:05:20', '2024-01-15 09:06:00', 40),
(2, 'sess_001_002', '/', '', '2024-01-16 10:00:00', '2024-01-16 10:01:00', 60),
(2, 'sess_001_002', '/category', '/', '2024-01-16 10:01:10', '2024-01-16 10:02:00', 50),
-- User 2 (ID=3) visit paths
(3, 'sess_002_001', '/', '', '2024-01-15 14:00:00', '2024-01-15 14:01:30', 90),
(3, 'sess_002_001', '/product/107', '/', '2024-01-15 14:01:40', '2024-01-15 14:03:00', 80),
(3, 'sess_002_001', '/product/109', '/product/107', '2024-01-15 14:03:10', '2024-01-15 14:04:00', 50),
-- User 3 (ID=4) visit paths
(4, 'sess_003_001', '/', '', '2024-01-16 11:00:00', '2024-01-16 11:00:45', 45),
(4, 'sess_003_001', '/product/101', '/', '2024-01-16 11:00:55', '2024-01-16 11:02:00', 65),
-- User 4 (ID=5) visit paths
(5, 'sess_004_001', '/', '', '2024-01-15 16:00:00', '2024-01-15 16:00:30', 30),
(5, 'sess_004_001', '/product/108', '/', '2024-01-15 16:00:40', '2024-01-15 16:01:20', 40),
-- Unauthenticated visitors
(NULL, 'sess_guest_001', '/', '', '2024-01-17 08:00:00', '2024-01-17 08:01:00', 60),
(NULL, 'sess_guest_001', '/product/100', '/', '2024-01-17 08:01:10', '2024-01-17 08:02:00', 50);

-- ============================================================
-- 7. Verification queries (run after execution)
-- ============================================================
-- SELECT u.username, COUNT(ub.id) AS behavior_count,
--        SUM(CASE WHEN ub.behavior_type = 1 THEN 1 ELSE 0 END) AS views,
--        SUM(CASE WHEN ub.behavior_type = 2 THEN 1 ELSE 0 END) AS favorites,
--        SUM(CASE WHEN ub.behavior_type = 3 THEN 1 ELSE 0 END) AS purchases,
--        SUM(CASE WHEN ub.behavior_type = 4 THEN 1 ELSE 0 END) AS reviews
-- FROM user u
-- LEFT JOIN user_behavior ub ON u.id = ub.user_id
-- WHERE u.id BETWEEN 2 AND 9
-- GROUP BY u.id, u.username
-- ORDER BY u.id;
