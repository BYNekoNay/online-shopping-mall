/**
 * F-08 商家数据统计页 smoke 测试。
 *
 * 真实挂载 merchant/statistics/Dashboard.vue，mock api/merchant，BaseChart 用 stub 占位
 * （echarts 无法在 jsdom 真渲染）：
 * - 趋势图容器：BaseChart stub 挂载渲染
 * - 指标卡片：销售总额 / 订单数量 / 热销 TOP10（含空态 el-empty）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getSalesStatisticsMock, getTopProductsMock } = vi.hoisted(() => ({
  getSalesStatisticsMock: vi.fn(),
  getTopProductsMock: vi.fn()
}))

vi.mock('@/api/merchant', () => ({
  default: {
    getSalesStatistics: getSalesStatisticsMock,
    getTopProducts: getTopProductsMock
  }
}))

import MerchantDashboard from '@/views/merchant/statistics/Dashboard.vue'

const stubs = {
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  'el-select': { template: '<div class="stub-select"><slot /></div>' },
  'el-option': { template: '<div />' },
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  BaseChart: { props: ['option', 'height'], template: '<div class="stub-chart" />' },
  'el-table': {
    props: ['data'],
    template:
      '<div class="stub-table"><slot /><div v-for="row in data || []" :key="row.productId" class="stub-row">{{ row.name }}</div></div>'
  },
  'el-table-column': { template: '<div />' },
  'el-empty': {
    props: ['description'],
    template: '<div class="stub-empty">{{ description }}</div>'
  }
}

function mountDashboard() {
  return mount(MerchantDashboard, { global: { stubs } })
}

describe('merchant/statistics/Dashboard.vue 数据统计页（F-08）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('趋势图容器渲染：BaseChart stub 挂载且无热销数据时空态提示', async () => {
    getSalesStatisticsMock.mockResolvedValue({
      totalAmount: '1234.00',
      totalOrders: 3,
      trend: [
        { date: '2024-01-01', amount: 100 },
        { date: '2024-01-02', amount: 200 }
      ]
    })
    getTopProductsMock.mockResolvedValue([])
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.stub-chart').exists()).toBe(true)
    const text = wrapper.text()
    expect(text).toContain('数据统计')
    expect(text).toContain('暂无数据')
    wrapper.unmount()
  })

  it('指标卡片渲染：销售总额/订单数量/热销 TOP10', async () => {
    getSalesStatisticsMock.mockResolvedValue({
      totalAmount: '1234.00',
      totalOrders: 3,
      trend: [{ date: '2024-01-01', amount: 100 }]
    })
    getTopProductsMock.mockResolvedValue([
      { productId: 1, name: '热销品A', sales: 10, amount: 200, positiveRate: '98%' }
    ])
    const wrapper = mountDashboard()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('销售总额')
    expect(text).toContain('¥ 1234.00')
    expect(text).toContain('订单数量')
    expect(text).toContain('3 单')
    expect(text).toContain('热销商品 TOP10')
    expect(text).toContain('热销品A')
    wrapper.unmount()
  })
})
