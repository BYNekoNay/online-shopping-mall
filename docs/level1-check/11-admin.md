# 第一级检查记录 · admin 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + admin 专项检查点
> 检查文件：AdminUserController / AdminProductController / AdminShopController / AdminCategoryController / AdminConfigController / AdminSystemController / AdminRecommendController / OperationLogService / SystemConfigService

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 1 |
| P2 | 5 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| AD-01 | AdminUserController（仅 list/status）+ UserService.register（固定 role=1）+ ShopService.audit（无角色联动） | **用户角色分配功能缺失 → 商家端业务断裂**：①任务书 FR-A-01"用户角色分配"无对应接口；②商家注册 role=1，入驻审核通过仅 shop.status=1，**全仓库无任何 user.role 升级代码路径** → 入驻成功商家访问 /api/merchant/*（@RequireRole(2)）与前端商家路由（roles:[2]）全部 403，商家端实际不可用 | 任务书 三.3(3) 用户角色分配；任务书 三.3(2) 商家模块；09-接口规范（商家接口鉴权） | **P1** | ①AdminUserController 增加角色分配接口 PUT /api/admin/users/{id}/role；②ShopService.audit 审核通过时事务内升级 merchant_user 的 role=2（推荐，闭环自动化） | 待修复 |
| AD-02 | AdminProductController.audit:53-68 / offline:76-88 | **原子更新不检查影响行数**：UPDATE WHERE status=xx 影响 0 行（如并发双审、重复下架）时仍返回成功，调用方无感知 | 幂等性 | P2 | 检查 update 返回值，=0 抛 ORDER_STATUS_INVALID | 待修复 |
| AD-03 | AdminProductController（audit/offline）、AdminCategoryController、AdminRecommendController | **审核/下架/分类变更/推荐刷新未记录 operation_log**（现有调用点仅 AdminUser/AdminShop/AdminConfig/AdminSystem） | 任务书 操作日志（覆盖面） | P2 | 补齐上述操作的日志记录 | 待修复 |
| AD-04 | AdminProductController.list:36-48 | **管理员商品列表 N+1**：selectPage 后逐条 toVO（每条查 category+sku） | 性能 | P2 | 复用 ProductService.listPage 批量构建 | 待修复 |
| AD-05 | AdminUserController.updateStatus + StatusDTO | StatusDTO 无 @NotNull：status=null 时 `user.setStatus(null)` 将状态置空 | G1 入参校验 | P2 | StatusDTO.status 加 @NotNull，校验值 ∈{0,1} | 待修复 |
| AD-06 | OperationLogService.record | operatorRole 硬编码 3（管理员）：商家操作（发货/退款审核）无日志记录，且若扩展商家日志则角色写死错误 | 任务书 操作日志 | P2 | 日志服务接受角色参数或按调用方注入 | 待修复 |

## 3. 通过项与良好实践

- ✅ 全部 admin 接口统一 @RequireRole(3)，权限收紧
- ✅ 禁用用户防自禁（H-03）+ 防禁用其他管理员；禁用后 evict 账号状态缓存使旧 JWT 立即失效（H-2）
- ✅ 商品审核原子更新 WHERE status=PENDING 防重复审核；仅 PENDING 可审（R3-C1/R3-C2）
- ✅ 店铺审核原子更新 WHERE status=0（H-09/M-17）防并发双审；审核/等级调整均记操作日志
- ✅ 用户列表分页 + 角色/状态/关键词过滤
- ✅ SystemConfig upsert 幂等；操作日志服务轻量可用
