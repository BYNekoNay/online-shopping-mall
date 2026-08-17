/** 订单列表页（/orders）。 */
export class OrderListPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/orders')
    await this.page.waitForTimeout(1500)
  }

  /** 切换订单状态 tab（0 待付款 / 1 待发货 / 2 已发货 / 5 已取消 / 6 退款中 / 7 已退款）。 */
  async switchTab(status) {
    const label = { 0: '待付款', 1: '待发货', 2: '已发货', 5: '已取消', 6: '退款中', 7: '已退款' }[String(status)]
    await this.page.locator('.el-tabs__item', { hasText: label }).first().click()
    await this.page.waitForTimeout(1000)
  }

  /** 在列表中查找订单号。 */
  async findOrder(orderNo) {
    await this.page.getByText(orderNo).first().waitFor({ state: 'visible', timeout: 15_000 })
  }

  /** 断言订单号在列表中可见（findOrder 别名，供 spec 语义化调用）。 */
  async expectOrderVisible(orderNo) {
    await this.findOrder(orderNo)
  }

  /** 取消订单（需待付款状态）。 */
  async cancelOrder(orderNo) {
    const row = this.page.locator('.order-card, .order-item, .el-card, [class*="order"]', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /取消订单/ }).first().click()
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  /** 去支付（待付款状态）。 */
  async goPay(orderNo) {
    const row = this.page.locator('.order-card, .order-item, .el-card, [class*="order"]', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /去支付/ }).first().click()
  }

  /** 确认收货（已发货状态）。 */
  async confirmReceive(orderNo) {
    const row = this.page.locator('.order-card, .order-item, .el-card, [class*="order"]', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /确认收货/ }).first().click()
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  /** 申请退款（待发货/已发货等状态）。退款对话框需选商品、填金额、填原因，按钮为"提交申请"。 */
  async applyRefund(orderNo, { amount } = {}) {
    const row = this.page.locator('.order-card, .order-item, .el-card, [class*="order"]', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /申请退款/ }).first().click()
    await this.fillRefundDialog(amount)
  }

  /** 对列表第一条可见订单申请退款（新消费者通常只有一条订单）。 */
  async applyRefundFirstRow({ amount } = {}) {
    await this.page.getByRole('button', { name: /申请退款/ }).first().click()
    await this.fillRefundDialog(amount)
  }

  /** 填写并提交退款对话框（选商品 + 金额 + 原因 + 提交申请）。 */
  async fillRefundDialog(amount) {
    const dialog = this.page.locator('.el-dialog', { hasText: '申请退款' })
    await dialog.waitFor({ state: 'visible', timeout: 10_000 })
    // 选择退款商品（第一项）
    await dialog.locator('.el-select').first().click()
    const option = this.page.locator('.el-select-dropdown__item').first()
    await option.waitFor({ state: 'visible', timeout: 10_000 })
    await option.click()
    await this.page.waitForTimeout(300)
    // 退款金额（默认 1，不超过订单行金额）
    const amountInput = dialog.locator('.el-input-number input').first()
    await amountInput.fill(String(amount ?? 1))
    await amountInput.press('Enter')
    // 退款原因（必填）
    await dialog.locator('textarea').first().fill('E2E 自动化测试退款')
    // 提交申请
    await dialog.getByRole('button', { name: /提交申请/ }).first().click()
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  /** 查看订单详情。 */
  async viewDetail(orderNo) {
    const row = this.page.locator('.order-card, .order-item, .el-card, [class*="order"]', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /查看详情/ }).first().click()
  }

  /** 从 URL 提取订单号。 */
  orderNoFromUrl() {
    const match = this.page.url().match(/\/orders\/(\d+)/)
    return match ? match[1] : null
  }
}
