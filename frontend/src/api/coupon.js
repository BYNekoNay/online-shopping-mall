import request from '@/utils/request'

/** 获取当前用户可领取/可用的优惠券列表 */
export function getAvailableCoupons() {
  return request.get('/coupons/available')
}

/** 获取当前用户已领取的优惠券 */
export function getUserCoupons(params) {
  return request.get('/user/coupons', { params })
}

export function getPoints() {
  return request.get('/user/points')
}

export function getPointsRecords(params) {
  return request.get('/user/points/records', { params })
}

export default { getAvailableCoupons, getUserCoupons, getPoints, getPointsRecords }
