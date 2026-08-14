# 第一级检查记录 · recommend 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + recommend 专项检查点（对照 docs/12 算法设计）
> 检查文件：RecommendCalculateService(819L) / RecommendService(318L) / RecommendController / RecommendRefreshTask / RecommendResultService

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 8 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| R-01 | RecommendCalculateService.java:153-161 | **纯新用户分支为死代码**：ratingMatrix 仅含有行为用户，`behaviorCount == 0` 分支永不执行 → `recommend_result` 表 user_id IS NULL（全局热门）记录**永不生成**。匿名用户实际走 RecommendService.guessYouLike 兜底（按 sales 热门），功能可用但与 docs/12 §9"全局热门预生成"设计不符 | 12-核心算法设计文档 §9 | P2 | ①删除死代码，文档改述为"匿名动态兜底"；或②calculateForAll 显式生成一条 user_id IS NULL 全局热门记录 | 待修复 |
| R-02 | RecommendCalculateService.java:179-184 | 步骤8 的 `allUserIds`/`existingUserIds` 查询为**死代码**（查出后未使用） | 代码质量 | P2 | 删除 | 待修复 |
| R-03 | RecommendCalculateService.java:704-723 (calculateForUser) | **"增量计算"实为全量重算**：每次单用户刷新都加载全量行为并重算全量 UserCF/ItemCF 相似度矩阵，仅结果写入是增量的；数据量大时性能差 | 12-核心算法设计文档 §9.2（准实时） | P2 | 预热相似度矩阵（如 30 分钟缓存）或改批量增量 | 待修复 |
| R-04 | RecommendCalculateService.java:760-818 (computeSimilarProducts) | 相似商品**每次全量加载行为 + 全量转置 + 逐商品点积**，首查无缓存时延迟高（有 Redis 缓存但未预热） | 性能 | P2 | 复用 R-03 的预热相似度；或定时预热相似 Top-N 到 Redis | 待修复 |
| R-05 | RecommendService.writeToRedis:212-228 + RecommendRefreshTask | **缓存与库不一致**：用户推荐 ZSet 24h 过期，但定时任务只重算 DB + 热门缓存，不清用户 ZSet → 用户缓存最长 24h 陈旧（"准实时"名不副实） | 12-算法文档 §9.2 准实时 | P2 | 定时任务中批量删除 `mall:recommend:*` 用户键（或缩短 TTL） | 待修复 |
| R-06 | RecommendService.readFromRedis:200-206 | 缓存读取时 **score 用位置构造**（`(limit-idx)/limit`）而非真实推荐分；算法类型写死 3 | 展示口径 | P2 | Redis ZSet 已存真实 score，直接用 reverseRangeWithScores 读取 | 待修复 |
| R-07 | RecommendService.guessYouLike:99-109 vs calculateForAll.computeHotRank | **热门兜底口径不一致**：查询兜底用纯 sales 排序，全量计算热门用 0.7×销量+0.3×交互 | 12-算法文档（热门公式） | P2 | 统一热门计算口径，查询兜底复用缓存的热门列表 | 待修复 |
| R-08 | RecommendController（guess-you-like/similar） | **num 参数无上限**：可传超大值导致 Redis reverseRange/DB 分页大查询（DoS 面） | G1 入参校验 | P2 | num 钳制（如 max=50），非法值报 10001 | 待修复 |

## 3. 通过项与良好实践（对照 docs/12 逐项核对）

- ✅ 权重 1:3:5:4（getWeight）、时间衰减 exp(-λ·days) λ=0.05（14 天衰减 50%）
- ✅ 评分矩阵 min-max 归一化（每用户独立，避免活跃用户分偏高）
- ✅ UserCF 余弦相似度（K=20）+ 倒排索引优化 O(Σ|item_users|²)
- ✅ ItemCF 余弦相似度（M=10）+ 倒排索引优化
- ✅ 动态 α 混合：行为数≥20 → α=0.6，否则 0.4
- ✅ 冷启动：行为数<5 → ItemCF + 热门补位；热门公式 0.7 销量 + 0.3 交互
- ✅ 全程过滤下架商品（productMap + status==1 双保险）
- ✅ 分页加载行为数据防 OOM；50 万条阈值告警；计算耗时日志
- ✅ 推荐结果 Redis ZSet 缓存 + 24h 过期；相似商品缓存；热门商品缓存（02:00 刷新）
- ✅ 查询三级兜底（Redis → DB → 热门）逻辑完整
- ✅ 无行为商品相似度返回空 → 同分类热门兜底

## 4. 移交第二级核对项

1. R-05 缓存一致性 → 第二级"行为→推荐准实时"链路（⑤）
2. R-07 热门口径 → 与 statistics/热门商品口径统一核对
3. 匿名用户推荐体验（R-01）→ 前端"未登录猜你喜欢"展示核对
