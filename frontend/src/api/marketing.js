import request from '@/utils/request'

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

export default { getCoupons, createCoupon, updateCoupon, offlineCoupon, deleteCoupon, getPromotions, createPromotion, updatePromotion, offlinePromotion, deletePromotion }
