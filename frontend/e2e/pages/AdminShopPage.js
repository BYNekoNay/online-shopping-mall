import { expect } from '@playwright/test'
import { confirmDialog } from '../helpers/selectors.js'

/** 管理员店铺审核页（/admin/shops）。 */
export class AdminShopPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/admin/shops')
    await this.page.waitForTimeout(1500)
  }

  shopRow(shopName) {
    return this.page.locator('.el-table__row', { hasText: shopName }).first()
  }

  /** 审核店铺（通过/拒绝）。店铺列表带分页，先按名称查找；行操作弹 ElMessageBox 确认。 */
  async auditShop(shopName, approved = true) {
    const row = this.shopRow(shopName)
    await row.waitFor({ state: 'visible', timeout: 15_000 })
    await row.getByRole('button', { name: approved ? /^通过$/ : /^拒绝$/ }).first().click()
    // ElMessageBox 确认（class .el-message-box，确认按钮为 primary；统一 helper 处理）
    await confirmDialog(this.page, { timeout: 10_000 })
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  /** 断言店铺行包含状态文本（el-tag 可能处于 zoom 过渡动画，用行文本断言更稳）。 */
  async expectShopStatus(shopName, statusText) {
    const row = this.shopRow(shopName)
    await row.waitFor({ state: 'attached', timeout: 15_000 })
    await expect(row).toContainText(statusText, { timeout: 10_000 })
  }
}
