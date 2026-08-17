/**
 * E2E 账号工厂与前置条件封装（T01/T03/T04）。
 *
 * 账号策略（用户已确认）：
 *  - 种子数据仅 admin(管理员)/testuser(消费者)，无商家账号；
 *  - 商家一律 E2E 自建：注册消费者 → 管理员分配角色 role=2 → 商家入驻申请 → 管理员审核通过；
 *  - 全部测试账号使用 e2e_ 前缀 + 时间戳后缀，保证幂等可重复执行、可识别、可清理。
 *
 * 本模块同时承担 E2E 前置数据准备（API 直连后端），与 UI 操作解耦。
 */

const BASE_URL = process.env.E2E_BASE_URL || 'http://8.160.181.12'
const API_BASE = `${BASE_URL}/api`

export const SEED = {
  admin: { username: 'admin', password: 'Admin@2026' },
  consumer: { username: 'testuser', password: 'Mall@2026' },
}

let timestampCounter = 0

/**
 * 生成唯一后缀（短、纯数字）。
 * 前端登录/注册表单对用户名校验为 3~20 位，完整用户名 = "e2e_c_" + 后缀 需 ≤ 20；
 * 本后缀取时间戳末 9 位 + 进程内自增 2 位 = 11 位数字，e2e_c_ + 11 = 17 位，满足校验；
 * 同时保证进程内并发唯一、跨进程（时间戳末位变化）可重复执行。
 */
export function uniqueSuffix() {
  timestampCounter += 1
  return `${String(Date.now()).slice(-9)}${String(timestampCounter).padStart(2, '0')}`
}

/** 生成唯一手机号（13 + 9 位，满足后端 ^1[3-9]\d{9}$ 校验；取唯一后缀末 9 位避免碰撞）。 */
function makeUniquePhone(prefix) {
  return `${prefix}${String(uniqueSuffix()).slice(-9)}`
}

/** 生成唯一消费者账号（e2e_c_<ts>@test.com）。 */
export function makeConsumerAccount() {
  const suffix = uniqueSuffix()
  return {
    username: `e2e_c_${suffix}`,
    password: 'Mall@2026',
    nickname: `消费者${suffix}`,
    email: `e2e_c_${suffix}@test.com`,
    phone: makeUniquePhone('13'),
  }
}

/** 生成唯一商家账号（e2e_m_<ts>@test.com）。 */
export function makeMerchantAccount() {
  const suffix = uniqueSuffix()
  return {
    username: `e2e_m_${suffix}`,
    password: 'Mall@2026',
    nickname: `商家${suffix}`,
    email: `e2e_m_${suffix}@test.com`,
    phone: makeUniquePhone('18'),
    shopName: `E2E店铺${suffix}`,
  }
}

/** 生成唯一商品名。 */
export function makeProductName(prefix = 'E2E商品') {
  return `${prefix}${uniqueSuffix()}`
}

/**
 * 解析后端 JSON 响应（处理雪花 ID 精度）：先把"以 Id/id 结尾的键 + 15 位以上数字"
 * 改写为字符串再 parse，避免 JSON.parse 对 19 位雪花 ID 丢精度导致按 ID 操作 404。
 */
function parseApiJson(text) {
  // 1) "xxxId"/"xxxid" 键 + 15 位以上数字 → 引号包裹；2) "data": <15 位以上裸数字> → 引号包裹
  let safeText = text.replace(/"(\w*[Ii][Dd])":\s*(\d{15,})/g, '"$1":"$2"')
  safeText = safeText.replace(/"data":\s*(\d{15,})/g, '"data":"$1"')
  return JSON.parse(safeText)
}

/**
 * 轻量 API 客户端（Node 内置 fetch）。
 * 返回 { code, message, data }；HTTP 非 2xx 时抛错。
 */
export async function api(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    throw new Error(`API ${method} ${path} 失败: HTTP ${res.status}`)
  }
  return parseApiJson(await res.text())
}

/** 登录并返回 { token, userId, role, nickname }。 */
export async function login(username, password) {
  const resp = await api('POST', '/auth/login', { body: { username, password } })
  if (resp.code !== 0) {
    throw new Error(`登录失败 ${username}: code=${resp.code} message=${resp.message}`)
  }
  return resp.data
}

/** 注册消费者账号（返回登录态）。 */
export async function registerConsumer(account = makeConsumerAccount()) {
  const reg = await api('POST', '/auth/register', {
    body: {
      username: account.username,
      password: account.password,
      nickname: account.nickname,
      phone: account.phone,
      email: account.email,
    },
  })
  if (reg.code !== 0) {
    throw new Error(`注册失败 ${account.username}: code=${reg.code} message=${reg.message}`)
  }
  const session = await login(account.username, account.password)
  return { account, session }
}

/** 管理员登录（缓存在内存）。 */
let cachedAdminSession = null
export async function adminSession() {
  if (cachedAdminSession) return cachedAdminSession
  cachedAdminSession = await login(SEED.admin.username, SEED.admin.password)
  return cachedAdminSession
}

/** 查询用户 ID（管理员接口，按用户名精确匹配）。 */
export async function findUserIdByUsername(adminToken, username) {
  const resp = await api('GET', `/admin/users?keyword=${encodeURIComponent(username)}&pageSize=20`, {
    token: adminToken,
  })
  if (resp.code !== 0) return null
  const hit = (resp.data?.records || []).find((u) => u.username === username)
  return hit ? hit.id : null
}

/** 管理员给用户分配角色（1/2/3）。 */
export async function assignRole(userId, role, adminToken) {
  const resp = await api('PUT', `/admin/users/${userId}/role`, {
    token: adminToken,
    body: { role },
  })
  if (resp.code !== 0) {
    throw new Error(`分配角色失败 userId=${userId} role=${role}: code=${resp.code} message=${resp.message}`)
  }
  return resp
}

/**
 * 完整创建商家账号并完成入驻审核（幂等）：
 * 注册消费者 → 管理员分配 role=2 → 商家提交入驻申请 → 管理员审核通过。
 * 返回 { account, session, shopId }。
 */
export async function createMerchantWithShop(merchantAccount = makeMerchantAccount()) {
  // 1. 注册（role=1）
  const reg = await api('POST', '/auth/register', {
    body: {
      username: merchantAccount.username,
      password: merchantAccount.password,
      nickname: merchantAccount.nickname,
      phone: merchantAccount.phone,
      email: merchantAccount.email,
    },
  })
  if (reg.code !== 0) {
    throw new Error(`商家注册失败 ${merchantAccount.username}: code=${reg.code} message=${reg.message}`)
  }
  // 2. 管理员分配角色 role=2
  const admin = await adminSession()
  const userId = await findUserIdByUsername(admin.token, merchantAccount.username)
  if (!userId) throw new Error(`未找到商家用户 ${merchantAccount.username}`)
  await assignRole(userId, 2, admin.token)

  // 3. 商家重新登录（JWT 内含角色声明，须取新 token）并提交入驻申请
  const session = await login(merchantAccount.username, merchantAccount.password)
  const apply = await api('POST', '/merchant/shop/apply', {
    token: session.token,
    body: {
      name: merchantAccount.shopName,
      contactName: merchantAccount.nickname,
      contactPhone: merchantAccount.phone,
      licenseNo: `LIC-E2E-${uniqueSuffix()}`,
      licenseImage: '',
      applyReason: 'E2E 自动化测试自建商家',
    },
  })
  if (apply.code !== 0) {
    throw new Error(`入驻申请失败 ${merchantAccount.username}: code=${apply.code} message=${apply.message}`)
  }
  const shopId = apply.data?.shopId
  if (!shopId) throw new Error(`入驻申请未返回 shopId: ${JSON.stringify(apply.data)}`)

  // 4. 管理员审核通过
  const audit = await api('PUT', `/admin/shops/${shopId}/audit`, {
    token: admin.token,
    body: { approved: true, reason: 'E2E 自动审核通过' },
  })
  if (audit.code !== 0) {
    throw new Error(`店铺审核失败 shopId=${shopId}: code=${audit.code} message=${audit.message}`)
  }
  return { account: merchantAccount, session, shopId }
}

/** 管理员审核商品（通过/拒绝）。 */
export async function auditProduct(productId, approved, adminToken) {
  const resp = await api('PUT', `/admin/products/${productId}/audit`, {
    token: adminToken,
    body: { approved, reason: approved ? 'E2E 自动审核通过' : 'E2E 自动驳回' },
  })
  if (resp.code !== 0) {
    throw new Error(`商品审核失败 productId=${productId} approved=${approved}: code=${resp.code} message=${resp.message}`)
  }
  return resp
}

/** 商家发布商品（走 API，返回商品 id）。 */
export async function createProductByMerchant(session, { name, price = 99.0, stock = 100, categoryId = 1 } = {}) {
  const body = {
    categoryId,
    name,
    mainImage: 'https://example.com/e2e-product.jpg',
    images: '["https://example.com/e2e-product-1.jpg"]',
    detail: 'E2E 自动化测试商品详情',
    price,
    originalPrice: price * 2,
    stock,
    skus: [{ specJson: '{"规格":"默认"}', price, stock }],
  }
  const resp = await api('POST', '/merchant/products', { token: session.token, body })
  if (resp.code !== 0) {
    throw new Error(`商品发布失败 ${name}: code=${resp.code} message=${resp.message}`)
  }
  return resp.data?.id
}

/** 商家批量操作商品（on=提交审核/off=下架/delete=删除）。 */
export async function batchOperateProductByMerchant(session, productId, action) {
  const resp = await api('PUT', '/merchant/products/batch', {
    token: session.token,
    body: { productIds: [productId], action },
  })
  if (resp.code !== 0) {
    throw new Error(`批量操作失败 productId=${productId} action=${action}: code=${resp.code} message=${resp.message}`)
  }
  return resp.data
}

/** 消费者创建一笔订单（不支付，返回 orderId/orderNo），用于取消/状态流转前置。 */
export async function createOrderForConsumer(consumerSession, { productId, quantity = 1, addressId } = {}) {
  let targetAddressId = addressId
  if (!targetAddressId) {
    const addr = await api('POST', '/user/addresses', {
      token: consumerSession.token,
      body: {
        receiver: 'E2E收货人',
        phone: '13800138000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detail: '科技园路1号',
        isDefault: 1,
      },
    })
    if (addr.code !== 0) throw new Error(`新增地址失败: code=${addr.code} message=${addr.message}`)
    targetAddressId = addr.data
  }
  const createResp = await api('POST', '/orders', {
    token: consumerSession.token,
    body: {
      addressId: targetAddressId,
      productItems: [{ productId, quantity }],
      usePoints: false,
      requestId: `e2e-req-${uniqueSuffix()}`,
    },
  })
  if (createResp.code !== 0) {
    throw new Error(`下单失败: code=${createResp.code} message=${createResp.message}`)
  }
  const orders = createResp.data || []
  if (!orders.length) throw new Error('下单未返回订单')
  const order = orders[0]
  return { orderId: order.orderId, orderNo: order.orderNo, addressId: targetAddressId }
}

/** 消费者创建一笔已支付订单（返回订单 id），用于商家发货/售后场景前置。 */
export async function createPaidOrderForConsumer(consumerSession, { productId, quantity = 1, addressId } = {}) {
  const { orderId, orderNo, addressId: addrId } = await createOrderForConsumer(consumerSession, {
    productId,
    quantity,
    addressId,
  })
  // 模拟支付（payType=2 模拟支付宝，无需真实余额）
  const payResp = await api('POST', `/orders/${orderId}/pay`, {
    token: consumerSession.token,
    body: { payType: 2 },
  })
  if (payResp.code !== 0) {
    throw new Error(`支付失败 orderId=${orderId}: code=${payResp.code} message=${payResp.message}`)
  }
  return { orderId, orderNo, addressId: addrId }
}

/** 将登录态注入浏览器 localStorage（与 src/store/user.js 的键保持一致）。 */
export async function injectSession(page, session) {
  await page.addInitScript(({ token, userId, role, nickname }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('userId', String(userId))
    localStorage.setItem('role', String(role))
    localStorage.setItem('nickname', nickname)
    localStorage.setItem(
      'user',
      JSON.stringify({ token, userId, role, nickname })
    )
  }, session)
}
