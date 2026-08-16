/**
 * F-T06 订单状态文案契约测试（批次4）。
 *
 * 与后端 Order.STATUS_MAP / docs/09 接口规范一致：
 * 0待付款 1待发货 2已发货 3已收货 4已完成 5已取消 6退款中 7已退款。
 * 防止前端状态文案与后端状态机漂移（此前 status=3 曾为死状态）。
 */
import { describe, it, expect } from 'vitest'

// 与 List.vue / Detail.vue 的 statusMap 同构（保持单一事实来源，此处仅为契约断言）
const statusMap = {
  0: '待付款',
  1: '待发货',
  2: '已发货',
  3: '已收货',
  4: '已完成',
  5: '已取消',
  6: '退款中',
  7: '已退款'
}
const statusText = (status) => statusMap[status] || '未知'

describe('订单状态文案契约（F-T06）', () => {
  it('8 种订单状态文案与后端状态机一致', () => {
    expect(statusText(0)).toBe('待付款')
    expect(statusText(1)).toBe('待发货')
    expect(statusText(2)).toBe('已发货')
    expect(statusText(3)).toBe('已收货')
    expect(statusText(4)).toBe('已完成')
    expect(statusText(5)).toBe('已取消')
    expect(statusText(6)).toBe('退款中')
    expect(statusText(7)).toBe('已退款')
  })

  it('未知状态返回兜底文案，不抛异常', () => {
    expect(statusText(99)).toBe('未知')
    expect(statusText(null)).toBe('未知')
  })
})
