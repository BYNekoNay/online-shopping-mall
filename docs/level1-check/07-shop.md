# 第一级检查记录 · shop 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + shop 专项检查点
> 检查文件：ShopService / ShopController / AdminShopController / ShopApplyDTO / ShopUpdateDTO / Shop

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 3 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| SH-01 | ShopService.apply + ShopApplyDTO | **入驻申请入参无校验**：ShopApplyDTO 无校验注解（name/contactName/contactPhone/licenseNo 可空、超长入库） | G1 入参校验 | P2 | DTO 补 @NotBlank/@Size/@Pattern；licenseImage 校验 | 待修复 |
| SH-02 | ShopService.apply:30 | dto 为 null（空请求体）时 NPE | G2 边界 | P2 | 判空返回 10001 | 待修复 |
| SH-03 | AdminShopController.audit（无角色联动） | **入驻审核通过不联动升级 user.role**：商家注册时 role=1，审核通过仅 shop.status=1，用户仍为消费者角色 → 商家访问 /api/merchant/*（@RequireRole(2)）403，需管理员在用户管理手动改角色才能用商家端（流程割裂，待前端/文档核对是否为预期设计） | 任务书 商家入驻流程；09-接口规范 | P2 | 审核通过时事务内升级 user.role=2（或文档明确"管理员手动分配角色"步骤） | 待确认 |

## 3. 通过项与良好实践

- ✅ 入驻状态机完整：待审核(0)/正常(1)/已拒绝(2)/已禁用(3)；拒绝后可重新提交复用记录；0/1/3 拒绝重复申请
- ✅ 审核原子更新 WHERE status=0，防并发双审后写覆盖（H-09/M-17）
- ✅ 商家接口统一 getMerchantShopIdOrThrow（status=1 校验，无店铺返回 10004，与 docs/09 一致）
- ✅ 店铺信息更新逐字段 null 守卫（M-18），仅允许更新 name/logo/description/decorationConfig
