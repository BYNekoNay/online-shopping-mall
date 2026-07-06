-- ============================================================
-- Mall Database Schema
-- Generated from 10-数据库规范.md
-- MySQL 8.0+, utf8mb4, InnoDB
-- ============================================================

DROP DATABASE IF EXISTS mall;
CREATE DATABASE mall CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mall;

-- ============================================================
-- 2.1 用户与店铺
-- ============================================================

CREATE TABLE `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色：1-消费者，2-商家，3-管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `points` INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `shop` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `merchant_user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联user.id（角色=商家）',
  `name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
  `logo` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
  `decoration_config` JSON DEFAULT NULL COMMENT '店铺首页装修配置',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺等级',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-正常，2-已拒绝，3-已禁用',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '入驻申请联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '入驻申请联系电话',
  `license_no` VARCHAR(50) NOT NULL COMMENT '营业执照编号',
  `license_image` VARCHAR(255) NOT NULL COMMENT '营业执照图片URL',
  `apply_reason` VARCHAR(500) DEFAULT NULL COMMENT '申请入驻说明',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核拒绝原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_user_id` (`merchant_user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺信息表';

-- ============================================================
-- 2.2 商品
-- ============================================================

CREATE TABLE `category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
  `name` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE `product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT UNSIGNED NOT NULL,
  `category_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `main_image` VARCHAR(255) DEFAULT NULL,
  `images` JSON DEFAULT NULL COMMENT '多图URL数组',
  `detail` LONGTEXT DEFAULT NULL COMMENT '富文本详情',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `status` TINYINT NOT NULL DEFAULT 2 COMMENT '0-下架，1-上架，2-待审核，3-审核拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE `sku` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `spec_json` VARCHAR(500) NOT NULL COMMENT '规格组合，如{"颜色":"红色","尺码":"L"}',
  `price` DECIMAL(10,2) NOT NULL,
  `stock` INT NOT NULL DEFAULT 0,
  `image` VARCHAR(255) DEFAULT NULL COMMENT '规格图',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU规格表';

CREATE TABLE `freight_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `region_rule_json` JSON NOT NULL COMMENT '按省份/地区的运费规则数组',
  `free_shipping_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '包邮金额门槛',
  `default_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '默认运费',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板表';

-- ============================================================
-- 2.3 购物车与订单
-- ============================================================

CREATE TABLE `cart` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `sku_id` BIGINT UNSIGNED DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `selected` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否选中：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product_sku` (`user_id`,`product_id`,`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `user_id` BIGINT UNSIGNED NOT NULL,
  `shop_id` BIGINT UNSIGNED NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额（商品金额+运费）',
  `freight_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '运费金额',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待付款，1-待发货，2-已发货，3-已收货，4-已完成，5-已取消，6-退款中，7-已退款',
  `address_snapshot` JSON NOT NULL COMMENT '下单时收货地址快照',
  `pay_type` TINYINT DEFAULT NULL COMMENT '1-余额，2-模拟支付宝',
  `pay_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '用户下单备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_status_create_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `sku_id` BIGINT UNSIGNED DEFAULT NULL,
  `product_name_snapshot` VARCHAR(200) NOT NULL COMMENT '下单时商品名称快照',
  `product_image_snapshot` VARCHAR(255) DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL COMMENT '下单时单价快照',
  `quantity` INT NOT NULL,
  `is_gift` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为满赠赠品行：1-是，0-正常购买行',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

CREATE TABLE `address` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `receiver` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `district` VARCHAR(50) NOT NULL,
  `detail` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

CREATE TABLE `review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_item_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `rating` TINYINT NOT NULL COMMENT '1-5分',
  `content` VARCHAR(1000) DEFAULT NULL,
  `images` JSON DEFAULT NULL COMMENT '评价图片URL数组',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_item_id` (`order_item_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

CREATE TABLE `refund` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `order_item_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '为空表示整单售后',
  `type` TINYINT NOT NULL COMMENT '1-仅退款，2-退货退款',
  `reason` VARCHAR(255) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL COMMENT '申请退款金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核，1-审核通过，2-审核拒绝，3-已退款',
  `handle_remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款/退货申请表';

-- ============================================================
-- 2.4 营销、支付、物流
-- ============================================================

CREATE TABLE `coupon` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1-新人券，2-满减券，3-品类券，4-店铺券',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '店铺券/品类券归属店铺，平台券为空',
  `discount_rule` JSON NOT NULL COMMENT '优惠规则：满减阈值/折扣率等',
  `valid_from` DATETIME NOT NULL,
  `valid_to` DATETIME NOT NULL,
  `stock` INT NOT NULL DEFAULT 0 COMMENT '发放总量',
  `received_count` INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

CREATE TABLE `user_coupon` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `coupon_id` BIGINT UNSIGNED NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未使用，1-已使用，2-已过期',
  `use_time` DATETIME DEFAULT NULL,
  `related_order_id` BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领取的优惠券表';

CREATE TABLE `promotion` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1-限时折扣，2-满减，3-满赠，4-组合套餐',
  `rule_json` JSON NOT NULL,
  `scope` VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' COMMENT '作用范围：PRODUCT/CATEGORY/SHOP',
  `scope_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '对应scope的目标ID',
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-已下线，1-生效中',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_scope` (`scope`,`scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='促销活动表';

CREATE TABLE `points_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `change_amount` INT NOT NULL COMMENT '变动数量，正数为获取，负数为消耗',
  `type` TINYINT NOT NULL COMMENT '1-下单获取，2-订单抵扣，3-兑换',
  `related_order_id` BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

CREATE TABLE `payment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `pay_no` VARCHAR(32) NOT NULL COMMENT '支付流水号',
  `amount` DECIMAL(10,2) NOT NULL,
  `pay_type` TINYINT NOT NULL COMMENT '1-余额，2-模拟支付宝',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-支付中，1-成功，2-失败',
  `callback_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

CREATE TABLE `logistics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `company` VARCHAR(50) NOT NULL COMMENT '物流公司名称',
  `company_code` VARCHAR(20) DEFAULT NULL COMMENT '物流公司编码',
  `tracking_no` VARCHAR(50) NOT NULL COMMENT '物流单号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-已发货，1-运输中，2-已签收，3-查询异常',
  `last_track_info` VARCHAR(500) DEFAULT NULL COMMENT '最近一条物流轨迹',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_tracking_no` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息表';

-- ============================================================
-- 2.5 用户行为、推荐与统计
-- ============================================================

CREATE TABLE `user_behavior` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `behavior_type` TINYINT NOT NULL COMMENT '1-浏览，2-收藏，3-购买，4-评价',
  `behavior_weight` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '行为权重',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_product_type` (`user_id`,`product_id`,`behavior_type`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

CREATE TABLE `user_score` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `score` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '综合评分',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-商品评分矩阵表';

CREATE TABLE `recommend_result` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '为空表示全局热门兜底结果',
  `product_id` BIGINT UNSIGNED NOT NULL,
  `algorithm_type` TINYINT NOT NULL COMMENT '1-UserCF，2-ItemCF，3-混合，4-热门兜底',
  `score` DECIMAL(6,4) NOT NULL,
  `generate_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id_score` (`user_id`,`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果缓存表';

CREATE TABLE `search_history` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `keyword` VARCHAR(100) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id_create_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索历史表';

CREATE TABLE `page_view_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '未登录访客为空',
  `session_id` VARCHAR(64) NOT NULL,
  `page_path` VARCHAR(255) NOT NULL,
  `referrer_page` VARCHAR(255) DEFAULT NULL,
  `enter_time` DATETIME NOT NULL,
  `leave_time` DATETIME DEFAULT NULL,
  `stay_duration` INT DEFAULT NULL COMMENT '停留时长（秒）',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_enter_time` (`enter_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志表';

-- ============================================================
-- 2.6 系统管理
-- ============================================================

CREATE TABLE `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT UNSIGNED NOT NULL,
  `operator_role` TINYINT NOT NULL COMMENT '操作人角色',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作描述',
  `target` VARCHAR(100) DEFAULT NULL COMMENT '操作目标',
  `ip` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE `dict` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dict_type` VARCHAR(50) NOT NULL COMMENT '字典类型',
  `dict_key` VARCHAR(50) NOT NULL,
  `dict_value` VARCHAR(100) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_key` (`dict_type`,`dict_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认管理员账号（密码: admin123，BCrypt加密后存入，此处先用明文占位，后续用INSERT INTO ... VALUES (..., '$2a$10$...', ...)）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 3, 1);

-- 商品分类初始数据
INSERT INTO `category` (`parent_id`, `name`, `sort`) VALUES
(0, '电子产品', 1),
(0, '服装鞋帽', 2),
(0, '食品饮料', 3),
(0, '家居生活', 4),
(0, '美妆个护', 5),
(1, '手机', 1),
(1, '电脑', 2),
(1, '耳机音箱', 3),
(2, '男装', 1),
(2, '女装', 2),
(2, '运动鞋', 3);

-- 物流公司字典
INSERT INTO `dict` (`dict_type`, `dict_key`, `dict_value`, `sort`) VALUES
('LOGISTICS_COMPANY', 'SF', '顺丰速运', 1),
('LOGISTICS_COMPANY', 'YTO', '圆通速递', 2),
('LOGISTICS_COMPANY', 'ZTO', '中通快递', 3),
('LOGISTICS_COMPANY', 'STO', '申通快递', 4),
('LOGISTICS_COMPANY', 'YD', '韵达快递', 5),
('LOGISTICS_COMPANY', 'HTKY', '百世快递', 6);

-- 订单状态字典
INSERT INTO `dict` (`dict_type`, `dict_key`, `dict_value`, `sort`) VALUES
('ORDER_STATUS', '0', '待付款', 1),
('ORDER_STATUS', '1', '待发货', 2),
('ORDER_STATUS', '2', '已发货', 3),
('ORDER_STATUS', '3', '已收货', 4),
('ORDER_STATUS', '4', '已完成', 5),
('ORDER_STATUS', '5', '已取消', 6),
('ORDER_STATUS', '6', '退款中', 7),
('ORDER_STATUS', '7', '已退款', 8);
