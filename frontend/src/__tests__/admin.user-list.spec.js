/**
 * F-08 admin 用户列表页 smoke 测试。
 *
 * 真实挂载 admin/user/List.vue，mock api/admin + element-plus（命令式组件）：
 * - 表格渲染：getUsers 返回 2 条记录 → el-table stub 渲染行（用户名）→ 分页 total 展示
 * - 空态：getUsers 返回空 records → 无表格行，页面正常渲染
 *
 * el-* 组件在 vitest 中不自动注册（批次 2 按需引入后 main.js 不再全量注册），
 * 页面模板里的 el-* 一律用自定义 stub 兜底；el-table stub 直接渲染 data 行，
 * 使「数据是否流入表格」在 smoke 层可见。
 * F-09 后页面搜索区/表格/弹窗收敛到 AppSearchForm/AppTable/AppDialog（真实渲染），
 * 其内部 el-* 同样由全局 stub 兜底（AppSearchForm 的 el-form/el-form-item 等）。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getUsersMock, messageMock, confirmMock } = vi.hoisted(() => ({
  getUsersMock: vi.fn(),
  messageMock: vi.fn(),
  confirmMock: vi.fn().mockResolvedValue('confirm')
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: messageMock, error: messageMock, warning: messageMock },
  ElMessageBox: { confirm: confirmMock }
}))

vi.mock('@/api/admin', () => ({
  default: {
    getUsers: getUsersMock,
    updateUserStatus: vi.fn(),
    updateUserRole: vi.fn(),
    getUserDetail: vi.fn()
  }
}))

import AdminUserList from '@/views/admin/user/List.vue'

const stubs = {
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  // F-09 后搜索区收敛到 AppSearchForm（真实渲染），内部 el-form/el-form-item/el-date-picker 需 stub 兜底
  'el-form': { template: '<div class="stub-form"><slot /></div>' },
  'el-form-item': { template: '<div class="stub-form-item"><slot /></div>' },
  'el-date-picker': { template: '<div class="stub-date-picker" />' },
  'el-select': { template: '<div class="stub-select"><slot /></div>' },
  'el-option': { template: '<div />' },
  'el-input': { template: '<div class="stub-input"><slot /></div>' },
  'el-button': { template: '<button class="stub-button"><slot /></button>' },
  'el-table': {
    props: ['data'],
    template:
      '<div class="stub-table"><slot /><div v-for="row in data || []" :key="row.id" class="stub-row">{{ row.username }}</div></div>'
  },
  'el-table-column': { template: '<div />' },
  'el-tag': { template: '<span class="stub-tag"><slot /></span>' },
  'el-pagination': {
    props: ['total'],
    template: '<div class="stub-pagination">共 {{ total }} 条</div>'
  },
  'el-dialog': {
    props: ['modelValue'],
    template: '<div class="stub-dialog" v-if="modelValue"><slot /></div>'
  },
  'el-descriptions': { template: '<div><slot /></div>' },
  'el-descriptions-item': { template: '<div><slot /></div>' }
}

function mountList() {
  return mount(AdminUserList, { global: { stubs } })
}

describe('admin/user/List.vue 用户列表页（F-08）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('表格渲染：展示用户名行数据与分页总数', async () => {
    getUsersMock.mockResolvedValue({
      records: [
        { id: 1, username: 'alice', nickname: '爱丽丝', role: 1, status: 1, createTime: '2024-01-01 10:00:00' },
        { id: 2, username: 'bob', nickname: '鲍勃', role: 2, status: 0, createTime: '2024-02-01 10:00:00' }
      ],
      total: 2
    })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(2)
    const text = wrapper.text()
    expect(text).toContain('用户管理')
    expect(text).toContain('alice')
    expect(text).toContain('bob')
    expect(text).toContain('共 2 条')
    wrapper.unmount()
  })

  it('空态：无数据时页面正常渲染且无表格行', async () => {
    getUsersMock.mockResolvedValue({ records: [], total: 0 })
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.findAll('.stub-row')).toHaveLength(0)
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('共 0 条')
    wrapper.unmount()
  })
})
