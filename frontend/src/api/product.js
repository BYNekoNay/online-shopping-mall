import request from '@/utils/request'

export function getProducts(params) {
  return request.get('/products', { params })
}

export function getProduct(id) {
  return request.get(`/products/${id}`)
}

export function searchProducts(params) {
  // 统一使用 getProducts，后端 /api/products/search 已标记 @deprecated
  return request.get('/products', { params: { ...params, keyword: params?.keyword } })
}

export function getCategories() {
  return request.get('/products/categories/tree')
}

export function getProductReviews(id) {
  return request.get(`/products/${id}/reviews`)
}

/** 获取商品评分概况 */
export function getProductRating(id) {
  return request.get(`/products/${id}/rating`)
}

// D-3 搜索历史
export function getSearchHistory(limit = 10) {
  return request.get('/products/search/history', { params: { limit } })
}

export function clearSearchHistory() {
  return request.delete('/products/search/history')
}

export default { getProducts, getProduct, searchProducts, getCategories, getProductReviews, getProductRating, getSearchHistory, clearSearchHistory }
