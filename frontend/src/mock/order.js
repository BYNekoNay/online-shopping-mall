import Mock from 'mockjs'

export default [
  {
    url: '/api/orders',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          orderNo: '20260706153000000001',
          shopId: 1,
          status: 1,
          statusText: '待发货',
          totalAmount: 299.0,
          freightAmount: 10.0,
          discountAmount: 10.0,
          payAmount: 299.0,
          addressSnapshot:
            '{"receiver":"张三","phone":"13800138000","province":"四川","city":"成都","district":"武侯区","detail":"磨子桥1号"}',
          payType: 2,
          payTime: '2026-07-07 10:05:00',
          remark: '',
          createTime: '2026-07-07 10:00:00',
          items: [
            {
              id: 1,
              productId: 1,
              skuId: 101,
              productName: 'Mock 商品 A',
              productImage: 'https://via.placeholder.com/200',
              price: 99.0,
              quantity: 2,
              isGift: 0
            }
          ]
        }
      ]
    })
  },
  {
    url: '/api/orders',
    method: 'post',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          orderId: 1,
          orderNo: '20260706153000000001',
          shopId: 1,
          payAmount: 299.0,
          freightAmount: 10.0,
          promotionDiscountAmount: 0,
          couponDiscountAmount: 0,
          pointsDeductAmount: 0
        }
      ]
    })
  },
  {
    url: '/api/orders/:id(\\d+)',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        id: 1,
        orderNo: '20260706153000000001',
        shopId: 1,
        status: 1,
        statusText: '待发货',
        totalAmount: 299.0,
        freightAmount: 10.0,
        discountAmount: 10.0,
        payAmount: 299.0,
        addressSnapshot:
          '{"receiver":"张三","phone":"13800138000","province":"四川","city":"成都","district":"武侯区","detail":"磨子桥1号"}',
        payType: 2,
        payTime: '2026-07-07 10:05:00',
        remark: '',
        createTime: '2026-07-07 10:00:00',
        items: [
          {
            id: 1,
            productId: 1,
            skuId: 101,
            productName: 'Mock 商品 A',
            productImage: 'https://via.placeholder.com/200',
            price: 99.0,
            quantity: 2,
            isGift: 0
          }
        ]
      }
    })
  },
  {
    url: '/api/orders/:id(\\d+)/cancel',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null })
  },
  {
    url: '/api/orders/:id(\\d+)/confirm',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null })
  },
  {
    url: '/api/orders/:id(\\d+)/pay',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { paySuccess: true, payNo: 'PAY20260707103000000001' } })
  },
  {
    url: '/api/orders/items/:id(\\d+)/review',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null })
  },
  {
    url: '/api/merchant/orders',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          orderNo: '20260706153000000001',
          status: 1,
          payAmount: 299.0,
          createTime: '2026-07-07 10:00:00',
          items: [{ id: 1, productName: 'Mock 商品 A', price: 99.0, quantity: 2, isGift: 0 }]
        }
      ]
    })
  },
  {
    url: '/api/user/points',
    method: 'get',
    response: () => ({ code: 0, message: 'success', data: { points: 320 } })
  },
  {
    url: '/api/user/points/records',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        total: 2,
        records: [
          { id: 1, changeAmount: 100, type: 1, relatedOrderId: 1, createTime: '2026-07-06 10:00:00' },
          { id: 2, changeAmount: -20, type: 2, relatedOrderId: 2, createTime: '2026-07-05 15:00:00' }
        ]
      }
    })
  },
  {
    url: '/api/refunds',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 1 } })
  },
  {
    url: '/api/refunds/:id(\\d+)/audit',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null })
  },
  {
    url: '/api/coupons/available',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          name: '新人专享券',
          type: 1,
          discount: 20,
          minAmount: 50,
          validFrom: '2026-07-01',
          validTo: '2026-07-31',
          status: 1
        }
      ]
    })
  },
  {
    url: '/api/promotions/active',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        {
          id: 1,
          name: '夏日特惠',
          type: 1,
          ruleJson: '{"discountPercent":0.8}',
          scope: 'SHOP',
          scopeId: 1,
          startTime: '2026-07-01 00:00:00',
          endTime: '2026-07-31 23:59:59'
        }
      ]
    })
  }
]
