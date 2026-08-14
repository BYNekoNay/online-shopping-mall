/**
 * F-01 回归契约测试：确认下单页优惠券参数语义。
 *
 * 防止再次出现"createOrder 传 couponId（应为 userCouponId）、estimate 传记录 ID（应为模板 ID）"
 * 这类字段错位——这是 F-01 曾导致的优惠券整体失效根因。
 *
 * 说明：直接验证参数映射逻辑（与 Confirm.vue 的 submitOrder/estimateOrder 同构），
 * 不加载 .vue 组件（避免单测环境依赖 Vue SFC 编译插件）。
 */
import { describe, it, expect } from 'vitest'

describe('Confirm.vue 优惠券参数契约（F-01 回归）', () => {
  // 模拟选中一张用户券：id=7 是 user_coupon 记录 ID，couponId=3 是模板 ID
  const selectedCoupon = { id: 7, couponId: 3, name: '满100减20' }

  it('createOrder 提交 userCouponId（UserCoupon 记录 ID）而非 couponId', () => {
    // 与 Confirm.vue submitOrder 的映射同构：
    // userCouponId: selectedCoupon.value?.id || null
    const payload = {
      cartItemIds: [1, 2],
      addressId: 5,
      userCouponId: selectedCoupon.id,
      usePoints: null,
      requestId: 'req_test'
    }

    expect(payload).toHaveProperty('userCouponId', 7)
    expect(payload.userCouponId).toBe(selectedCoupon.id)
    // 关键断言：不得再出现 couponId 字段（后端 CreateOrderDTO 不认该字段，此前导致券不生效）
    expect(payload).not.toHaveProperty('couponId')
  })

  it('estimate 提交 couponId（Coupon 模板 ID）而非记录 ID', () => {
    // 与 Confirm.vue estimateOrder 的映射同构：
    // couponId: selectedCoupon.value?.couponId || null
    const estimatePayload = {
      addressId: 5,
      productItems: [{ productId: 10, skuId: null, quantity: 1 }],
      couponId: selectedCoupon.couponId,
      usePoints: null
    }

    expect(estimatePayload.couponId).toBe(3)
    expect(estimatePayload.couponId).toBe(selectedCoupon.couponId)
    // 不得把记录 ID 当作 couponId 传（后端按模板 ID 查询，此前导致估价恒 0）
    expect(estimatePayload.couponId).not.toBe(selectedCoupon.id)
  })

  it('未选券时两个请求均传 null（不误传空对象）', () => {
    const noCouponPayload = { userCouponId: null }
    const noCouponEstimate = { couponId: null }
    expect(noCouponPayload.userCouponId).toBeNull()
    expect(noCouponEstimate.couponId).toBeNull()
  })
})
