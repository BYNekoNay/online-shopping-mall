/**
 * 接口测试：商家端（T02）。
 * 覆盖 docs/09 §2.5 入驻申请/商品管理/订单发货/统计/运费模板。
 */
import { describe, it, expect, beforeAll } from 'vitest'
import { api, expectOk, expectCode } from './helpers/client.js'
import { sessions, createConsumer, createMerchant, findUserId, uniqueName } from './helpers/accounts.js'

describe('merchant', () => {
  let merchant
  let productId

  beforeAll(async () => {
    merchant = await createMerchant('e2e_mct')
  }, 60_000)

  it('MER-01 商家入驻申请返回 shopId 且 status=0', async () => {
    // 手工创建第二个商家，仅到申请环节，验证返回结构
    const username = uniqueName('e2e_mct2')
    await api('POST', '/auth/register', {
      body: { username, password: 'Mall@2026', nickname: `商家${username}` },
    })
    const uid = await findUserId(username)
    await api('PUT', `/admin/users/${uid}/role`, { token: sessions.admin.token, body: { role: 2 } })
    const login = await api('POST', '/auth/login', { body: { username, password: 'Mall@2026' } })
    const session = expectOk(login)
    const apply = await api('POST', '/merchant/shop/apply', {
      token: session.token,
      body: {
        name: `申请店铺${username}`,
        contactName: '联系人',
        contactPhone: '13800138000',
        licenseNo: `LIC-${Date.now()}`,
        licenseImage: '',
        applyReason: '接口测试',
      },
    })
    const data = expectOk(apply, '入驻申请')
    expect(data.shopId).toBeTruthy()
    expect(data.status).toBe(0)
  })

  it('MER-02 重复入驻申请返回 10001', async () => {
    const resp = await api('POST', '/merchant/shop/apply', {
      token: merchant.session.token,
      body: {
        name: '重复申请店铺',
        contactName: '联系人',
        contactPhone: '13800138000',
        licenseNo: `LIC-${Date.now()}`,
        licenseImage: '',
        applyReason: '重复',
      },
    })
    expectCode(resp, 10001)
  })

  it('MER-03 消费者访问商家接口返回 10003', async () => {
    const resp = await api('GET', '/merchant/products', { token: sessions.consumer.token })
    expectCode(resp, 10003)
  })

  it('MER-04 发布商品成功返回商品 id（默认待审核）', async () => {
    const resp = await api('POST', '/merchant/products', {
      token: merchant.session.token,
      body: {
        categoryId: 1,
        name: `商家测试商品${Date.now()}`,
        mainImage: 'https://example.com/m.jpg',
        images: '["https://example.com/m1.jpg"]',
        detail: '商家接口测试',
        price: 199,
        originalPrice: 299,
        stock: 200,
        skus: [{ specJson: '{"规格":"标准"}', price: 199, stock: 200 }],
      },
    })
    productId = expectOk(resp, '发布商品').id
    expect(productId).toBeTruthy()
    const detail = expectOk(
      await api('GET', `/merchant/products/${productId}`, { token: merchant.session.token }),
      '商家商品详情'
    )
    expect(detail.status).toBe(2) // PENDING 待审核
  })

  it('MER-05 商家商品列表仅返回本店铺商品', async () => {
    const data = expectOk(await api('GET', '/merchant/products', { token: merchant.session.token }), '商家商品列表')
    expect(Array.isArray(data.records)).toBe(true)
    expect(data.records.every((p) => String(p.shopId) === String(merchant.shopId))).toBe(true)
  })

  it('MER-06 编辑商品名称与价格', async () => {
    const resp = await api('PUT', `/merchant/products/${productId}`, {
      token: merchant.session.token,
      body: {
        categoryId: 1,
        name: `编辑后商品${Date.now()}`,
        mainImage: 'https://example.com/m.jpg',
        images: '["https://example.com/m1.jpg"]',
        detail: '商家接口测试-编辑',
        price: 259,
        originalPrice: 359,
        stock: 200,
        skus: [{ specJson: '{"规格":"标准"}', price: 259, stock: 200 }],
      },
    })
    expectOk(resp, '编辑商品')
    const detail = expectOk(
      await api('GET', `/merchant/products/${productId}`, { token: merchant.session.token }),
      '商家商品详情'
    )
    expect(detail.name).toContain('编辑后商品')
    expect(Number(detail.price)).toBe(259)
  })

  it('MER-07 商品批量上下架状态变更（on=提交审核/off=下架）', async () => {
    // 商品当前 PENDING，先审核通过
    const adminLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: 'Admin@2026' } })
    const adminToken = expectOk(adminLogin).token
    await api('PUT', `/admin/products/${productId}/audit`, { token: adminToken, body: { approved: true } })
    // 下架
    const off = await api('PUT', '/merchant/products/batch', {
      token: merchant.session.token,
      body: { productIds: [productId], action: 'off' },
    })
    const offData = expectOk(off, '批量下架')
    expect(offData.success).toBe(1)
    let detail = expectOk(
      await api('GET', `/merchant/products/${productId}`, { token: merchant.session.token }),
      '下架后详情'
    )
    expect(detail.status).toBe(0)
    // 上架（提交审核 → PENDING，最终由管理员审核）
    const on = await api('PUT', '/merchant/products/batch', {
      token: merchant.session.token,
      body: { productIds: [productId], action: 'on' },
    })
    const onData = expectOk(on, '批量上架')
    expect(onData.success).toBe(1)
    detail = expectOk(
      await api('GET', `/merchant/products/${productId}`, { token: merchant.session.token }),
      '上架后详情'
    )
    expect(detail.status).toBe(2)
    // 恢复为已上架，供后续订单测试使用
    await api('PUT', `/admin/products/${productId}/audit`, { token: adminToken, body: { approved: true } })
    detail = expectOk(
      await api('GET', `/merchant/products/${productId}`, { token: merchant.session.token }),
      '恢复上架'
    )
    expect(detail.status).toBe(1)
  })

  it('MER-08 商家发货（消费者已支付订单）', async () => {
    const buyer = await createConsumer('e2e_mctb')
    const addr = await api('POST', '/user/addresses', {
      token: buyer.session.token,
      body: {
        receiver: '买家',
        phone: '13800138000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detail: '收货地址1号',
        isDefault: 1,
      },
    })
    const addressId = expectOk(addr)
    const created = await api('POST', '/orders', {
      token: buyer.session.token,
      body: {
        addressId,
        productItems: [{ productId, quantity: 1 }],
        usePoints: false,
        requestId: `e2e-ship-${Date.now()}`,
      },
    })
    const orders = expectOk(created, '买家下单')
    const orderId = orders[0].orderId
    await api('POST', `/orders/${orderId}/pay`, { token: buyer.session.token, body: { payType: 2 } })
    // 商家订单列表可见该订单
    const merchantOrders = expectOk(
      await api('GET', '/merchant/orders', { token: merchant.session.token }),
      '商家订单列表'
    )
    expect(merchantOrders.some((o) => String(o.orderId) === String(orderId))).toBe(true)
    // 发货
    const ship = await api('PUT', `/merchant/orders/${orderId}/ship`, {
      token: merchant.session.token,
      body: { logisticsCompany: '圆通速递', trackingNo: `YT${Date.now()}` },
    })
    expectOk(ship, '商家发货')
    const detail = expectOk(
      await api('GET', `/orders/${orderId}`, { token: buyer.session.token }),
      '买家查看订单详情'
    )
    expect(detail.status).toBe(2) // 已发货
  })

  it('MER-09 销售统计返回总览与趋势', async () => {
    const data = expectOk(
      await api('GET', '/merchant/statistics/sales', {
        token: merchant.session.token,
        params: {
          startDate: '2026-08-01',
          endDate: '2026-08-31',
          granularity: 'day',
        },
      }),
      '销售统计'
    )
    expect(data).toBeTruthy()
    expect(Array.isArray(data.trend)).toBe(true)
  })

  it('MER-10 热销商品 TOP10 返回数组', async () => {
    const data = expectOk(
      await api('GET', '/merchant/statistics/top-products', { token: merchant.session.token }),
      '热销TOP10'
    )
    expect(Array.isArray(data)).toBe(true)
  })

  it('MER-11 运费模板创建与查询（FR-M-11）', async () => {
    const created = await api('POST', '/merchant/freight-templates', {
      token: merchant.session.token,
      body: {
        name: `运费模板${Date.now()}`,
        regionRuleJson: JSON.stringify([{ region: '华东', fee: 8 }]),
        freeShippingThreshold: 99,
        defaultFee: 10,
      },
    })
    expectOk(created, '创建运费模板')
    const list = expectOk(
      await api('GET', '/merchant/freight-templates', { token: merchant.session.token }),
      '运费模板列表'
    )
    expect(Array.isArray(list)).toBe(true)
    expect(list.length).toBeGreaterThan(0)
  })
})
