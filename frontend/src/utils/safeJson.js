/**
 * 雪花 ID 精度保护（生产环境真实缺陷修复）。
 *
 * 背景：后端 ID 生成器使用雪花算法（19 位 Long），超过 JS Number 安全整数 2^53，
 * `JSON.parse` 会丢精度（末位截断），导致按 ID 操作的请求 404 / 10004。
 * 解决方案：在原始 JSON 文本解析前，把"以 Id / id 结尾的键"以及顶层 `data`
 * 这两类键的 15 位以上裸数字改写为字符串，再 parse，ID 原值无损。
 *
 * 小 ID（种子数据，如商品 100）与 total / price / score 等字段不受影响。
 * 与 api-tests/helpers/client.js#parseApiJson 实现等价，供 src 与测试侧共用。
 *
 * @param {string} text 原始 JSON 文本
 * @returns {any} 解析后的对象（已对 15+ 位 *Id 数字做字符串化）
 */
export function parseApiJsonSafe(text) {
  if (typeof text !== 'string') return text
  // 1) "xxxId"/"xxxid" 键 + 15 位以上数字 → 引号包裹（如 userId/shopId/orderId/id）
  let safe = text.replace(/"(\w*[Ii][Dd])":\s*(\d{15,})/g, '"$1":"$2"')
  // 2) 顶层/内层 "data": <15 位以上裸数字> → 引号包裹（如 POST /user/addresses 返回 data=地址ID）
  safe = safe.replace(/"data":\s*(\d{15,})/g, '"data":"$1"')
  return JSON.parse(safe)
}
