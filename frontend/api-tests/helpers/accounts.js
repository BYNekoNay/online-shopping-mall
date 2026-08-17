/**
 * 接口测试账号管理（T02）。
 *
 *  - beforeAll 登录种子账号 admin/testuser 并缓存 token（全 spec 共享）；
 *  - 提供唯一账号注册工厂（e2e_ 前缀 + 时间戳，幂等可重复执行）；
 *  - 提供商家自建前置（注册→管理员分配 role=2→入驻申请→审核通过），供 merchant/admin spec 使用。
 */
import { beforeAll } from 'vitest'
import { api, expectOk } from './client.js'

export const SEED = {
  admin: { username: 'admin', password: 'Admin@2026' },
  consumer: { username: 'testuser', password: 'Mall@2026' },
}

export const sessions = {
  admin: null,
  consumer: null,
}

let seq = 0
export function uniqueName(prefix = 'e2e') {
  seq += 1
  return `${prefix}_${Date.now()}_${seq}`
}

/** 全局 beforeAll：登录 admin/testuser。 */
beforeAll(async () => {
  for (const [key, acc] of Object.entries(SEED)) {
    const resp = await api('POST', '/auth/login', { body: { username: acc.username, password: acc.password } })
    if (resp.code !== 0) {
      throw new Error(`种子账号登录失败 ${acc.username}: code=${resp.code} message=${resp.message}`)
    }
    sessions[key] = resp.data
  }
}, 30_000)

/** 生成唯一手机号（13/18 + 9 位，满足后端校验；取唯一名序号保证不碰撞）。 */
function makeUniquePhone(prefix) {
  seq += 1
  return `${prefix}${String(Date.now()).slice(-5)}${String(seq).padStart(4, '0')}`
}

/** 注册唯一消费者并登录，返回 { account, session }。 */
export async function createConsumer(prefix = 'e2e_c') {
  const username = uniqueName(prefix)
  const account = {
    username,
    password: 'Mall@2026',
    nickname: `测试${username}`,
    phone: makeUniquePhone('13'),
    email: `${username}@test.com`,
  }
  const reg = await api('POST', '/auth/register', {
    body: {
      username: account.username,
      password: account.password,
      nickname: account.nickname,
      phone: account.phone,
      email: account.email,
    },
  })
  expectOk(reg, `注册 ${username}`)
  const loginResp = await api('POST', '/auth/login', { body: { username, password: account.password } })
  return { account, session: expectOk(loginResp, `登录 ${username}`) }
}

/** 查询用户 ID（管理员接口）。 */
export async function findUserId(username, adminToken = sessions.admin.token) {
  const resp = await api('GET', '/admin/users', { token: adminToken, params: { keyword: username, pageSize: 20 } })
  if (resp.code !== 0) return null
  return (resp.data?.records || []).find((u) => u.username === username)?.id ?? null
}

/** 创建商家账号并完成入驻审核，返回 { account, session, shopId }。 */
export async function createMerchant(prefix = 'e2e_m') {
  const username = uniqueName(prefix)
  const account = {
    username,
    password: 'Mall@2026',
    nickname: `商家${username}`,
    phone: makeUniquePhone('18'),
    email: `${username}@test.com`,
    shopName: `测试店铺${username}`,
  }
  // 1. 注册（role=1）
  await api('POST', '/auth/register', {
    body: {
      username: account.username,
      password: account.password,
      nickname: account.nickname,
      phone: account.phone,
      email: account.email,
    },
  })
  // 2. 管理员分配 role=2
  const uid = await findUserId(account.username)
  if (!uid) throw new Error(`未找到用户 ${account.username}`)
  const roleResp = await api('PUT', `/admin/users/${uid}/role`, {
    token: sessions.admin.token,
    body: { role: 2 },
  })
  expectOk(roleResp, `分配角色 ${account.username}`)
  // 3. 商家登录 + 入驻申请
  const loginResp = await api('POST', '/auth/login', { body: { username, password: account.password } })
  const session = expectOk(loginResp, `商家登录 ${username}`)
  const apply = await api('POST', '/merchant/shop/apply', {
    token: session.token,
    body: {
      name: account.shopName,
      contactName: account.nickname,
      contactPhone: account.phone,
      licenseNo: `LIC-${Date.now()}`,
      licenseImage: '',
      applyReason: '接口测试自建商家',
    },
  })
  const shopId = expectOk(apply, `入驻申请 ${username}`)?.shopId
  if (!shopId) throw new Error(`入驻申请未返回 shopId ${username}`)
  // 4. 管理员审核通过
  const audit = await api('PUT', `/admin/shops/${shopId}/audit`, {
    token: sessions.admin.token,
    body: { approved: true, reason: '接口测试自动审核' },
  })
  expectOk(audit, `店铺审核 ${username}`)
  return { account, session, shopId }
}
