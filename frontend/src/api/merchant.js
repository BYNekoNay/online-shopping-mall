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

// C-4 物流公司下拉（仅启用）
export function getLogisticsCompanies() {
  return request.get('/merchant/logistics-companies')
}

export function getRefunds() {
  return request.get('/merchant/refunds')
}

export function auditRefund(id, data) {
  return request.put(`/merchant/refunds/${id}/audit`, data)
}

export function getSalesStatistics(params = {}) {
  if (!params.startDate) {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 30)
    params.startDate = formatDate(start)
    params.endDate = formatDate(end)
  }
  return request.get('/merchant/statistics/sales', { params })
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function getTopProducts() {
  return request.get('/merchant/statistics/top-products')
}

/** 获取商品分类树（用于商品编辑表单的分类选择器） */
export function getCategoriesTree() {
  return request.get('/products/categories/tree')
}

/** 获取商品详情（商家端，校验店铺归属） */
export function getProductDetail(id) {
  return request.get(`/merchant/products/${id}`)
}

/** 获取商家订单详情 */
export function getMerchantOrderDetail(id) {
  return request.get(`/merchant/orders/${id}`)
}

/** 运费试算 */
export function calculateFreight(params) {
  return request.get('/merchant/freight-templates/calculate', { params })
}

export default { applyShop, getApplyStatus, getShopInfo, updateShopInfo, getFreightTemplates, saveFreightTemplate, getMerchantProducts, createProduct, updateProduct, batchOperateProducts, getMerchantOrders, shipOrder, getLogisticsCompanies, getRefunds, auditRefund, getSalesStatistics, getTopProducts, getCategoriesTree, getProductDetail, getMerchantOrderDetail, calculateFreight }
