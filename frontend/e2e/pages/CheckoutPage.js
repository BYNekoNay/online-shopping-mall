/** 订单确认页（/order/confirm）。 */
export class CheckoutPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/order/confirm')
  }

  /** 等待收货地址列表加载（Confirm.vue 使用 el-select 选择地址）。 */
  async expectLoaded() {
    await this.page.waitForTimeout(1500)
    // 确认页始终渲染地址 el-select；用 .el-select 直接等待（避免 CSS/text 混合选择器解析失败）
    await this.page.locator('.el-select').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  /** 选择第一个收货地址（点击地址下拉并选择第一项）。 */
  async selectFirstAddress() {
    const select = this.page.locator('.el-select').first()
    await select.waitFor({ state: 'visible', timeout: 15_000 })
    await select.click()
    const option = this.page.locator('.el-select-dropdown__item').first()
    await option.waitFor({ state: 'visible', timeout: 10_000 })
    await option.click()
    await this.page.waitForTimeout(500)
  }

  /** 提交订单（返回提交成功后的响应，含订单号）。 */
  async submitOrder() {
    await this.page.getByRole('button', { name: /提交订单/ }).first().click()
    // 跳转到支付页或订单列表（支付页含外链资源 load 永不触发，必须用 domcontentloaded）
    await this.page.waitForURL(
      (url) => url.pathname.includes('/order/pay/') || url.pathname.includes('/order/batch-pay') || url.pathname.includes('/orders'),
      { waitUntil: 'domcontentloaded', timeout: 20_000 }
    )
  }

  /** 提交订单后应进入支付页。 */
  async expectPayPage() {
    await this.page.waitForURL((url) => url.pathname.includes('/order/pay/') || url.pathname.includes('/order/batch-pay'), {
      waitUntil: 'domcontentloaded',
      timeout: 20_000,
    })
  }

  async orderIdFromUrl() {
    const url = this.page.url()
    const match = url.match(/\/order\/pay\/(\d+)/)
    return match ? match[1] : null
  }
}
