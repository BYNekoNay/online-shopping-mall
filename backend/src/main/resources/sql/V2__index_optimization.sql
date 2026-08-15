-- =============================================================================
-- V2 增量迁移（E-4 大表索引优化，2026-08-16）
-- 适用：已按 V1 建库、需在线补充索引的环境。
-- 新环境直接跑 V1（索引已并入），本脚本可跳过。
-- 执行：mysql -uroot -p mall < V2__index_optimization.sql
-- =============================================================================

-- E-4：user_behavior 行为推荐/浏览历史查询路径（WHERE user_id=? AND behavior_type=? ORDER BY create_time DESC）
-- 覆盖 A-1 浏览历史推荐 / D-5 购买推荐 / 首页行为统计
ALTER TABLE `user_behavior`
  ADD INDEX `idx_user_type_time` (`user_id`, `behavior_type`, `create_time`);

-- 说明：page_view_log（idx_user_id/idx_enter_time/idx_session_id）、
--       operation_log（idx_operator_id/idx_create_time）、
--       points_record（idx_user_id/idx_expire_time）现有索引已覆盖常用查询路径，无需补充。
