/**
 * E2E 帮助：Element Plus 关键 UI 选择器常量（T01）。
 *
 * 集中管理 Element Plus / 业务页面中的常见选择器，避免测试文件内散落魔法字符串。
 * 命名规范：TEXT_* 为可见文本（配合 getByText / getByRole），CLS_* 为稳定 class 前缀。
 */
export const SEL = {
  // ---------- 通用表单 ----------
  INPUT_USERNAME: 'input[placeholder*="用户名"]',
  INPUT_PASSWORD: 'input[placeholder*="密码"]',
  INPUT_NICKNAME: 'input[placeholder*="昵称"]',
  INPUT_CONFIRM_PASSWORD: 'input[placeholder*="确认密码"]',
  INPUT_SEARCH: 'input[placeholder*="搜索"]',
  BTN_LOGIN: 'button:has-text("登 录"), button:has-text("登录")',
  BTN_REGISTER: 'button:has-text("注册")',
  BTN_SUBMIT: 'button:has-text("提交")',
  BTN_CONFIRM: 'button:has-text("确定")',
  BTN_CANCEL: 'button:has-text("取消")',
  BTN_SEARCH: 'button:has-text("搜索")',

  // ---------- 顶部导航 ----------
  NAV_CART_BADGE: '.el-badge__content',
  NAV_USER_DROPDOWN: '.el-dropdown',
  NAV_LOGOUT: 'span:has-text("退出登录"), li:has-text("退出登录")',

  // ---------- 商品卡片 ----------
  PRODUCT_CARD: '.product-card, .goods-card, .el-card',
  PRODUCT_NAME: '.product-name, .goods-name, .el-card__body .name',

  // ---------- Element Plus 通用控件 ----------
  DIALOG: '.el-dialog',
  DIALOG_TITLE: '.el-dialog__title',
  MESSAGE: '.el-message',
  MESSAGE_SUCCESS: '.el-message--success',
  MESSAGE_ERROR: '.el-message--error',
  EMPTY: '.el-empty__description',
  TABLE_ROW: '.el-table__row',
  TAB: '.el-tabs__item',
  PAGINATION: '.el-pagination',
  TAG: '.el-tag',
  SWITCH: '.el-switch',
}

/** 消息 toast 文本（Element Plus 消息组件出现即代表后端/前端提示）。 */
export async function expectMessage(page, text, { timeout = 8000 } = {}) {
  await page.locator(`${SEL.MESSAGE}:has-text("${text}")`).first().waitFor({ state: 'visible', timeout })
}

/** 断言页面 URL 包含某路径。 */
export async function expectPath(page, pathPart) {
  await page.waitForURL((url) => url.pathname.includes(pathPart), { timeout: 15_000 })
}

/**
 * 确认对话框（统一处理两种弹窗）：
 *  - ElMessageBox.confirm() → .el-message-box（确认按钮为 primary，文案 OK/确定 不定）
 *  - AppDialog / el-dialog → .el-dialog（confirm-text 为"保存/确认/确定"不定）
 * 统一策略：在可见的弹窗容器内点击最后一个 primary 按钮。
 */
export async function confirmDialog(page, { timeout = 10_000 } = {}) {
  const dialog = page.locator('.el-message-box:visible, .el-dialog:visible').last()
  await dialog.waitFor({ state: 'visible', timeout })
  const primary = dialog.locator('.el-button--primary').last()
  await primary.waitFor({ state: 'visible', timeout })
  await primary.click()
}
