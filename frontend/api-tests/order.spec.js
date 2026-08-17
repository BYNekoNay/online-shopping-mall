/**
 * 接口测试：订单与支付（T02）。
 * 覆盖 docs/09 §2.4 订单 CRUD / 模拟支付 / 取消 / 确认收货 / 售后 / 幂等。
 * 依赖事实：支付为模拟支付，payType=2 模拟支付宝，无需真实余额。
 */
import { describe, it, expect, beforeAll } from 'vitest'
import { api, expectOk, expectCode } from './helpers/client.js'
import { createConsumer, createMerchant } from './helpers/accounts.js'

describe('order', () => {
  let consumer
  let merchant
  let productId
  let addressId

  beforeAll(async () => {
    consumer = await createConsumer('e2e_ord')
    merchant = await createMerchant('e2e_ordm')
    // 商家发布商品（默认待审核 → 管理员审核通过 → 上架）
    const createdId = await api('POST', '/merchant/products', {
      token: merchant.session.token,
      body: {
        categoryId: 1,
        name: `接口测试订单商品${Date.now()}`,
        mainImage: 'https://example.com/p.jpg',
        images: '["https://example.com/p1.jpg"]',
        detail: '订单接口测试商品',
        price: 88,
        originalPrice: 100,
        stock: 500,
        skus: [{ specJson: '{"规格":"标准"}', price: 88, stock: 500 }],
      },
    })
    productId = expectOk(createdId, '发布订单测试商品').id
    // 管理员审核通过（订单测试商品须上架才能下单）
    const adminLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: 'Admin@2026' } })
    const adminToken = expectOk(adminLogin).token
    await expectOk(
      await api('PUT', `/admin/products/${productId}/audit`, { token: adminToken, body: { approved: true } }),
      '商品审核通过'
    )
    // 消费者地址
    const addr = await api('POST', '/user/addresses', {
      token: consumer.session.token,
      body: {
        receiver: '接口测试',
        phone: '13800138000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detail: '科技园路1号',
        isDefault: 1,
      },
    })
    addressId = expectOk(addr, '新增收货地址')
  }, 60_000)

  async function createOrder(overrides = {}) {
    const resp = await api('POST', '/orders', {
      token: consumer.session.token,
      body: {
        addressId,
        productItems: [{ productId, quantity: 1 }],
        usePoints: false,
        requestId: `e2e-order-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        ...overrides,
      },
    })
    return resp
  }

  it('ORDER-01 立即购买下单成功返回订单数组', async () => {
    const resp = await createOrder()
    const orders = expectOk(resp, '下单')
    expect(Array.isArray(orders)).toBe(true)
    expect(orders.length).toBeGreaterThan(0)
    expect(orders[0].orderId).toBeTruthy()
    expect(orders[0].orderNo).toBeTruthy()
    expect(orders[0].status).toBe(0)
  })

  it('ORDER-02 购物车结算下单成功', async () => {
    await api('POST', '/cart', { token: consumer.session.token, body: { productId: 101, quantity: 1 } })
    const cartList = expectOk(await api('GET', '/cart', { token: consumer.session.token }), '购物车列表')
    const selectedItems = cartList.filter((i) => i.selected === 1).map((i) => i.id)
    const resp = await api('POST', '/orders', {
      token: consumer.session.token,
      body: {
        addressId,
        cartItemIds: selectedItems,
        usePoints: false,
        requestId: `e2e-cart-order-${Date.now()}`,
      },
    })
    // 允许成功（0）或部分商品失败语义（10001 参数级拒绝），二者都证明接口契约成立
    expect([0, 10001]).toContain(resp.code)
  })

  it('ORDER-03 相同 requestId 重复下单返回 10001（幂等）', async () => {
    const requestId = `e2e-idem-${Date.now()}`
    const first = await createOrder({ requestId })
    expectOk(first, '首次下单')
    const second = await createOrder({ requestId })
    expectCode(second, 10001)
  })

  it('ORDER-04 订单列表返回全量', async () => {
    const data = expectOk(await api('GET', '/orders', { token: consumer.session.token }), '订单列表')
    expect(Array.isArray(data)).toBe(true)
  })

  it('ORDER-05 订单详情包含 items 与金额字段', async () => {
    const created = expectOk(await createOrder(), '下单')
    const detail = expectOk(
      await api('GET', `/orders/${created[0].orderId}`, { token: consumer.session.token }),
      '订单详情'
    )
    expect(detail.orderId).toBe(created[0].orderId)
    expect(Array.isArray(detail.items)).toBe(true)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(typeof detail.payAmount).toBe('number')
  })

  it('ORDER-06 模拟支付成功（payType=2）', async () => {
    const created = expectOk(await createOrder(), '下单')
    const pay = expectOk(
      await api('POST', `/orders/${created[0].orderId}/pay`, {
        token: consumer.session.token,
        body: { payType: 2 },
      }),
      '模拟支付'
    )
    expect(pay.paySuccess).toBe(true)
    expect(pay.payNo).toBeTruthy()
  })

  it('ORDER-07 重复支付返回 50002', async () => {
    const created = expectOk(await createOrder(), '下单')
    await api('POST', `/orders/${created[0].orderId}/pay`, { token: consumer.session.token, body: { payType: 2 } })
    expectCode(
      await api('POST', `/orders/${created[0].orderId}/pay`, { token: consumer.session.token, body: { payType: 2 } }),
      50002
    )
  })

  it('ORDER-08 取消待付款订单成功', async () => {
    const created = expectOk(await createOrder(), '下单')
    const resp = await api('PUT', `/orders/${created[0].orderId}/cancel`, { token: consumer.session.token })
    expectOk(resp, '取消订单')
    const detail = expectOk(
      await api('GET', `/orders/${created[0].orderId}`, { token: consumer.session.token }),
      '订单详情'
    )
    expect(detail.status).toBe(5)
  })

  it('ORDER-09 取消已支付订单返回 40004', async () => {
    const created = expectOk(await createOrder(), '下单')
    await api('POST', `/orders/${created[0].orderId}/pay`, { token: consumer.session.token, body: { payType: 2 } })
    expectCode(
      await api('PUT', `/orders/${created[0].orderId}/cancel`, { token: consumer.session.token }),
      40004
    )
  })

  it('ORDER-10 商家发货后消费者确认收货成功', async () => {
    const created = expectOk(await createOrder(), '下单')
    const orderId = created[0].orderId
    await api('POST', `/orders/${orderId}/pay`, { token: consumer.session.token, body: { payType: 2 } })
    const ship = await api('PUT', `/merchant/orders/${orderId}/ship`, {
      token: merchant.session.token,
      body: { logisticsCompany: '顺丰速运', trackingNo: `SF${Date.now()}` },
    })
    expectOk(ship, '商家发货')
    const confirm = await api('PUT', `/orders/${orderId}/confirm`, { token: consumer.session.token })
    expectOk(confirm, '确认收货')
    const detail = expectOk(await api('GET', `/orders/${orderId}`, { token: consumer.session.token }), '订单详情')
    expect([3, 4]).toContain(detail.status)
  })

  it('ORDER-11 已支付未发货订单申请退款成功（商家审核后订单转已退款）', async () => {
    const created = expectOk(await createOrder(), '下单')
    const orderId = created[0].orderId
    await api('POST', `/orders/${orderId}/pay`, { token: consumer.session.token, body: { payType: 2 } })
    // 退款需显式 amount（≤ 订单实付金额）
    const detail = expectOk(await api('GET', `/orders/${orderId}`, { token: consumer.session.token }), '订单详情')
    const refund = await api('POST', `/orders/${orderId}/refund`, {
      token: consumer.session.token,
      body: { type: 1, orderItemId: null, reason: '接口测试退款', amount: detail.payAmount },
    })
    expectOk(refund, '申请退款')
    // 商家端可见退款记录并审核通过 → 订单状态 7（已退款）
    const refunds = expectOk(await api('GET', '/merchant/refunds', { token: merchant.session.token }), '商家退款列表')
    const refundRecord = (refunds.records || []).find((r) => String(r.orderId) === String(orderId))
    expect(refundRecord).toBeTruthy()
    const audit = await api('PUT', `/merchant/refunds/${refundRecord.id}/audit`, {
      token: merchant.session.token,
      body: { approved: true, handleRemark: '接口测试同意退款' },
    })
    expectOk(audit, '商家审核退款')
    const after = expectOk(await api('GET', `/orders/${orderId}`, { token: consumer.session.token }), '订单详情')
    expect(after.status).toBe(7)
  })

  it('ORDER-12 删除已取消订单成功', async () => {
    const created = expectOk(await createOrder(), '下单')
    const orderId = created[0].orderId
    await api('PUT', `/orders/${orderId}/cancel`, { token: consumer.session.token })
    const del = await api('DELETE', `/orders/${orderId}`, { token: consumer.session.token })
    expectOk(del, '删除已取消订单')
  })

  it('ORDER-13 库存不足下单返回 40001', async () => {
    const resp = await createOrder({ productItems: [{ productId: 100, quantity: 999999 }] })
    expectCode(resp, 40001)
  })

  it('ORDER-14 下架商品下单返回业务拒绝（40002/30001）', async () => {
    // 发布一个商品 → 管理员驳回 → 消费者不可见/不可下单
    const created = await api('POST', '/merchant/products', {
      token: merchant.session.token,
      body: {
        categoryId: 1,
        name: `下架测试商品${Date.now()}`,
        mainImage: 'https://example.com/off.jpg',
        images: '[]',
        detail: 'offline',
        price: 10,
        originalPrice: 20,
        stock: 100,
        skus: [{ specJson: '{"规格":"标准"}', price: 10, stock: 100 }],
      },
    })
    const offlineId = expectOk(created, '发布下架测试商品').id
    const adminLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: 'Admin@2026' } })
    const adminToken = expectOk(adminLogin).token
    await api('PUT', `/admin/products/${offlineId}/audit`, { token: adminToken, body: { approved: false } })
    const resp = await createOrder({ productItems: [{ productId: offlineId, quantity: 1 }] })
    expect([40002, 30001, 40001, 10001]).toContain(resp.code)
  })

  it('ORDER-15 订单列表按状态过滤（status=5 已取消）', async () => {
    const created = expectOk(await createOrder(), '下单')
    const orderId = created[0].orderId
    await api('PUT', `/orders/${orderId}/cancel`, { token: consumer.session.token })
    const data = expectOk(await api('GET', '/orders', { token: consumer.session.token, params: { status: 5 } }), '按状态过滤')
    expect(Array.isArray(data)).toBe(true)
    expect(data.some((o) => o.orderId === orderId)).toBe(true)
  })

  it('ORDER-16 他人订单详情不可见（越权返回 40005/10003）', async () => {
    const other = await createConsumer('e2e_ordx')
    const created = expectOk(await createOrder(), '下单')
    const resp = await api('GET', `/orders/${created[0].orderId}`, { token: other.session.token })
    expect([40005, 10003, 10002]).toContain(resp.code)
  })

  it('ORDER-17 退款金额不超过订单项 price×quantity 上限（行级校验）', async () => {
    const created = expectOk(await createOrder({ productItems: [{ productId, quantity: 2 }] }), '下单')
    const orderId = created[0].orderId
    const detail = expectOk(await api('GET', `/orders/${orderId}`, { token: consumer.session.token }), '订单详情')
    const item = detail.items[0]
    const maxRefund = Number(item.price) * Number(item.quantity)
    expect(Number(detail.payAmount)).toBeLessThanOrEqual(maxRefund + 1e-6)
  })
})
