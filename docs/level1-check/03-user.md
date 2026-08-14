# 第一级检查记录 · user 模块

> 日期：2026-08-12 | 检查人：SeniorDeveloper | 依据：docs/22 第一级 G1~G7 + user 专项检查点
> 检查文件：UserService(134L) / UserController / AddressService / LoginAttemptService / RegisterDTO / LoginDTO / UpdateProfileDTO

## 1. 结论概览

| 级别 | 数量 |
|---|---|
| P0 | 0 |
| P1 | 0 |
| P2 | 7 |

## 2. 问题登记表

| # | 位置 | 问题描述 | 违反约定 | 级别 | 建议修复 | 状态 |
|---|---|---|---|---|---|---|
| U-01 | UserService.updateProfile:124-133 | **手机号/邮箱唯一性未校验**：updateProfile 只更新非 null 字段，若改为他人已用 phone/email → DuplicateKeyException 未被捕获 → 全局 500 | G3 异常处理 | P2 | 更新前校验唯一性（排除自身），或捕获 DuplicateKeyException 返回友好错误 | 待修复 |
| U-02 | RegisterDTO（@Size(min=6)） vs UserService.register:38（长度≥8） | **密码长度校验不一致**：DTO 允许 6~7 位通过校验，service 再报"至少 8 位"（错误码 PARAM_ERROR） | G1 入参校验一致性 | P2 | 统一为 @Size(min=8) | 待修复 |
| U-03 | UserController.addAddress（@Validated @RequestBody Address） | **Address 实体无校验注解**，@Validated 形同虚设：receiver/phone/detail 可空/超长入库 | G1 入参校验 | P2 | 地址 DTO 化并补 @NotBlank/@Size/@Pattern | 待修复 |
| U-04 | UserController.profile:80-89 | **profile() @RequireRole(1)**：商家(2)/管理员(3)无法获取个人资料；若前端商家/管理端调用 /user/profile 会 403 | 09-接口规范鉴权 | P2 | 放行为任意登录角色（去掉 @RequireRole(1)，仅要求登录）；待前端核对调用方 | 待核对 |
| U-05 | AddressService.delete:44-50 | **删除默认地址后无新默认地址兜底**（其他地址不会自动补为默认） | 需求（默认地址管理） | P2 | 删除默认地址后，将最近地址置为默认（可选优化） | 待修复 |
| U-06 | AddressService.add:33-41 | isDefault 缺省(null)时插入，用户可能无任何默认地址 | 需求（默认地址管理） | P2 | 用户无默认地址时自动设为首个为默认（与 U-05 合并处理） | 待修复 |
| U-07 | AddressService.listByUser | 地址列表无分页（个人地址量小，可接受，备注） | — | P2 | 暂不处理 | 备注 |

## 3. 通过项与良好实践

- ✅ 注册/登录 DTO 校验齐全（@NotBlank/@Size/@Pattern/@Email）
- ✅ 登录防爆破（M-21）：用户名+IP 双维度 + IP 汇总双键限流；Lua 原子自增；可信代理 CIDR 校验（H-1 防 XFF 伪造绕过）
- ✅ 密码 BCrypt 加密；全仓库无密码明文泄露点（getPassword 无 VO 输出）
- ✅ 注册唯一性：预查 + DB 唯一约束 + DuplicateKeyException 分类处理
- ✅ 地址增删改均校验归属（防 IDOR）；默认地址切换用 UPDATE 统一清除
- ✅ updateById 仅更新非 null 字段（MP 默认策略），不会误清字段

## 4. 移交第二级核对项

1. U-04 profile 角色限制与前端调用方核对（router/store/user.js）
2. U-01 与数据库唯一索引核对（phone/email 是否均有 uk 索引）
