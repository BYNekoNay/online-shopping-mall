import Mock from 'mockjs'

export default [
  {
    url: '/api/merchant/shop/apply-status',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { status: 1, rejectReason: '', shopId: 1 },
    }),
  },
  {
    url: '/api/merchant/shop/apply',
    method: 'post',
    response: () => ({
      code: 0,
      message: 'success',
      data: { status: 0, shopId: 1 },
    }),
  },
  {
    url: '/api/merchant/shop',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { id: 1, name: 'Mock 店铺', status: 1, address: '成都市武侯区' },
    }),
  },
  {
    url: '/api/merchant/shop',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
]
