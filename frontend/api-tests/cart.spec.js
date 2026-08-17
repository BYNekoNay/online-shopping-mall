/**
 * 接口测试：购物车（T02）。
 * 覆盖 docs/09 §2.3 购物车 CRUD + D-4 全选/取消全选。
 */
import { describe, it, expect, beforeAll } from 'vitest'
import { api, expectOk, expectCode } from './helpers/client.js'
import { createConsumer } from './helpers/accounts.js'

describe('cart', () => {
  let ctx

  beforeAll(async () => {
    ctx = await createConsumer('e2e_cart')
  }, 30_000)

  async function addToCart(productId = 101, quantity = 1) {
    const resp = await api('POST', '/cart', {
      token: ctx.session.token,
      body: { productId, quantity },
    })
    expectOk(resp, `加入购物车 ${productId}`)
    const list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    return list.find((item) => item.productId === productId)
  }

  it('CART-01 加入购物车成功且列表可见', async () => {
    const item = await addToCart(101, 2)
    expect(item).toBeTruthy()
    expect(item.quantity).toBe(2)
    expect(item.selected).toBe(1)
  })

  it('CART-02 修改购物车数量', async () => {
    const item = await addToCart(103, 1)
    const resp = await api('PUT', `/cart/${item.id}`, {
      token: ctx.session.token,
      body: { quantity: 3 },
    })
    expectOk(resp, '修改数量')
    const list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    const updated = list.find((i) => String(i.id) === String(item.id))
    expect(updated.quantity).toBe(3)
  })

  it('CART-03 修改选中状态', async () => {
    const item = await addToCart(104, 1)
    await api('PUT', `/cart/${item.id}`, { token: ctx.session.token, body: { selected: 0 } })
    const list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    expect(list.find((i) => String(i.id) === String(item.id)).selected).toBe(0)
  })

  it('CART-04 删除购物车项', async () => {
    const item = await addToCart(105, 1)
    const resp = await api('DELETE', `/cart/${item.id}`, { token: ctx.session.token })
    expectOk(resp, '删除购物车项')
    const list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    expect(list.some((i) => String(i.id) === String(item.id))).toBe(false)
  })

  it('CART-05 全选/取消全选（D-4）', async () => {
    await addToCart(106, 1)
    await addToCart(107, 1)
    // 取消全选
    await api('PUT', '/cart/select-all', { token: ctx.session.token, body: { selected: 0 } })
    let list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    expect(list.every((i) => i.selected === 0)).toBe(true)
    // 全选
    await api('PUT', '/cart/select-all', { token: ctx.session.token, body: { selected: 1 } })
    list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    expect(list.length).toBeGreaterThan(0)
    expect(list.every((i) => i.selected === 1)).toBe(true)
  })

  it('CART-06 购物车列表字段契约（productName/price/quantity/stockEnough）', async () => {
    const list = expectOk(await api('GET', '/cart', { token: ctx.session.token }), '购物车列表')
    expect(list.length).toBeGreaterThan(0)
    const item = list[0]
    expect(item.productName).toBeTruthy()
    expect(typeof item.price).toBe('number')
    expect(item.quantity).toBeGreaterThan(0)
    expect(typeof item.stockEnough).toBe('boolean')
  })

  it('CART-07 未登录访问购物车返回 10002', async () => {
    expectCode(await api('GET', '/cart'), 10002)
  })

  it('CART-08 加入不存在的商品返回错误码（30001/40002 任一为业务拒绝）', async () => {
    const resp = await api('POST', '/cart', { token: ctx.session.token, body: { productId: 999999, quantity: 1 } })
    expect(resp.code).not.toBe(0)
  })
})
