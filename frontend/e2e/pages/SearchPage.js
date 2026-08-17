/** 搜索页（/search）。 */
export class SearchPage {
  constructor(page) {
    this.page = page
  }

  async goto(keyword) {
    const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
    await this.page.goto(`/search${query}`)
    await this.page.waitForSelector('input[placeholder*="搜索"]', { state: 'visible', timeout: 15_000 })
  }

  async search(keyword) {
    await this.page.fill('input[placeholder*="搜索"]', keyword)
    await this.page.getByRole('button', { name: '搜索' }).first().click()
  }

  /** 商品结果卡（ProductCard 组件，class=product-card-modern）。 */
  productCards() {
    return this.page.locator('.product-card-modern')
  }

  async expectResultsNonEmpty() {
    await this.page.waitForFunction(
      () => document.querySelectorAll('.product-card-modern').length > 0,
      { timeout: 15_000 }
    )
  }

  /** 按价格升序排序。 */
  async sortByPriceAsc() {
    await this.page.getByText('价格升序').first().click()
    // 等待排序后列表刷新
    await this.page.waitForTimeout(800)
  }

  async firstResultPrice() {
    const card = this.productCards().first()
    await card.waitFor({ state: 'visible', timeout: 10_000 })
    const text = await card.innerText()
    const match = text.match(/¥\s*([\d.]+)/)
    return match ? Number(match[1]) : null
  }

  async clickFirstResult() {
    await this.productCards().first().click()
  }
}
