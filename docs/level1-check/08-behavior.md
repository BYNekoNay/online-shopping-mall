# 第一级检查记录 · behavior 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + behavior 专项检查点
> 检查文件：BehaviorService / BehaviorController / 各 DTO / UserBehavior / PageViewLog

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 3 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| BE-01 | BehaviorService.record:20-38 | **behavior_weight 冗余存储**：写入权重（1/3/5/4）但算法 buildRatingMatrix 用 getWeight() 重算，不读 DB 字段 → 双口径存在未来漂移风险 | 12-算法文档（权重口径单一来源） | P2 | 算法改为读 DB weight，或删除冗余列（保留展示用途则注明） | 待修复 |
| BE-02 | BehaviorService.recordRecommendExposure | **推荐曝光仅打日志不落库**：CTR/转化率评估依赖曝光数据，未持久化则第三级"推荐算法效果"统计口径无法实现（注释为预留） | 任务书 7.6 推荐算法效果（点击率、转化率） | P2 | 若答辩要求 CTR 指标，需落 recommend_exposure_log 表；否则文档声明 | 第三级核对 |
| BE-03 | BehaviorService.record | user_behavior **无去重/限频**：页面刷新即记浏览，行为表快速增长（设计如此，标注） | 性能 | P2 | 可考虑同商品浏览去重窗口（可选优化） | 备注 |

## 3. 通过项与良好实践

- ✅ userId 一律服务端取值（M-26），不信任前端传值，防伪造他人行为/页面访问
- ✅ 页面停留回填校验日志归属（M-27 IDOR 防护）+ 时长封顶（负数归零、上限 86400s）
- ✅ 权重口径与算法 getWeight() 对齐写入（M-10）
- ✅ 行为类型非法时默认按浏览处理，productId/userId null 防御（算法侧也跳过）
- ✅ 购买/评价行为在订单事务内 best-effort 记录（失败不影响订单，M-06）
