/**
 * 消费端「分类页」分页 + 计数口径 + A3 分类 ID 语义（永久回归 spec）。
 *
 * 真实挂载 consumer/category/Index.vue，mock vue-router（useRoute）+ @/api/product：
 * - A1 计数口径：header 展示后端 total（不是当前页 products.length）
 * - A2 请求参数：{ pageNum, pageSize: 12 }；categoryId 一律字符串（docs/08 §3.7）
 * - A2 翻页：handlePageChange 加载目标页 + scrollIntoView 回顶，不重置页码
 * - A2 切分类：selectCategory 必须把 pageNum 重置为 1，并携带字符串 categoryId
 * - A3 applyQueryCategory：用 findCategoryById（递归、String 比较）校验分类真伪
 *   · ID 存在 → 选中分类 + header 显示分类名
 *   · ID 不存在（含 19 位雪花）→ 回落「全部商品」+ 请求不含 categoryId
 *   · ID 存在但无商品 → 保留 header 分类名 + 「该分类下暂无商品」空态
 *   · 前导零归一化 '03'/'003' → '3'；全零 '0'/'00' → 回落全部商品
 *   · total=0 时分页组件不渲染
 *
 * 页面模板里的 el-* 一律用自定义 stub 兜底（vitest 不自动注册 element-plus 组件）；
 * ProductCard 用 stub 替身，避免其 useRouter/useCartStore 等重依赖干扰本页断言。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数/route 状态必须用 vi.hoisted 声明（避免 TDZ）
const { routeMock, getCategoriesMock, getProductsMock } = vi.hoisted(() => ({
  routeMock: { query: {} },
  getCategoriesMock: vi.fn(),
  getProductsMock: vi.fn()
}))

// 仅覆盖 useRoute；保留 createRouter 等真实导出（页面间接引用的 ProductCard → @/api/cart
// → @/utils/request → @/router 会在导入期真正调用 createRouter/createWebHistory）
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRoute: () => routeMock
}))

vi.mock('@/api/product', () => ({
  default: {
    getCategories: getCategoriesMock,
    getProducts: getProductsMock
  }
}))

import CategoryIndex from '@/views/consumer/category/Index.vue'

// 含一层 children，用于验证 findCategoryById 的递归查找
const CATEGORY_TREE = [
  { id: 3, name: '数码' },
  { id: 1, name: '服装', children: [{ id: 11, name: '男装' }, { id: 12, name: '女装' }] }
]

function makeProducts(n, prefix = 'P') {
  return Array.from({ length: n }, (_, i) => ({ id: `${prefix}-${i + 1}`, name: `${prefix}商品${i + 1}` }))
}

const stubs = {
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
  'el-pagination': {
    props: ['total', 'currentPage', 'pageSize'],
    emits: ['current-change'],
    template:
      '<div class="stub-pagination"><button class="stub-page-3" @click="$emit(\'current-change\', 3)">3</button>共 {{ total }} 条</div>'
  },
  'el-empty': { props: ['description'], template: '<div class="stub-empty">{{ description }}</div>' },
  ProductCard: { props: ['item'], template: '<div class="stub-product-card">{{ item.name }}</div>' }
}

function mountPage() {
  return mount(CategoryIndex, { global: { stubs } })
}

describe('consumer/category/Index.vue 分页与计数（永久回归）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeMock.query = {}
    getCategoriesMock.mockResolvedValue(CATEGORY_TREE)
    getProductsMock.mockResolvedValue({ records: makeProducts(12), total: 100 })
    // jsdom 未实现 scrollIntoView，注入 mock 以断言「翻页回顶」
    window.Element.prototype.scrollIntoView = vi.fn()
  })

  it('A1 计数口径：header 展示后端 total，而非当前页 products.length', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.header-count').text()).toContain('100 件商品')
    expect(wrapper.find('.header-count').text()).not.toContain('12 件商品')
    // 当前页实际只渲染 12 张卡片 —— 证明计数并非来自 products.length
    expect(wrapper.findAll('.stub-product-card')).toHaveLength(12)
    wrapper.unmount()
  })

  it('A2 请求携带 { pageNum, pageSize: 12 }（无分类时不带 categoryId）', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12 })
    wrapper.unmount()
  })

  it('A2 total=0 时分页组件不渲染', async () => {
    getProductsMock.mockResolvedValue({ records: [], total: 0 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.stub-pagination').exists()).toBe(false)
    wrapper.unmount()
  })

  it('A2 翻页：handlePageChange 加载目标页并 scrollIntoView 回顶（不重置页码）', async () => {
    const wrapper = mountPage()
    await flushPromises()
    getProductsMock.mockClear()

    await wrapper.find('.stub-page-3').trigger('click')
    await flushPromises()

    expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 3, pageSize: 12 })
    expect(window.Element.prototype.scrollIntoView).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('A2 切分类：selectCategory 把页码重置为 1 并携带字符串 categoryId', async () => {
    const wrapper = mountPage()
    await flushPromises()
    // 先翻到第 3 页
    await wrapper.find('.stub-page-3').trigger('click')
    await flushPromises()
    expect(getProductsMock).toHaveBeenLastCalledWith({ pageNum: 3, pageSize: 12 })

    // 切到「数码」(id=3)：页码必须回到 1，否则从第 3 页切到只有 1 页的分类会拿到空列表
    await wrapper.findAll('.category-item')[0].trigger('click')
    await flushPromises()

    expect(getProductsMock).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 12, categoryId: '3' })
    wrapper.unmount()
  })

  it('A3 分类 ID 存在：选中分类 + header 显示分类名 + 请求带字符串 categoryId', async () => {
    routeMock.query = { id: '3' }
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.selected-header h3').text()).toBe('数码')
    expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12, categoryId: '3' })
    wrapper.unmount()
  })

  it('A3 递归查找：嵌套子分类 ID 命中（children 支持）', async () => {
    routeMock.query = { id: '11' }
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.selected-header h3').text()).toBe('男装')
    expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12, categoryId: '11' })
    wrapper.unmount()
  })

  it('A3 分类 ID 不存在（含 19 位雪花）：回落全部商品，请求不含 categoryId', async () => {
    routeMock.query = { id: '1234567890123456789' }
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.selected-header h3').text()).toBe('全部商品')
    expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12 })
    expect(getProductsMock.mock.calls[0][0]).not.toHaveProperty('categoryId')
    wrapper.unmount()
  })

  it("A3 前导零归一化：'03'/'003' → '3' 命中分类", async () => {
    for (const raw of ['03', '003']) {
      getProductsMock.mockClear()
      routeMock.query = { id: raw }
      const wrapper = mountPage()
      await flushPromises()

      expect(wrapper.find('.selected-header h3').text()).toBe('数码')
      expect(getProductsMock).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 12, categoryId: '3' })
      wrapper.unmount()
    }
  })

  it("A3 全零 ID（'0'/'00'）回落全部商品（不带 categoryId=0）", async () => {
    for (const raw of ['0', '00']) {
      getProductsMock.mockClear()
      routeMock.query = { id: raw }
      const wrapper = mountPage()
      await flushPromises()

      expect(wrapper.find('.selected-header h3').text()).toBe('全部商品')
      expect(getProductsMock).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12 })
      wrapper.unmount()
    }
  })

  it('A3 分类存在但无商品：保留 header 分类名 + 展示「该分类下暂无商品」空态', async () => {
    routeMock.query = { id: '3' }
    getProductsMock.mockResolvedValue({ records: [], total: 0 })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.selected-header h3').text()).toBe('数码')
    const empty = wrapper.find('.stub-empty')
    expect(empty.exists()).toBe(true)
    expect(empty.text()).toContain('该分类下暂无商品')
    // 无商品且 total=0 → 分页组件也不渲染
    expect(wrapper.find('.stub-pagination').exists()).toBe(false)
    wrapper.unmount()
  })
})
