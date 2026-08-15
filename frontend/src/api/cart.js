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

// D-4 全选/取消全选
export function selectAllCart(selected) {
  return request.put('/cart/select-all', { selected })
}

export default { getCart, addToCart, updateCartItem, deleteCartItem, selectAllCart }
