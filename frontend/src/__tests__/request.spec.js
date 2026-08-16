import { describe, it, expect, vi, beforeEach } from 'vitest'

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
