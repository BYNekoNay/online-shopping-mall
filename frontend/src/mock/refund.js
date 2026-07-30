import { Mock } from 'mockjs'

// M-27 修复：Mock 地址与真实后端端点对齐——
// 商家退款列表/审核：MerchantRefundController = GET/PUT /api/merchant/refunds[/{id}/audit]
// 消费者退款申请：OrderController = POST /api/orders/{id}/refund
// 原实现的 /api/refunds 在后端不存在，Mock 模式下退款页面恒 404；
// 响应结构同步对齐 Result<PageResult<RefundVO>>（records/total/pageNum/pageSize/pages）
export default [
  {
    url: '/api/merchant/refunds',
    method: 'get',
    response: () => ({
      code: 0,
      message: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        pages: 1,
        records: [
          {
            id: 1,
            orderId: 1,
            orderItemId: 1,
            type: 1,
            reason: '不想要了',
            amount: 99.0,
            status: 1,
            handleRemark: '',
            createTime: '2026-07-07 12:00:00',
          },
        ],
      },
    }),
  },
  {
    url: '/api/merchant/refunds/\\d+/audit',
    method: 'put',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
  {
    url: '/api/orders/\\d+/refund',
    method: 'post',
    response: () => ({ code: 0, message: 'success', data: null }),
  },
]
