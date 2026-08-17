/**
 * 接口测试：安全与权限（T02）。
 * 覆盖 docs/09 §1.4 鉴权语义与错误码：未登录 10002 / 越权 10003 / 参数校验 10001。
 */
import { describe, it, expect } from 'vitest'
import { api, expectCode } from './helpers/client.js'
import { sessions, createConsumer, createMerchant, uniqueName } from './helpers/accounts.js'

describe('security', () => {
  it('SEC-01 未登录访问需鉴权接口返回 10002', async () => {
    expectCode(await api('GET', '/user/profile'), 10002)
    expectCode(await api('GET', '/cart'), 10002)
    expectCode(await api('GET', '/orders'), 10002)
  })

  it('SEC-02 伪造 Token 访问返回 10002', async () => {
    expectCode(await api('GET', '/user/profile', { token: 'fake.jwt.token' }), 10002)
  })

  it('SEC-03 消费者访问商家接口返回 10003', async () => {
    expectCode(await api('GET', '/merchant/products', { token: sessions.consumer.token }), 10003)
    expectCode(await api('GET', '/merchant/orders', { token: sessions.consumer.token }), 10003)
  })

  it('SEC-04 消费者访问管理员接口返回 10003', async () => {
    expectCode(await api('GET', '/admin/users', { token: sessions.consumer.token }), 10003)
  })

  it('SEC-05 商家访问管理员接口返回 10003', async () => {
    const { session } = await createMerchant('e2e_secm')
    expectCode(await api('GET', '/admin/users', { token: session.token }), 10003)
  })

  it('SEC-06 弱密码注册返回 10001', async () => {
    const username = uniqueName('e2e_secp')
    const resp = await api('POST', '/auth/register', {
      body: { username, password: 'short', nickname: 'x' },
    })
    expectCode(resp, 10001)
  })

  it('SEC-07 非法手机号注册返回 10001', async () => {
    const username = uniqueName('e2e_sech')
    const resp = await api('POST', '/auth/register', {
      body: { username, password: 'Mall@2026', nickname: 'x', phone: '12345' },
    })
    expectCode(resp, 10001)
  })

  it('SEC-08 未登录不能领取优惠券/查积分', async () => {
    expectCode(await api('POST', '/coupons/1/receive'), 10002)
    expectCode(await api('GET', '/user/points'), 10002)
  })

  it('SEC-09 新用户密码强度合规可注册（正向对照）', async () => {
    const { session } = await createConsumer('e2e_secok')
    expect(session.role).toBe(1)
  })
})
