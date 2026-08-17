/**
 * 接口测试：个性化推荐（T02）。
 * 覆盖 docs/09 §2.2 猜你喜欢（可选登录）/相似商品，§2.8 A-1 浏览历史推荐、D-5 购买推荐。
 */
import { describe, it, expect } from 'vitest'
import { api, expectOk } from './helpers/client.js'
import { sessions, createConsumer } from './helpers/accounts.js'

function assertRecommendShape(list, label) {
  expect(Array.isArray(list), `${label} 应为数组`).toBe(true)
  for (const item of list) {
    expect(item.productId, `${label} productId`).toBeTruthy()
    expect(item.name, `${label} name`).toBeTruthy()
    expect(typeof item.price, `${label} price`).toBe('number')
    expect(typeof item.score, `${label} score`).toBe('number')
    expect(item.algorithmType, `${label} algorithmType`).toBeTruthy()
  }
}

describe('recommend', () => {
  it('REC-01 猜你喜欢（未登录）返回热门兜底数组', async () => {
    const data = expectOk(await api('GET', '/recommend/guess-you-like', { params: { num: 5 } }), '猜你喜欢未登录')
    assertRecommendShape(data, '猜你喜欢未登录')
  })

  it('REC-02 猜你喜欢（登录）返回个性化数组', async () => {
    const data = expectOk(
      await api('GET', '/recommend/guess-you-like', { token: sessions.consumer.token, params: { num: 5 } }),
      '猜你喜欢登录'
    )
    assertRecommendShape(data, '猜你喜欢登录')
  })

  it('REC-03 相似商品推荐返回数组（id=100）', async () => {
    const data = expectOk(await api('GET', '/recommend/similar/100', { params: { num: 5 } }), '相似商品')
    assertRecommendShape(data, '相似商品')
  })

  it('REC-04 不存在的商品相似推荐返回空数组', async () => {
    const data = expectOk(await api('GET', '/recommend/similar/999999'), '相似商品不存在')
    expect(data).toEqual([])
  })

  it('REC-05 浏览历史推荐（登录）返回数组', async () => {
    const data = expectOk(
      await api('GET', '/recommend/history', { token: sessions.consumer.token, params: { num: 5 } }),
      '浏览历史推荐'
    )
    assertRecommendShape(data, '浏览历史推荐')
  })

  it('REC-06 购买推荐（登录）返回数组', async () => {
    const data = expectOk(
      await api('GET', '/recommend/purchase', { token: sessions.consumer.token, params: { num: 5 } }),
      '购买推荐'
    )
    assertRecommendShape(data, '购买推荐')
  })

  it('REC-07 num 参数钳制（num=100 超上限回退默认 10）', async () => {
    const data = expectOk(await api('GET', '/recommend/guess-you-like', { params: { num: 100 } }), 'num 钳制')
    expect(data.length).toBeLessThanOrEqual(10)
  })

  it('REC-08 推荐接口 algorithmType 取值合法（1-UserCF/2-ItemCF/3-混合/4-热门）', async () => {
    const data = expectOk(
      await api('GET', '/recommend/guess-you-like', { token: sessions.consumer.token, params: { num: 20 } }),
      'algorithmType 合法'
    )
    const valid = new Set([1, 2, 3, 4])
    for (const item of data) {
      expect(valid.has(item.algorithmType), `非法 algorithmType=${item.algorithmType}`).toBe(true)
    }
  })

  it('REC-09 新注册用户（无行为）猜你喜欢走热门兜底且可返回', async () => {
    const { session } = await createConsumer('e2e_rec')
    const data = expectOk(
      await api('GET', '/recommend/guess-you-like', { token: session.token, params: { num: 5 } }),
      '冷启动推荐'
    )
    assertRecommendShape(data, '冷启动推荐')
  })
})
