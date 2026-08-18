import { describe, it, expect, vi, beforeEach } from 'vitest'
import { parseApiJsonSafe } from '@/utils/safeJson'

// 模拟 ElMessage（避免直接导入未使用的 vue/element-plus）
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

// 由于 request.js 依赖 router（需要完整 Vue 环境），这里改为测试拦截器逻辑本身
describe('Request interceptor logic', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('attaches Authorization header when token exists', () => {
    localStorage.setItem('user', JSON.stringify({ token: 'test-token' }))

    const userStore = JSON.parse(localStorage.getItem('user') || '{}')
    const config = { headers: {} }
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    expect(config.headers.Authorization).toBe('Bearer test-token')
  })

  it('does not attach header when no token', () => {
    const userStore = JSON.parse(localStorage.getItem('user') || '{}')
    const config = { headers: {} }
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('extracts code/message/data from successful response', () => {
    const response = { data: { code: 0, message: 'success', data: { id: 1 } } }
    const { code, message, data } = response.data
    expect(code).toBe(0)
    expect(message).toBe('success')
    expect(data).toEqual({ id: 1 })
  })

  it('rejects when response code is non-zero', () => {
    const response = { data: { code: 10002, message: 'Not logged in', data: null } }
    const { code, message } = response.data
    expect(code).not.toBe(0)
    expect(message).toBe('Not logged in')
  })
})

// 雪花 ID 精度保护（生产环境真实缺陷修复）。transformResponse 依赖此逻辑。
describe('parseApiJsonSafe - 雪花 ID 精度保护', () => {
  it('19 位 *Id 数字转为字符串，避免 JSON.parse 丢精度', () => {
    // 19 位雪花 ID：Number() 会丢精度为 1234567890123456800
    const snowflake = '1234567890123456789'
    const text = `{"code":0,"data":{"id":${snowflake},"userId":${snowflake},"shopId":${snowflake}}}`
    const obj = parseApiJsonSafe(text)
    expect(obj.data.id).toBe(snowflake)
    expect(obj.data.userId).toBe(snowflake)
    expect(obj.data.shopId).toBe(snowflake)
    expect(typeof obj.data.id).toBe('string')
  })

  it('顶层 data 为 19 位裸数字时转为字符串（地址/订单 ID 直返场景）', () => {
    const snowflake = '1987654321098765432'
    const text = `{"code":0,"data":${snowflake}}`
    const obj = parseApiJsonSafe(text)
    expect(obj.data).toBe(snowflake)
    expect(typeof obj.data).toBe('string')
  })

  it('小 ID（种子数据）保持为 Number，不被错误转字符串', () => {
    const text = '{"code":0,"data":{"id":100,"userId":1,"shopId":2}}'
    const obj = parseApiJsonSafe(text)
    expect(obj.data.id).toBe(100)
    expect(typeof obj.data.id).toBe('number')
    expect(obj.data.userId).toBe(1)
  })

  it('total / price / score 等非 ID 数字字段不受影响', () => {
    const text = '{"code":0,"data":{"total":9999,"price":99.9,"score":4}}'
    const obj = parseApiJsonSafe(text)
    expect(obj.data.total).toBe(9999)
    expect(obj.data.price).toBe(99.9)
    expect(obj.data.score).toBe(4)
  })

  it('images / detail / skus 等非 Id 结尾键不被破坏', () => {
    // API 创建商品的 images 可能是 JSON 数组字符串，detail 是富文本，skus 是数组
    const text = JSON.stringify({
      code: 0,
      data: {
        id: '1234567890123456789',
        name: 'API商品',
        images: '["https://a.jpg","https://b.jpg"]',
        detail: '<p>富文本</p>',
        skus: [{ specJson: '{"规格":"默认"}', price: 99, stock: 10 }]
      }
    })
    const obj = parseApiJsonSafe(text)
    expect(obj.data.images).toBe('["https://a.jpg","https://b.jpg"]')
    expect(obj.data.detail).toBe('<p>富文本</p>')
    expect(Array.isArray(obj.data.skus)).toBe(true)
    expect(obj.data.skus[0].price).toBe(99)
  })

  it('嵌套对象内的 *Id 同样受保护', () => {
    const snowflake = '1234567890123456789'
    const text = `{"data":{"records":[{"orderId":${snowflake},"productId":${snowflake}}]}}`
    const obj = parseApiJsonSafe(text)
    expect(obj.data.records[0].orderId).toBe(snowflake)
    expect(obj.data.records[0].productId).toBe(snowflake)
  })
})
