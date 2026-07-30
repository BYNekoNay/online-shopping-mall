-- ============================================================
-- Recommend Module Seed Data
-- 推荐算法联调与离线评估专用模拟数据
--
-- 设计目标：
--   1. 为 UserCF / ItemCF / 混合策略提供有意义的测试数据
--   2. 覆盖高活跃 / 中等活跃 / 低活跃 / 纯新用户四种场景
--   3. 时间跨度覆盖近 30 天，使时间衰减函数产生差异化效果
--   4. 预留离线评估用训练集/测试集切分标记
--
-- 依赖：V1__init_schema.sql 已执行（表结构 + 管理员账号）
--       R__seed_mock_behavior.sql 已执行（M-36 修复：演示用户 user_id 2~21 由其创建）
-- 支持重复执行：先清理再插入
-- ============================================================

-- ============================================================
-- 0. 清理已有推荐相关模拟数据
-- ============================================================
DELETE FROM user_behavior WHERE user_id BETWEEN 2 AND 20;
DELETE FROM recommend_result WHERE user_id BETWEEN 2 AND 20 OR user_id IS NULL;

-- ============================================================
-- 1. 用户行为数据（带时间戳，用于时间衰减 + 离线评估）
-- ============================================================
-- 商品 ID 速查：
--   100=LaptopPro15, 101=LaptopAir14, 102=TabletPro,
--   103=Headphones, 104=Speaker, 105=PhoneX1, 106=PhoneLite,
--   107=TShirt, 108=Jeans, 109=Dress, 110=Sneakers,
--   111=Jacket, 112=RunningShoes
--
-- 行为类型：1=浏览, 2=收藏, 3=购买, 4=评价
--
-- 时间戳策略：
--   - 近 3 天：高频交互（模拟活跃用户近期行为）
--   - 7~14 天：中频交互（模拟一般兴趣）
--   - 15~30 天：低频交互（模拟历史兴趣）
--   - 超过 30 天：极低频（时间衰减后权重趋近 0）

-- ============================================================
-- User 1 (user1, ID=2): 高活跃数字产品用户
-- 行为数 >= T_HIGH(20)，用于测试 UserCF 高权重混合
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
-- 近 3 天：强交互（购买 + 评价）
(2, 100, 3, NOW() - INTERVAL 1 DAY),
(2, 100, 4, NOW() - INTERVAL 1 DAY),
(2, 103, 3, NOW() - INTERVAL 2 DAY),
(2, 103, 2, NOW() - INTERVAL 2 DAY),
(2, 105, 1, NOW() - INTERVAL 1 DAY),
(2, 105, 3, NOW() - INTERVAL 1 DAY),
-- 7~14 天：中等交互
(2, 101, 1, NOW() - INTERVAL 10 DAY),
(2, 101, 2, NOW() - INTERVAL 10 DAY),
(2, 104, 1, NOW() - INTERVAL 12 DAY),
(2, 106, 1, NOW() - INTERVAL 8 DAY),
(2, 106, 2, NOW() - INTERVAL 8 DAY),
-- 15~30 天：历史浏览
(2, 100, 1, NOW() - INTERVAL 20 DAY),
(2, 103, 1, NOW() - INTERVAL 25 DAY),
(2, 105, 1, NOW() - INTERVAL 18 DAY),
(2, 107, 1, NOW() - INTERVAL 22 DAY),
(2, 108, 1, NOW() - INTERVAL 28 DAY),
-- 超过 30 天：极低频
(2, 101, 1, NOW() - INTERVAL 35 DAY),
(2, 104, 1, NOW() - INTERVAL 40 DAY);

-- ============================================================
-- User 2 (user2, ID=3): 中等活跃跨品类用户
-- 行为数在 [T_LOW, T_HIGH) 区间，用于测试 α=0.4 混合
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
-- 近 3 天
(3, 103, 1, NOW() - INTERVAL 2 DAY),
(3, 103, 3, NOW() - INTERVAL 2 DAY),
(3, 107, 1, NOW() - INTERVAL 1 DAY),
(3, 107, 3, NOW() - INTERVAL 1 DAY),
(3, 109, 1, NOW() - INTERVAL 3 DAY),
(3, 109, 2, NOW() - INTERVAL 3 DAY),
-- 7~14 天
(3, 100, 1, NOW() - INTERVAL 10 DAY),
(3, 100, 2, NOW() - INTERVAL 10 DAY),
(3, 104, 1, NOW() - INTERVAL 12 DAY),
(3, 108, 1, NOW() - INTERVAL 8 DAY),
-- 15~30 天
(3, 105, 1, NOW() - INTERVAL 20 DAY),
(3, 110, 1, NOW() - INTERVAL 18 DAY);

-- ============================================================
-- User 3 (user3, ID=4): 数字产品忠实用户
-- 与 User 1 高度相似（测试 UserCF 相似度计算）
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
-- 近 3 天
(4, 101, 1, NOW() - INTERVAL 1 DAY),
(4, 101, 3, NOW() - INTERVAL 1 DAY),
(4, 101, 4, NOW() - INTERVAL 1 DAY),
(4, 106, 1, NOW() - INTERVAL 2 DAY),
(4, 106, 2, NOW() - INTERVAL 2 DAY),
(4, 103, 1, NOW() - INTERVAL 1 DAY),
-- 7~14 天
(4, 100, 1, NOW() - INTERVAL 9 DAY),
(4, 105, 1, NOW() - INTERVAL 11 DAY),
(4, 104, 1, NOW() - INTERVAL 13 DAY),
-- 15~30 天
(4, 101, 1, NOW() - INTERVAL 22 DAY),
(4, 106, 1, NOW() - INTERVAL 25 DAY);

-- ============================================================
-- User 4 (user4, ID=5): 服饰类忠实用户
-- 与 User 2 部分相似（跨品类测试）
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
-- 近 3 天
(5, 107, 1, NOW() - INTERVAL 2 DAY),
(5, 107, 2, NOW() - INTERVAL 2 DAY),
(5, 107, 3, NOW() - INTERVAL 2 DAY),
(5, 108, 1, NOW() - INTERVAL 1 DAY),
(5, 108, 2, NOW() - INTERVAL 1 DAY),
(5, 109, 1, NOW() - INTERVAL 3 DAY),
(5, 109, 3, NOW() - INTERVAL 3 DAY),
-- 7~14 天
(5, 110, 1, NOW() - INTERVAL 10 DAY),
(5, 110, 2, NOW() - INTERVAL 10 DAY),
(5, 111, 1, NOW() - INTERVAL 12 DAY),
-- 15~30 天
(5, 107, 1, NOW() - INTERVAL 20 DAY),
(5, 108, 1, NOW() - INTERVAL 25 DAY),
(5, 109, 1, NOW() - INTERVAL 18 DAY);

-- ============================================================
-- User 5 (user5, ID=6): 低活跃用户（冷启动中间态）
-- 行为数在 [0, T_LOW) 区间，用于测试 ItemCF + 热门补位
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(6, 107, 1, NOW() - INTERVAL 5 DAY),
(6, 107, 3, NOW() - INTERVAL 5 DAY),
(6, 112, 1, NOW() - INTERVAL 3 DAY),
(6, 112, 2, NOW() - INTERVAL 3 DAY);

-- ============================================================
-- User 6 (user6, ID=7): 另类高活跃用户（测试 UserCF 多样性）
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
-- 近 3 天
(7, 100, 1, NOW() - INTERVAL 1 DAY),
(7, 100, 2, NOW() - INTERVAL 1 DAY),
(7, 100, 3, NOW() - INTERVAL 1 DAY),
(7, 105, 1, NOW() - INTERVAL 2 DAY),
(7, 105, 2, NOW() - INTERVAL 2 DAY),
(7, 103, 1, NOW() - INTERVAL 1 DAY),
(7, 103, 3, NOW() - INTERVAL 1 DAY),
(7, 104, 1, NOW() - INTERVAL 3 DAY),
-- 7~14 天
(7, 101, 1, NOW() - INTERVAL 9 DAY),
(7, 106, 1, NOW() - INTERVAL 11 DAY),
-- 15~30 天
(7, 100, 1, NOW() - INTERVAL 20 DAY),
(7, 103, 1, NOW() - INTERVAL 22 DAY),
(7, 105, 1, NOW() - INTERVAL 25 DAY);

-- ============================================================
-- User 7 (user7, ID=8): 极低活跃用户（冷启动边界）
-- 行为数 < T_LOW(5)
-- ============================================================
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(8, 100, 1, NOW() - INTERVAL 10 DAY),
(8, 107, 1, NOW() - INTERVAL 15 DAY);

-- ============================================================
-- User 8 (user9, ID=9): 纯新用户（无任何行为）
-- 不插入 user_behavior，用于测试热门兜底
-- ============================================================
-- (无记录)

-- ============================================================
-- User 9~20 (ID=10~21): 补充用户，用于测试覆盖率和相似度
-- ============================================================

-- User 9 (ID=10): 电子产品爱好者，与 User 1 相似
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(10, 100, 1, NOW() - INTERVAL 2 DAY),
(10, 100, 3, NOW() - INTERVAL 2 DAY),
(10, 103, 1, NOW() - INTERVAL 1 DAY),
(10, 103, 2, NOW() - INTERVAL 1 DAY),
(10, 105, 1, NOW() - INTERVAL 3 DAY),
(10, 101, 1, NOW() - INTERVAL 5 DAY),
(10, 104, 1, NOW() - INTERVAL 7 DAY);

-- User 10 (ID=11): 服饰爱好者，与 User 4 相似
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(11, 107, 1, NOW() - INTERVAL 2 DAY),
(11, 107, 3, NOW() - INTERVAL 2 DAY),
(11, 108, 1, NOW() - INTERVAL 1 DAY),
(11, 108, 2, NOW() - INTERVAL 1 DAY),
(11, 109, 1, NOW() - INTERVAL 3 DAY),
(11, 110, 1, NOW() - INTERVAL 5 DAY),
(11, 111, 1, NOW() - INTERVAL 7 DAY);

-- User 11 (ID=12): 混合兴趣用户
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(12, 100, 1, NOW() - INTERVAL 3 DAY),
(12, 107, 1, NOW() - INTERVAL 2 DAY),
(12, 109, 1, NOW() - INTERVAL 1 DAY),
(12, 103, 1, NOW() - INTERVAL 5 DAY),
(12, 110, 1, NOW() - INTERVAL 4 DAY);

-- User 12~20 (ID=13~21): 稀疏行为用户（增加数据稀疏性，测试算法鲁棒性）
INSERT INTO `user_behavior` (`user_id`, `product_id`, `behavior_type`, `create_time`) VALUES
(13, 100, 1, NOW() - INTERVAL 5 DAY),
(14, 103, 1, NOW() - INTERVAL 3 DAY),
(15, 107, 1, NOW() - INTERVAL 4 DAY),
(16, 105, 1, NOW() - INTERVAL 6 DAY),
(17, 108, 1, NOW() - INTERVAL 2 DAY),
(18, 112, 1, NOW() - INTERVAL 3 DAY),
(19, 104, 1, NOW() - INTERVAL 5 DAY),
(20, 109, 1, NOW() - INTERVAL 4 DAY);

-- ============================================================
-- 2. 离线评估用：推荐结果预计算（供测试对比使用）
-- ============================================================
-- 这些记录模拟算法计算后的 recommend_result，
-- 用于前端联调时无需等待定时任务即可看到推荐结果。
-- algorithm_type: 1=UserCF, 2=ItemCF, 3=混合, 4=热门兜底

-- User 1 (ID=2): 高活跃用户 - 混合推荐
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
(2, 101, 3, 0.9520, NOW()),
(2, 103, 3, 0.8930, NOW()),
(2, 105, 3, 0.8750, NOW()),
(2, 104, 3, 0.7200, NOW()),
(2, 106, 3, 0.6850, NOW()),
(2, 107, 3, 0.4100, NOW()),
(2, 108, 3, 0.3500, NOW()),
(2, 109, 3, 0.2800, NOW()),
(2, 110, 3, 0.1500, NOW()),
(2, 111, 3, 0.1200, NOW());

-- User 2 (ID=3): 中等活跃用户 - 混合推荐（偏向 ItemCF）
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
(3, 109, 3, 0.9100, NOW()),
(3, 107, 3, 0.8800, NOW()),
(3, 103, 3, 0.6500, NOW()),
(3, 100, 3, 0.5800, NOW()),
(3, 108, 3, 0.5200, NOW()),
(3, 110, 3, 0.4500, NOW()),
(3, 104, 3, 0.3200, NOW()),
(3, 105, 3, 0.2800, NOW()),
(3, 111, 3, 0.1900, NOW()),
(3, 112, 3, 0.1500, NOW());

-- User 5 (ID=6): 低活跃用户 - ItemCF + 热门补位
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
(6, 112, 3, 0.7500, NOW()),
(6, 107, 3, 0.6200, NOW()),
(6, 108, 3, 0.5800, NOW()),
(6, 109, 3, 0.4500, NOW()),
(6, 110, 3, 0.4100, NOW()),
(6, 100, 4, 0.9000, NOW()),
(6, 105, 4, 0.8500, NOW()),
(6, 103, 4, 0.7800, NOW()),
(6, 111, 3, 0.3200, NOW()),
(6, 104, 3, 0.2800, NOW());

-- User 9 (ID=9): 纯新用户 - 热门兜底
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
(9, 100, 4, 0.9500, NOW()),
(9, 105, 4, 0.8800, NOW()),
(9, 103, 4, 0.8200, NOW()),
(9, 107, 4, 0.7500, NOW()),
(9, 106, 4, 0.6800, NOW()),
(9, 108, 4, 0.6000, NOW()),
(9, 110, 4, 0.5200, NOW()),
(9, 101, 4, 0.4800, NOW()),
(9, 112, 4, 0.4000, NOW()),
(9, 109, 4, 0.3500, NOW());

-- 未登录用户（user_id IS NULL）：全局热门兜底
INSERT INTO `recommend_result` (`user_id`, `product_id`, `algorithm_type`, `score`, `generate_time`) VALUES
(NULL, 100, 4, 0.9500, NOW()),
(NULL, 105, 4, 0.8800, NOW()),
(NULL, 103, 4, 0.8200, NOW()),
(NULL, 107, 4, 0.7500, NOW()),
(NULL, 106, 4, 0.6800, NOW()),
(NULL, 108, 4, 0.6000, NOW()),
(NULL, 110, 4, 0.5200, NOW()),
(NULL, 101, 4, 0.4800, NOW()),
(NULL, 112, 4, 0.4000, NOW()),
(NULL, 109, 4, 0.3500, NOW());

-- ============================================================
-- 3. 离线评估用：user_score 表预填充
-- ============================================================
-- 用于测试评分矩阵持久化与查询逻辑
INSERT INTO `user_score` (`user_id`, `product_id`, `score`) VALUES
-- User 1 评分
(2, 100, 5.00), (2, 101, 3.50), (2, 103, 4.50), (2, 104, 2.00),
(2, 105, 4.80), (2, 106, 3.00), (2, 107, 1.50), (2, 108, 1.20),
-- User 2 评分
(3, 100, 3.00), (3, 103, 3.50), (3, 104, 2.50), (3, 105, 1.50),
(3, 107, 3.20), (3, 108, 1.80), (3, 109, 2.80), (3, 110, 1.50),
-- User 3 评分
(4, 101, 4.50), (4, 106, 3.80), (4, 103, 2.50), (4, 100, 1.80),
(4, 104, 1.50), (4, 105, 1.20);

-- ============================================================
-- 4. 验证查询（执行后运行以下 SQL 检查数据分布）
-- ============================================================
-- SELECT user_id, COUNT(*) AS behavior_count,
--        SUM(CASE WHEN behavior_type = 1 THEN 1 ELSE 0 END) AS views,
--        SUM(CASE WHEN behavior_type = 2 THEN 1 ELSE 0 END) AS favorites,
--        SUM(CASE WHEN behavior_type = 3 THEN 1 ELSE 0 END) AS purchases,
--        MIN(create_time) AS earliest, MAX(create_time) AS latest
-- FROM user_behavior
-- WHERE user_id BETWEEN 2 AND 20
-- GROUP BY user_id
-- ORDER BY user_id;
