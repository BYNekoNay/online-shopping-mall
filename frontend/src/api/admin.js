import request from '@/utils/request'

export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, data) {
  return request.put(`/admin/users/${id}/status`, data)
}

export function getProducts(params) {
  return request.get('/admin/products', { params })
}

export function auditProduct(id, data) {
  return request.put(`/admin/products/${id}/audit`, data)
}

export function offlineProduct(id, data) {
  return request.put(`/admin/products/${id}/offline`, data)
}

export function getCategories() {
  return request.get('/admin/categories')
}

export function getShops(params) {
  return request.get('/admin/shops', { params })
}

export function auditShop(id, data) {
  return request.put(`/admin/shops/${id}/audit`, data)
}

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getStatisticsDetail(params) {
  return request.get('/admin/dashboard/statistics/detail', { params })
}

export function getLogs(params) {
  return request.get('/admin/logs', { params })
}

export function getDicts() {
  return request.get('/admin/dicts')
}
