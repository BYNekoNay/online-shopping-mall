/**
 * 消费端「搜索结果页」分页 + 计数口径（永久回归 spec）。
 *
 * 真实挂载 consumer/search/Index.vue，mock vue-router（useRoute）+ @/store/user + @/api/product：
 * - B 请求携带 { pageNum, pageSize: 12 }，total 取后端值并渲染结果
 * - B handleSearch()：关键词/排序/价格/清空/历史回填/初始化 全部走它 → 页码重置为 1
 * - B handlePageChange()：翻页 → 不重置页码，命中目标页
 * - B el-empty 仅在「非加载中且结果为空」时显示
 *
 * 页面模板里的 el-* 一律用自定义 stub 兜底；ProductCard 用 stub 替身，
 * 避免其 useRouter/useCartStore 等重依赖干扰本页断言。
 * stub 上挂载可点击元素用于触发 handleSearch / handlePageChange 两条路径。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数/route 状态必须用 vi.hoisted 声明（避免 TDZ）
const { routeMock, searchProductsMock, getSearchHistoryMock, clearSearchHistoryMock } = vi.hoisted(() => ({
  routeMock: { query: {} },
  searchProductsMock: vi.fn(),
  getSearchHistoryMock: vi.fn(),
  clearSearchHistoryMock: vi.fn()
}))

// 仅覆盖 useRoute；保留 createRouter 等真实导出（页面间接引用的 ProductCard → @/api/cart
// → @/utils/request → @/router 会在导入期真正调用 createRouter/createWebHistory）
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRoute: () => routeMock
}))

// useUserStore 在无 pinia 实例时会抛错；本轮只关心分页，直接提供未登录替身（token 空 → 不加载历史）
vi.mock('@/store/user', () => ({
  useUserStore: () => ({ token: '', setUser: vi.fn(), logout: vi.fn() })
}))

vi.mock('@/api/product', () => ({
  default: {
    searchProducts: searchProductsMock,
    getProducts: searchProductsMock,
    getCategories: vi.fn()
  },
  searchProducts: searchProductsMock,
  getSearchHistory: getSearchHistoryMock,
  clearSearchHistory: clearSearchHistoryMock
}))

import SearchIndex from '@/views/consumer/search/Index.vue'

function makeProducts(n, prefix = 'S') {
  return Array.from({ length: n }, (_, i) => ({ id: `${prefix}-${i + 1}`, name: `${prefix}商品${i + 1}` }))
}

const stubs = {
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  // 只渲染默认 + append 插槽（跳过 #prefix，避免加载 Search 图标组件）
  'el-input': { template: '<div class="stub-input"><slot /><slot name="append" /></div>' },
  'el-icon': { template: '<i><slot /></i>' },
  'el-button': { template: '<button class="stub-button" @click="$emit(\'click\')"><slot /></button>' },
  'el-tag': { props: ['size'], template: '<span class="stub-tag"><slot /></span>' },
  // 排序变化触发 handleSearch（@change）
  'el-radio-group': {
    props: ['modelValue'],
    emits: ['change', 'update:modelValue'],
    template:
      '<div class="stub-radio-group"><button class="stub-sort-btn" @click="$emit(\'change\', \'sales\')">排序</button><slot /></div>'
  },
  'el-radio-button': { template: '<label><slot /></label>' },
  'el-input-number': { props: ['modelValue'], template: '<div class="stub-input-number" />' },
  'el-empty': { props: ['description'], template: '<div class="stub-empty">{{ description }}</div>' },
  'el-pagination': {
    props: ['total', 'currentPage', 'pageSize'],
    emits: ['current-change'],
    template:
      '<div class="stub-pagination"><button class="stub-page-3" @click="$emit(\'current-change\', 3)">3</button>共 {{ total }} 条</div>'
  },
  ProductCard: { props: ['item'], template: '<div class="stub-product-card">{{ item.name }}</div>' }
}

function mountSearch() {
  return mount(SearchIndex, { global: { stubs } })
}

describe('consumer/search/Index.vue 分页与计数（永久回归）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeMock.query = {}
    searchProductsMock.mockResolvedValue({ records: [], total: 0 })
    getSearchHistoryMock.mockResolvedValue([])
    clearSearchHistoryMock.mockResolvedValue({})
  })

  it('B 请求携带 { pageNum, pageSize: 12 }，total 取后端值并渲染结果', async () => {
    routeMock.query = { keyword: '手机' }
    searchProductsMock.mockResolvedValue({ records: makeProducts(12), total: 88 })
    const wrapper = mountSearch()
    await flushPromises()

    expect(searchProductsMock).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '手机', pageNum: 1, pageSize: 12 })
    )
    expect(wrapper.text()).toContain('共 88 件商品')
    expect(wrapper.findAll('.stub-product-card')).toHaveLength(12)
    expect(wrapper.find('.stub-pagination').exists()).toBe(true)
    wrapper.unmount()
  })

  it('B handleSearch 重置页码为 1（翻到第 3 页后再触发搜索回到第 1 页）', async () => {
    routeMock.query = { keyword: '手机' }
    searchProductsMock.mockResolvedValue({ records: makeProducts(12), total: 88 })
    const wrapper = mountSearch()
    await flushPromises()

    // 翻页到第 3 页
    await wrapper.find('.stub-page-3').trigger('click')
    await flushPromises()
    expect(searchProductsMock).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 3 }))

    // 排序变化 → handleSearch → 页码必须回到 1
    await wrapper.find('.stub-sort-btn').trigger('click')
    await flushPromises()
    expect(searchProductsMock).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 12 }))
    wrapper.unmount()
  })

  it('B handlePageChange 翻页不重置页码（命中目标页）', async () => {
    routeMock.query = { keyword: '手机' }
    searchProductsMock.mockResolvedValue({ records: makeProducts(12), total: 88 })
    const wrapper = mountSearch()
    await flushPromises()

    await wrapper.find('.stub-page-3').trigger('click')
    await flushPromises()

    expect(searchProductsMock).toHaveBeenLastCalledWith(
      expect.objectContaining({ pageNum: 3, pageSize: 12 })
    )
    wrapper.unmount()
  })

  it('B 结果为空且非加载中：展示「暂无搜索结果」空态，分页不渲染', async () => {
    routeMock.query = { keyword: '无结果词' }
    searchProductsMock.mockResolvedValue({ records: [], total: 0 })
    const wrapper = mountSearch()
    await flushPromises()

    const empty = wrapper.find('.stub-empty')
    expect(empty.exists()).toBe(true)
    expect(empty.text()).toContain('暂无搜索结果')
    expect(wrapper.find('.stub-pagination').exists()).toBe(false)
    wrapper.unmount()
  })

  it('B 加载中（searching=true）即使结果为空也不展示空态', async () => {
    routeMock.query = { keyword: '慢查询' }
    let resolveSearch
    searchProductsMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveSearch = resolve
        })
    )
    const wrapper = mountSearch()
    await wrapper.vm.$nextTick()

    // 请求尚未完成：searching=true → 空态不显示
    expect(searchProductsMock).toHaveBeenCalled()
    expect(wrapper.find('.stub-empty').exists()).toBe(false)

    // 完成后仍为空 → 空态显示
    resolveSearch({ records: [], total: 0 })
    await flushPromises()
    expect(wrapper.find('.stub-empty').exists()).toBe(true)
    wrapper.unmount()
  })
})
