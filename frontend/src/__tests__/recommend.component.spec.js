/**
 * F-T11~12 RecommendList 埋点测试（批次4）。
 *
 * 真实挂载组件，mock api/recommend + api/behavior：
 * - F-T11：数据加载后上报曝光（source 按 mode 区分、productIds 全量、仅一次）
 * - F-T12：点击商品上报 recommendClick（position 1-based）并跳转详情
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { pushMock, getRecommendationsMock, getSimilarProductsMock, getHistoryRecommendationsMock, getPurchaseRecommendationsMock, recommendExposureMock, recommendClickMock } =
  vi.hoisted(() => ({
    pushMock: vi.fn(),
    getRecommendationsMock: vi.fn(),
    getSimilarProductsMock: vi.fn(),
    getHistoryRecommendationsMock: vi.fn(),
    getPurchaseRecommendationsMock: vi.fn(),
    recommendExposureMock: vi.fn(),
    recommendClickMock: vi.fn(),
  }))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/api/recommend', () => ({
  getRecommendations: getRecommendationsMock,
  getSimilarProducts: getSimilarProductsMock,
  getHistoryRecommendations: getHistoryRecommendationsMock,
  getPurchaseRecommendations: getPurchaseRecommendationsMock,
}))

vi.mock('@/api/behavior', () => ({
  recommendExposure: recommendExposureMock,
  recommendClick: recommendClickMock,
}))

import RecommendList from '@/components/RecommendList.vue'

const stubs = {
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  // v-bind="$attrs" 透传 @click，保证 trigger('click') 能触发组件监听器
  'el-card': {
    inheritAttrs: false,
    template: '<div class="stub-card" v-bind="$attrs"><slot /></div>',
  },
  'el-empty': { template: '<div class="stub-empty"><slot /></div>' },
}

function mountList(props = {}) {
  return mount(RecommendList, { props, global: { stubs } })
}

describe('RecommendList 埋点（F-T11~12）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    pushMock.mockReset()
    recommendExposureMock.mockResolvedValue({})
    recommendClickMock.mockResolvedValue({})
  })

  it('F-T11 guess 模式加载后上报曝光：source=home-guess、productIds 全量、仅一次', async () => {
    getRecommendationsMock.mockResolvedValue([
      { productId: 10, name: '商品A', price: 99, mainImage: '/a.jpg' },
      { productId: 20, name: '商品B', price: 199, mainImage: '/b.jpg' },
    ])
    const wrapper = mountList()
    await flushPromises()

    expect(recommendExposureMock).toHaveBeenCalledTimes(1)
    expect(recommendExposureMock).toHaveBeenCalledWith({
      source: 'home-guess',
      productIds: [10, 20],
    })
    wrapper.unmount()
  })

  it('F-T11 similar 模式 source=product-similar（与 guess 区分归因）', async () => {
    getSimilarProductsMock.mockResolvedValue([
      { productId: 30, name: '相似品', price: 88, mainImage: '/c.jpg' },
    ])
    const wrapper = mountList({ mode: 'similar', productId: 10 })
    await flushPromises()

    expect(recommendExposureMock).toHaveBeenCalledWith({
      source: 'product-similar',
      productIds: [30],
    })
    wrapper.unmount()
  })

  it('F-T12 点击商品上报 recommendClick（position 1-based）并跳转详情', async () => {
    getRecommendationsMock.mockResolvedValue([
      { productId: 10, name: '商品A', price: 99, mainImage: '/a.jpg' },
      { productId: 20, name: '商品B', price: 199, mainImage: '/b.jpg' },
    ])
    const wrapper = mountList()
    await flushPromises()

    const cards = wrapper.findAll('.stub-card')
    expect(cards.length).toBeGreaterThanOrEqual(2)
    await cards[1].trigger('click') // 点击第 2 个商品

    expect(recommendClickMock).toHaveBeenCalledWith({
      source: 'home-guess',
      productId: 20,
      position: 2, // 1-based 位置
    })
    expect(pushMock).toHaveBeenCalledWith({
      path: '/product/20',
      query: { from: 'recommend' },
    })
    wrapper.unmount()
  })

  it('A-1 history 模式加载后上报曝光：source=home-history（浏览历史推荐归因）', async () => {
    getHistoryRecommendationsMock.mockResolvedValue([
      { productId: 40, name: '历史相似A', price: 66, mainImage: '/d.jpg' },
    ])
    const wrapper = mountList({ mode: 'history' })
    await flushPromises()

    expect(getHistoryRecommendationsMock).toHaveBeenCalledTimes(1)
    expect(recommendExposureMock).toHaveBeenCalledWith({
      source: 'home-history',
      productIds: [40],
    })
    wrapper.unmount()
  })

  it('D-5 purchase 模式加载后上报曝光：source=home-purchase（购买推荐归因）', async () => {
    getPurchaseRecommendationsMock.mockResolvedValue([
      { productId: 50, name: '购买同类A', price: 88, mainImage: '/e.jpg' },
    ])
    const wrapper = mountList({ mode: 'purchase' })
    await flushPromises()

    expect(getPurchaseRecommendationsMock).toHaveBeenCalledTimes(1)
    expect(recommendExposureMock).toHaveBeenCalledWith({
      source: 'home-purchase',
      productIds: [50],
    })
    wrapper.unmount()
  })
})
