import { Mock } from 'mockjs'

export default [
  {
    url: '/api/auth/login',
    method: 'post',
    response: () => {
      const id = Mock.mock('@id')
      return {
        code: 0,
        message: 'success',
        data: {
          token: 'mock-token-' + id,
          userId: 1,
          role: 1,
          nickname: 'MockUser',
        },
      }
    },
  },
  {
    url: '/api/auth/register',
    method: 'post',
    response: () => ({
      code: 0,
      message: 'success',
      data: null,
    }),
  },
  {
    url: '/api/user/profile',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        id: 1,
        username: 'mockuser',
        nickname: 'MockUser',
        email: 'mock@example.com',
        phone: '13800138000',
        avatar: '',
        role: 1,
        createdAt: '2026-01-01T00:00:00Z',
        createTime: '2026-01-01 00:00:00',
      },
    }),
  },
  {
    url: '/api/user/addresses',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          userId: 1,
          receiver: 'MockUser',
          phone: '13800138000',
          province: '四川',
          city: '成都',
          district: '武侯区',
          detail: '磨子桥 1 号',
          isDefault: 1,
        },
      ],
    }),
  },
  {
    url: '/api/cart',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          productId: 1,
          skuId: 101,
          quantity: 2,
          selected: 1,
          productName: 'Mock 商品 A',
          skuSpec: '红色 / XL',
          price: 99.00,
          image: 'https://via.placeholder.com/120',
        },
      ],
    }),
  },
]
