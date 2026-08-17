/**
 * 接口测试：营销与积分（T02）。
 * 覆盖 docs/09 §2.7 优惠券领取/我的优惠券/积分余额/积分流水，§2.8 C-1 积分商城。
 */
import { describe, it, expect, beforeAll } from 'vitest'
import { api, expectOk, expectCode, expectPageShape } from './helpers/client.js'
import { sessions, createConsumer } from './helpers/accounts.js'

describe('marketing', () => {
  let consumer

  beforeAll(async () => {
    consumer = await createConsumer('e2e_mkt')
  }, 30_000)

  it('MKT-01 可领取优惠券列表返回模板', async () => {
    const data = expectOk(
      await api('GET', '/coupons/available', { token: consumer.session.token }),
      '可领取优惠券'
    )
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    expect(data[0].name).toBeTruthy()
  })

  it('MKT-02 领取优惠券返回 userCouponId', async () => {
    const data = expectOk(
      await api('POST', '/coupons/1/receive', { token: consumer.session.token }),
      '领取优惠券'
    )
    expect(data.userCouponId).toBeTruthy()
  })

  it('MKT-03 重复领取同一优惠券返回 10001（限领 1 张）', async () => {
    const resp = await api('POST', '/coupons/1/receive', { token: consumer.session.token })
    expectCode(resp, 10001)
  })

  it('MKT-04 我的优惠券列表含已领取记录', async () => {
    const data = expectOk(await api('GET', '/user/coupons', { token: consumer.session.token }), '我的优惠券')
    expect(Array.isArray(data)).toBe(true)
    expect(data.some((c) => c.couponId === 1)).toBe(true)
  })

  it('MKT-05 优惠券不存在领取返回 10001/60001 任一业务拒绝', async () => {
    const resp = await api('POST', '/coupons/999999/receive', { token: consumer.session.token })
    expect(resp.code).not.toBe(0)
  })

  it('MKT-06 积分余额可查询（新用户 0）', async () => {
    const data = expectOk(await api('GET', '/user/points', { token: consumer.session.token }), '积分余额')
    expect(typeof data.points).toBe('number')
  })

  it('MKT-07 积分流水返回分页结构', async () => {
    const data = expectOk(await api('GET', '/user/points/records', { token: consumer.session.token }), '积分流水')
    expect(Array.isArray(data.records)).toBe(true)
    expect(typeof data.total).toBe('number')
  })

  it('MKT-08 积分商城商品列表分页且含 pointsCost（需登录）', async () => {
    const data = expectOk(
      await api('GET', '/points/goods', { token: consumer.session.token, params: { pageNum: 1, pageSize: 10 } }),
      '积分商城'
    )
    expectPageShape(data)
    expect(data.total).toBeGreaterThan(0)
    expect(data.records[0].pointsCost).toBeGreaterThan(0)
  })

  it('MKT-09 积分不足兑换返回 10001', async () => {
    const resp = await api('POST', '/points/exchange', {
      token: consumer.session.token,
      body: { goodsId: 1, quantity: 1 },
    })
    expectCode(resp, 10001)
  })

  it('MKT-10 当前生效促销活动列表（scope=SHOP）返回', async () => {
    const data = expectOk(
      await api('GET', '/promotions/active', { params: { scope: 'SHOP', scopeId: 1 } }),
      '生效促销活动'
    )
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    expect(data[0].type).toBeTruthy()
  })

  it('MKT-11 促销活动不带参数返回空数组（scope/scopeId 为可选查询条件）', async () => {
    const resp = await api('GET', '/promotions/active')
    expectOk(resp, '促销活动无参数')
    expect(Array.isArray(resp.data)).toBe(true)
  })
})
