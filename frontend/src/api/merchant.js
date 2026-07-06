import request from '@/utils/request'

export function applyShop(data) {
  return request.post('/merchant/shop/apply', data)
}

export function getApplyStatus() {
  return request.get('/merchant/shop/apply-status')
}

export function getShopInfo() {
  return request.get('/merchant/shop')
}

export function updateShopInfo(data) {
  return request.put('/merchant/shop', data)
}

export function getFreightTemplates() {
  return request.get('/merchant/freight-templates')
}

export function saveFreightTemplate(data) {
  return request.post('/merchant/freight-templates', data)
}

export function getMerchantProducts(params) {
  return request.get('/merchant/products', { params })
}

export function createProduct(data) {
  return request.post('/merchant/products', data)
}

export function updateProduct(id, data) {
  return request.put(`/merchant/products/${id}`, data)
}

export function batchOperateProducts(data) {
  return request.put('/merchant/products/batch', data)
}

export function getMerchantOrders(params) {
  return request.get('/merchant/orders', { params })
}

export function shipOrder(id, data) {
  return request.put(`/merchant/orders/${id}/ship`, data)
}

export function getRefunds() {
  return request.get('/merchant/refunds')
}

export function auditRefund(id, data) {
  return request.put(`/merchant/refunds/${id}/audit`, data)
}

export function getSalesStatistics(params) {
  return request.get('/merchant/statistics/sales', { params })
}

export function getTopProducts() {
  return request.get('/merchant/statistics/top-products')
}
