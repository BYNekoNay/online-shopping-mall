import { Mock } from 'mockjs'

export default [
  {
    url: '/api/refunds',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, orderId: 1, reason: '不想要了', status: 1, createdAt: '2026-07-07T12:00:00Z' },
      ],
    }),
  },
  {
    url: '/api/refunds',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 1 } }),
  },
  {
    url: '/api/refunds/\\d+/audit',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
]
