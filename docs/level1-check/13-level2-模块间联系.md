# 第二级检查记录 · 模块间联系

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第二级（依赖矩阵/契约比对/6 条链路/共享状态/数据传递）
> 说明：本层基于第一级已读代码做跨模块走查 + 补充验证；动态验证（启动/并发压测）留待第三级

## 1. 结论概览

| 级别 | 数量 | 说明 |
|---|---|---|
| P0 | 0 | — |
| P1 | 3 | 新增（准实时死代码 / 事务内 Redis / 改库存不同步 Redis） |
| P2 | 5 | 新增（越权查物流 / 退款券规则 / 分页未接 / 跨层调用 / 裸 JSON 契约） |

## 2. 依赖矩阵结论

```
order → product(14) marketing(4) cart(3) user(2) behavior(2) logistics(1)
statistics → order(6) behavior(4) cart(2) user(2) recommend(1)
recommend → behavior / product（无 order 依赖）
marketing → user / product（无 order 依赖）
```
- ✅ **无循环依赖**（grep 注入核对，Spring 启动日志可在动态验证阶段最终确认）
- ⚠️ **P2 跨层调用**：10 个 controller 直连 Mapper（Admin×5 / Behavior / Coupon / Points / Promotion / MerchantRefund），绕过 service 层

## 3. 契约差异清单（前端 api vs 后端 controller）

| # | 差异 | 结论 |
|---|---|---|
| ① | **优惠券字段（F-01，第一级已确认）**：createOrder 传 couponId / 应为 userCouponId；estimate 传 UserCoupon 记录 ID / 应为模板 ID | **P1 资金相关**，前端修复 |
| ② | 商家退款列表：后端支持分页（PageResult），前端 getRefunds() 未传分页参数 | P2，前端 Refunds.vue 待核对 |
| ③ | 物流轨迹：后端返回裸 JSON 字符串（Result<String>），前端需自行 parse | P2（LG-04 同源） |
| ④ | 其余核对通过：products/search、points/records、promotions/active、freight calculate、logistics track 端点均存在且路径一致 | ✅ |

## 4. 跨模块链路走查（6 条）

### ① 下单链路 cart→product→order→marketing→logistics→behavior —— 发现 2 处断裂
- 断裂点 A（P1，F-01）：前端 createOrder 券字段错 → 优惠券不生效、不核销（markUsed 不调用，券仍可用）
- 断裂点 B（P1，M-02）：即使字段修复，店铺券/品类券不校验适用范围 → 跨店/跨品类误用
- 其余贯通：cartItemIds（归属校验+按成功分组精确清理）→ product 校验（状态/SKU 绑定）→ order 拆单独立事务 → marketing 结算（券/积分/促销）→ logistics 运费 → behavior 购买埋点 best-effort ✅
- 金额口径统一：全部用 DB 单价（sku/product.price），防篡改 ✅

### ② 支付→订单→库存（DB 实扣）
- pay()：归属校验 → Redis 幂等快速失败（C4 降级）→ 原子 UPDATE status 0→1 → afterCommit 写 Redis 已付标记（C1）→ sku/product 原子实扣（WHERE stock>=）→ 积分发放 → 购买行为
- ✅ 幂等闭环（DB status + Redis 标记双保险）；❌ 已知：product.stock 非原子读改写（O-04，第一级）

### ③ 取消/超时→回退（券/积分/库存）
- doCancel：原子 status 0→5 → **Redis 库存归还** → 券释放（DB）→ 积分返还（DB，type=4 幂等）
- ❌ **L1-02（P1）Redis 操作在 DB 事务内**：cancelOrder/cancelOrderBySystem @Transactional 内调 stockService.rollback（Redis），违反文档 §10"Redis 操作放事务外"（createOrder 注释明确此原则但 doCancel 未遵守）。若事务内后续 DB 步骤失败回滚 → 库存已归还但订单未取消 → 可售库存虚增
- ✅ 超时任务幂等（Redis SET NX cancel lock + cancelOrderBySystem 静默跳过）

### ④ 退款链路 order→product→marketing
- apply：幂等键（❌ O-02 前置拦截）→ 归属/状态/金额校验 → 赠品排除 → 插入退款
- audit：归属校验 → 原子审核 → 订单置 7 → 积分 clawback+refundDeduct
- ❌ **O-01（P1）库存不恢复**（第一级已确认）
- ⚠️ **L1-04（P2）退款通过后优惠券不退还**（audit 无 releaseByOrderId；规则未定义，需确认需求）

### ⑤ 行为→推荐（准实时）
- behavior.record 写 user_behavior（权重 1/3/5/4 对齐）✅
- ❌ **L1-01（P1）`calculateForUser` 无调用方**：docs/12 §9.2"购买后准实时刷新"为死代码 → 行为变化后推荐最快要次日 02:00 全量重算才反映（用户 Redis 缓存 24h 过期回源 DB 仍旧）→ 创新点④"准实时"实为"日更"
- 推荐查询三级兜底（Redis→DB→热门）✅

### ⑥ 统计口径
- ❌ ST-01（P1）商家销售统计未过滤订单状态，与平台 GMV 口径矛盾（第一级已确认）
- ⚠️ CTR 口径依赖曝光落库决策（BE-02），当前近似

## 5. 共享状态与并发核对

| 检查项 | 结论 |
|---|---|
| 库存 key 读写两侧 | ❌ **L1-08（P1）管理员改库存不同步 Redis**：MerchantProductController.update 直接 updateById（product/sku.stock），不更新 Redis stock key → 已有预扣的 key 保持旧值 → Redis 与 DB 永久漂移，后续下单基于陈旧库存（可能超卖/错卖）。修复：改库存时同步更新/删除 Redis key（触发懒加载） |
| 券核销并发 | ✅ markUsed WHERE status=0 原子，影响行数=0 回滚 |
| 锁命名空间隔离 | ✅ stock:{skuId} vs stock:product:{productId} 分离，无重叠 |
| 积分并发 | ✅ 原子 UPDATE（points>= 守卫）+ 幂等（type=4） |
| 订单状态并发 | ✅ 全链路原子 UPDATE WHERE status=xx 防 TOCTOU |
| 推荐缓存一致性 | ⚠️ R-05 缓存 24h 陈旧（第一级） |

## 6. 新增问题登记（第二级）

| # | 位置 | 问题描述 | 级别 | 建议修复 |
|---|---|---|---|---|
| L2-01 | RecommendCalculateService.calculateForUser（无调用方） | **"购买后准实时刷新"死代码**：准实时推荐未接线，创新点④名不副实 | **P1** | behavior.record / pay / 收藏后异步调用 calculateForUser（低频+合并）；或定时增量刷新（如每 10 分钟刷新增量用户） |
| L2-02 | OrderService.doCancel（cancelOrder/cancelOrderBySystem @Transactional 内） | **Redis 库存归还置于 DB 事务内**，违反文档 §10；事务失败回滚时 Redis 已执行 → 库存虚增 | **P1** | Redis 操作移至 afterCommit；或先 DB 后 Redis + 失败补偿（重试/对账） |
| L2-03 | MerchantProductController.update / 管理员改库存路径 | **改库存不刷新 Redis stock key**：Redis 与 DB 库存漂移，预扣基于陈旧值 | **P1** | 改库存时同步 set/del Redis key（懒加载兜底） |
| L2-04 | FreightTemplateController.track | **物流轨迹无订单归属校验**（任意登录用户可查任意 orderId，虽为模拟数据） | P2 | 校验订单归属（消费者本人/商家店铺） |
| L2-05 | RefundService.audit | 退款通过后优惠券不退还（规则未定义） | P2 | 与需求确认退款券规则（退/不退）并实现+文档化 |
| L2-06 | api/merchant.js getRefunds | 商家退款列表前端未接分页 | P2 | 前端传分页参数+分页 UI（待 Refunds.vue 核对） |
| L2-07 | 10 个 controller 直连 Mapper | 跨层调用违反分层 | P2 | 后续重构收敛（可选） |

## 7. 移交第三级

1. 动态验证：Spring 启动确认无循环依赖；下单/支付/取消/退款链路实测；并发下单压测（验证 L2-03 修复效果）
2. 需求覆盖矩阵：M-01 满赠、O-05 支付记录、LG-04 物流对接、AD-01 角色分配、L2-05 退款券规则的"未实现/裁剪"声明
3. 统计口径统一：ST-01 修复 + CTR 曝光落库决策 + 匿名停留数据缺失
4. 安全复核：track 越权、F-01 修复后回归
