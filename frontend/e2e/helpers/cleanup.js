/**
 * E2E 测试数据清理说明与辅助（T01）。
 *
 * 清理策略（用户已确认）：
 *  - 线上环境接受测试数据残留，全部测试数据以 e2e_ 前缀标识，便于识别与清理；
 *  - 答辩前一键重置：执行 `scripts/reset-demo.sh`（drop 重建 mall 库 + 导入种子数据），
 *    所有 e2e_ 前缀账号/商品/订单/店铺随之清空；
 *  - 单个测试失败无需手工清理：每次运行使用唯一时间戳账号，重复执行幂等。
 */
export const TEST_DATA_PREFIX = 'e2e_'

/** 输出一条清理指引（在 beforeAll 失败等场景给出提示）。 */
export function printCleanupHint(context = '') {
  // eslint-disable-next-line no-console
  console.log(
    `[cleanup] ${context} 如需清理测试数据，请执行: ./scripts/reset-demo.sh` +
      `（或对 e2e_ 前缀账号/商品/订单执行 SQL 清理）`
  )
}

/**
 * 校验当前页面未残留任何 e2e_ 数据（用于验收演示前置检查）。
 * 仅为辅助断言，不强制调用。
 */
export async function assertNoTestDataVisible(page, { timeout = 3000 } = {}) {
  const count = await page.getByText(/e2e_/).count()
  if (count > 0) {
    throw new Error(`页面仍可见 ${count} 处 e2e_ 测试数据，请先执行 scripts/reset-demo.sh 清理`)
  }
  return true
}
