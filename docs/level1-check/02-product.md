# 第一级检查记录 · product 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + product 专项检查点
> 检查文件：ProductService(247L) / ProductController / MerchantProductController / CategoryService / ReviewService / ProductQueryDTO / Mapper XML×3

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 11 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| P-01 | ProductService.getDetail:156-176 / ProductController.detail | **商品详情不校验 ONLINE**：下架(2)/待审核(0)商品可通过直接 URL 访问详情（列表已过滤，但直链可看） | 任务书"违规商品下架处理"（下架后应不可见） | P2 | getDetail 增加状态校验：非 ONLINE 抛 PRODUCT_OFFLINE（30002），商家端详情另行放行 | 待修复 |
| P-02 | MerchantProductController.update:113-130 | **编辑商品"软删旧 SKU + 重建"**：旧 SKU 逻辑删除后，用户购物车/收藏中引用旧 SKU 结算时 selectById 返回 null → 30003 SKU_NOT_FOUND，无法下单 | 任务书 7.1 购物流程连贯性 | P2 | ①购物车/结算对失效 SKU 友好提示（前端）；②或改为"更新式"保留 SKU ID；③文档声明该简化 | 待修复 |
| P-03 | ProductService.getDetail:169-173 | **商家/管理员查看详情也记录浏览行为**（getDetail 无条件 record behaviorType=1，不区分角色）→ 污染推荐数据（商家看自家商品被计入行为矩阵） | 12-核心算法设计文档（行为数据口径） | P2 | 仅消费者角色（role=1）且非本人店铺商品才记浏览行为 | 待修复 |
| P-04 | MerchantProductController.SkuDTO | **SKU 入参无校验**：price/stock 可为 null 或负值入库 | G1 入参校验 | P2 | SkuDTO 补 @NotNull/@DecimalMin/@Min 校验；service 再兜底 | 待修复 |
| P-05 | MerchantProductController.batchOperate:150-156 | batch catch 吞异常（无 log.error，仅记"操作失败"文案） | G3 异常处理 | P2 | catch 中记录异常栈 | 待修复 |
| P-06 | MerchantProductController.create/update | **product.stock 与 sku 库存无一致性校验**：可同时填且互相矛盾（如 product.stock=100、sku 合计 50） | 10-数据库规范（stock 口径） | P2 | 创建/编辑时校验：有 SKU 时 product.stock 与 sku 合计一致（或明确 product.stock 仅展示口径） | 待修复 |
| P-07 | MerchantProductController.update | **已上架商品编辑价格/库存直接生效**，不触发重新审核（仅 REJECTED→PENDING） | 任务书"商品审核（上架前审核）" | P2 | 确认设计意图：如"改价需重审"，ONLINE 编辑时置 PENDING；否则文档声明 | 待确认 |
| P-08 | ProductService.listPage:93-97 | **搜索历史每条分页都记录**（翻页重复）+ 无条数上限 | 需求 FR-S 搜索历史 | P2 | 同关键词去重/限频；或仅首页搜索记录；history 表加容量上限 | 待修复 |
| P-09 | ProductService.listPage:98-113 | **product.price 口径**：筛选/排序用 product.price，但 SKU 商品实际售价在 sku.price，价格区间/排序可能与真实价格不符 | 09-接口规范价格语义 | P2 | 确认 product.price 语义（建议=最低 SKU 价或均价），并保证创建/编辑时同步 | 待确认 |
| P-10 | CategoryService.listTree | ①分类树**不过滤 status**（禁用分类仍展示）②仅支持**两层**（根+子），孙分类被丢弃 | 03-需求（分类展示） | P2 | 过滤禁用分类；或支持多级递归 | 待修复 |
| P-11 | ReviewService.submit | **评分无 1~5 范围校验**（rating 可任意值入库；与 order 模块 O-10 同源） | G1 入参校验 | P2 | 补 @Min(1)@Max(5) + content @Size | 待修复 |
| P-12 | MerchantProductController.create | name/detail 长度无上限（富文本 detail 可超大）；图片字段数量/URL 无校验 | G1 入参校验 | P2 | @Size 限制；图片 URL 白名单校验（可选） | 待修复 |

## 3. 通过项与良好实践

- ✅ 消费者列表强制 status=ONLINE（M-15）；商家端走独立端点
- ✅ 批量上下架防止绕过审核（H-02：ONLINE 只能由管理员审核达成）
- ✅ 评价分页（H-22，pageSize 上限 100）；评分动态计算不依赖 product 冗余字段
- ✅ listPage 批量加载 category/sku 消除 N+1
- ✅ 商家更新/批量操作均校验 shopId 归属
- ✅ 搜索历史写入 best-effort（失败不影响主流程）

## 4. 移交第二级核对项

1. P-02 购物车旧 SKU 失效与 cart 模块联动（结算链路）
2. P-09 product.price 与 sku.price 口径 → 确认后回写 09/10 文档
3. P-01 下架商品可见性与 recommend 推荐结果（推荐是否已过滤 OFF LINE 商品）
