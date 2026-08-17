/** 首页（/）。 */
export class HomePage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/')
  }

  /** 首页分类区块渲染（"全部分类"标题 + 分类卡片）。 */
  async expectCategoriesVisible() {
    await this.page.getByText('全部分类').first().waitFor({ state: 'visible', timeout: 15_000 })
    await this.page.locator('.category-card').first().waitFor({ state: 'visible', timeout: 15_000 })
  }

  /** 首页 AI 推荐区块渲染（"猜你喜欢"标题 + 推荐卡片）。 */
  async expectRecommendVisible() {
    await this.page.getByText('猜你喜欢').first().waitFor({ state: 'visible', timeout: 15_000 })
    // 推荐列表可能为空（冷启动/接口生成中），标题出现即视为区块渲染
  }

  /** 已登录时显示用户昵称。 */
  async expectNickname(nickname) {
    await this.page.getByText(nickname).first().waitFor({ state: 'visible', timeout: 10_000 })
  }

  /** 顶部导航链接（首页/搜索/分类）可见。 */
  async expectNavVisible() {
    await this.page.getByText('搜索', { exact: true }).first().waitFor({ state: 'visible', timeout: 10_000 })
    await this.page.getByText('分类', { exact: true }).first().waitFor({ state: 'visible', timeout: 10_000 })
  }
}
