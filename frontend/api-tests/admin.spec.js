/**
 * 接口测试：管理员端（T02）。
 * 覆盖 docs/09 §2.6 用户/商品/店铺/看板管理。
 */
import { describe, it, expect, beforeAll } from 'vitest'
import { api, expectOk, expectCode, expectPageShape } from './helpers/client.js'
import { sessions, createConsumer, uniqueName, findUserId } from './helpers/accounts.js'

describe('admin', () => {
  let adminToken
  let disabledUser

  beforeAll(async () => {
    adminToken = sessions.admin.token
  }, 30_000)

  it('ADMIN-01 用户列表返回分页结构', async () => {
    const data = expectOk(await api('GET', '/admin/users', { token: adminToken, params: { pageSize: 5 } }), '用户列表')
    expectPageShape(data)
    expect(data.total).toBeGreaterThan(0)
  })

  it('ADMIN-02 按角色过滤用户列表（role=3 管理员）', async () => {
    const data = expectOk(await api('GET', '/admin/users', { token: adminToken, params: { role: 3, pageSize: 20 } }), '角色过滤')
    expect(data.records.length).toBeGreaterThan(0)
    expect(data.records.every((u) => u.role === 3)).toBe(true)
  })

  it('ADMIN-03 禁用用户后该账号无法登录（20002）', async () => {
    const { account } = await createConsumer('e2e_adm')
    const uid = await findUserId(account.username)
    const disable = await api('PUT', `/admin/users/${uid}/status`, {
      token: adminToken,
      body: { status: 0 },
    })
    expectOk(disable, '禁用用户')
    const loginResp = await api('POST', '/auth/login', { body: { username: account.username, password: account.password } })
    // 禁用账号返回 20002；若该用户名此前已有失败记录且触发限流则可能为 10005
    expect([20002, 10005]).toContain(loginResp.code)
    disabledUser = { uid, account }
  })

  it('ADMIN-04 启用用户后恢复可登录', async () => {
    const enable = await api('PUT', `/admin/users/${disabledUser.uid}/status`, {
      token: adminToken,
      body: { status: 1 },
    })
    expectOk(enable, '启用用户')
    const loginResp = await api('POST', '/auth/login', {
      body: { username: disabledUser.account.username, password: disabledUser.account.password },
    })
    expectOk(loginResp, '恢复登录')
  })

  it('ADMIN-05 用户角色分配（role=2）', async () => {
    const { account } = await createConsumer('e2e_admr')
    const uid = await findUserId(account.username)
    const resp = await api('PUT', `/admin/users/${uid}/role`, { token: adminToken, body: { role: 2 } })
    expectOk(resp, '角色分配')
  })

  it('ADMIN-06 用户详情聚合（B-2）', async () => {
    const data = expectOk(await api('GET', `/admin/users/${sessions.consumer.userId}`, { token: adminToken }), '用户详情')
    expect(data.id).toBe(sessions.consumer.userId)
    expect(typeof data.orderCount).toBe('number')
    expect(Array.isArray(data.recentBehaviors)).toBe(true)
  })

  it('ADMIN-07 商品列表返回分页', async () => {
    const data = expectOk(await api('GET', '/admin/products', { token: adminToken, params: { pageSize: 5 } }), '商品列表')
    expectPageShape(data)
  })

  it('ADMIN-08 商品审核通过后消费者可见（status=1）', async () => {
    // 商家发布待审核商品（需店铺已审核通过，getMerchantShopIdOrThrow 要求 status=1）
    const username = uniqueName('e2e_admp')
    await api('POST', '/auth/register', { body: { username, password: 'Mall@2026', nickname: `商家${username}` } })
    const uid = await findUserId(username)
    await api('PUT', `/admin/users/${uid}/role`, { token: adminToken, body: { role: 2 } })
    const login = await api('POST', '/auth/login', { body: { username, password: 'Mall@2026' } })
    const mSession = expectOk(login)
    const apply = await api('POST', '/merchant/shop/apply', {
      token: mSession.token,
      body: {
        name: `审核店铺${username}`,
        contactName: '联系人',
        contactPhone: '13800138000',
        licenseNo: `LIC-${Date.now()}`,
        licenseImage: '',
        applyReason: '接口测试',
      },
    })
    const shopId = expectOk(apply, '入驻申请').shopId
    await expectOk(
      await api('PUT', `/admin/shops/${shopId}/audit`, { token: adminToken, body: { approved: true } }),
      '店铺审核通过（前置）'
    )
    const created = await api('POST', '/merchant/products', {
      token: mSession.token,
      body: {
        categoryId: 1,
        name: `审核通过商品${Date.now()}`,
        mainImage: 'https://example.com/a.jpg',
        images: '[]',
        detail: '审核测试',
        price: 66,
        originalPrice: 88,
        stock: 100,
        skus: [{ specJson: '{"规格":"标准"}', price: 66, stock: 100 }],
      },
    })
    const pid = expectOk(created, '发布待审核商品').id
    // 消费者端不可见（PENDING）
    const before = await api('GET', `/products/${pid}`)
    expect(before.code).not.toBe(0)
    // 管理员审核通过
    await expectOk(
      await api('PUT', `/admin/products/${pid}/audit`, { token: adminToken, body: { approved: true } }),
      '商品审核通过'
    )
    const after = await api('GET', `/products/${pid}`)
    expectOk(after, '审核后可见')
    expect(after.data.status).toBe(1)
  })

  it('ADMIN-09 商品审核拒绝后消费者不可见', async () => {
    const username = uniqueName('e2e_admr2')
    await api('POST', '/auth/register', { body: { username, password: 'Mall@2026', nickname: `商家${username}` } })
    const uid = await findUserId(username)
    await api('PUT', `/admin/users/${uid}/role`, { token: adminToken, body: { role: 2 } })
    const login = await api('POST', '/auth/login', { body: { username, password: 'Mall@2026' } })
    const mSession = expectOk(login)
    const apply = await api('POST', '/merchant/shop/apply', {
      token: mSession.token,
      body: {
        name: `拒绝店铺${username}`,
        contactName: '联系人',
        contactPhone: '13800138000',
        licenseNo: `LIC-${Date.now()}`,
        licenseImage: '',
        applyReason: '接口测试',
      },
    })
    const shopId = expectOk(apply, '入驻申请').shopId
    await expectOk(
      await api('PUT', `/admin/shops/${shopId}/audit`, { token: adminToken, body: { approved: true } }),
      '店铺审核通过（前置）'
    )
    const created = await api('POST', '/merchant/products', {
      token: mSession.token,
      body: {
        categoryId: 1,
        name: `审核拒绝商品${Date.now()}`,
        mainImage: 'https://example.com/r.jpg',
        images: '[]',
        detail: '审核拒绝测试',
        price: 55,
        originalPrice: 77,
        stock: 100,
        skus: [{ specJson: '{"规格":"标准"}', price: 55, stock: 100 }],
      },
    })
    const pid = expectOk(created, '发布待审核商品').id
    await expectOk(
      await api('PUT', `/admin/products/${pid}/audit`, { token: adminToken, body: { approved: false, reason: '测试驳回' } }),
      '商品审核拒绝'
    )
    const after = await api('GET', `/products/${pid}`)
    // 被拒商品消费者不可见：详情接口返回 30001（不存在）或 40002（已下架）均符合
    expect([30001, 40002]).toContain(after.code)
  })

  it('ADMIN-10 店铺列表按状态过滤（status=0 待审核）', async () => {
    const data = expectOk(await api('GET', '/admin/shops', { token: adminToken, params: { status: 0, pageSize: 5 } }), '店铺列表')
    expectPageShape(data)
  })

  it('ADMIN-11 店铺审核通过后商家可获取店铺信息', async () => {
    // 前置：创建商家并提交申请（未审核）
    const username = uniqueName('e2e_adms')
    await api('POST', '/auth/register', { body: { username, password: 'Mall@2026', nickname: `商家${username}` } })
    const uid = await findUserId(username)
    await api('PUT', `/admin/users/${uid}/role`, { token: adminToken, body: { role: 2 } })
    const login = await api('POST', '/auth/login', { body: { username, password: 'Mall@2026' } })
    const mSession = expectOk(login)
    const apply = await api('POST', '/merchant/shop/apply', {
      token: mSession.token,
      body: {
        name: `审核店铺${username}`,
        contactName: '联系人',
        contactPhone: '13800138000',
        licenseNo: `LIC-${Date.now()}`,
        licenseImage: '',
        applyReason: '接口测试',
      },
    })
    const shopId = expectOk(apply, '入驻申请').shopId
    // 审核前商家接口不可用
    const before = await api('GET', '/merchant/shop', { token: mSession.token })
    expect(before.code).not.toBe(0)
    // 管理员审核
    await expectOk(
      await api('PUT', `/admin/shops/${shopId}/audit`, { token: adminToken, body: { approved: true } }),
      '店铺审核通过'
    )
    const after = await api('GET', '/merchant/shop', { token: mSession.token })
    const shop = expectOk(after, '审核后店铺可用')
    expect(shop.status).toBe(1)
  })

  it('ADMIN-12 看板总览返回核心指标', async () => {
    const data = expectOk(await api('GET', '/admin/dashboard', { token: adminToken }), '看板总览')
    expect('gmv' in data).toBe(true)
    expect('orderCount' in data).toBe(true)
    expect('newUserCount' in data).toBe(true)
  })

  it('ADMIN-13 统计明细返回 PV/UV/漏斗', async () => {
    const data = expectOk(
      await api('GET', '/admin/dashboard/statistics/detail', {
        token: adminToken,
        params: { startDate: '2026-08-01', endDate: '2026-08-31' },
      }),
      '统计明细'
    )
    expect('pv' in data).toBe(true)
    expect('uv' in data).toBe(true)
    expect(data.funnel).toBeTruthy()
  })

  it('ADMIN-14 未授权访问管理员接口返回 10002/10003', async () => {
    const resp = await api('GET', '/admin/users')
    expect([10002, 10003]).toContain(resp.code)
  })
})
