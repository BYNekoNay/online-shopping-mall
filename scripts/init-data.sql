-- ============================================================
-- 初始化数据脚本
-- 在 V1__init_schema.sql 之后执行
-- 注意：管理员账号 admin 已由 V1__init_schema.sql 创建，此处不再重复插入
-- ============================================================

USE mall;

-- 测试消费者账号（H-11 修复：密码由 user123 轮换为 Mall@2026）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`, `points`)
VALUES (
    'testuser',
    '$2a$10$WvEVuxzCG5dhPoM7ZXN9cuKBo52OX7nySsVN/3/TiP0AKBD5NAt6u',
    '测试用户',
    1,
    1,
    100
);

-- 系统配置初始数据
INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
    ('site_name', '智慧商城', '网站名称'),
    ('order_timeout_minutes', '30', '订单超时取消时间（分钟）'),
    ('recommend_refresh_interval', '86400', '推荐结果刷新间隔（秒），默认1天'),
    ('upload_max_size', '10485760', '上传文件大小限制（字节），默认10MB')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- 运费模板示例（全国默认运费，满99包邮）
INSERT INTO `freight_template` (`shop_id`, `name`, `region_rule_json`, `free_shipping_threshold`, `default_fee`)
VALUES
    (1, '默认全国运费', '[]', 99.00, 10.00);

-- 优惠券示例（discount_rule 格式与 CouponService 解析逻辑一致：{threshold, discount}）
INSERT INTO `coupon` (`name`, `type`, `discount_rule`, `stock`, `received_count`, `valid_from`, `valid_to`, `create_time`)
VALUES
    ('新人立减10元', 1, '{"threshold":0,"discount":10}', 1000, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW()),
    ('满200减20', 2, '{"threshold":200,"discount":20}', 500, 0, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW());

-- 促销活动示例
INSERT INTO `promotion` (`name`, `type`, `rule_json`, `scope`, `scope_id`, `start_time`, `end_time`, `status`, `create_time`)
VALUES
    ('限时8折', 1, '{"discountPercent":0.8}', 'SHOP', 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, NOW()),
    ('满100减15', 2, '{"threshold":100,"reduce":15}', 'SHOP', 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, NOW());

-- C-4 物流公司字典种子数据
INSERT INTO `logistics_company` (`name`, `code`, `sort`, `status`) VALUES
    ('顺丰速运', 'SF', 1, 1),
    ('圆通速递', 'YTO', 2, 1),
    ('中通快递', 'ZTO', 3, 1),
    ('韵达快递', 'YD', 4, 1),
    ('邮政EMS', 'EMS', 5, 1);

-- C-1 积分商城商品种子数据
INSERT INTO `points_goods` (`name`, `image`, `points_cost`, `stock`, `description`, `status`) VALUES
    ('定制帆布袋', NULL, 500, 100, '环保帆布袋，限量定制', 1),
    ('商城纪念徽章', NULL, 300, 200, '平台周年纪念徽章', 1),
    ('20元无门槛券', NULL, 2000, 50, '全平台通用优惠券（下单抵扣）', 1);
