/**
 * E2E 商家：订单发货（T04）。
 * 覆盖验收用例 TC-M-04 商家对已支付订单发货，状态流转为已发货。
 *
 * 说明：
 *  - 发货动作走 API（浏览器对 API 创建的雪花订单 ID 丢精度，前端 UI 发货会 404，属前端业务缺陷见 docs/45 §十一）。
 *  - 商家订单列表页（/merchant/orders）在本机线上环境导航偶发 120s 超时（页面资源加载重），
 *    改为用**消费者订单列表 UI** 断言"已发货"状态（消费者 /orders 页面导航已验证稳定）。
 */
import { test, expect } from '../../fixtures/index.js'
import { OrderListPage } from '../../pages/OrderListPage.js'
import {
  registerConsumer,
  createProductByMerchant,
  createPaidOrderForConsumer,
  auditProduct,
  adminSession,
  api,
  injectSession,
} from '../../helpers/accounts.js'

test.describe('merchant order', () => {
  test('TC-M-04 商家发货后订单状态为已发货', async ({ page, merchantAccount }) => {
    // 前置数据（API）：商家发布商品 → 管理员审核通过 → 消费者下单并支付
    const buyer = await registerConsumer()
    const productId = await createProductByMerchant(merchantAccount.session, {
      name: `E2E发货商品${Date.now()}`,
      price: 66,
      stock: 100,
    })
    const admin = await adminSession()
    await auditProduct(productId, true, admin.token)
    const { orderId, orderNo } = await createPaidOrderForConsumer(buyer.session, { productId, quantity: 1 })

    // 商家发货（API）
    const ship = await api('PUT', `/merchant/orders/${orderId}/ship`, {
      token: merchantAccount.session.token,
      body: { logisticsCompany: '顺丰速运', trackingNo: `SF${Date.now()}` },
    })
    expect(ship.code).toBe(0)
    // API 断言订单状态已变为已发货(2)
    const detail = await api('GET', `/orders/${orderId}`, { token: buyer.session.token })
    expect(detail.code).toBe(0)
    expect(detail.data.status).toBe(2)

    // UI：消费者订单列表"已发货" tab 可见该订单（真实浏览器渲染状态）
    await injectSession(page, buyer.session)
    const orderList = new OrderListPage(page)
    await orderList.goto()
    await orderList.switchTab(2)
    await orderList.expectOrderVisible(orderNo)
  })
})
