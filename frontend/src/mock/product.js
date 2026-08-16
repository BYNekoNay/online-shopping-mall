import Mock from 'mockjs'

export default [
  {
    url: '/api/products',
    method: 'get',
    response: () => {
      const list = Mock.mock({
        'list|10': [
          {
            'id|+1': 1,
            name: 'Mock 商品 @id',
            mainImage: 'https://via.placeholder.com/300',
            price: '@float(10, 500, 2)',
            originalPrice: '@float(500, 1000, 2)',
            stock: '@integer(10, 200)',
            sales: '@integer(0, 500)',
            categoryName: 'Mock 分类',
            shopName: 'Mock 店铺',
            status: 1,
            createTime: '@datetime',
          },
        ],
        total: 10,
        pages: 1,
        pageNum: 1,
        pageSize: 10,
      })
      return {
        code: 0,
        message: 'success',
        data: list,
      }
    },
  },
  {
    url: '/api/products/:id',
    method: 'get',
    response: (request) => {
      const id = request.params?.id || 1
      return {
        code: 0,
        message: 'success',
        data: {
          id: Number(id),
          name: 'Mock 商品 ' + id,
          mainImage: 'https://via.placeholder.com/600',
          images: ['https://via.placeholder.com/600'],
          detail: '这是 Mock 商品描述',
          price: 99.00,
          originalPrice: 129.00,
          stock: 100,
          sales: 50,
          categoryName: '电子产品',
          shopName: 'Mock 店铺',
          status: 1,
          skuList: [
            {
              id: 101,
              specJson: '{"颜色":"红色","尺码":"XL"}',
              price: 99.00,
              stock: 100,
              image: 'https://via.placeholder.com/200',
            },
            {
              id: 102,
              specJson: '{"颜色":"蓝色","尺码":"L"}',
              price: 89.00,
              stock: 50,
              image: 'https://via.placeholder.com/200',
            },
          ],
          activePromotion: null,
        },
      }
    },
  },
  {
    url: '/api/products/search',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        list: [
          {
            id: 1,
            name: '搜索结果商品',
            mainImage: 'https://via.placeholder.com/300',
            price: 99.00,
            originalPrice: 129.00,
            stock: 100,
            sales: 50,
            categoryName: '电子产品',
            status: 1,
          },
        ],
        total: 1,
        pages: 1,
        pageNum: 1,
        pageSize: 10,
      },
    }),
  },
  {
    url: '/api/products/categories/tree',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, parentId: 0, name: '电子产品', icon: '', sort: 1, status: 1, children: [
          { id: 11, parentId: 1, name: '手机', icon: '', sort: 1, status: 1 },
          { id: 12, parentId: 1, name: '电脑', icon: '', sort: 2, status: 1 },
        ]},
        { id: 2, parentId: 0, name: '服装鞋帽', icon: '', sort: 2, status: 1, children: [
          { id: 21, parentId: 2, name: '男装', icon: '', sort: 1, status: 1 },
        ]},
      ],
    }),
  },
  {
    url: '/api/recommend/guess-you-like',
    method: 'get',
    response: () => {
      const items = Mock.mock({
        'list|8': [
          {
            'productId|+1': 100,
            name: '猜你喜欢 @id',
            mainImage: 'https://via.placeholder.com/200',
            price: '@float(10, 300, 2)',
          },
        ],
      })
      return {
        code: 0,
        message: 'success',
        data: items.list,
      }
    },
  },
]
