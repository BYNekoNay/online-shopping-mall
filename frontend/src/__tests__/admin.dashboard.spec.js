/**
 * F-08 admin 看板页 smoke 测试。
 *
 * 真实挂载 admin/dashboard/Index.vue，mock api/admin，BaseChart 用 stub 占位
 * （echarts 无法在 jsdom 真渲染）：
 * - 统计卡片渲染：GMV / 订单数 / 新增用户 / 订单转化率 / 推荐点击率
 * - 转化率卡片格式化（后端小数 → 百分比）与图表容器
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getDashboardMock, getStatisticsDetailMock } = vi.hoisted(() => ({
  getDashboardMock: vi.fn(),
  getStatisticsDetailMock: vi.fn()
}))

vi.mock('@/api/admin', () => ({
  default: {
    getDashboard: getDashboardMock,
    getStatisticsDetail: getStatisticsDetailMock
  }
}))

import AdminDashboard from '@/views/admin/dashboard/Index.vue'

const stubs = {
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  BaseChart: { props: ['option', 'height'], template: '<div class="stub-chart" />' }
}

function mountDashboard() {
  return mount(AdminDashboard, { global: { stubs } })
}

describe('admin/dashboard/Index.vue 看板页（F-08）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('统计卡片渲染：GMV/订单数/新增用户/转化率/推荐点击率', async () => {
    getDashboardMock.mockResolvedValue({
      gmv: '¥12,345.00',
      orderCount: 88,
      newUserCount: 10,
      conversionRate: 0.12,
      recommendCtr: '5%'
    })
    getStatisticsDetailMock.mockResolvedValue({
      pv: 100,
      uv: 50,
      bounceRate: 30,
      avgStayDuration: 120,
      funnel: { view: 100, cart: 50, order: 20, pay: 10 }
    })
    const wrapper = mountDashboard()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('GMV')
    expect(text).toContain('订单数')
    expect(text).toContain('新增用户')
    expect(text).toContain('订单转化率')
    expect(text).toContain('推荐点击率')
    expect(text).toContain('12.00%')
    wrapper.unmount()
  })

  it('转化率卡片格式化（小数→百分比）且图表容器渲染', async () => {
    getDashboardMock.mockResolvedValue({
      gmv: '0',
      orderCount: 0,
      newUserCount: 0,
      conversionRate: 0.25,
      recommendCtr: '0%'
    })
    getStatisticsDetailMock.mockResolvedValue({
      pv: 100,
      uv: 50,
      bounceRate: 30,
      avgStayDuration: 120,
      funnel: { view: 100, cart: 50, order: 20, pay: 10 }
    })
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('25.00%')
    expect(wrapper.text()).toContain('PV（页面浏览量）')
    expect(wrapper.find('.stub-chart').exists()).toBe(true)
    wrapper.unmount()
  })
})
