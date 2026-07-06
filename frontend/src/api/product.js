import request from '@/utils/request'

export function getProducts(params) {
  return request.get('/products', { params })
}

export function getProduct(id) {
  return request.get(`/products/${id}`)
}

export function searchProducts(params) {
  return request.get('/products/search', { params })
}

export function getCategories() {
  return request.get('/products/categories/tree')
}

export function addToCart(data) {
  return request.post('/cart', data)
}
