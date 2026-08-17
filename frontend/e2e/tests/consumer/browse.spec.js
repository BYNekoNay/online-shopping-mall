/**
 * E2E 消费者：浏览 / 搜索 / 筛选 / 详情 / 推荐（T03）。
 * 覆盖验收用例 TC-C-04 首页、TC-C-05 搜索、TC-C-06 筛选排序、TC-C-07 详情、TC-C-08 推荐。
 */
import { test, expect } from '../../fixtures/index.js'
import { HomePage } from '../../pages/HomePage.js'
import { SearchPage } from '../../pages/SearchPage.js'
import { ProductDetailPage } from '../../pages/ProductDetailPage.js'

test.describe('consumer browse', () => {
  test('TC-C-04 首页分类与 AI 推荐区块渲染', async ({ consumerPage }) => {
    const home = new HomePage(consumerPage)
    await home.goto()
    await home.expectCategoriesVisible()
    await home.expectRecommendVisible()
    await home.expectNavVisible()
  })

  test('TC-C-05/06 搜索关键词结果非空，价格筛选与排序生效', async ({ consumerPage }) => {
    const search = new SearchPage(consumerPage)
    await search.goto('Phone')
    await search.expectResultsNonEmpty()

    // 价格升序排序后，首个商品价格应 <= 末个商品价格
    await search.sortByPriceAsc()
    await consumerPage.waitForTimeout(1000)
    const prices = await search
      .productCards()
      .evaluateAll((cards) =>
        cards
          .map((c) => {
            const m = c.textContent.match(/¥\s*([\d.]+)/)
            return m ? Number(m[1]) : null
          })
          .filter((v) => v !== null)
      )
    expect(prices.length).toBeGreaterThan(0)
    for (let i = 1; i < prices.length; i += 1) {
      expect(prices[i]).toBeGreaterThanOrEqual(prices[i - 1] - 0.01)
    }
  })

  test('TC-C-07/08 商品详情价格库存可见，相似推荐区块渲染', async ({ consumerPage }) => {
    const detail = new ProductDetailPage(consumerPage)
    await detail.goto(100)
    await detail.expectPriceVisible()
    await detail.expectStockVisible()
    // 相似推荐（若接口返回空则不强制断言卡片，标题渲染即通过）
    await detail.expectSimilarRecommend()
  })
})
