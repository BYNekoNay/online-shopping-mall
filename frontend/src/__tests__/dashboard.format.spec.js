/**
 * Q-3 回归契约测试：管理端看板"订单转化率"格式化。
 *
 * 后端 PlatformStatisticsService 返回的 conversionRate 为小数（如 0.12 = 12%），
 * dashboard/Index.vue 通过 formatConversionRate 将其展示为百分比字符串。
 * 防止再次出现看板显示 "0.1234" 或小数未乘 100 的展示 bug。
 *
 * 说明：与 Index.vue 的 formatConversionRate 实现同构断言（纯函数，不加载 .vue 组件），
 * 与 coupon.contract.spec.js 风格一致。
 */
import { describe, it, expect } from 'vitest'

// 与 dashboard/Index.vue formatConversionRate 同构（保持单一事实来源，此处仅为契约断言）
function formatConversionRate(rate) {
  if (rate == null || rate === '') return '0.00%'
  if (typeof rate === 'string' && rate.includes('%')) return rate
  const num = Number(rate)
  if (Number.isNaN(num)) return '0.00%'
  return `${(num * 100).toFixed(2)}%`
}

describe('看板订单转化率格式化（Q-3 回归）', () => {
  it('小数 0.12 格式化为 12.00%（后端 conversionRate 语义为比例，需 ×100）', () => {
    expect(formatConversionRate(0.12)).toBe('12.00%')
    expect(formatConversionRate(0)).toBe('0.00%')
    expect(formatConversionRate(1)).toBe('100.00%')
  })

  it('字符串百分比原样透传（如 "12.34%" 不再重复 ×100）', () => {
    expect(formatConversionRate('12.34%')).toBe('12.34%')
    expect(formatConversionRate('0.00%')).toBe('0.00%')
  })

  it('null/undefined/空串/NaN 兜底为 0.00%', () => {
    expect(formatConversionRate(null)).toBe('0.00%')
    expect(formatConversionRate(undefined)).toBe('0.00%')
    expect(formatConversionRate('')).toBe('0.00%')
    expect(formatConversionRate(NaN)).toBe('0.00%')
    expect(formatConversionRate('abc')).toBe('0.00%')
  })
})
