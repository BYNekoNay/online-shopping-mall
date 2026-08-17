/** 商家商品管理页（/merchant/products）。 */
export class MerchantProductPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/merchant/products')
    await this.page.waitForTimeout(1000)
  }

  /** 进入发布商品页。等待表单与富文本编辑器 ready（wangeditor 加载重，4-worker 并发下易拖慢）。 */
  async goCreate() {
    await this.page.getByRole('button', { name: /发布商品/ }).first().click()
    await this.page.waitForURL((url) => url.pathname.includes('/merchant/products/edit'), {
      waitUntil: 'domcontentloaded',
      timeout: 20_000,
    })
    await this.page.locator('input[placeholder="请输入商品名称"]').waitFor({ state: 'visible', timeout: 20_000 })
    // 等 wangeditor 富文本 ready（contenteditable 出现），避免提交链路被编辑器初始化拖慢
    await this.page
      .locator('[contenteditable="true"], .wangeditor, .w-e-text-container')
      .first()
      .waitFor({ state: 'attached', timeout: 20_000 })
      .catch(() => {})
  }

  /** 填写商品表单（不含富文本详情，简化）。 */
  async fillProductForm({ name, price, stock = 100, categoryLabel = '电子产品' }) {
    await this.page.fill('input[placeholder="请输入商品名称"]', name)
    // 选择分类
    await this.page.locator('.el-select').first().click()
    await this.page.locator('.el-select-dropdown__item', { hasText: categoryLabel }).first().click()
    // 价格/库存
    const numbers = this.page.locator('.el-input-number input')
    await numbers.nth(0).fill(String(price))
    await numbers.nth(0).press('Enter')
    await numbers.nth(1).fill(String(price * 2))
    await numbers.nth(1).press('Enter')
    await numbers.nth(2).fill(String(stock))
    await numbers.nth(2).press('Enter')
    // 主图
    const imageInput = this.page.locator('input[placeholder="请输入图片URL"]').first()
    await imageInput.fill('https://example.com/e2e-main.jpg')
    await this.page.waitForTimeout(300)
  }

  /** 提交商品表单。toast 等待放宽到 60s（wangeditor 初始化/响应链路并发下可能较慢）。 */
  async submitProduct() {
    await this.page.getByRole('button', { name: /^提交$/ }).first().click()
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 60_000 })
  }

  /** 商品列表行（按名称）。 */
  productRow(name) {
    return this.page.locator('.el-table__row', { hasText: name }).first()
  }

  async expectProductVisible(name) {
    await this.productRow(name).waitFor({ state: 'visible', timeout: 15_000 })
  }

  async expectProductStatus(name, statusText) {
    const row = this.productRow(name)
    await row.locator('.el-tag', { hasText: statusText }).waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 勾选商品并批量下架/上架。 */
  async batchOperate(name, actionLabel) {
    const row = this.productRow(name)
    await row.locator('.el-checkbox__input, input[type="checkbox"]').first().check({ force: true })
    await this.page.waitForTimeout(300)
    await this.page.getByRole('button', { name: actionLabel }).first().click()
    await this.page.waitForTimeout(800)
  }

  /** 进入编辑页。 */
  async goEdit(name) {
    const row = this.productRow(name)
    await row.getByRole('button', { name: /编辑/ }).first().click()
    await this.page.waitForURL((url) => url.pathname.includes('/merchant/products/edit'), {
      waitUntil: 'domcontentloaded',
      timeout: 20_000,
    })
  }
}
