import request from '@/utils/request'

/**
 * 记录用户行为（浏览/收藏/购买/评价）。
 * @param {number} userId - 用户ID
 * @param {number} productId - 商品ID
 * @param {number} behaviorType - 1=浏览, 2=收藏, 3=购买, 4=评价
 */
export function recordBehavior(userId, productId, behaviorType) {
  return request.post('/behavior/record', { userId, productId, behaviorType })
}

/**
 * 推荐位曝光埋点。
 * @param {object} params - { userId, source, productIds }
 */
export function recommendExposure(params) {
  return request.post('/behavior/recommend-exposure', params)
}

/**
 * 推荐位点击埋点（记为浏览行为）。
 * @param {object} params - { userId, source, productId, position }
 */
export function recommendClick(params) {
  return request.post('/behavior/recommend-click', params)
}

/**
 * 页面进入上报。
 * @param {object} params - { userId, sessionId, pagePath, referrerPage }
 * @returns {Promise<number>} 返回 pageViewLog 的 id
 */
export function pageEnter(params) {
  return request.post('/behavior/page-view', params)
}

/**
 * 页面离开回填。
 * @param {number} id - pageViewLog 的 id
 * @param {number} stayDuration - 停留时长（秒）
 */
export function pageLeave(id, stayDuration) {
  return request.put(`/behavior/page-view/${id}/leave`, { stayDuration })
}

export default { recordBehavior, recommendExposure, recommendClick, pageEnter, pageLeave }
