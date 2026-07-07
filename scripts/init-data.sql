-- ============================================================
-- 初始化数据脚本
-- 在 V1__init_schema.sql 之后执行
-- ============================================================

USE mall;

-- 管理员初始账号（密码: admin123）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`, `points`)
VALUES (
    'admin',
    '$2b$10$zED.4.xcM8BJVOoDY4RaoOhAWf95O9Bsn6N8qCrYmsIa6vzyV24eC',
    '系统管理员',
    3,
    1,
    0
);

-- 测试消费者账号（密码: user123）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`, `points`)
VALUES (
    'testuser',
    '$2b$10$KnoKqbk8aBkq9qP/mbV6eO2fxlEf63TaIlVGLnGGLgSu2F/.IvyZy',
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

-- 运费模板示例（全国默认运费）
INSERT INTO `freight_template` (`shop_id`, `name`, `type`, `default_fee`, `free_threshold`, `rule_json`, `is_default`)
VALUES
    (1, '默认全国运费', 1, 10.00, 99.00, '[]', 1);

-- 优惠券示例
INSERT INTO `coupon` (`name`, `type`, `discount_rule`, `min_amount`, `total_count`, `remain_count`, `valid_from`, `valid_to`, `status`, `create_time`)
VALUES
    ('新人立减10元', 1, '{"type":"fixed","value":10}', 50.00, 1000, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW()),
    ('满200减20', 2, '{"type":"threshold","threshold":200,"reduce":20}', 200.00, 500, 500, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW());

-- 促销活动示例
INSERT INTO `promotion` (`name`, `type`, `rule_json`, `scope`, `scope_id`, `start_time`, `end_time`, `status`, `create_time`)
VALUES
    ('限时8折', 1, '{"discountPercent":0.8}', 'SHOP', 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, NOW()),
    ('满100减15', 2, '{"threshold":100,"reduce":15}', 'SHOP', 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, NOW());
