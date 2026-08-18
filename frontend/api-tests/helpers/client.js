/**
 * 接口测试客户端（T02）。
 *
 * 零新增依赖：仅使用 Node 22 内置 fetch + vitest。
 *  - baseUrl 默认线上测试环境 http://8.160.181.12/api，可用环境变量 API_BASE_URL 切换；
 *  - 统一携带 Bearer token；
 *  - 提供断言 helper：expectOk（code=0）、expectCode（指定错误码）。
 */
import { parseApiJsonSafe } from '../../src/utils/safeJson.js'

export const API_BASE = process.env.API_BASE_URL || 'http://8.160.181.12/api'

/**
 * 解析后端 JSON 响应（处理雪花 ID 精度）：
 * 后端雪花 ID（Long，19 位数字）超过 JS 安全整数 2^53，JSON.parse 会丢精度，
 * 导致按 ID 操作的请求 404。实现委托给 src/utils/safeJson（生产侧同源），确保
 * 线上 axios 拦截器与接口测试解析口径一致。
 */
export function parseApiJson(text) {
  return parseApiJsonSafe(text)
}

/**
 * 发起接口请求。
 * @param {string} method GET/POST/PUT/DELETE
 * @param {string} path 以 / 开头的接口路径（不含 /api 前缀）
 * @param {{token?: string, body?: object, params?: Record<string, any>}} options
 * @returns {Promise<{code:number,message:string,data:any,httpStatus?:number}>}
 */
export async function api(method, path, { token, body, params } = {}) {
  const url = new URL(`${API_BASE}${path}`)
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v))
    })
  }
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(url.toString(), {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch (err) {
    throw new Error(`请求失败 ${method} ${path}: ${err.message}`)
  }
  const httpStatus = res.status
  const text = await res.text()
  let json
  try {
    json = parseApiJson(text)
  } catch {
    throw new Error(`响应非 JSON ${method} ${path}: HTTP ${httpStatus}`)
  }
  return { ...json, httpStatus }
}

/** 断言业务成功（code=0）。 */
export function expectOk(resp, message = '预期业务成功') {
  expect(resp.code, `${message}：resp=${JSON.stringify(resp)}`).toBe(0)
  return resp.data
}

/** 断言指定业务错误码。 */
export function expectCode(resp, code, message = `预期错误码 ${code}`) {
  expect(resp.code, `${message}：resp=${JSON.stringify(resp)}`).toBe(code)
  return resp
}

/** 断言 HTTP 状态码。 */
export function expectHttp(resp, status, message = `预期 HTTP ${status}`) {
  expect(resp.httpStatus, `${message}：resp=${JSON.stringify(resp)}`).toBe(status)
}

/** 断言分页结构（{records,total,pageNum,pageSize}）。 */
export function expectPageShape(data, message = '预期分页结构') {
  expect(data, message).toBeTruthy()
  expect(Array.isArray(data.records), `${message}：records 应为数组`).toBe(true)
  expect(typeof data.total, `${message}：total 应为数字`).toBe('number')
  expect(typeof data.pageNum, `${message}：pageNum 应为数字`).toBe('number')
  expect(typeof data.pageSize, `${message}：pageSize 应为数字`).toBe('number')
}
