# 第一级检查记录 · cart 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + cart 专项检查点
> 检查文件：CartService(209L) / CartController / Cart/CartVO

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 4 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| CR-01 | CartService.incrementQuantity:165 | `setSql("quantity = quantity + " + quantity)` 字符串拼接数值（quantity 为 int 且经 0~99 校验，无注入面，但风格不佳） | 代码规范（参数化） | P2 | 改为 `setSql("quantity = quantity + {0}", quantity)` 或先查后写 | 待修复 |
| CR-02 | CartService.update:179-196 | **修改数量不校验上限与库存**：仅校验 >0，可改成 9999（超库存/超 99 上限）；结算时下单会被库存扣减拦截（无资金损失），但购物车"库存校验"需求未闭环 | 任务书 购物车（库存校验） | P2 | update 复用 add 的校验逻辑（≤99、≤库存、商品仍 ONLINE） | 待修复 |
| CR-03 | CartService.add:138-156 | **并发累加可超限**：add 的数量上限/库存校验在累加前做，并发两次各 60 可累加出 120（>99、可能超库存）；下单仍有最终兜底 | 任务书 购物车（库存校验） | P2 | 累加后再次校验总量（或用 UPDATE ... AND quantity+N<=上限） | 待修复 |
| CR-04 | CartService.listVO:90-99 | 无 SKU 商品的 `stockEnough` 未设置（null），前端可能误判库存 | 前端契约 | P2 | 无 SKU 分支也设置 stockEnough | 待修复 |

## 3. 通过项与良好实践

- ✅ 原子 upsert 防并发重复行（M-05：先累加，无行 INSERT，唯一键冲突回退累加）
- ✅ 增删改均校验购物车项归属（IDOR 防护）
- ✅ Mass Assignment 防护（M-03：update 只写 quantity/selected，禁止覆写 userId/productId/skuId）
- ✅ 加购校验：商品存在/ONLINE、SKU 绑定、数量上限 99、库存充足
- ✅ 批量加载商品/SKU 消除 N+1
