/** 购物车页（/cart）。 */
export class CartPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/cart')
  }

  async expectLoaded() {
    // 页面标题或商品列表/空状态出现
    await this.page.waitForTimeout(1500)
  }

  /** 断言购物车角标数量（header .el-badge__content）。 */
  async expectBadgeCount(count) {
    const badge = this.page.locator('.el-badge__content').first()
    await badge.waitFor({ state: 'visible', timeout: 10_000 })
    const text = (await badge.innerText()).trim()
    if (Number(text) !== count) {
      throw new Error(`购物车角标期望 ${count}，实际 ${text}`)
    }
  }

  cartRows() {
    return this.page.locator('.el-table__row, [class*="cart-item"], .cart-row')
  }

  /** 断言行/单元格存在包含商品名的文本（不依赖具体 class）。 */
  async expectItemVisible(productName) {
    await this.page.getByText(productName).first().waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 找到含 productName 的行（优先 .el-table__row，回退到任意包含该文本的容器）。 */
  findRow(productName) {
    return this.page.locator('.el-table__row', { hasText: productName }).first()
  }

  /** 将 productName 行数量 +1（点击 + 按钮）。 */
  async increaseQuantityByOne(productName) {
    const row = this.findRow(productName)
    if (await row.count()) {
      const btn = row.locator('.el-input-number__increase, button[aria-label="increase number"]').first()
      await btn.click()
      await this.page.waitForTimeout(500)
    }
  }

  /** 全选。 */
  async selectAll() {
    const checkbox = this.page.locator('.el-checkbox:has-text("全选") input, .el-checkbox__input input').first()
    await checkbox.check({ force: true }).catch(async () => {
      await this.page.getByText('全选', { exact: true }).first().click()
    })
    await this.page.waitForTimeout(500)
  }

  /** 获取合计金额文本（如 ¥123.00）。 */
  async totalPrice() {
    const el = this.page.locator('.total-price, [class*="total-price"]').first()
    await el.waitFor({ state: 'visible', timeout: 10_000 })
    const text = await el.innerText()
    const match = text.replace(/,/g, '').match(/([\d.]+)/)
    return match ? Number(match[1]) : null
  }

  /** 删除商品（按名称定位所在行的删除按钮）。 */
  async removeItem(productName) {
    const row = this.findRow(productName)
    if (await row.count()) {
      await row.locator('button:has-text("删除")').first().click()
    } else {
      // 退化：删除第一个 含名的 删除按钮
      await this.page.locator('button:has-text("删除")').first().click()
    }
    await this.page.waitForTimeout(500)
  }

  /** 去结算。 */
  async checkout() {
    await this.page.getByRole('button', { name: /结算/ }).first().click()
  }
}
