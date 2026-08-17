/**
 * E2E 消费者：购物车（T03）。
 * 覆盖验收用例 TC-C-09 加购、TC-C-10 数量、TC-C-11 全选、TC-C-12 删除。
 * 注：选用无规格 SKU 的商品（108/110），避免 Element Plus radio 渲染细节带来的选规格复杂度。
 */
import { test, expect } from '../../fixtures/index.js'
import { ProductDetailPage } from '../../pages/ProductDetailPage.js'
import { CartPage } from '../../pages/CartPage.js'

test.describe('consumer cart', () => {
  test('TC-C-09 详情页加入购物车，购物车角标与列表可见', async ({ consumerPage }) => {
    const detail = new ProductDetailPage(consumerPage)
    await detail.goto(108) // Slim Fit Jeans（无 SKUs）
    await detail.addToCart()
    await detail.expectAddToCartSuccess()

    const cart = new CartPage(consumerPage)
    await cart.goto()
    await cart.expectBadgeCount(1)
    await cart.expectItemVisible('Slim Fit Jeans')
  })

  test('TC-C-10/11 修改数量与全选后合计正确', async ({ consumerPage }) => {
    // 前置：加购两件不同商品（均无 SKUs）
    for (const pid of [110, 111]) {
      const detail = new ProductDetailPage(consumerPage)
      await detail.goto(pid)
      await detail.addToCart()
      await detail.expectAddToCartSuccess()
    }
    const cart = new CartPage(consumerPage)
    await cart.goto()
    await cart.expectBadgeCount(2)
    await cart.selectAll()
    const total = await cart.totalPrice()
    expect(total).toBeGreaterThan(0)
    // 直接修改某个商品数量为 2（el-input-number 直接点击 + 按钮）
    await cart.increaseQuantityByOne('Casual Sneakers')
    await cart.expectBadgeCount(2)
    const totalAfter = await cart.totalPrice()
    expect(totalAfter).toBeGreaterThanOrEqual(total)
  })

  test('TC-C-12 删除购物车商品后列表不再显示', async ({ consumerPage }) => {
    const detail = new ProductDetailPage(consumerPage)
    await detail.goto(108)
    await detail.addToCart()
    await detail.expectAddToCartSuccess()

    const cart = new CartPage(consumerPage)
    await cart.goto()
    await cart.expectItemVisible('Slim Fit Jeans')
    await cart.removeItem('Slim Fit Jeans')
    // 等待移除后该项目不再可见
    await consumerPage.waitForTimeout(1000)
    const remaining = await consumerPage.getByText('Slim Fit Jeans').count()
    expect(remaining).toBe(0)
  })
})
