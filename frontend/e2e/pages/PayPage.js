/** 支付页（/order/pay/:id）。 */
export class PayPage {
  constructor(page) {
    this.page = page
  }

  async goto(orderId) {
    await this.page.goto(`/order/pay/${orderId}`)
  }

  /** 选择模拟支付宝（payType=2）并确认支付。 */
  async payWithMockAlipay() {
    // 点击"模拟支付宝"单选项
    await this.page.getByText('模拟支付宝').first().click()
    await this.page.waitForTimeout(300)
    await this.page.getByRole('button', { name: /确认支付/ }).first().click()
  }

  /** 断言支付成功提示。 */
  async expectPaySuccess() {
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 15_000 })
  }
}
