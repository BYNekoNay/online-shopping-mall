# 第一级检查记录 · 前端各域

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级前端检查点
> 检查文件：api/*.js（12 模块）/ router/index.js / store/user.js / utils/request.js / Confirm.vue（下单链路）/ RecommendList.vue / plugins/page-view.js / layouts×3 / Login.vue / cart/Index.vue / merchant/apply

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 1 |
| P2 | 6 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| F-01 | views/consumer/order/Confirm.vue:179-204 | **优惠券字段名/语义双重错误（优惠券功能整体失效）**：①createOrder 传 `couponId`，后端 CreateOrderDTO 字段为 `userCouponId` → 字段被忽略 → **下单优惠券完全不生效/不核销**；②estimateOrder 传的 `couponId` 是 UserCoupon **记录 ID**（来自 getUserCoupons 返回的 UserCouponVO.id），后端按 Coupon **模板 ID** 查 → **估价永远显示 0 券折扣** | 09-接口规范（下单/估价请求体）；资金相关 | **P1** | ①createOrder 改传 `userCouponId: selectedCouponId`（记录 ID）；②estimateOrder 改传 `couponId: 选中 UserCoupon 对象的 couponId`（模板 ID，UserCouponVO 已有该字段） | 待修复 |
| F-02 | plugins/page-view.js reportPageLeave | **匿名用户停留时长无法回填**：leave 端点（PUT /behavior/page-view/{id}/leave）非白名单需鉴权，匿名用户请求被 JwtInterceptor 拦 401，前端 fetch 静默 → 匿名 PV 停留数据永久缺失（影响统计口径） | 7.6 数据统计（停留时长） | P2 | leave 端点加入白名单（可选登录）；或前端匿名跳过 leave | 待修复 |
| F-03 | api/behavior.js + views/consumer/user/Favorites.vue（收藏） | **重复收藏不去重**：favorite 可重复调 record(type=2) 产生多条记录，favorites 列表无 DISTINCT → 收藏列表出现重复商品 | 需求（收藏夹） | P2 | favorite 前查重（存在则跳过）；favorites 查询 DISTINCT | 待修复 |
| F-04 | api/behavior.js favorites | favorites 返回 Product 实体（含 detail 富文本大字段） | 性能 | P2 | 定义轻量收藏 VO | 待修复 |
| F-05 | views/common/Login.vue:50-56 + router/index.js | 商家（role=2）登录直接跳 /merchant/products，**入驻未审核/角色未升级的用户被路由 403 拦截**（与后端 AD-01 同源；MerchantLayout 的 redirectMap 只有进入布局后才有机会执行） | 09-接口规范鉴权 | P2 | AD-01 修复后联动验证；登录跳转前查 apply-status 或前端放开 /merchant/apply | 待 AD-01 后 |
| F-06 | plugins/page-view.js:36-45 | pageEnter 前端传 userId 冗余（后端 M-26 强制取登录态、忽略传值） | 代码质量 | P2 | 移除前端 userId 传参 | 待修复 |
| F-07 | 前端测试 | 仅 2 个单测（路由守卫/请求拦截），页面组件无测试（Confirm 的优惠券字段错位本可被契约测试拦截） | 测试覆盖 | P2 | 下单/估价参数契约测试；关键页面组件测试 | 待补充 |

## 3. 通过项与良好实践

- ✅ request.js 统一错误处理：token 注入、10003→/403、10002/401→登出跳登录、网络错误提示
- ✅ 路由守卫：requiresAuth + roles 双层，子路由可覆盖父路由（商家入驻页允许消费者访问）
- ✅ 页面埋点质量高：enter/leave 时序正确（M-29 站内跳转先 leave 后 enter）、去重口径合理（M-28）、leave 用 fetch+keepalive 防丢失、H-19 带 token
- ✅ 推荐位组件：曝光仅一次上报（nextTick 后）、点击上报+携带推荐来源跳转
- ✅ 购物车页数量 UI 约束（max=stock||99，缓解后端 CR-02）
- ✅ 下单/估价地址必选、requestId 每次生成（缓解 O-08）、批量支付承接（sessionStorage pendingOrders）
- ✅ 商家布局按 shopStatus 重定向（Apply/Pending/Info 分流）

## 4. 移交第二级核对项

1. F-01 优惠券链路（与 M-02/O-12 合并统一修复）
2. F-05 商家角色（与 AD-01 合并）
3. F-02 匿名停留时长（与统计口径 ST 合并）
