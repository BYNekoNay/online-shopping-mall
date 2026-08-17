import { confirmDialog } from '../helpers/selectors.js'

/**
 * 管理员商品审核页（/admin/products）。
 * 注意：后台列表为全量展示（无搜索/分页），线上数据量大时新商品可能不在首屏，
 * 端到端用例建议走 API 审核 + UI 断言可见性（见 tests/admin/audit.spec.js）。
 * 本 Page Object 仍保留供数据量小的 staging 环境使用。
 */
export class AdminProductPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/admin/products')
    await this.page.waitForTimeout(1500)
  }

  productRow(name) {
    return this.page.locator('.el-table__row', { hasText: name }).first()
  }

  /** 审核商品（通过/拒绝）。AppDialog confirm-text="确认"；拒绝时须填写原因。 */
  async auditProduct(name, approved = true) {
    const row = this.productRow(name)
    await row.waitFor({ state: 'visible', timeout: 30_000 })
    await row.getByRole('button', { name: approved ? /^通过$/ : /^拒绝$/ }).first().click()
    // AppDialog 审核弹窗（class .el-dialog，确认按钮为 primary）
    const dialog = this.page.locator('.el-dialog:visible').last()
    await dialog.waitFor({ state: 'visible', timeout: 10_000 })
    if (!approved) {
      const reason = dialog.locator('textarea').first()
      await reason.waitFor({ state: 'visible', timeout: 10_000 })
      await reason.fill(`E2E 自动驳回 ${Date.now()}`)
    }
    await confirmDialog(this.page, { timeout: 10_000 })
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  async expectProductStatus(name, statusText) {
    const row = this.productRow(name)
    await row.locator('.el-tag', { hasText: statusText }).waitFor({ state: 'visible', timeout: 10_000 })
  }
}
