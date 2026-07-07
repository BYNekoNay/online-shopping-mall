import { Mock } from 'mockjs'

export default [
  {
    url: '/api/admin/products',
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
            status: '@integer(0, 3)',
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
    url: '/api/admin/categories',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, parentId: 0, name: '电子产品', icon: '', sort: 1, status: 1 },
        { id: 2, parentId: 0, name: '服装鞋帽', icon: '', sort: 2, status: 1 },
        { id: 11, parentId: 1, name: '手机', icon: '', sort: 1, status: 1 },
        { id: 12, parentId: 1, name: '电脑', icon: '', sort: 2, status: 1 },
      ],
    }),
  },
  {
    url: '/api/admin/categories',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 3 } }),
  },
  {
    url: '/api/admin/categories/3',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/user/search-history',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { keyword: '手机', createTime: '2026-07-06 10:00:00' },
        { keyword: '耳机', createTime: '2026-07-05 15:30:00' },
      ],
    }),
  },
  {
    url: '/api/user/search-history',
    method: 'delete',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/users',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        records: [
          { id: 1, username: 'admin', nickname: '管理员', role: 3, status: 1, createTime: '2026-07-01 00:00:00' },
          { id: 2, username: 'merchant01', nickname: '商家A', role: 2, status: 1, createTime: '2026-07-02 00:00:00' },
          { id: 3, username: 'user01', nickname: '用户A', role: 1, status: 1, createTime: '2026-07-03 00:00:00' },
        ],
        total: 3,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
      },
    }),
  },
  {
    url: '/api/admin/users/\\d+/status',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/shops',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        records: [
          { id: 1, name: 'Mock 店铺', status: 1, contactName: '张三', contactPhone: '13800138000', level: 3, createTime: '2026-07-01 00:00:00' },
          { id: 2, name: '待审核店铺', status: 0, contactName: '李四', contactPhone: '13900139000', level: 1, createTime: '2026-07-05 00:00:00' },
        ],
        total: 2,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
      },
    }),
  },
  {
    url: '/api/admin/shops/\\d+/audit',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/shops/\\d+/level',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/system/config',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        'mall.name': '智能推荐网络商城',
        'order.timeout': '30',
        'recommend.refresh.hours': '24',
        'logistics.timeout': '3',
      },
    }),
  },
  {
    url: '/api/admin/system/config/.*',
    method: 'get',
    response: (uri) => {
      const key = uri.replace('/api/admin/system/config/', '')
      const map = { 'mall.name': '智能推荐网络商城', 'order.timeout': '30', 'recommend.refresh.hours': '24', 'logistics.timeout': '3' }
      return { code: 0, message: 'success', data: map[key] || '' }
    },
  },
  {
    url: '/api/admin/system/config/.*',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/system/dicts',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { id: 3 } }),
  },
  {
    url: '/api/admin/dicts/\\d+',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/dicts/\\d+',
    method: 'delete',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/dashboard',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { gmv: 128000, orderCount: 320, newUserCount: 56, conversionRate: 0.12, recommendCtr: 0.08 },
    }),
  },
  {
    url: '/api/admin/dashboard/statistics/detail',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        pv: 5000,
        uv: 1200,
        bounceRate: 0.35,
        avgStayDuration: 180,
        funnel: { view: 5000, cart: 800, order: 320, pay: 280 },
      },
    }),
  },
  {
    url: '/api/admin/system/logs',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        list: [
          { id: 1, operatorId: 1, operatorRole: 3, operation: '审核商品', target: '商品#1', createTime: '2026-07-06 10:00:00' },
        ],
        total: 1,
      },
    }),
  },
  {
    url: '/api/admin/system/dicts',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { dictType: 'LOGISTICS_COMPANY', dictKey: 'SF', dictValue: '顺丰速运', sort: 1, status: 1 },
        { dictType: 'ORDER_STATUS', dictKey: '0', dictValue: '待付款', sort: 1, status: 1 },
      ],
    }),
  },
  {
    url: '/api/admin/system/config',
    method: 'get',
    response: () => ({ code: 0, message: 'success', data: { key: 'mall.name', value: '智能商城' } }),
  },
  {
    url: '/api/admin/coupons',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, name: '新人满减券', type: 1, shopId: null, discountRule: '{"threshold":50,"discount":10}', validFrom: '2026-07-01 00:00:00', validTo: '2026-12-31 23:59:59', stock: 1000, receivedCount: 320, createTime: '2026-07-01 10:00:00', updateTime: '2026-07-06 10:00:00', isDeleted: 0 },
        { id: 2, name: '数码品类券', type: 3, shopId: 1, discountRule: '{"threshold":100,"discount":20,"categoryId":2}', validFrom: '2026-07-01 00:00:00', validTo: '2026-12-31 23:59:59', stock: 500, receivedCount: 180, createTime: '2026-07-01 10:00:00', updateTime: '2026-07-06 10:00:00', isDeleted: 0 },
      ],
    }),
  },
  {
    url: '/api/admin/coupons',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/coupons/\\d+',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/coupons/\\d+/offline',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/coupons/\\d+',
    method: 'delete',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/promotions',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 1, name: '夏日大促', type: 1, ruleJson: '{"discountPercent":0.8}', scope: 'PLATFORM', scopeId: null, startTime: '2026-07-01 00:00:00', endTime: '2026-07-31 23:59:59', status: 1, createTime: '2026-07-01 10:00:00', updateTime: '2026-07-06 10:00:00', isDeleted: 0 },
        { id: 2, name: '满100减20', type: 2, ruleJson: '{"threshold":100,"reduce":20}', scope: 'SHOP', scopeId: 1, startTime: '2026-07-01 00:00:00', endTime: '2026-07-31 23:59:59', status: 1, createTime: '2026-07-01 10:00:00', updateTime: '2026-07-06 10:00:00', isDeleted: 0 },
      ],
    }),
  },
  {
    url: '/api/admin/promotions',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/promotions/\\d+',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/promotions/\\d+/offline',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/admin/promotions/\\d+',
    method: 'delete',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/coupons/available',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { id: 3, name: '新用户专享', type: 1, shopId: null, discountRule: '{"threshold":50,"discount":10}', validFrom: '2026-07-01 00:00:00', validTo: '2026-12-31 23:59:59', stock: 1000, receivedCount: 0, createTime: '2026-07-01 10:00:00' },
      ],
    }),
  },
  {
    url: '/api/coupons/\\d+/receive',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: { userCouponId: 1 } }),
  },
  {
    url: '/api/user/points',
    method: 'get',
    response: () => ({ code: 0, message: 'success', data: { points: 1280 } }),
  },
  {
    url: '/api/user/points/records',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        records: [
          { id: 1, userId: 2, changeAmount: 100, type: 1, relatedOrderId: 1001, createTime: '2026-07-05 10:00:00' },
          { id: 2, userId: 2, changeAmount: -50, type: 2, relatedOrderId: 1002, createTime: '2026-07-06 14:00:00' },
        ],
        total: 2,
        pageNum: 1,
        pageSize: 20,
      },
    }),
  },
  {
    url: '/api/logistics/\\d+/track',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { status: '运输中', tracks: [{ time: '2026-07-06 10:00:00', desc: '快件已从深圳发出' }, { time: '2026-07-07 08:00:00', desc: '快件到达杭州转运中心' }] },
    }),
  },
  {
    url: '/api/admin/dashboard',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: { gmv: 128000, orderCount: 320, newUserCount: 56, conversionRate: 0.12, recommendCtr: '8.50%' },
    }),
  },
  {
    url: '/api/admin/dashboard/statistics/detail',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        pv: 5000,
        uv: 1200,
        bounceRate: 0.35,
        avgStayDuration: 180,
        funnel: { view: 5000, cart: 800, order: 320, pay: 280 },
      },
    }),
  },
  {
    url: '/api/merchant/statistics/sales',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        totalAmount: 25600.50,
        totalOrders: 48,
        trend: [
          { date: '2026-06-18', amount: 1200.00, orders: 3 },
          { date: '2026-06-19', amount: 980.00, orders: 2 },
          { date: '2026-06-20', amount: 1500.00, orders: 4 },
          { date: '2026-06-21', amount: 2100.00, orders: 5 },
          { date: '2026-06-22', amount: 1800.00, orders: 4 },
        ],
      },
    }),
  },
  {
    url: '/api/merchant/statistics/top-products',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: [
        { productId: 1, name: '智能手机 X12', sales: 128, amount: 128000.00 },
        { productId: 3, name: '蓝牙耳机 Pro', sales: 96, amount: 19200.00 },
        { productId: 5, name: '机械键盘 K1', sales: 64, amount: 12800.00 },
        { productId: 7, name: '运动跑鞋 V2', sales: 52, amount: 10400.00 },
        { productId: 9, name: '保温杯 500ml', sales: 48, amount: 2400.00 },
      ],
    }),
  },
]
