# 第一级检查记录 · order 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + order 专项检查点
> 检查文件：OrderService(706L) / OrderGroupProcessor(267L) / RefundService(225L) / StockService(206L) / OrderNoGenerator / OrderTimeoutTask / OrderController / MerchantOrderController / MerchantRefundController / OrderEstimateController / DTO×4 / OrderMapper.xml

## 1. 结论概览

| 级别 | 数量 | 状态 |
|---|---|---|
| P0 | 0 | — |
| P1 | 5 | 待确认修复 |
| P2 | 10 | 待汇总 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| O-01 | RefundService.java:143-186 (audit) | **退款审核通过后不恢复库存**：支付时已 DB 实扣（pay→deductStock），但 audit 通过仅改订单状态为 7 并处理积分，无任何库存归还 → 商品库存永久丢失 | 任务书 7.1(3)"退货后恢复库存"；10-数据库规范 | **P1** | audit 通过后按 OrderItem 归还 sku/product 库存（与 doCancel 同构：rollback/rollbackProduct），注意与 Redis 预扣口径一致 | 待修复 |
| O-02 | RefundService.java:64-68 (apply) | **退款幂等键在全部校验前写入**：校验失败（金额超限/订单状态不符/赠品）后，同订单+同明细 72h 内无法重试，一律返回"已提交过退款申请" | 09-接口规范（错误语义）；可用性 | **P1** | 将 SET NX 移至全部校验通过之后；或在失败分支删除幂等键 | 待修复 |
| O-03 | OrderService.java:241-243, 269-279 | **catch 块吞异常无日志**：①库存预扣减 catch(Exception) ②processGroup catch(Exception)，异常根因完全丢失，仅记录"店铺xx失败" | G3 异常处理；日志规范 | **P1** | catch 中 log.error 记录异常栈（可保留业务 continue） | 待修复 |
| O-04 | OrderService.java:558-564 (pay) | **product.stock 非原子读改写**：SKU 商品支付时先 deductStock（原子）再 selectById+setStock+updateById（读改写），并发支付同一商品不同 SKU 时 product.stock 丢失更新、漂移（sku 库存正确，商品总库存失真） | 数据一致性；10-数据库规范（product.stock 与 sku 关联） | **P1** | 方案A：改为主键原子 UPDATE `product SET stock = GREATEST(stock - qty, 0)`；方案B：SKU 商品不再维护 product.stock，展示侧聚合 sku | 待修复 |
| O-05 | PaymentMapper.java（全仓库仅此一处引用） | **payment 表完全无读写**：pay() 仅写 orders.payType/payTime，不落 payment 记录；PaymentMapper 无任何调用方 → 任务书 7.3"支付记录管理"未落地 | 任务书 7.3 支付模块设计 | **P1** | 确认 docs/09 是否定义支付记录；若需则 pay() 事务内插入 Payment；若属"模拟支付裁剪"应更新文档声明 | 待修复 |
| O-06 | OrderService.java:473-493 + STATUS_MAP:59-62 | **订单状态 3（已收货）为死状态**：confirmReceive 直接 2→4，STATUS_MAP 定义 3="已收货"、ReviewService 也引用 3（均不可达）→ 状态机跳跃"已发货→已完成" | 05/09 状态机定义（待发货→…→已收货→已完成） | P2 | 确认设计意图：如需完整状态机，confirmReceive 置 3，另设"自动完成"或前端确认完成；否则统一文档状态定义 | 待确认 |
| O-07 | OrderService.java:316-339 | listByUser / listByMerchant **无分页**，全量 selectList 返回 | 09-接口规范（订单列表分页） | P2 | 增加 PageResult 分页（注意前端配合） | 待修复 |
| O-08 | OrderService.java:127-137 | 下单幂等键（requestId）在**任何校验前写入**：校验失败也占用 24h；依赖前端每次提交生成新 UUID（待前端核对 confirm 页是否复用） | 幂等设计文档 §1.5 | P2 | 校验通过后写幂等键，或失败时删除 | 待核对 |
| O-09 | DTO 全套（CreateOrderDTO/ProductItemDTO/RefundApplyDTO/PayDTO/ReviewDTO/ShipDTO/AuditDTO） | **无 @Valid 校验注解**，controller 的 @Validated 形同虚设；requestId/addressId/quantity/金额等均靠 service 手工判断 | G1 入参校验 | P2 | 补充 jakarta.validation 注解（@NotNull/@NotBlank/@Min/@Size），service 校验保留作纵深 | 待修复 |
| O-10 | ReviewService.java:48-100 | **评分无 1~5 范围校验**，rating 可传任意值（0/-1/10）入库；content 无长度限制 | G1 入参校验 | P2 | @Min(1)@Max(5)、content @Size 校验 | 待修复 |
| O-11 | OrderService.java:197-198 | 地址快照**手拼 JSON 字符串**，receiver/phone 含引号或特殊字符将破坏结构 | 数据完整性 | P2 | 改用 Jackson 序列化（ObjectMapper） | 待修复 |
| O-12 | OrderEstimateController.java:135-190 | 估价：shopName 写死"店铺"+id；payAmount 无负数钳制（processGroup 有）；优惠券入参语义与下单不一致（estimate 用 couponId=模板ID，下单用 userCouponId=用户券ID）→ 估价与实际金额可能不一致 | 09-接口规范一致性 | P2 | 统一入参语义；负数钳制；shopName 查表 | 待 marketing 核对 |
| O-13 | OrderMapper.xml selectTimeoutUnpaidOrders | 超时取消**单批 LIMIT 100**，订单积压>100/分钟时延后处理（小规模可接受） | 性能 | P2 | 循环取批直到取空，或提高上限 | 待修复 |
| O-14 | MerchantRefundController.java audit | AuditDTO.approved 可空，null 时按"拒绝"处理（`Boolean.TRUE.equals`），语义含糊 | G1 入参校验 | P2 | approved 加 @NotNull | 待修复 |
| O-15 | OrderController.java pay | payType 未校验合法值（余额/模拟支付宝枚举） | G1 入参校验 | P2 | payType 枚举校验 | 待修复 |

## 3. 通过项与良好实践（记录）

- ✅ 防价格篡改：计价用 DB 单价（sku.getPrice()/product.getPrice()），SKU-商品绑定双重校验（createOrder + processGroup + estimate 三处同口径）
- ✅ 并发防护：下单/支付/取消/发货/确认收货/审核均用原子 UPDATE（WHERE status=xx）防 TOCTOU
- ✅ 越权防护：订单/地址/退款/订单行均校验归属；购物车删除附加 user_id（防 IDOR）
- ✅ 库存双保险：Redis Lua 预扣（无 TTL key）+ 支付时 DB `UPDATE ... WHERE stock>=?` 实扣
- ✅ 幂等设计：下单 requestId、支付 Redis 标记（afterCommit 落）、退款 SET NX、超时取消 SET NX
- ✅ 事务边界清晰：createOrder 不标 @Transactional（Redis 操作出事务），每组订单独立事务
- ✅ 性能细节：toVOList 批量查 items 避免 N+1；退款列表/商家退款分页
- ✅ 线程安全：LoginUserContext ThreadLocal 在 afterCompletion 清理；OrderNoGenerator 实例标识+取模序列无并发重复
- ✅ 超时任务健壮：阈值只减一次（H-9）；系统级取消无登录上下文（H-16）；取消幂等

## 4. 待跨模块核对项（移交第二级）

1. O-12 估价 vs 下单的优惠券入参语义（couponId vs userCouponId）→ 查 CouponService 两个方法 + 前端
2. processGroup 中优惠券应用到"首个成功分组"→ 若为店铺券需核对店铺匹配逻辑（CouponService）
3. O-01 修复涉及库存口径：需统一"退款恢复库存"与 Redis 预扣/DB 实扣的关系（第二级链路②③）

## 5. 检查方法记录

- 静态读码：OrderService/OrderGroupProcessor/RefundService/StockService/OrderNoGenerator/OrderTimeoutTask 全量精读
- 控制器/DTO/Mapper XML 通读
- grep 验证：状态 3 无写入方、PaymentMapper 无调用方、RefundService 无库存归还
- 动态验证（未做，需本地 MySQL/Redis 启动后补）：退款流程实测、并发支付压测
