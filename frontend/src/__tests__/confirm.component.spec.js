/**
 * F-T01~03 Confirm.vue 组件行为测试（批次4）。
 *
 * 真实挂载组件（@vue/test-utils），mock 外部依赖：
 * - api/order|user|coupon（request 层）
 * - vue-router / element-plus / cart store
 * 验证：挂载即估价（未选券 couponId=null）、金额区正确渲染、折扣行按数据展示。
 * 券字段映射（couponId/userCouponId）由 coupon.contract.spec.js 契约测试兜底。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { estimateOrderMock, createOrderMock, getAddressesMock, getUserCouponsMock, getPointsMock } = vi.hoisted(() => ({
  estimateOrderMock: vi.fn(),
  createOrderMock: vi.fn(),
  getAddressesMock: vi.fn(),
  getUserCouponsMock: vi.fn(),
  getPointsMock: vi.fn()
}))

// ---- mock 依赖 ----
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
}))

vi.mock('@/api/order', () => ({
  default: { estimateOrder: estimateOrderMock, createOrder: createOrderMock }
}))

vi.mock('@/api/user', () => ({
  default: { getAddresses: getAddressesMock }
}))

vi.mock('@/api/coupon', () => ({
  default: { getUserCoupons: getUserCouponsMock, getPoints: getPointsMock }
}))

// mock cart store：返回固定购物车数据（2 个店铺各 1 件选中商品）
vi.mock('@/store/cart', () => ({
  useCartStore: () => ({
    items: [
      {
        id: 1,
        productId: 10,
        shopId: 1,
        shopName: '店铺A',
        selected: true,
        productName: '商品A',
        productImage: '/a.jpg',
        price: 100,
        quantity: 1,
        specDesc: ''
      },
      {
        id: 2,
        productId: 20,
        shopId: 2,
        shopName: '店铺B',
        selected: true,
        productName: '商品B',
        productImage: '/b.jpg',
        price: 100,
        quantity: 1,
        specDesc: ''
      }
    ],
    clear: vi.fn()
  })
}))

import Confirm from '@/views/consumer/order/Confirm.vue'

function mountConfirm() {
  // shallowMount：子组件（el-*）自动 stub，聚焦组件自身渲染与行为逻辑
  return shallowMount(Confirm)
}

describe('Confirm.vue 组件行为（F-T01~03）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAddressesMock.mockResolvedValue([
      {
        id: 5,
        isDefault: 1,
        receiver: '测试',
        phone: '138',
        province: '四川',
        city: '攀枝花',
        district: '东区',
        detail: '学院'
      }
    ])
    getUserCouponsMock.mockResolvedValue([])
    getPointsMock.mockResolvedValue({ points: 0 })
    estimateOrderMock.mockResolvedValue([
      {
        shopId: 1,
        goodsAmount: 100,
        freightAmount: 10,
        promotionDiscountAmount: 0,
        couponDiscountAmount: 0,
        pointsDeductAmount: 0,
        payAmount: 110
      },
      {
        shopId: 2,
        goodsAmount: 100,
        freightAmount: 10,
        promotionDiscountAmount: 0,
        couponDiscountAmount: 0,
        pointsDeductAmount: 0,
        payAmount: 110
      }
    ])
  })

  it('F-T01 挂载后自动估价，未选券时 couponId 传 null', async () => {
    const wrapper = mountConfirm()
    await flushPromises()

    expect(estimateOrderMock).toHaveBeenCalled()
    const lastCall = estimateOrderMock.mock.calls[estimateOrderMock.mock.calls.length - 1]
    expect(lastCall[0]).toMatchObject({ couponId: null })
    expect(lastCall[0].productItems).toHaveLength(2)
    wrapper.unmount()
  })

  it('F-T02 金额区正确渲染：商品金额/运费/实付', async () => {
    const wrapper = mountConfirm()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('商品金额：')
    expect(text).toContain('¥220.00') // 两店 100+10 合计 payAmount
    expect(text).toContain('实付金额')
    wrapper.unmount()
  })

  it('F-T03 券折扣>0 时显示优惠券行，实付=商品-折扣', async () => {
    estimateOrderMock.mockResolvedValue([
      {
        shopId: 1,
        goodsAmount: 100,
        freightAmount: 10,
        promotionDiscountAmount: 0,
        couponDiscountAmount: 20,
        pointsDeductAmount: 0,
        payAmount: 90
      },
      {
        shopId: 2,
        goodsAmount: 100,
        freightAmount: 10,
        promotionDiscountAmount: 0,
        couponDiscountAmount: 0,
        pointsDeductAmount: 0,
        payAmount: 110
      }
    ])
    const wrapper = mountConfirm()
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('-¥20.00') // 券折扣行
    expect(text).toContain('¥200.00') // 90 + 110 实付
    wrapper.unmount()
  })
})
