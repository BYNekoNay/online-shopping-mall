import { SEL } from '../helpers/selectors.js'

/** 登录页（/login）。 */
export class LoginPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/login')
    await this.page.waitForSelector('input[placeholder*="用户名"]', { state: 'visible', timeout: 15_000 })
  }

  async login(username, password) {
    await this.page.fill('input[placeholder*="用户名"]', username)
    await this.page.fill('input[placeholder*="密码"]', password)
    await this.page.locator(SEL.BTN_LOGIN).first().click()
  }

  /** 登录后等待跳转离开登录页，并返回登录成功提示是否出现。 */
  async waitLoginSuccess() {
    await this.page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15_000 })
  }

  async expectErrorMessage(text) {
    await this.page.locator(`.el-message:has-text("${text}")`).first().waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 按正则匹配错误/提示消息（兼容登录限流等不同文案）。 */
  async expectErrorMessageRegex(regex) {
    const msg = this.page.locator('.el-message--error, .el-message').first()
    await msg.waitFor({ state: 'visible', timeout: 10_000 })
    const text = await msg.innerText()
    if (!regex.test(text)) {
      throw new Error(`登录失败提示不符合预期：实际为 "${text}"`)
    }
  }
}
