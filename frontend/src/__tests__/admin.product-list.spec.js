/**
 * F-08 admin 商品审核页 smoke 测试。
 *
 * 真实挂载 admin/product/List.vue，mock api/admin + element-plus（命令式组件）：
 * - 待审核商品：渲染「通过 / 拒绝」审核操作按钮
 * - 已上架商品：渲染状态标签与「下架」操作按钮
 *
 * 页面模板的 el-* 用自定义 stub 兜底；el-table-column stub 渲染默认插槽并注入
 * 假行数据（fakeRow），使 scoped slot 里的状态标签/操作按钮在 smoke 层可见；
 * el-table stub 同时渲染 data 行，验证数据流入表格。
 * F-09 后表格/弹窗收敛到 AppTable/AppDialog（真实渲染），el-table/el-table-column/
 * el-pagination stub 经全局 stub 级联生效，行为断言保持不变。
 * 注：本页无分页区（F-08 计划中的分页区覆盖点不适用，改用状态标签覆盖）。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getProductsMock, messageMock } = vi.hoisted(() => ({
  getProductsMock: vi.fn(),
  messageMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: messageMock, error: messageMock, warning: messageMock }
}))

vi.mock('@/api/admin', () => ({
  default: {
    getProducts: getProductsMock,
    auditProduct: vi.fn(),
    offlineProduct: vi.fn()
  }
}))

import AdminProductList from '@/views/admin/product/List.vue'

// el-table-column stub：渲染默认插槽并注入一行假数据，使状态标签/操作按钮（scoped slot）可见
const fakeRow = ref({ id: 1, name: '测试商品', shopName: '测试店铺', status: 2 })

const stubs = {
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  // F-09 后表格收敛到 AppTable（真实渲染），内部 el-pagination 未传分页配置不渲染，stub 兜底避免告警
  'el-pagination': { props: ['total'], template: '<div class="stub-pagination">共 {{ total }} 条</div>' },
  'el-table': {
    props: ['data'],
    template:
      '<div class="stub-table"><div v-for="row in data || []" :key="row.id" class="stub-row">{{ row.name }}</div><slot /></div>'
  },
  'el-table-column': {
    props: ['prop', 'label'],
    setup: () => ({ fakeRow }),
    template: '<div class="stub-col"><slot :row="fakeRow" /></div>'
  },
  'el-tag': { template: '<span class="stub-tag"><slot /></span>' },
  'el-button': { template: '<button class="stub-button"><slot /></button>' },
  'el-dialog': {
    props: ['modelValue'],
    template: '<div class="stub-dialog" v-if="modelValue"><slot /></div>'
  },
  'el-form': { template: '<div><slot /></div>' },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': { template: '<div class="stub-input"><slot /></div>' }
}

function mountList() {
  return mount(AdminProductList, { global: { stubs } })
}

describe('admin/product/List.vue 商品审核页（F-08）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('待审核商品渲染「通过 / 拒绝」审核操作按钮', async () => {
    fakeRow.value = { id: 1, name: '测试商品', shopName: '测试店铺', status: 2 }
    getProductsMock.mockResolvedValue({
      records: [{ id: 1, name: '测试商品', shopName: '测试店铺', status: 2 }]
    })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(1)
    const text = wrapper.text()
    expect(text).toContain('商品审核')
    expect(text).toContain('通过')
    expect(text).toContain('拒绝')
    wrapper.unmount()
  })

  it('已上架商品渲染状态标签与「下架」操作按钮', async () => {
    fakeRow.value = { id: 2, name: '在售商品', shopName: '测试店铺', status: 1 }
    getProductsMock.mockResolvedValue({
      records: [{ id: 2, name: '在售商品', shopName: '测试店铺', status: 1 }]
    })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(1)
    const text = wrapper.text()
    expect(text).toContain('在售商品')
    expect(text).toContain('已上架')
    expect(text).toContain('下架')
    wrapper.unmount()
  })
})
