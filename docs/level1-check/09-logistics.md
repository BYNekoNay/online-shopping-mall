# 第一级检查记录 · logistics 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + logistics 专项检查点
> 检查文件：FreightService / LogisticsQueryService / FreightTemplateController / FreightTemplate / Logistics

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 4 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| LG-01 | FreightService.calculate:21-30 | **多模板场景计算不确定**：店铺可建多个运费模板（listByShop 全量返回），但 calculate 用 `LIMIT 1` 无排序取任意一条 → 模板多时运费结果随机 | 10-数据库规范（freight_template 与运费计算口径） | P2 | 明确设计：单模板（listByShop 限制）或多模板按匹配区域/优先级选取 | 待确认 |
| LG-02 | FreightService.save + calculate | **模板配置无校验**：defaultFee/fee/threshold 可为负值（运费负数 → 订单金额错误）；region_rule_json 非法 JSON 不拦截（解析时容错为 0） | G1 入参校验 | P2 | save 校验金额非负、JSON 合法 | 待修复 |
| LG-03 | FreightService.PROVINCE_REGION | 省份-大区映射**缺港澳台及少数地区**：未收录省份走 defaultFee（可接受，但运费模板按大区配置时覆盖不全） | 10-数据库规范 §2.2.1 | P2 | 补全省份映射（含港澳台/自治区） | 待修复 |
| LG-04 | LogisticsQueryService.query | **物流查询纯模拟（TODO）**：返回硬编码 JSON 字符串，未对接快递100 API（任务书明确要求"物流轨迹查询（对接快递100 API）"）；接口返回裸 JSON 字符串无结构化契约 | 任务书 7.3 物流模块设计 | P2 | ①文档声明"模拟实现"；②若答辩要求则对接快递100；③至少改为结构化 VO 返回 | 待确认 |

## 3. 通过项与良好实践

- ✅ 满额免邮（freeShippingThreshold）逻辑正确
- ✅ 大区规则 JSON 解析容错（失败降级 defaultFee + 日志）
- ✅ 未配置模板默认免运费（明确降级）
- ✅ 发货写入物流记录（状态=待揽收 + 轨迹快照）与订单状态更新同事务（order.ship）
