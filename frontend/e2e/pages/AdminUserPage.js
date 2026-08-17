import { confirmDialog } from '../helpers/selectors.js'

/** 管理员用户管理页（/admin/users）。 */
export class AdminUserPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/admin/users')
    await this.page.waitForTimeout(1500)
  }

  /** 按用户名搜索（AppSearchForm 搜索按钮文案为"查询"）。 */
  async search(username) {
    const input = this.page.locator('input[placeholder*="用户名"], input[placeholder*="昵称"]').first()
    await input.fill(username)
    await this.page.getByRole('button', { name: /查询/ }).first().click()
    await this.page.waitForTimeout(1000)
  }

  userRow(username) {
    return this.page.locator('.el-table__row', { hasText: username }).first()
  }

  /** 禁用/启用用户（根据当前状态按钮文案）。行操作弹 ElMessageBox 确认。 */
  async toggleStatus(username) {
    const row = this.userRow(username)
    await row.waitFor({ state: 'visible', timeout: 15_000 })
    await row.getByRole('button', { name: /禁用|启用/ }).first().click()
    // ElMessageBox 确认（class .el-message-box，按钮 OK/确定 不定，统一 helper 处理）
    await confirmDialog(this.page, { timeout: 10_000 })
    await this.page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })
  }

  async expectUserStatus(username, statusText) {
    const row = this.userRow(username)
    await row.locator('.el-tag', { hasText: statusText }).waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 看板页加载出核心指标。导航容错：线上环境该页偶发挂起，用短超时 + catch 避免拖挂用例。 */
  async expectDashboardLoaded() {
    await this.page.goto('/admin/dashboard', { timeout: 20_000 }).catch(() => {})
    await this.page.waitForTimeout(2500)
    await this.page.locator('.el-card, [class*="stat-card"], [class*="dashboard"]').first().waitFor({
      state: 'visible',
      timeout: 15_000,
    })
  }
}
