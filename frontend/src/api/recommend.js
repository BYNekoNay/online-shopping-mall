import request from '@/utils/request'

export function getRecommendations(num = 10) {
  return request.get('/recommend/guess-you-like', { params: { num } })
}

export function getSimilarProducts(productId, num = 10) {
  return request.get(`/recommend/similar/${productId}`, { params: { num } })
}

// A-1 浏览历史推荐（需登录）
export function getHistoryRecommendations(num = 10) {
  return request.get('/recommend/history', { params: { num } })
}

export default { getRecommendations, getSimilarProducts, getHistoryRecommendations }
