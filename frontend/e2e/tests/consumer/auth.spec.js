/**
 * E2E 消费者：注册 / 登录（T03）。
 * 覆盖验收用例 TC-C-01 注册、TC-C-02 登录、TC-C-03 登录态保护。
 */
import { test, expect } from '../../fixtures/index.js'
import { LoginPage } from '../../pages/LoginPage.js'
import { RegisterPage } from '../../pages/RegisterPage.js'
import { HomePage } from '../../pages/HomePage.js'
import { makeConsumerAccount } from '../../helpers/accounts.js'

test.describe('consumer auth', () => {
  test('TC-C-01 注册新用户成功并跳转登录页，随后可登录进入首页显示昵称', async ({ page }) => {
    const account = makeConsumerAccount()
    const registerPage = new RegisterPage(page)
    await registerPage.goto()
    await registerPage.register({
      username: account.username,
      nickname: account.nickname,
      password: account.password,
    })
    // 注册成功 → 跳转登录页
    await registerPage.expectRedirectToLogin()
    await page.locator('.el-message--success').first().waitFor({ state: 'visible', timeout: 30_000 })

    // 用新账号登录 → 进入首页显示昵称
    const loginPage = new LoginPage(page)
    await loginPage.login(account.username, account.password)
    await loginPage.waitLoginSuccess()
    const homePage = new HomePage(page)
    await homePage.expectNickname(account.nickname)
  })

  test('TC-C-02 密码错误登录失败并提示', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login('testuser', 'WrongPassword@2026')
    // 系统自带登录限流：错误密码提示 20001；同 IP 窗口内可能触发 10005 限流提示
    await loginPage.expectErrorMessageRegex(/用户名或密码错误|登录失败次数过多|尝试次数过多/)
  })

  test('TC-C-03 未登录访问购物车跳转登录页', async ({ page }) => {
    await page.goto('/cart')
    await page.waitForURL((url) => url.pathname.includes('/login'), { timeout: 15_000 })
  })
})
