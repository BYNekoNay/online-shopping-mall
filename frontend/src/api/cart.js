import request from '@/utils/request'

export function getCart() {
  return request.get('/cart')
}

export function addToCart(data) {
  return request.post('/cart', data)
}

export function updateCartItem(id, data) {
  return request.put(`/cart/${id}`, data)
}

export function deleteCartItem(id) {
  return request.delete(`/cart/${id}`)
}

export default { getCart, addToCart, updateCartItem, deleteCartItem }
