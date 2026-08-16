import Mock from 'mockjs'

export default [
  {
    url: '/api/recommend/guess-you-like',
    method: 'get',
    response: () => {
      const items = Mock.mock({
        'list|8': [
          {
            'productId|+1': 100,
            name: '猜你喜欢 @productId',
            mainImage: 'https://via.placeholder.com/200',
            price: '@float(10, 300, 2)',
            score: '@float(0, 1, 4)',
            algorithmType: 3,
          },
        ],
      })
      return { code: 0, message: 'success', data: items.list }
    },
  },
  {
    url: '/api/recommend/similar/:id(\\d+)',
    method: 'get',
    response: () => {
      const items = Mock.mock({
        'list|5': [
          {
            'productId|+1': 200,
            name: '相似商品 @productId',
            mainImage: 'https://via.placeholder.com/200',
            price: '@float(10, 300, 2)',
            score: '@float(0, 1, 4)',
            algorithmType: 2,
          },
        ],
      })
      return { code: 0, message: 'success', data: items.list }
    },
  },
  {
    url: '/api/behavior/recommend-exposure',
    method: 'post',
    response: () => {
      return { code: 0, message: 'success', data: null }
    },
  },
  {
    url: '/api/behavior/recommend-click',
    method: 'post',
    response: () => {
      return { code: 0, message: 'success', data: null }
    },
  },
]
