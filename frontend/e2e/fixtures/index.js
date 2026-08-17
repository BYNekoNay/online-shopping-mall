/**
 * Playwright 自定义 fixtures（T01）。
 *
 * 提供三个已登录页面对象与账号上下文：
 *  - consumerAccount / consumerPage：全新注册的消费者（每个用例独立账号）
 *  - merchantAccount / merchantPage：自建并审核通过的商家
 *  - adminPage：种子管理员 admin
 *
 * 登录态通过 API 获取后注入 localStorage（与 src/store/user.js 键一致），
 * 避免每个用例走 UI 登录，提升稳定性与速度。
 * *Account 暴露 session 供 API 前置调用（地址、订单、看板等）。
 */
import { test as base, expect } from '@playwright/test'
import {
  adminSession,
  createMerchantWithShop,
  injectSession,
  registerConsumer,
} from '../helpers/accounts.js'

export const test = base.extend({
  /**
   * 全局 page fixture（QA 验证补充）：拦截外链图片并 abort。
   * 原因：被测系统图片大量使用 picsum/example.com 等外链，线上环境外链慢/不稳定会拖慢甚至拖挂 E2E；
   * 所有断言均不依赖图片加载（前端已有兜底底色），abort 不影响结果，可显著提速。
   */
  page: async ({ page }, use) => {
    const EXTERNAL_IMAGE_HOSTS = ['picsum.photos', 'example.com', 'via.placeholder.com', 'placehold.co', 'dummyimage.com', 'placehold.jp']
    await page.route('**/*', (route) => {
      const host = new URL(route.request().url()).host
      if (EXTERNAL_IMAGE_HOSTS.some((h) => host === h || host.endsWith(`.${h}`))) {
        return route.abort()
      }
      return route.continue()
    })
    // 导航改为 domcontentloaded：线上环境部分页面存在永不结束的外链/长连接资源，
    // 默认 waitUntil:'load' 会一直等待直到测试超时（实测 /product/108 卡满 120s）。
    // 所有断言均等待可见元素（不依赖 load 事件），domcontentloaded 足够且显著提速。
    // 线上环境个别页面导航偶发挂起（dashboard/订单/详情页实测），用 30s 短超时 + 1 次重试兜底。
    const originalGoto = page.goto.bind(page)
    page.goto = async (url, options = {}) => {
      const navOptions = { waitUntil: 'domcontentloaded', timeout: 30_000, ...options }
      try {
        return await originalGoto(url, navOptions)
      } catch (err) {
        await page.waitForTimeout(1000)
        return await originalGoto(url, navOptions)
      }
    }
    // reload 同样改为 domcontentloaded + 短超时重试
    const originalReload = page.reload.bind(page)
    page.reload = async (options = {}) => {
      const navOptions = { waitUntil: 'domcontentloaded', timeout: 30_000, ...options }
      try {
        return await originalReload(navOptions)
      } catch (err) {
        await page.waitForTimeout(1000)
        return await originalReload(navOptions)
      }
    }
    await use(page)
  },

  /** 全新消费者账号 + session */
  consumerAccount: async ({}, use) => {
    const { account, session } = await registerConsumer()
    await use({ account, session })
  },

  /** 全新消费者（API 注册 + 注入登录态）。不预加载首页，避免首屏图源拖慢用例。 */
  consumerPage: async ({ page, consumerAccount }, use) => {
    await injectSession(page, consumerAccount.session)
    await use(page)
  },

  /** 自建商家账号上下文（含 session/shopId，供 API 前置调用）。 */
  merchantAccount: async ({}, use) => {
    const merchant = await createMerchantWithShop()
    await use(merchant)
  },

  /** 自建商家（含店铺审核通过）。不预加载商家后台。 */
  merchantPage: async ({ page, merchantAccount }, use) => {
    await injectSession(page, merchantAccount.session)
    await use(page)
  },

  /** 种子管理员。不预加载看板，避免图表懒加载干扰。 */
  adminPage: async ({ page }, use) => {
    const session = await adminSession()
    await injectSession(page, session)
    await use(page)
  },
})

export { expect }
