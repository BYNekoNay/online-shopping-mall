-- ============================================================
-- 完整测试数据脚本
-- 覆盖：用户、店铺、商品、SKU、地址、优惠券、促销、
--       购物车、订单、支付、物流、评价、积分、行为、推荐
-- 执行前会清空所有业务表数据（保留表结构）
-- 适用于 MySQL 8.0+
-- 使用方式：mysql -uroot -p mall < seed-test-data.sql
-- ============================================================

USE mall;

-- ============================================================
-- 0. 清理已有数据（按外键依赖顺序反向删除）
-- ============================================================
DELETE FROM page_view_log;
DELETE FROM search_history;
DELETE FROM recommend_result;
DELETE FROM user_score;
DELETE FROM user_behavior;
DELETE FROM points_record;
DELETE FROM review;
DELETE FROM refund;
DELETE FROM order_item;
DELETE FROM logistics;
DELETE FROM payment;
DELETE FROM orders;
DELETE FROM cart;
DELETE FROM user_coupon;
DELETE FROM promotion;
DELETE FROM coupon;
DELETE FROM address;
DELETE FROM sku;
DELETE FROM product;
DELETE FROM freight_template;
DELETE FROM shop;
DELETE FROM operation_log;
DELETE FROM user;

-- ============================================================
-- 1. BCrypt 密码常量（H-11 修复：轮换弱口令）
--    admin    -> Admin@2026 -> $2a$10$UBi4W0ASv2kytcew8cYuqO.mCtIYMetRi3xlRwYItvSzDcEn8pYf6
--    user/merchant 演示账号 -> Mall@2026 -> $2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u
-- ============================================================

-- ============================================================
-- 2. 测试用户
-- ============================================================
-- 管理员
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `points`) VALUES
(1, 'admin', '$2a$10$UBi4W0ASv2kytcew8cYuqO.mCtIYMetRi3xlRwYItvSzDcEn8pYf6', '管理员', 3, 1, 0);

-- 消费者 user1~user8 (ID=2~9)
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`, `points`) VALUES
(2, 'user1', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小明', '13800000001', 'user1@test.com', 1, 1, 500),
(3, 'user2', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小红', '13800000002', 'user2@test.com', 1, 1, 300),
(4, 'user3', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小刚', '13800000003', 'user3@test.com', 1, 1, 200),
(5, 'user4', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小丽', '13800000004', 'user4@test.com', 1, 1, 100),
(6, 'user5', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小华', '13800000005', 'user5@test.com', 1, 1, 50),
(7, 'user6', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小强', '13800000006', 'user6@test.com', 1, 1, 0),
(8, 'user7', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小芳', '13800000007', 'user7@test.com', 1, 1, 0),
(9, 'user8', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小军', '13800000008', 'user8@test.com', 1, 1, 0);

-- 补充用户 user9~user14 (ID=10~15)
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`, `points`) VALUES
(10, 'user9',  '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小张', '13800000009', 'user9@test.com',  1, 1, 0),
(11, 'user10', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小李', '13800000010', 'user10@test.com', 1, 1, 0),
(12, 'user11', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小王', '13800000011', 'user11@test.com', 1, 1, 0),
(13, 'user12', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小赵', '13800000012', 'user12@test.com', 1, 1, 0),
(14, 'user13', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小孙', '13800000013', 'user13@test.com', 1, 1, 0),
(15, 'user14', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '小周', '13800000014', 'user14@test.com', 1, 1, 0);

-- 商家（角色=2）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `role`, `status`, `points`) VALUES
(20, 'merchant1', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '数码商家', '13900000001', 'merchant1@shop.com', 2, 1, 0),
(21, 'merchant2', '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u', '时尚商家', '13900000002', 'merchant2@shop.com', 2, 1, 0);

-- ============================================================
-- 3. 店铺
-- ============================================================
INSERT INTO `shop` (`id`, `merchant_user_id`, `name`, `logo`, `description`, `level`, `status`,
                     `contact_name`, `contact_phone`, `license_no`, `license_image`, `apply_reason`) VALUES
(1, 20, 'DigitalStore',    '/images/shop/digital.png',  '专业数码电子产品旗舰店', 2, 1, '张三', '13900000001', 'LIC2024001', '/images/license/digital.jpg', '主营数码电子'),
(2, 21, 'FashionStore',    '/images/shop/fashion.png',  '时尚潮流服饰精品店',   1, 1, '李四', '13900000002', 'LIC2024002', '/images/license/fashion.jpg', '主营服装鞋帽');

-- ============================================================
-- 4. 商品（上架状态）
-- ============================================================
INSERT INTO `product` (`id`, `shop_id`, `category_id`, `name`, `main_image`, `images`, `detail`,
                        `price`, `original_price`, `stock`, `sales`, `status`) VALUES
-- DigitalStore 商品 (shop_id=1)
(100, 1, 7, 'Laptop Pro 15 高性能笔记本',        '/images/p100.jpg',  '["/images/p100_1.jpg","/images/p100_2.jpg"]',  '15.6英寸 4K屏 i9-13900H 32GB 1TB SSD',              5999.00, 6999.00, 100, 50, 1),
(101, 1, 7, 'Laptop Air 14 轻薄办公本',          '/images/p101.jpg',  '["/images/p101_1.jpg","/images/p101_2.jpg"]',  '14英寸 2.5K屏 i7-1360P 16GB 512GB SSD',             4299.00, 4999.00, 80,  30, 1),
(102, 1, 6, 'Tablet Pro 12.9 平板电脑',           '/images/p102.jpg',  '["/images/p102_1.jpg"]',                       '12.9英寸 M2芯片 256GB Wi-Fi版',                      5499.00, 6499.00, 60,  25, 1),
(103, 1, 8, '降噪蓝牙耳机 ANC Pro',              '/images/p103.jpg',  '["/images/p103_1.jpg"]',                       '主动降噪 40dB 蓝牙5.3 30小时续航',                   899.00,  1299.00, 200, 120, 1),
(104, 1, 8, '智能蓝牙音箱 SoundPlus',            '/images/p104.jpg',  '["/images/p104_1.jpg"]',                       '360°环绕立体声 智能语音助手 IPX7防水',               299.00,  399.00,  150, 60,  1),
(105, 1, 6, '旗舰手机 X1 5G',                    '/images/p105.jpg',  '["/images/p105_1.jpg","/images/p105_2.jpg"]',  '骁龙8Gen3 12GB+256GB 5000mAh 120W快充',             3999.00, 4599.00, 50,  80,  1),
(106, 1, 6, '性价比手机 Lite 5G',                 '/images/p106.jpg',  '["/images/p106_1.jpg"]',                       '天玑8200 8GB+128GB 4800mAh 67W快充',                1999.00, 2299.00, 120, 150, 1),
-- FashionStore 商品 (shop_id=2)
(107, 2, 9, '纯棉经典圆领T恤',                    '/images/p107.jpg',  '["/images/p107_1.jpg","/images/p107_2.jpg"]',  '100%纯棉 舒适透气 多色可选',                         99.00,   159.00,  500, 300, 1),
(108, 2, 9, '修身弹力牛仔裤',                     '/images/p108.jpg',  '["/images/p108_1.jpg"]',                       '弹力面料 修身版型 经典五袋款',                       199.00,  299.00,  200, 100, 1),
(109, 2, 10,'碎花连衣裙 夏季新款',                '/images/p109.jpg',  '["/images/p109_1.jpg"]',                       '轻盈雪纺面料 碎花印花 收腰设计',                     259.00,  399.00,  80,  60,  1),
(110, 2, 11,'休闲百搭运动鞋',                     '/images/p110.jpg',  '["/images/p110_1.jpg","/images/p110_2.jpg"]',  '透气网面 EVA缓震底 轻便舒适',                       359.00,  499.00,  100, 90,  1),
(111, 2, 9, '男士休闲夹克外套',                   '/images/p111.jpg',  '["/images/p111_1.jpg"]',                       '春秋薄款 防风面料 简约设计',                         299.00,  459.00,  60,  40,  1),
(112, 2, 11,'专业缓震跑步鞋',                     '/images/p112.jpg',  '["/images/p112_1.jpg"]',                       '全掌气垫 透气飞织 专业跑步',                         499.00,  699.00,  70,  55,  1);

-- ============================================================
-- 5. SKU 规格
-- ============================================================
INSERT INTO `sku` (`product_id`, `spec_json`, `price`, `stock`, `image`) VALUES
(100, '{"color":"深空灰","storage":"256GB"}', 5999.00, 50, '/images/sku/100_gray_256.jpg'),
(100, '{"color":"银色","storage":"512GB"}',   6999.00, 30, '/images/sku/100_silver_512.jpg'),
(101, '{"color":"深空灰","storage":"512GB"}', 4799.00, 40, '/images/sku/101_gray_512.jpg'),
(101, '{"color":"金色","storage":"256GB"}',   4299.00, 40, '/images/sku/101_gold_256.jpg'),
(105, '{"color":"黑色","storage":"128GB"}',   1999.00, 60, '/images/sku/105_black_128.jpg'),
(105, '{"color":"蓝色","storage":"256GB"}',   2299.00, 40, '/images/sku/105_blue_256.jpg'),
(106, '{"color":"白色","storage":"128GB"}',   2199.00, 50, '/images/sku/106_white_128.jpg'),
(107, '{"size":"M","color":"白色"}',           99.00, 200, '/images/sku/107_white_m.jpg'),
(107, '{"size":"L","color":"白色"}',           99.00, 150, '/images/sku/107_white_l.jpg'),
(107, '{"size":"XL","color":"黑色"}',         109.00, 100, '/images/sku/107_black_xl.jpg'),
(108, '{"size":"29","color":"深蓝"}',         199.00,  80, '/images/sku/108_blue_29.jpg'),
(108, '{"size":"30","color":"深蓝"}',         199.00, 120, '/images/sku/108_blue_30.jpg');

-- ============================================================
-- 6. 收货地址
-- ============================================================
INSERT INTO `address` (`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`, `is_default`) VALUES
(2,  '小明',   '13800000001', '广东省', '深圳市', '南山区', '科技园南路88号腾讯大厦B座12楼',       1),
(3,  '小红',   '13800000002', '北京市', '北京市',  '海淀区', '中关村大街1号海淀黄庄小区3号楼2单元501', 1),
(4,  '小刚',   '13800000003', '上海市', '上海市',  '浦东新区', '陆家嘴金融中心A座2501室',              1),
(5,  '小丽',   '13800000004', '浙江省', '杭州市',  '西湖区', '文三路478号华星科技大厦8楼',            1),
(6,  '小华',   '13800000005', '四川省', '成都市',  '高新区', '天府大道中段1号软件园C区7栋',            1),
(7,  '小强',   '13800000006', '湖北省', '武汉市',  '洪山区', '珞喻路1037号华中科技大学主校区',         1),
(8,  '小芳',   '13800000007', '江苏省', '南京市',  '鼓楼区', '中山北路200号南京工业大学',              1),
(9,  '小军',   '13800000008', '广东省', '广州市',  '天河区', '天河路580号太古汇广场',                  1);

-- ============================================================
-- 7. 优惠券模板
--    type: 1-新人券, 2-满减券, 3-品类券, 4-店铺券
-- ============================================================
INSERT INTO `coupon` (`id`, `name`, `type`, `shop_id`, `discount_rule`, `valid_from`, `valid_to`, `stock`, `received_count`) VALUES
(1, '新人立减10元',      1, NULL, '{"type":"fixed","value":10}',              NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1000, 100),
(2, '满200减20',         2, NULL, '{"type":"threshold","threshold":200,"reduce":20}', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 500,  80),
(3, '满500减50',         2, NULL, '{"type":"threshold","threshold":500,"reduce":50}', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 200,  30),
(4, '数码品类8折券',     3, 1,    '{"type":"percent","percent":0.8,"maxReduce":200}', NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 300,  50),
(5, '服装满300减40',     4, 2,    '{"type":"threshold","threshold":300,"reduce":40}', NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 200,  40);

-- ============================================================
-- 8. 用户领取的优惠券
-- ============================================================
INSERT INTO `user_coupon` (`user_id`, `coupon_id`, `status`, `create_time`) VALUES
(2, 1, 0, NOW() - INTERVAL 10 DAY),  -- user1 新人券
(2, 2, 0, NOW() - INTERVAL 5 DAY),   -- user1 满减券
(2, 4, 0, NOW() - INTERVAL 3 DAY),   -- user1 数码券
(3, 1, 0, NOW() - INTERVAL 8 DAY),   -- user2 新人券
(3, 5, 0, NOW() - INTERVAL 2 DAY),   -- user2 服装券
(4, 2, 1, NOW() - INTERVAL 7 DAY),   -- user3 已用满减券
(5, 1, 0, NOW() - INTERVAL 6 DAY),   -- user4 新人券
(5, 3, 0, NOW() - INTERVAL 4 DAY),   -- user4 满500减50
(6, 1, 0, NOW() - INTERVAL 1 DAY),   -- user5 新人券
(7, 1, 0, NOW() - INTERVAL 9 DAY);   -- user6 新人券

-- ============================================================
-- 9. 促销活动
--    type: 1-限时折扣, 2-满减, 3-满赠, 4-组合套餐
-- ============================================================
INSERT INTO `promotion` (`name`, `type`, `rule_json`, `scope`, `scope_id`, `start_time`, `end_time`, `status`) VALUES
('限时8折 - 数码全场',    1, '{"discountPercent":0.8}',           'SHOP',     1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY),  1),
('满100减15 - 数码店',    2, '{"threshold":100,"reduce":15}',   'SHOP',     1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY),  1),
('服装满200减30',         2, '{"threshold":200,"reduce":30}',   'SHOP',     2, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), 1),
('耳机限时特价799',       1, '{"discountPrice":799}',            'PRODUCT', 103, NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY), 1),
('笔记本满5000减300',     2, '{"threshold":5000,"reduce":300}', 'CATEGORY', 7, NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1);

-- ============================================================
-- 10. 购物车数据
-- ============================================================
INSERT INTO `cart` (`user_id`, `product_id`, `sku_id`, `quantity`, `selected`) VALUES
(2,  100, NULL, 1, 1),   -- user1 笔记本
(2,  103, NULL, 2, 1),   -- user1 2个耳机
(2,  107, 8,    3, 0),   -- user1 未选中T恤
(3,  107, 8,    1, 1),   -- user2 白M T恤
(3,  109, NULL, 1, 1),   -- user2 连衣裙
(4,  105, 5,    1, 1),   -- user3 黑128G手机
(4,  104, NULL, 1, 0),   -- user3 未选中音箱
(5,  108, 11,   1, 1),   -- user4 29码牛仔裤
(5,  110, NULL, 1, 1),   -- user4 运动鞋
(6,  100, 1,    1, 0);   -- user5 未选中笔记本

-- ============================================================
-- 11. 订单数据（覆盖所有状态）
--    0-待付款, 1-待发货, 2-已发货, 3-已收货, 4-已完成,
--    5-已取消, 6-退款中, 7-已退款
-- ============================================================
INSERT INTO `orders` (`order_no`, `user_id`, `shop_id`, `total_amount`, `freight_amount`, `discount_amount`, `pay_amount`, `status`, `address_snapshot`, `pay_type`, `pay_time`, `remark`, `create_time`) VALUES
-- user1 订单
('20260701001', 2, 1, 6898.00, 0.00, 20.00, 6878.00, 0, '{"receiver":"小明","phone":"13800000001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园南路88号腾讯大厦B座12楼"}', NULL, NULL, '请尽快发货', NOW() - INTERVAL 30 MINUTE),
('20260628001', 2, 1, 5999.00, 0.00, 50.00, 5949.00, 4, '{"receiver":"小明","phone":"13800000001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园南路88号腾讯大厦B座12楼"}', 1, NOW() - INTERVAL 7 DAY, '', NOW() - INTERVAL 10 DAY),
('20260625001', 2, 2, 498.00, 10.00, 0.00, 508.00, 5, '{"receiver":"小明","phone":"13800000001","province":"广东省","city":"深圳市","district":"南山区","detail":"科技园南路88号腾讯大厦B座12楼"}', NULL, NULL, '不想要了', NOW() - INTERVAL 13 DAY),
-- user2 订单
('20260702001', 3, 2, 358.00, 0.00, 0.00, 358.00, 1, '{"receiver":"小红","phone":"13800000002","province":"北京市","city":"北京市","district":"海淀区","detail":"中关村大街1号海淀黄庄小区3号楼2单元501"}', 2, NOW() - INTERVAL 1 DAY, '', NOW() - INTERVAL 2 DAY),
('20260630001', 3, 2, 498.00, 0.00, 40.00, 458.00, 2, '{"receiver":"小红","phone":"13800000002","province":"北京市","city":"北京市","district":"海淀区","detail":"中关村大街1号海淀黄庄小区3号楼2单元501"}', 1, NOW() - INTERVAL 3 DAY, '', NOW() - INTERVAL 4 DAY),
-- user3 订单
('20260620001', 4, 1, 1999.00, 0.00, 0.00, 1999.00, 3, '{"receiver":"小刚","phone":"13800000003","province":"上海市","city":"上海市","district":"浦东新区","detail":"陆家嘴金融中心A座2501室"}', 2, NOW() - INTERVAL 17 DAY, '', NOW() - INTERVAL 18 DAY),
('20260701002', 4, 2, 398.00, 0.00, 0.00, 398.00, 6, '{"receiver":"小刚","phone":"13800000003","province":"上海市","city":"上海市","district":"浦东新区","detail":"陆家嘴金融中心A座2501室"}', 1, NOW() - INTERVAL 1 DAY, '尺码偏大，申请退款', NOW() - INTERVAL 2 DAY),
-- user4 订单
('20260703001', 5, 2, 858.00, 0.00, 40.00, 818.00, 0, '{"receiver":"小丽","phone":"13800000004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路478号华星科技大厦8楼"}', NULL, NULL, '', NOW() - INTERVAL 1 HOUR),
('20260615001', 5, 1, 1298.00, 0.00, 0.00, 1298.00, 4, '{"receiver":"小丽","phone":"13800000004","province":"浙江省","city":"杭州市","district":"西湖区","detail":"文三路478号华星科技大厦8楼"}', 2, NOW() - INTERVAL 20 DAY, '不错', NOW() - INTERVAL 23 DAY),
-- user5 订单
('20260622001', 6, 1, 299.00, 0.00, 0.00, 299.00, 5, '{"receiver":"小华","phone":"13800000005","province":"四川省","city":"成都市","district":"高新区","detail":"天府大道中段1号软件园C区7栋"}', NULL, NULL, '', NOW() - INTERVAL 15 DAY);

-- ============================================================
-- 12. 订单明细
-- ============================================================
INSERT INTO `order_item` (`order_id`, `product_id`, `sku_id`, `product_name_snapshot`, `product_image_snapshot`, `price`, `quantity`, `is_gift`, `create_time`) VALUES
-- 订单1 (待付款): user1 笔记本+耳机
(1, 100, 1, 'Laptop Pro 15 高性能笔记本',          '/images/p100.jpg',  5999.00, 1, 0, NOW() - INTERVAL 30 MINUTE),
(1, 103, NULL, '降噪蓝牙耳机 ANC Pro',              '/images/p103.jpg',  899.00,  1, 0, NOW() - INTERVAL 30 MINUTE),
-- 订单2 (已完成): user1 笔记本
(2, 100, 1, 'Laptop Pro 15 高性能笔记本',          '/images/p100.jpg',  5999.00, 1, 0, NOW() - INTERVAL 10 DAY),
-- 订单3 (已取消): user1 耳机+T恤
(3, 103, NULL, '降噪蓝牙耳机 ANC Pro',              '/images/p103.jpg',  899.00,  1, 0, NOW() - INTERVAL 13 DAY),
(3, 107, 8, '纯棉经典圆领T恤',                     '/images/p107.jpg',  99.00,   2, 0, NOW() - INTERVAL 13 DAY),
-- 订单4 (待发货): user2 连衣裙+运动鞋
(4, 109, NULL, '碎花连衣裙 夏季新款',                '/images/p109.jpg',  259.00, 1, 0, NOW() - INTERVAL 2 DAY),
(4, 110, NULL, '休闲百搭运动鞋',                     '/images/p110.jpg',  99.00,  1, 0, NOW() - INTERVAL 2 DAY),
-- 订单5 (已发货): user2 T恤+牛仔裤
(5, 107, 8, '纯棉经典圆领T恤',                     '/images/p107.jpg',  99.00,   2, 0, NOW() - INTERVAL 4 DAY),
(5, 108, 11, '修身弹力牛仔裤',                       '/images/p108.jpg',  199.00, 1, 0, NOW() - INTERVAL 4 DAY),
-- 订单6 (已收货): user3 手机
(6, 105, 5, '旗舰手机 X1 5G',                       '/images/p105.jpg',  1999.00,1, 0, NOW() - INTERVAL 18 DAY),
-- 订单7 (退款中): user3 夹克+T恤
(7, 111, NULL, '男士休闲夹克外套',                   '/images/p111.jpg',  299.00, 1, 0, NOW() - INTERVAL 2 DAY),
(7, 107, 8, '纯棉经典圆领T恤',                     '/images/p107.jpg',  99.00,  1, 0, NOW() - INTERVAL 2 DAY),
-- 订单8 (待付款): user4 运动鞋+牛仔裤
(8, 110, NULL, '休闲百搭运动鞋',                     '/images/p110.jpg',  359.00, 1, 0, NOW() - INTERVAL 1 HOUR),
(8, 108, 12, '修身弹力牛仔裤',                       '/images/p108.jpg',  199.00, 1, 0, NOW() - INTERVAL 1 HOUR),
-- 订单9 (已完成): user4 耳机2个
(9, 103, NULL, '降噪蓝牙耳机 ANC Pro',              '/images/p103.jpg',  899.00, 2, 0, NOW() - INTERVAL 23 DAY),
-- 订单10 (已取消): user5 音箱
(10, 104, NULL, '智能蓝牙音箱 SoundPlus',           '/images/p104.jpg',  299.00, 1, 0, NOW() - INTERVAL 15 DAY);

-- ============================================================
-- 13. 支付记录
-- ============================================================
INSERT INTO `payment` (`order_id`, `pay_no`, `amount`, `pay_type`, `status`, `callback_time`, `create_time`) VALUES
(2,  'PAY20260628001', 5949.00, 1, 1, NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 10 DAY),
(4,  'PAY20260702001', 358.00,  2, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY),
(5,  'PAY20260630001', 458.00,  1, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 4 DAY),
(6,  'PAY20260620001', 1999.00, 2, 1, NOW() - INTERVAL 17 DAY,NOW() - INTERVAL 18 DAY),
(7,  'PAY20260701002', 398.00,  1, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY),
(9,  'PAY20260615001', 1298.00, 2, 1, NOW() - INTERVAL 20 DAY,NOW() - INTERVAL 23 DAY);

-- ============================================================
-- 14. 物流信息
-- ============================================================
INSERT INTO `logistics` (`order_id`, `company`, `company_code`, `tracking_no`, `status`, `last_track_info`, `create_time`) VALUES
(5, '顺丰速运', 'SF', 'SF1234567890', 0, '【深圳市】快件已到达深圳集散中心', NOW() - INTERVAL 3 DAY),
(6, '中通快递', 'ZTO', 'ZTO9876543210', 2, '【上海市】已签收，签收人：本人', NOW() - INTERVAL 17 DAY);

-- ============================================================
-- 15. 评价数据
-- ============================================================
INSERT INTO `review` (`order_item_id`, `user_id`, `product_id`, `rating`, `content`, `images`, `create_time`) VALUES
(3, 2, 100, 5, '很棒的笔记本！性能强劲，屏幕显示效果非常好',        '["/images/review/100_1.jpg"]', NOW() - INTERVAL 6 DAY),
(12, 5, 103, 4, '降噪效果不错，佩戴舒适，就是续航可以再长点',     NULL, NOW() - INTERVAL 20 DAY),
(1, 2, 100, 5, '第二次购买了，送朋友的',                          NULL, NOW() - INTERVAL 5 DAY),
(2, 2, 103, 4, '音质好，降噪效果满意',                           '["/images/review/103_1.jpg"]', NOW() - INTERVAL 5 DAY);

-- ============================================================
-- 16. 退款申请
-- ============================================================
INSERT INTO `refund` (`order_id`, `order_item_id`, `type`, `reason`, `amount`, `status`, `create_time`) VALUES
(7, 10, 2, '夹克尺码偏大，申请退货退款', 398.00, 0, NOW() - INTERVAL 1 DAY);

-- ============================================================
-- 17. 积分记录
-- ============================================================
INSERT INTO `points_record` (`user_id`, `change_amount`, `type`, `related_order_id`, `create_time`) VALUES
(2, 200, 1, 2, NOW() - INTERVAL 10 DAY),   -- user1 下单积分
(2, -50, 2, 1, NOW() - INTERVAL 30 MINUTE), -- user1 抵扣积分
(3, 100, 1, 4, NOW() - INTERVAL 2 DAY),     -- user2 下单积分
(4, 300, 1, 6, NOW() - INTERVAL 18 DAY),    -- user3 下单积分
(5, 100, 1, 9, NOW() - INTERVAL 23 DAY),    -- user4 下单积分
(5, -50, 2, 8, NOW() - INTERVAL 1 HOUR),    -- user4 抵扣积分
(6, 50,  1, 5, NOW() - INTERVAL 4 DAY),     -- user5 下单积分
(2, 100, 1, 1, NOW() - INTERVAL 30 MINUTE); -- user1 订单1积分

-- ============================================================
-- 18. 用户行为数据（推荐算法用）
--    behavior_type: 1-浏览, 2-收藏, 3-购买, 4-评价
-- ============================================================
-- user1 (ID=2): 高活跃数码产品用户
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(2, 100, 3, 3.00, NOW() - INTERVAL 1 DAY),
(2, 100, 4, 4.00, NOW() - INTERVAL 1 DAY),
(2, 103, 3, 3.00, NOW() - INTERVAL 2 DAY),
(2, 103, 2, 2.00, NOW() - INTERVAL 2 DAY),
(2, 105, 1, 1.00, NOW() - INTERVAL 1 DAY),
(2, 105, 3, 3.00, NOW() - INTERVAL 1 DAY),
(2, 101, 1, 1.00, NOW() - INTERVAL 10 DAY),
(2, 101, 2, 2.00, NOW() - INTERVAL 10 DAY),
(2, 104, 1, 1.00, NOW() - INTERVAL 12 DAY),
(2, 106, 1, 1.00, NOW() - INTERVAL 8 DAY),
(2, 106, 2, 2.00, NOW() - INTERVAL 8 DAY),
(2, 100, 1, 1.00, NOW() - INTERVAL 20 DAY),
(2, 103, 1, 1.00, NOW() - INTERVAL 25 DAY),
(2, 107, 1, 1.00, NOW() - INTERVAL 22 DAY),
(2, 108, 1, 1.00, NOW() - INTERVAL 28 DAY);

-- user2 (ID=3): 中等活跃跨品类用户
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(3, 103, 1, 1.00, NOW() - INTERVAL 2 DAY),
(3, 103, 3, 3.00, NOW() - INTERVAL 2 DAY),
(3, 107, 1, 1.00, NOW() - INTERVAL 1 DAY),
(3, 107, 3, 3.00, NOW() - INTERVAL 1 DAY),
(3, 109, 1, 1.00, NOW() - INTERVAL 3 DAY),
(3, 109, 2, 2.00, NOW() - INTERVAL 3 DAY),
(3, 100, 1, 1.00, NOW() - INTERVAL 10 DAY),
(3, 100, 2, 2.00, NOW() - INTERVAL 10 DAY),
(3, 104, 1, 1.00, NOW() - INTERVAL 12 DAY),
(3, 108, 1, 1.00, NOW() - INTERVAL 8 DAY),
(3, 105, 1, 1.00, NOW() - INTERVAL 20 DAY),
(3, 110, 1, 1.00, NOW() - INTERVAL 18 DAY);

-- user3 (ID=4): 数码产品忠实用户（与 user1 高度相似，测试 UserCF）
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(4, 101, 1, 1.00, NOW() - INTERVAL 1 DAY),
(4, 101, 3, 3.00, NOW() - INTERVAL 1 DAY),
(4, 101, 4, 4.00, NOW() - INTERVAL 1 DAY),
(4, 106, 1, 1.00, NOW() - INTERVAL 2 DAY),
(4, 106, 2, 2.00, NOW() - INTERVAL 2 DAY),
(4, 103, 1, 1.00, NOW() - INTERVAL 1 DAY),
(4, 100, 1, 1.00, NOW() - INTERVAL 9 DAY),
(4, 105, 1, 1.00, NOW() - INTERVAL 11 DAY),
(4, 104, 1, 1.00, NOW() - INTERVAL 13 DAY),
(4, 101, 1, 1.00, NOW() - INTERVAL 22 DAY),
(4, 106, 1, 1.00, NOW() - INTERVAL 25 DAY);

-- user4 (ID=5): 服饰类忠实用户
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(5, 107, 1, 1.00, NOW() - INTERVAL 2 DAY),
(5, 107, 2, 2.00, NOW() - INTERVAL 2 DAY),
(5, 107, 3, 3.00, NOW() - INTERVAL 2 DAY),
(5, 108, 1, 1.00, NOW() - INTERVAL 1 DAY),
(5, 108, 2, 2.00, NOW() - INTERVAL 1 DAY),
(5, 108, 3, 3.00, NOW() - INTERVAL 1 DAY),
(5, 109, 1, 1.00, NOW() - INTERVAL 3 DAY),
(5, 109, 3, 3.00, NOW() - INTERVAL 3 DAY),
(5, 110, 1, 1.00, NOW() - INTERVAL 10 DAY),
(5, 110, 2, 2.00, NOW() - INTERVAL 10 DAY),
(5, 111, 1, 1.00, NOW() - INTERVAL 12 DAY),
(5, 107, 1, 1.00, NOW() - INTERVAL 20 DAY),
(5, 108, 1, 1.00, NOW() - INTERVAL 25 DAY);

-- user5 (ID=6): 低活跃用户（冷启动测试）
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(6, 107, 1, 1.00, NOW() - INTERVAL 5 DAY),
(6, 107, 3, 3.00, NOW() - INTERVAL 5 DAY),
(6, 112, 1, 1.00, NOW() - INTERVAL 3 DAY),
(6, 112, 2, 2.00, NOW() - INTERVAL 3 DAY);

-- user6 (ID=7): 高活跃数码用户（测试 UserCF 多样性）
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(7, 100, 1, 1.00, NOW() - INTERVAL 1 DAY),
(7, 100, 2, 2.00, NOW() - INTERVAL 1 DAY),
(7, 100, 3, 3.00, NOW() - INTERVAL 1 DAY),
(7, 105, 1, 1.00, NOW() - INTERVAL 2 DAY),
(7, 105, 2, 2.00, NOW() - INTERVAL 2 DAY),
(7, 103, 1, 1.00, NOW() - INTERVAL 1 DAY),
(7, 103, 3, 3.00, NOW() - INTERVAL 1 DAY),
(7, 104, 1, 1.00, NOW() - INTERVAL 3 DAY),
(7, 101, 1, 1.00, NOW() - INTERVAL 9 DAY),
(7, 106, 1, 1.00, NOW() - INTERVAL 11 DAY),
(7, 100, 1, 1.00, NOW() - INTERVAL 20 DAY),
(7, 105, 1, 1.00, NOW() - INTERVAL 25 DAY);

-- user7 (ID=8): 极低活跃用户（冷启动边界）
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(8, 100, 1, 1.00, NOW() - INTERVAL 10 DAY),
(8, 107, 1, 1.00, NOW() - INTERVAL 15 DAY);

-- user8 (ID=9): 纯新用户，无行为数据
-- (无记录)

-- 辅助用户 user9~user14 (ID=10~15)
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `behavior_weight`, `create_time`) VALUES
(10, 100, 1, 1.00, NOW() - INTERVAL 2 DAY),
(10, 100, 3, 3.00, NOW() - INTERVAL 2 DAY),
(10, 103, 1, 1.00, NOW() - INTERVAL 1 DAY),
(10, 103, 2, 2.00, NOW() - INTERVAL 1 DAY),
(10, 105, 1, 1.00, NOW() - INTERVAL 3 DAY),
(10, 101, 1, 1.00, NOW() - INTERVAL 5 DAY),
(10, 104, 1, 1.00, NOW() - INTERVAL 7 DAY),
(11, 107, 1, 1.00, NOW() - INTERVAL 2 DAY),
(11, 107, 3, 3.00, NOW() - INTERVAL 2 DAY),
(11, 108, 1, 1.00, NOW() - INTERVAL 1 DAY),
(11, 108, 2, 2.00, NOW() - INTERVAL 1 DAY),
(11, 109, 1, 1.00, NOW() - INTERVAL 3 DAY),
(11, 110, 1, 1.00, NOW() - INTERVAL 5 DAY),
(11, 111, 1, 1.00, NOW() - INTERVAL 7 DAY),
(12, 100, 1, 1.00, NOW() - INTERVAL 3 DAY),
(12, 107, 1, 1.00, NOW() - INTERVAL 2 DAY),
(12, 109, 1, 1.00, NOW() - INTERVAL 1 DAY),
(12, 103, 1, 1.00, NOW() - INTERVAL 5 DAY),
(12, 110, 1, 1.00, NOW() - INTERVAL 4 DAY),
(13, 100, 1, 1.00, NOW() - INTERVAL 5 DAY),
(14, 103, 1, 1.00, NOW() - INTERVAL 3 DAY),
(15, 107, 1, 1.00, NOW() - INTERVAL 4 DAY);

-- ============================================================
-- 19. 用户评分矩阵
-- ============================================================
INSERT INTO `user_score` (`user_id`, `product_id`, `score`) VALUES
(2, 100, 5.00), (2, 101, 3.50), (2, 103, 4.50), (2, 104, 2.00),
(2, 105, 4.80), (2, 106, 3.00), (2, 107, 1.50), (2, 108, 1.20),
(3, 100, 3.00), (3, 103, 3.50), (3, 104, 2.50), (3, 105, 1.50),
(3, 107, 3.20), (3, 108, 1.80), (3, 109, 2.80), (3, 110, 1.50),
(4, 101, 4.50), (4, 106, 3.80), (4, 103, 2.50), (4, 100, 1.80),
(4, 104, 1.50), (4, 105, 1.20),
(5, 107, 5.00), (5, 108, 4.50), (5, 109, 4.00), (5, 110, 3.50),
(5, 111, 3.00), (5, 112, 2.00);

-- ============================================================
-- 20. 推荐结果缓存
--    algorithm_type: 1-UserCF, 2-ItemCF, 3-混合, 4-热门兜底
-- ============================================================
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
-- user1 (ID=2): 高活跃混合推荐
(2, 101, 3, 0.9520, NOW()), (2, 103, 3, 0.8930, NOW()),
(2, 105, 3, 0.8750, NOW()), (2, 104, 3, 0.7200, NOW()),
(2, 106, 3, 0.6850, NOW()), (2, 107, 3, 0.4100, NOW()),
(2, 108, 3, 0.3500, NOW()), (2, 109, 3, 0.2800, NOW()),
(2, 110, 3, 0.1500, NOW()), (2, 111, 3, 0.1200, NOW()),
-- user2 (ID=3): 中等活跃混合
(3, 109, 3, 0.9100, NOW()), (3, 107, 3, 0.8800, NOW()),
(3, 103, 3, 0.6500, NOW()), (3, 100, 3, 0.5800, NOW()),
(3, 108, 3, 0.5200, NOW()), (3, 110, 3, 0.4500, NOW()),
(3, 104, 3, 0.3200, NOW()), (3, 105, 3, 0.2800, NOW()),
(3, 111, 3, 0.1900, NOW()), (3, 112, 3, 0.1500, NOW()),
-- user5 (ID=6): 低活跃 ItemCF + 热门
(6, 112, 3, 0.7500, NOW()), (6, 107, 3, 0.6200, NOW()),
(6, 108, 3, 0.5800, NOW()), (6, 109, 3, 0.4500, NOW()),
(6, 110, 3, 0.4100, NOW()), (6, 100, 4, 0.9000, NOW()),
(6, 105, 4, 0.8500, NOW()), (6, 103, 4, 0.7800, NOW()),
(6, 111, 3, 0.3200, NOW()), (6, 104, 3, 0.2800, NOW()),
-- user8 (ID=9): 纯新用户热门兜底
(9, 100, 4, 0.9500, NOW()), (9, 105, 4, 0.8800, NOW()),
(9, 103, 4, 0.8200, NOW()), (9, 107, 4, 0.7500, NOW()),
(9, 106, 4, 0.6800, NOW()), (9, 108, 4, 0.6000, NOW()),
(9, 110, 4, 0.5200, NOW()), (9, 101, 4, 0.4800, NOW()),
(9, 112, 4, 0.4000, NOW()), (9, 109, 4, 0.3500, NOW()),
-- 未登录用户全局热门
(NULL, 100, 4, 0.9500, NOW()), (NULL, 105, 4, 0.8800, NOW()),
(NULL, 103, 4, 0.8200, NOW()), (NULL, 107, 4, 0.7500, NOW()),
(NULL, 106, 4, 0.6800, NOW()), (NULL, 108, 4, 0.6000, NOW()),
(NULL, 110, 4, 0.5200, NOW()), (NULL, 101, 4, 0.4800, NOW()),
(NULL, 112, 4, 0.4000, NOW()), (NULL, 109, 4, 0.3500, NOW());

-- ============================================================
-- 21. 验证查询
-- ============================================================
-- SELECT 'user' AS tbl, COUNT(*) AS cnt FROM user WHERE id <= 21
-- UNION ALL SELECT 'shop', COUNT(*) FROM shop
-- UNION ALL SELECT 'product', COUNT(*) FROM product WHERE id BETWEEN 100 AND 112
-- UNION ALL SELECT 'sku', COUNT(*) FROM sku
-- UNION ALL SELECT 'address', COUNT(*) FROM address
-- UNION ALL SELECT 'coupon', COUNT(*) FROM coupon
-- UNION ALL SELECT 'user_coupon', COUNT(*) FROM user_coupon
-- UNION ALL SELECT 'promotion', COUNT(*) FROM promotion
-- UNION ALL SELECT 'cart', COUNT(*) FROM cart
-- UNION ALL SELECT 'orders', COUNT(*) FROM orders
-- UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
-- UNION ALL SELECT 'payment', COUNT(*) FROM payment
-- UNION ALL SELECT 'logistics', COUNT(*) FROM logistics
-- UNION ALL SELECT 'review', COUNT(*) FROM review
-- UNION ALL SELECT 'refund', COUNT(*) FROM refund
-- UNION ALL SELECT 'points_record', COUNT(*) FROM points_record
-- UNION ALL SELECT 'user_behavior', COUNT(*) FROM user_behavior
-- UNION ALL SELECT 'user_score', COUNT(*) FROM user_score
-- UNION ALL SELECT 'recommend_result', COUNT(*) FROM recommend_result
-- ORDER BY tbl;
