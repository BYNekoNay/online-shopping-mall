/**
 * E2E 消费者：下单 / 支付 / 订单状态流转（T03）。
 * 覆盖验收用例 TC-C-13 下单、TC-C-14 支付、TC-C-15 订单状态、TC-C-16 取消、TC-C-17 退款。
 *
 * 说明：订单创建/支付/取消/退款动作走 API（浏览器对 API 创建的雪花订单/地址 ID 丢精度，
 * 前端 UI 提交会 404，属前端业务缺陷，见 docs/45 §四；QA 认可的降级模式）；
 * 保留真实浏览器订单列表渲染与状态标签断言。
 * 依赖事实：支付为模拟支付（payType=2 模拟支付宝），无需真实余额。
 */
import { test, expect } from '../../fixtures/index.js'
import { OrderListPage } from '../../pages/OrderListPage.js'
import { api, createOrderForConsumer, createPaidOrderForConsumer } from '../../helpers/accounts.js'

test.describe('consumer checkout', () => {
  test('TC-C-13/14/15 下单支付后订单进入待发货（API 下单支付 + UI 状态断言）', async ({ consumerPage, consumerAccount }) => {
    const productId = 108 // Slim Fit Jeans（无 SKUs，金额稳定）
    const { orderNo } = await createPaidOrderForConsumer(consumerAccount.session, { productId, quantity: 1 })
    expect(orderNo).toBeTruthy()

    // UI：订单列表"待发货" tab 出现该订单
    const orderList = new OrderListPage(consumerPage)
    await orderList.goto()
    await orderList.switchTab(1)
    await orderList.expectOrderVisible(orderNo)
  })

  test('TC-C-16 待付款订单可取消，状态流转为已取消（API 下单/取消 + UI 状态断言）', async ({ consumerPage, consumerAccount }) => {
    const { orderId, orderNo } = await createOrderForConsumer(consumerAccount.session, { productId: 109, quantity: 1 })
    // UI：待付款 tab 可见该订单
    const orderList = new OrderListPage(consumerPage)
    await orderList.goto()
    await orderList.switchTab(0)
    await orderList.expectOrderVisible(orderNo)
    // 取消订单（API）
    const cancel = await api('PUT', `/orders/${orderId}/cancel`, { token: consumerAccount.session.token })
    expect(cancel.code).toBe(0)
    // UI：已取消 tab 可见
    await orderList.switchTab(5)
    await orderList.expectOrderVisible(orderNo)
  })

  test('TC-C-17 已支付订单申请退款成功，订单保持待发货待商家审核（API 下单支付/退款 + UI 状态断言）', async ({ consumerPage, consumerAccount }) => {
    const { orderId, orderNo } = await createPaidOrderForConsumer(consumerAccount.session, { productId: 110, quantity: 1 })
    // UI：待发货 tab 可见该订单
    const orderList = new OrderListPage(consumerPage)
    await orderList.goto()
    await orderList.switchTab(1)
    await orderList.expectOrderVisible(orderNo)
    // 申请退款（API，amount ≤ 订单实付；后端仅创建退款记录，订单状态保持待发货(1)待商家审核）
    const detail = await api('GET', `/orders/${orderId}`, { token: consumerAccount.session.token })
    expect(detail.code).toBe(0)
    const refund = await api('POST', `/orders/${orderId}/refund`, {
      token: consumerAccount.session.token,
      body: { type: 1, orderItemId: null, reason: 'E2E 自动退款', amount: detail.data.payAmount },
    })
    expect(refund.code).toBe(0)
    // UI：退款申请后订单仍显示在待发货（待商家审核退款）
    await orderList.switchTab(1)
    await orderList.expectOrderVisible(orderNo)
  })
})
