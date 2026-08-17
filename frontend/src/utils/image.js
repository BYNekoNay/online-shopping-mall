/**
 * 商品图片工具：占位图检测 + picsum 确定性兜底图。
 *
 * 背景：种子数据 / Mock 中的商品主图大量使用 example.com / via.placeholder.com 占位，
 * 直接改数据库成本高且需重部署。本模块在前端统一做“兜底”：
 * - 识别占位图 / 空图，替换为 https://picsum.photos/seed/{商品id}/400/400
 * - picsum 以 seed 保证同一商品每次请求图片一致，视觉上可复现
 * 采用“数据库不动、前端全兜底”策略，仅需重建前端。
 */

const PLACEHOLDER_PATTERNS = [
  /example\.com/i,
  /via\.placeholder\.com/i,
  /placehold\.co/i,
  /dummyimage\.com/i,
  /placehold\.jp/i,
  /^undefined$/i,
  /^null$/i,
  /^http:\/\/localhost/i, // 本机后端未启动时的假图
  /^data:\s*$/i
]

/**
 * 判断 URL 是否为占位/无效图。
 * @param {string} src 图片地址
 * @returns {boolean} true 表示需要兜底
 */
export function isPlaceholderUrl(src) {
  const s = String(src || '').trim()
  if (!s) return true
  return PLACEHOLDER_PATTERNS.some((pattern) => pattern.test(s))
}

/**
 * 生成 picsum 确定性兜底图地址。
 * @param {string|number} seed 商品标识（id/productId），同 seed 恒同图
 * @param {number} width 图片宽度
 * @param {number} height 图片高度
 * @returns {string} picsum URL
 */
export function buildFallbackUrl(seed = 'default', width = 400, height = 400) {
  const safeSeed = String(seed ?? '').trim() || 'default'
  return `https://picsum.photos/seed/${encodeURIComponent(safeSeed)}/${width}/${height}`
}

/**
 * 解析商品图片地址：占位/空图 → picsum 兜底，有效图 → 原样返回。
 * @param {string} src 原始图片地址
 * @param {string|number} seed 商品标识
 * @param {number} width 兜底图宽
 * @param {number} height 兜底图高
 * @returns {string} 可直接用于 <img src> 的地址
 */
export function resolveImg(src, seed = 'default', width = 400, height = 400) {
  if (isPlaceholderUrl(src)) return buildFallbackUrl(seed, width, height)
  return src
}

/**
 * 生成 <img @error> 使用的兜底处理器（切换为 picsum 图，并避免死循环）。
 * @param {string|number} seed 商品标识
 * @returns {(event: Event) => void} error 事件处理函数
 */
export function createImgErrorHandler(seed = 'default') {
  return (event) => {
    const img = event && event.target
    if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
    const fallback = buildFallbackUrl(seed, 400, 400)
    // 已兜底仍失败则停止重试，避免死循环请求
    if (img.getAttribute('src') === fallback) return
    img.setAttribute('src', fallback)
    // 兜底图加载成功前不显示破图
    img.style.background = '#f1f5f9'
  }
}
