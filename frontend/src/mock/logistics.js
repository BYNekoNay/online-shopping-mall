import { Mock } from 'mockjs'

export default [
  {
    url: '/api/logistics/\\d+',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        orderId: 1,
        traces: [
          { time: '2026-07-07 10:00', status: '已揽收', location: '成都转运中心' },
          { time: '2026-07-07 08:00', status: '已下单', location: '成都' },
        ],
      },
    }),
  },
]
