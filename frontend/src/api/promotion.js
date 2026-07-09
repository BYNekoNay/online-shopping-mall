import request from '@/utils/request'

export function getActivePromotions() {
  return request.get('/promotions/active')
}
