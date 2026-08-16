/**
 * F-08 商家商品列表页 smoke 测试。
 *
 * 真实挂载 merchant/product/List.vue，mock api/merchant + element-plus（命令式组件），
 * 模板 $router 用 global.mocks 提供：
 * - 上下架按钮：勾选商品（el-table 触发 selection-change）后出现批量上架/下架/删除
 * - 空态：无商品时页面正常渲染，批量按钮不出现
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getMerchantProductsMock, messageMock, pushMock } = vi.hoisted(() => ({
  getMerchantProductsMock: vi.fn(),
  messageMock: vi.fn(),
  pushMock: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: messageMock, error: messageMock }
}))

vi.mock('@/api/merchant', () => ({
  default: {
    getMerchantProducts: getMerchantProductsMock,
    batchOperateProducts: vi.fn()
  }
}))

import MerchantProductList from '@/views/merchant/product/List.vue'

const stubs = {
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  'el-input': { template: '<div class="stub-input"><slot /></div>' },
  'el-select': { template: '<div class="stub-select"><slot /></div>' },
  'el-option': { template: '<div />' },
  'el-button': { template: '<button class="stub-button"><slot /></button>' },
  'el-table': {
    props: ['data'],
    emits: ['selection-change'],
    template:
      '<div class="stub-table"><slot /><button class="stub-select-btn" @click="$emit(\'selection-change\', [{ id: 1 }, { id: 2 }])">勾选</button><div v-for="row in data || []" :key="row.id" class="stub-row">{{ row.name }}</div></div>'
  },
  'el-table-column': { template: '<div />' },
  'el-tag': { template: '<span class="stub-tag"><slot /></span>' }
}

function mountList() {
  return mount(MerchantProductList, {
    global: {
      stubs,
      mocks: { $router: { push: pushMock } }
    }
  })
}

describe('merchant/product/List.vue 商品管理页（F-08）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('勾选商品后渲染批量上架/下架/删除按钮', async () => {
    getMerchantProductsMock.mockResolvedValue({
      records: [
        { id: 1, name: '商品A', price: 100, stock: 5, sales: 2, status: 1 },
        { id: 2, name: '商品B', price: 50, stock: 0, sales: 0, status: 0 }
      ]
    })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(2)
    expect(wrapper.text()).toContain('商品A')
    // 未勾选时不显示批量操作按钮
    expect(wrapper.text()).not.toContain('批量上架')

    // 模拟勾选两行 → selection-change → 批量按钮出现
    await wrapper.find('.stub-select-btn').trigger('click')
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('批量上架')
    expect(text).toContain('批量下架')
    expect(text).toContain('批量删除')
    expect(text).toContain('发布商品')
    wrapper.unmount()
  })

  it('空态：无商品时页面正常渲染且无批量按钮', async () => {
    getMerchantProductsMock.mockResolvedValue({ records: [] })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(0)
    const text = wrapper.text()
    expect(text).toContain('商品管理')
    expect(text).toContain('发布商品')
    expect(text).not.toContain('批量上架')
    wrapper.unmount()
  })
})
