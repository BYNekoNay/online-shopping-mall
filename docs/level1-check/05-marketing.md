# 第一级检查记录 · marketing 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + marketing 专项检查点
> 检查文件：CouponService(304L) / PromotionService(185L) / PointsService(211L) / CouponController / Coupon/UserCoupon 实体

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 2 |
| P2 | 7 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| M-01 | PromotionService.calculateSingleDiscount case 3 + OrderGroupProcessor | **满赠促销（type=3）未实现**：折扣计算恒返回 0；OrderGroupProcessor 无赠品行（is_gift=1）插入与预扣库存逻辑 → `order_item.is_gift` 列存在但全仓库无代码写入；docs/19 声称"满赠插入赠品行并预扣减库存"已实现，与实际不符 | 任务书 7.2（满赠活动）；10-数据库规范 is_gift 列；19-发布说明 | **P1** | ①实现满赠：达标后按 rule 插入赠品行（is_gift=1，单价 0，预扣库存）；②或在文档声明"满赠未实现/裁剪"并同步 19-发布说明 | 待确认需求后修复 |
| M-02 | CouponService.calculateDiscount/parseAndCalc + OrderGroupProcessor:139-141 | **店铺券/品类券使用时不校验适用范围**：①coupon.shopId 与当前分组店铺不匹配仍计算折扣（跨店用券）②品类券 discount_rule.categoryId 与商品品类不匹配仍计算折扣 → 错误折扣（资金相关） | 10-数据库规范 §2.4.1（品类券 categoryId 端到端校验）；店铺券归属 | **P1** | calculateDiscountByUserCoupon 校验：①UserCoupon 归属+status=0+有效期②券类型规则：店铺券匹配分组 shopId、品类券匹配分组内商品品类；不匹配返回 0 或抛错 | 待修复 |
| M-03 | CouponService.receive:44-79 | **领取无"每人限领"校验**：listAvailable 仅前端层面排除已领，receive 接口可被直接重复调用领取多张同一券（stock 扣减正常但无每人限领约束；Coupon 无 perUserLimit 字段） | 任务书 7.2 优惠券（限领规则） | P2 | 确认需求是否每人限领 1 张；若是，receive 增加已领校验（若为资金/规则要求则升 P1） | 待确认 |
| M-04 | CouponService.calculateDiscountByUserCoupon:155-159 | **下单计价前不校验 UserCoupon 状态/归属**：先按券计算折扣，markUsed 时才校验归属+status → 校验滞后（他人类/已用券最终会回滚，但流程浪费且计价展示错误） | G3 业务规则校验顺序 | P2 | calculateDiscountByUserCoupon 前置校验：归属、status=0、有效期 | 待修复 |
| M-05 | CouponController.userCoupons:100-118 | **我的优惠券 N+1 查询**：循环 selectById(coupon) | 性能 | P2 | 批量 selectBatchIds | 待修复 |
| M-06 | CouponController.create/update + PromotionController admin CRUD | **管理端创建/更新无入参校验**：@RequestBody Coupon/Promotion 直接入库，discount_rule/rule_json 非法 JSON、stock/type 越界不拦截 | G1 入参校验 | P2 | DTO + @Valid；规则 JSON 合法性校验 | 待修复 |
| M-07 | PromotionService.calculateDiscount:32-42 | matchActive 取**全局最大折扣**，docs 描述为"同类型取抵扣金额最大"；当前跨类型取最大（不叠加，更严格）→ 文档需对齐 | 10-数据库规范 §2.4.2 | P2 | 按文档口径实现或修订文档 | 待修复 |
| M-08 | PromotionService.listActiveByShop | **促销 scope 仅实现 SHOP 级**：品类/全场促销未实现（scope=CATEGORY/ALL 无查询分支） | 任务书 7.2 促销（活动类型） | P2 | 确认需求范围；若需要则扩展 scope 匹配 | 待确认 |
| M-09 | PointsService.settleDeduct/settleEarn/clawback | **setSql 字符串拼接数值**（`points = points - N`）：数值均为内部计算（非用户输入）无注入风险，但风格不佳 | 代码规范 | P2 | 改为参数化 setSql（#{}） | 待修复 |

## 3. 通过项与良好实践

- ✅ 券超发防护：receive 乐观更新（received_count 比对）+ 原子 UPDATE
- ✅ 券核销防重复：markUsed WHERE status=0，影响行数=0 抛异常回滚订单（H-08）
- ✅ 取消订单释放券（releaseByOrderId 按 relatedOrderId + status=1 精确释放）
- ✅ 满减/折扣双模式统一解析（M-09）；rule_json 解析容错（异常返回 0 + 日志）
- ✅ 积分：原子扣减（WHERE points>=）、原子发放、原子扣回（GREATEST 防负）、返还幂等（type=4 守卫，H-5）
- ✅ 促销：H-14 套餐价防负折扣、H-12 过滤软删除促销、时间窗校验
- ✅ 折扣比例范围校验（0.01~0.99，M10）
- ✅ 积分流水分页查询

## 4. 移交第二级核对项

1. M-02 店铺券/品类券适用范围 → 与 order 模块 O-12（估价 vs 下单券入参语义）合并核对，确认前端传参
2. M-01 满赠未实现 → 需求覆盖矩阵（第三级）标记
3. M-03 限领规则 → 与 docs/03 需求核对
4. 退款场景券不退还（RefundService.audit 无 releaseByOrderId）→ 第二级退款链路确认是否应有券回退规则
