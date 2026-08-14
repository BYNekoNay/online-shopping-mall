# 第一级检查记录 · statistics 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + statistics 专项检查点（对照 FR-S-* 统计口径）
> 检查文件：MerchantStatisticsService / PlatformStatisticsService / StatisticsSnapshotTask / RecommendResultMapper（CTR 口径）/ AdminDashboardController / MerchantStatisticsController

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 1 |
| P2 | 5 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| ST-01 | MerchantStatisticsService.getSalesStatistics:36-48 + getTopProducts:67-73 | **商家销售统计未过滤订单状态**：只过滤 shopId+isDeleted+时间，**待付款(0)/已取消(5)/已退款(7) 订单全部计入**销售额与订单数 → 销售总额/热销 TOP10 严重虚高；与平台 GMV（selectAllTotalPayAmount 仅统计 1/2/3/4/6）口径矛盾 | 任务书 7.6 数据统计（销售总额口径）；docs/03 FR-S-* | **P1** | 统计 SQL 增加 `status IN (1,2,3,4,6)` 过滤（与 GMV 同口径）；热销 TOP10 同步 | 待修复 |
| ST-02 | PlatformStatisticsService.getDashboard:50-56 | **"今日订单数"未过滤状态**：含待付款/取消订单 | 统计口径 | P2 | 增加 status 过滤（或明确"订单创建数"口径） | 待修复 |
| ST-03 | PlatformStatisticsService.getDashboard:78-83 + RecommendResultMapper | **推荐 CTR 口径为近似值**：分母=预生成 recommend_result 记录数（非真实曝光，曝光仅打日志未落库 BE-02），分子=浏览行为且"存在于推荐结果"（近似点击归因）→ 指标失真（且 02:00 全量重算使 7 天窗口分母巨大，CTR 被稀释） | 任务书 7.6 推荐算法效果（点击率） | P2 | 若答辩要求真实 CTR：曝光/点击落库（recommend_exposure_log + 点击日志）；否则文档声明"近似归因" | 待确认 |
| ST-04 | PlatformStatisticsService.getStatisticsDetail:166-188 | **转化漏斗各层单位不一致**：view=行为条数、cart=购物车行数、order=订单数、pay=已付订单数（含未支付订单行）→ 漏斗比例无语义 | 统计口径 | P2 | 统一为用户数或订单数口径，注明定义 | 待修复 |
| ST-05 | StatisticsSnapshotTask.preComputeMerchantStatistics:88-96 | **商家统计预计算空实现**（仅打日志）；平台看板快照写入 Redis 但**无任何代码读取该缓存**（死缓存） | 任务书 定时统计；代码质量 | P2 | ①商家预计算实现或文档声明"实时聚合"②快照缓存接入读路径或删除 | 待修复 |
| ST-06 | MerchantStatisticsService.getSalesStatistics/getTopProducts | **全量加载订单+明细内存聚合**（虽批量无 N+1，但范围大时全量）；金额口径=商品原价合计（未扣优惠，与实付 payAmount 不一致） | 性能；口径 | P2 | SQL 侧聚合；金额口径与文档核对 | 待修复 |

## 3. 通过项与良好实践

- ✅ 平台统计大量使用 SQL 聚合（H-21：COUNT DISTINCT / GROUP BY session / AVG）避免全表加载 OOM
- ✅ 周粒度桶键修复（M-12 取 ISO 周一）
- ✅ 商家统计批量加载明细消除 N+1；排除赠品行（is_gift=1）
- ✅ 推荐 CTR 归因有意识防虚高（M-13：分子用"先被推荐后被浏览"的 EXISTS 关联）
- ✅ 看板/细项统计 SQL 均用参数绑定（无注入面）
