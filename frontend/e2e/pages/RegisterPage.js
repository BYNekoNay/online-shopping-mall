import { SEL } from '../helpers/selectors.js'

/** 注册页（/register）。 */
export class RegisterPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/register')
    await this.page.waitForSelector('input[placeholder*="用户名"]', { state: 'visible', timeout: 15_000 })
  }

  async register({ username, nickname, password }) {
    await this.page.fill('input[placeholder*="用户名"]', username)
    await this.page.fill('input[placeholder*="昵称"]', nickname)
    await this.page.fill('input[placeholder*="密码"]', password)
    await this.page.fill('input[placeholder*="确认密码"]', password)
    await this.page.locator(SEL.BTN_REGISTER).first().click()
  }

  /** 注册成功后应跳转到登录页。 */
  async expectRedirectToLogin() {
    await this.page.waitForURL((url) => url.pathname.includes('/login'), { timeout: 15_000 })
  }
}
