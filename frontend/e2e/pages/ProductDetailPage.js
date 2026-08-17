/** 商品详情页（/product/:id）。 */
export class ProductDetailPage {
  constructor(page) {
    this.page = page
  }

  async goto(id) {
    await this.page.goto(`/product/${id}`)
  }

  /** 等待商品名称与价格渲染。 */
  async expectLoaded() {
    await this.page.locator('.price, [class*="price-panel"]').first().waitFor({ state: 'visible', timeout: 15_000 })
  }

  async expectPriceVisible() {
    await this.page.locator('.price, [class*="price-row"] .price').first().waitFor({ state: 'visible', timeout: 15_000 })
  }

  async expectStockVisible() {
    await this.page.locator('[class*="stock-num"], [class*="stock"]').first().waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 商品名可见（用于断言进入正确的详情页）。 */
  async expectName(name) {
    await this.page.getByText(name).first().waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 相似商品推荐区块可见。 */
  async expectSimilarRecommend() {
    await this.page.getByText(/相似商品推荐|相似推荐|相关推荐/).first().waitFor({ state: 'visible', timeout: 15_000 })
  }

  /** 加入购物车（自动选择首个规格，避免因"请选择规格"导致按钮禁用）。 */
  async addToCart() {
    // 若存在规格区，先选中第一个规格（通过 role 定位，绕过 Element Plus 渲染细节）
    const radio = this.page.getByRole('radio').first()
    if (await radio.count()) {
      await radio.check({ force: true })
      await this.page.waitForTimeout(500)
    }
    // 主"加入购物车"按钮在 .actions 容器内（避免与相似推荐卡片上的快速加入按钮冲突）
    const mainBtn = this.page.locator('.actions .el-button--primary').first()
    await mainBtn.waitFor({ state: 'visible', timeout: 10_000 })
    await mainBtn.click()
  }

  /** 立即购买（跳转确认页）。 */
  async buyNow() {
    const radio = this.page.getByRole('radio').first()
    if (await radio.count()) {
      await radio.check({ force: true })
      await this.page.waitForTimeout(500)
    }
    const mainBtn = this.page.locator('.actions .el-button').filter({ hasText: /立即购买|直接购买|购买/ }).first()
    await mainBtn.click()
  }

  /** 断言加入购物车成功 toast。 */
  async expectAddToCartSuccess() {
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }
}
