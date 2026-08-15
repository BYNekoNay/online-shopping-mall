import request from '@/utils/request'

// C-1 积分商城
export function getPointsGoods(pageNum = 1, pageSize = 12) {
  return request.get('/points/goods', { params: { pageNum, pageSize } })
}

export function exchangeGoods(data) {
  return request.post('/points/exchange', data)
}

export function getExchangeLogs(limit = 10) {
  return request.get('/points/exchange-logs', { params: { limit } })
}

export default { getPointsGoods, exchangeGoods, getExchangeLogs }
