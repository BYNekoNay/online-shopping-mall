import { Mock } from 'mockjs'

export default [
  {
    url: '/api/user/coupons',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, name: '满100减10', type: 1, discount: 10, minAmount: 100, validFrom: '2026-07-01', validTo: '2026-07-31', status: 1 },
      ],
    }),
  },
  {
    url: '/api/coupons/\\d+/receive',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/coupons',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, name: '满100减10', type: 1, discount: 10, minAmount: 100, total: 1000, claimed: 340, validFrom: '2026-07-01', validTo: '2026-07-31', status: 1 },
      ],
    }),
  },
  {
    url: '/api/admin/coupons',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 2 } }),
  },
  {
    url: '/api/admin/promotions',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 1 } }),
  },
]
