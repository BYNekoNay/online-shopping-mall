import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function getAddresses() {
  return request.get('/user/addresses')
}

export function addAddress(data) {
  return request.post('/user/addresses', data)
}

export function updateAddress(id, data) {
  return request.put(`/user/addresses/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/user/addresses/${id}`)
}

export function getSearchHistory() {
  return request.get('/user/search-history')
}

export function clearSearchHistory() {
  return request.delete('/user/search-history')
}

export function getFavorites() {
  return request.get('/behavior/favorites')
}

export function favoriteProduct(productId) {
  return request.post(`/behavior/favorites/${productId}`)
}

export function unfavoriteProduct(productId) {
  return request.delete(`/behavior/favorites/${productId}`)
}

export default { login, register, getProfile, updateProfile, getAddresses, addAddress, updateAddress, deleteAddress, getSearchHistory, clearSearchHistory, getFavorites, favoriteProduct, unfavoriteProduct }
