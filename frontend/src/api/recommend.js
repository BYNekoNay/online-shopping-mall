import request from '@/utils/request'

export function getRecommendations(num = 10) {
  return request.get('/recommend/guess-you-like', { params: { num } })
}

export function getSimilarProducts(productId, num = 10) {
  return request.get(`/recommend/similar/${productId}`, { params: { num } })
}

export default { getRecommendations, getSimilarProducts }
