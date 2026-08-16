import Mock from 'mockjs'

export default [
  {
    url: '/api/merchant/freight-templates',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          name: '默认模板',
          type: 1,
          freeThreshold: 99,
          firstUnitPrice: 10,
          firstUnit: 1,
          additionalUnitPrice: 5,
          additionalUnit: 1,
          regions: ['四川', '广东'],
          status: 1
        }
      ]
    })
  },
  {
    url: '/api/merchant/freight-templates',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 2 } })
  },
  {
    url: '/api/merchant/freight-templates/calculate',
    method: 'get',
    response: () => ({ code: 0, message: 'success', data: 10.0 })
  }
]
