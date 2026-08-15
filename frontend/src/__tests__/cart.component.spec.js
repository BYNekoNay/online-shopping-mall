/**
 * F-T04~05 购物车页组件测试（批次4）。
 *
 * 真实挂载 Cart 页（pinia store 真实例 + mock api/cart + 路由/消息组件）：
 * - F-T04：购物车空态渲染
 * - F-T05：修改数量调用 updateCartItem（携带新数量）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getCartMock, updateCartItemMock, deleteCartItemMock, messageMock } = vi.hoisted(() => ({
  getCartMock: vi.fn(),
  updateCartItemMock: vi.fn(),
  deleteCartItemMock: vi.fn(),
  messageMock: vi.fn(),
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRouter: () => ({ push: vi.fn() }) }
})

vi.mock('element-plus', () => ({
  ElMessage: { success: messageMock, warning: messageMock },
  ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') },
}))

vi.mock('@/api/cart', () => ({
  default: {
    getCart: getCartMock,
    updateCartItem: updateCartItemMock,
    deleteCartItem: deleteCartItemMock,
  },
}))

// store/cart.js 内部走 @/utils/request（fetchList → request.get('/cart')），需一并 mock
vi.mock('@/utils/request', () => ({
  default: { get: getCartMock, put: vi.fn(), delete: vi.fn() },
}))

import CartIndex from '@/views/consumer/cart/Index.vue'
import { useCartStore } from '@/store/cart'

const InputNumberStub = {
  props: ['modelValue', 'min', 'max'],
  emits: ['update:modelValue', 'change'],
  template: '<button class="stub-input" @click="$emit(\'change\', modelValue)">+</button>',
}

const stubs = {
  'el-input-number': InputNumberStub,
  'el-card': { template: '<div class="stub-card"><slot /></div>' },
  'el-empty': {
    props: ['description'],
    template: '<div class="stub-empty">{{ description }}<slot /></div>',
  },
  'el-button': { template: '<button><slot /></button>' },
  'el-checkbox': { template: '<label><slot /></label>' },
  'el-row': { template: '<div><slot /></div>' },
  'el-col': { template: '<div><slot /></div>' },
}

function mountCart() {
  setActivePinia(createPinia())
  const wrapper = mount(CartIndex, { global: { stubs } })
  return { wrapper, store: useCartStore() }
}

describe('购物车页（F-T04~05）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('F-T04 购物车为空时渲染空态提示', async () => {
    getCartMock.mockResolvedValue([])
    const { wrapper } = mountCart()
    await flushPromises()

    expect(wrapper.find('.stub-empty').exists()).toBe(true)
    expect(wrapper.text()).toContain('购物车空空如也')
    wrapper.unmount()
  })

  it('F-T05 修改数量调用 updateCartItem（携带新数量）', async () => {
    getCartMock.mockResolvedValue([
      { id: 1, productId: 10, productName: '商品A', price: 100, quantity: 1, stock: 5, selected: 1 },
    ])
    updateCartItemMock.mockResolvedValue({ code: 0 })
    const { wrapper, store } = mountCart()

    // 触发 store 加载购物车（fetchList → utils/request.get('/cart')）
    await store.fetchList()
    await flushPromises()

    // 直接改 store 中的数量（等价用户点击 +1），再触发 change；
    // nextTick 确保模板重新渲染（item 对象为 quantity=3 的新引用）
    store.updateItem(1, { quantity: 3 })
    await nextTick()

    const input = wrapper.find('.stub-input')
    expect(input.exists()).toBe(true)
    await input.trigger('click') // emit change(3)

    expect(updateCartItemMock).toHaveBeenCalledWith(1, { quantity: 3 })
    wrapper.unmount()
  })
})
