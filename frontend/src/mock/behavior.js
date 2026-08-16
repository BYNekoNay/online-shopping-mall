import Mock from 'mockjs'

export default [
  {
    url: '/api/behavior/page-view',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null })
  },
  {
    url: '/api/behavior/record',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null })
  }
]
