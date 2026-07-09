import request from '@/utils/request'

export function queryLogistics(orderId) {
  return request.get(`/logistics/${orderId}/track`)
}
