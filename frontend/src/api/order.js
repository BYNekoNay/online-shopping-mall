import request from '@/utils/request'

export function getOrders(params) {
  return request.get('/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

export function createOrder(data) {
  return request.post('/orders', data)
}

export function payOrder(id, data) {
  return request.post(`/orders/${id}/pay`, data)
}

export function cancelOrder(id) {
  return request.put(`/orders/${id}/cancel`)
}

export function confirmOrder(id) {
  return request.put(`/orders/${id}/confirm`)
}

export function reviewOrder(orderItemId, data) {
  return request.post(`/orders/${orderItemId}/review`, data)
}

export function refundOrder(orderId, data) {
  return request.post(`/orders/${orderId}/refund`, data)
}

export function getPoints() {
  return request.get('/user/points')
}

export function getPointsRecords(params) {
  return request.get('/user/points/records', { params })
}

export default { getOrders, getOrderDetail, createOrder, payOrder, cancelOrder, confirmOrder, reviewOrder, refundOrder, getPoints, getPointsRecords }
