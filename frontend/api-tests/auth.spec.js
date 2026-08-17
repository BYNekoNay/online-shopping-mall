/**
 * 接口测试：用户与鉴权（T02 / TC-SEC 关联）。
 * 覆盖 docs/09 §2.1 注册/登录契约与错误码表（20001/20002/20003）。
 */
import { describe, it, expect } from 'vitest'
import { api, expectOk, expectCode } from './helpers/client.js'
import { SEED, sessions, createConsumer } from './helpers/accounts.js'

describe('auth', () => {
  it('AUTH-01 登录成功返回 token/userId/role/nickname', async () => {
    const data = expectOk(
      await api('POST', '/auth/login', { body: { username: SEED.admin.username, password: SEED.admin.password } }),
      '管理员登录'
    )
    expect(data.token).toBeTruthy()
    expect(data.userId).toBeTruthy()
    expect(data.role).toBe(3)
    expect(data.nickname).toBeTruthy()
  })

  it('AUTH-02 密码错误返回 20001（或命中登录限流 10005）', async () => {
    const resp = await api('POST', '/auth/login', { body: { username: SEED.consumer.username, password: 'Wrong@2026' } })
    // 系统自带登录暴力破解防护：同 IP+用户名失败 5 次 / IP 累计 30 次后返回 10005
    expect([20001, 10005]).toContain(resp.code)
  })

  it('AUTH-03 不存在的用户返回 20001（或命中登录限流 10005）', async () => {
    const resp = await api('POST', '/auth/login', { body: { username: 'no_such_user_xyz', password: 'Mall@2026' } })
    expect([20001, 10005]).toContain(resp.code)
  })

  it('AUTH-04 注册成功且新账号可登录', async () => {
    const { session } = await createConsumer('e2e_auth')
    expect(session.userId).toBeTruthy()
    expect(session.role).toBe(1)
  })

  it('AUTH-05 重复用户名注册返回 20003', async () => {
    const { account } = await createConsumer('e2e_auth_dup')
    const resp = await api('POST', '/auth/register', {
      body: { username: account.username, password: 'Mall@2026', nickname: 'dup' },
    })
    expectCode(resp, 20003)
  })

  it('AUTH-06 未登录访问需鉴权接口返回 10002', async () => {
    const resp = await api('GET', '/user/profile')
    expectCode(resp, 10002)
  })

  it('AUTH-07 种子账号 testuser 登录后积分余额为 100（依赖事实校验）', async () => {
    const points = expectOk(
      await api('GET', '/user/points', { token: sessions.consumer.token }),
      'testuser 积分余额'
    )
    expect(points.points).toBe(100)
  })
})
