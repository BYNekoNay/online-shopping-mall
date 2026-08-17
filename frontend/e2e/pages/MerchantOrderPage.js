/** 商家订单管理页（/merchant/orders）。 */
export class MerchantOrderPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/merchant/orders')
    await this.page.waitForTimeout(1500)
  }

  /** 对指定订单号发货（打开发货对话框，选择物流公司并确认）。 */
  async shipOrder(orderNo, trackingNo = `E2E${Date.now()}`) {
    const row = this.page.locator('.el-table__row', { hasText: orderNo }).first()
    await row.getByRole('button', { name: /发货/ }).first().click()
    const dialog = this.page.locator('.el-dialog', { hasText: '发货' })
    await dialog.waitFor({ state: 'visible', timeout: 10_000 })
    // 选择物流公司（下拉第一项）
    await dialog.locator('.el-select').first().click()
    await this.page.locator('.el-select-dropdown__item').first().click()
    // 填写物流单号
    await dialog.locator('input[placeholder*="物流单号"]').fill(trackingNo)
    await dialog.getByRole('button', { name: /确认发货/ }).first().click()
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  async expectOrderVisible(orderNo) {
    await this.page.getByText(orderNo).first().waitFor({ state: 'visible', timeout: 15_000 })
  }
}
