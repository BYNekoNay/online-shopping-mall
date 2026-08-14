import request from '@/utils/request'

export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, data) {
  return request.put(`/admin/users/${id}/status`, data)
}

export function updateUserRole(id, data) {
  return request.put(`/admin/users/${id}/role`, data)
}

export function getProducts(params) {
  return request.get('/admin/products', { params })
}

export function auditProduct(id, data) {
  return request.put(`/admin/products/${id}/audit`, data)
}

export function offlineProduct(id) {
  return request.put(`/admin/products/${id}/offline`)
}

export function getCategories() {
  return request.get('/admin/categories')
}

export function createCategory(data) {
  return request.post('/admin/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}

export function getShops(params) {
  return request.get('/admin/shops', { params })
}

export function auditShop(id, data) {
  return request.put(`/admin/shops/${id}/audit`, data)
}

export function updateShopLevel(id, data) {
  return request.put(`/admin/shops/${id}/level`, data)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getStatisticsDetail(params) {
  return request.get('/admin/dashboard/statistics/detail', { params })
}

export function getLogs(params) {
  return request.get('/admin/system/logs', { params })
}

export function getDicts() {
  return request.get('/admin/system/dicts')
}

export function createDict(data) {
  return request.post('/admin/system/dicts', data)
}

export function updateDict(id, data) {
  return request.put(`/admin/system/dicts/${id}`, data)
}

export function deleteDict(id) {
  return request.delete(`/admin/system/dicts/${id}`)
}

export function getConfig(key) {
  return request.get(`/admin/system/config/${key}`)
}

export function listConfig() {
  return request.get('/admin/system/config')
}

export function updateConfig(key, data) {
  return request.put(`/admin/system/config/${key}`, data)
}

export function getCoupons() {
  return request.get('/admin/coupons')
}

export function createCoupon(data) {
  return request.post('/admin/coupons', data)
}

export function updateCoupon(id, data) {
  return request.put(`/admin/coupons/${id}`, data)
}

export function offlineCoupon(id) {
  return request.put(`/admin/coupons/${id}/offline`)
}

export function deleteCoupon(id) {
  return request.delete(`/admin/coupons/${id}`)
}

export function getPromotions() {
  return request.get('/admin/promotions')
}

export function createPromotion(data) {
  return request.post('/admin/promotions', data)
}

export function updatePromotion(id, data) {
  return request.put(`/admin/promotions/${id}`, data)
}

export function offlinePromotion(id) {
  return request.put(`/admin/promotions/${id}/offline`)
}

export function deletePromotion(id) {
  return request.delete(`/admin/promotions/${id}`)
}

export function recommendRefresh() {
  return request.post('/admin/recommend/refresh')
}

export default { getUsers, updateUserStatus, getProducts, auditProduct, offlineProduct, getCategories, createCategory, updateCategory, getShops, auditShop, updateShopLevel, getDashboard, getStatisticsDetail, getLogs, getDicts, createDict, updateDict, deleteDict, getConfig, listConfig, updateConfig, getCoupons, createCoupon, updateCoupon, offlineCoupon, deleteCoupon, getPromotions, createPromotion, updatePromotion, offlinePromotion, deletePromotion, recommendRefresh }
