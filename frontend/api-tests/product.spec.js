/**
 * 接口测试：商品与搜索（T02）。
 * 覆盖 docs/09 §2.2 商品列表/搜索/详情/分类树契约。
 */
import { describe, it, expect } from 'vitest'
import { api, expectOk, expectCode, expectPageShape } from './helpers/client.js'

describe('product', () => {
  it('PROD-01 商品列表返回分页结构且 records 非空', async () => {
    const data = expectOk(await api('GET', '/products', { params: { pageNum: 1, pageSize: 5 } }), '商品列表')
    expectPageShape(data)
    expect(data.total).toBeGreaterThan(0)
    expect(data.records.length).toBeGreaterThan(0)
  })

  it('PROD-02 商品列表按分类过滤（categoryId=1 电子产品）', async () => {
    const data = expectOk(await api('GET', '/products', { params: { categoryId: 1, pageSize: 20 } }), '分类过滤')
    expect(data.total).toBeGreaterThan(0)
    expect(data.records.every((p) => p.categoryId === 1)).toBe(true)
  })

  it('PROD-03 商品列表价格区间过滤（minPrice/maxPrice）', async () => {
    const data = expectOk(
      await api('GET', '/products', { params: { minPrice: 100, maxPrice: 6000, pageSize: 50 } }),
      '价格过滤'
    )
    expect(data.total).toBeGreaterThan(0)
    for (const p of data.records) {
      expect(p.price).toBeGreaterThanOrEqual(100)
      expect(p.price).toBeLessThanOrEqual(6000)
    }
  })

  it('PROD-04 商品列表排序参数契约（sort=price_asc / sort=sales 均可响应）', async () => {
    const asc = expectOk(await api('GET', '/products', { params: { sort: 'price_asc', pageSize: 50 } }), '价格升序')
    const prices = asc.records.map((p) => Number(p.price))
    for (let i = 1; i < prices.length; i += 1) {
      expect(prices[i]).toBeGreaterThanOrEqual(prices[i - 1])
    }
    // 销量排序（TC-C-29 对应）：sort=sales 契约可响应且返回非空
    const sales = expectOk(await api('GET', '/products', { params: { sort: 'sales', pageSize: 50 } }), '销量排序')
    expect(sales.total).toBeGreaterThan(0)
    expect(Array.isArray(sales.records)).toBe(true)
  })

  it('PROD-05 关键词搜索命中预期商品（keyword=Phone）', async () => {
    const data = expectOk(await api('GET', '/products', { params: { keyword: 'Phone', pageSize: 20 } }), '关键词搜索')
    expect(data.total).toBeGreaterThan(0)
    expect(data.records.some((p) => /phone/i.test(p.name))).toBe(true)
  })

  it('PROD-06 商品详情返回价格/库存/SKU/分类（id=100）', async () => {
    const data = expectOk(await api('GET', '/products/100'), '商品详情 100')
    expect(data.id).toBe(100)
    expect(data.price).toBeGreaterThan(0)
    expect(data.stock).toBeGreaterThan(0)
    expect(Array.isArray(data.skuList)).toBe(true)
    expect(data.skuList.length).toBeGreaterThan(0)
  })

  it('PROD-07 不存在的商品详情返回 30001', async () => {
    expectCode(await api('GET', '/products/999999'), 30001)
  })

  it('PROD-08 分类树返回层级结构', async () => {
    const data = expectOk(await api('GET', '/products/categories/tree'), '分类树')
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    const root = data[0]
    expect(root.id).toBeTruthy()
    expect(root.name).toBeTruthy()
    expect(Array.isArray(root.children)).toBe(true)
  })

  it('PROD-09 商品评价与评分接口可用（id=100）', async () => {
    const reviews = expectOk(await api('GET', '/products/100/reviews'), '商品评价')
    expectPageShape(reviews)
    const rating = expectOk(await api('GET', '/products/100/rating'), '商品评分')
    expect(typeof rating.avgRating).toBe('number')
    expect(typeof rating.reviewCount).toBe('number')
  })
})
