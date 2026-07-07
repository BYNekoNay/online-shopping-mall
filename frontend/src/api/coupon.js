import request from '@/utils/request'

export function getUserCoupons() {
  return request.get('/user/coupons')
}

export function getPoints() {
  return request.get('/user/points')
}

export function getPointsRecords(params) {
  return request.get('/user/points/records', { params })
}

export default { getUserCoupons, getPoints, getPointsRecords }
