<template>
  <div class="order-confirm">
    <div class="page-header">
      <h2>确认订单</h2>
      <span class="page-sub">请核对收货信息与商品明细</span>
    </div>

    <!-- 收货地址选择 -->
    <div class="ecom-card section-card">
      <div class="section-label">收货地址</div>
      <el-select
        v-model="selectedAddressId"
        placeholder="请选择收货地址"
        style="width: 100%"
        @change="onAddressChange"
      >
        <el-option
          v-for="addr in addresses"
          :key="addr.id"
          :label="`${addr.receiver} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`"
          :value="addr.id"
        />
      </el-select>
    </div>

    <!-- 优惠券/积分选择 + 价格明细 -->
    <div class="ecom-card section-card" v-if="orderGroups.length > 0">
      <div class="section-label">优惠与抵扣</div>
      <div class="discount-section">
        <div class="coupon-row">
          <span class="row-label">优惠券：</span>
          <!-- F-01 修复：:value 绑定完整 UserCouponVO 对象，estimate 用 c.couponId（模板ID），createOrder 用 c.id（记录ID） -->
          <el-select
            v-model="selectedCoupon"
            placeholder="选择优惠券"
            clearable
            style="width: 260px"
            @change="estimateOrder"
          >
            <el-option
              v-for="c in availableCoupons"
              :key="c.id"
              :label="`${c.name}（满${getCouponThreshold(c.discountRule)}减${getCouponDiscount(c.discountRule)}）`"
              :value="c"
            />
          </el-select>
        </div>
        <div class="points-row">
          <el-checkbox v-model="usePoints" @change="estimateOrder">使用积分抵扣</el-checkbox>
          <span v-if="usePoints" class="points-info">
            可用积分：{{ userPoints }}，可抵扣 ¥{{ maxPointsDeduct.toFixed(2) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 订单明细 -->
    <div class="ecom-card section-card" v-if="orderGroups.length > 0">
      <div class="section-label">订单明细</div>
      <div v-for="group in orderGroups" :key="group.shopId" class="order-group">
        <div class="shop-name">{{ group.shopName }}</div>
        <div v-for="item in group.items" :key="item.id" class="order-item">
          <img
            :src="resolvedItemImage(item)"
            :data-seed="item.productId"
            :alt="item.productName"
            loading="lazy"
            @error="handleImgError"
          />
          <div class="item-info">
            <div class="item-name">{{ item.productName }}</div>
            <div v-if="item.specText" class="item-spec">{{ item.specText }}</div>
          </div>
          <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
        </div>
        <div class="group-total">
          商品小计：¥{{ group.goodsAmount.toFixed(2) }} + 运费：¥{{ group.freightAmount.toFixed(2) }}
        </div>
      </div>
      <div class="price-breakdown">
        <div class="breakdown-row">
          <span>商品金额：</span><span>¥{{ totalGoodsAmount.toFixed(2) }}</span>
        </div>
        <div class="breakdown-row">
          <span>运费：</span><span>¥{{ totalFreightAmount.toFixed(2) }}</span>
        </div>
        <div class="breakdown-row" v-if="totalPromotionDiscount > 0">
          <span>促销优惠：</span><span class="discount">-¥{{ totalPromotionDiscount.toFixed(2) }}</span>
        </div>
        <div class="breakdown-row" v-if="totalCouponDiscount > 0">
          <span>优惠券：</span><span class="discount">-¥{{ totalCouponDiscount.toFixed(2) }}</span>
        </div>
        <div class="breakdown-row" v-if="totalPointsDeduct > 0">
          <span>积分抵扣：</span><span class="discount">-¥{{ totalPointsDeduct.toFixed(2) }}</span>
        </div>
        <div class="breakdown-row total">
          <span>实付金额：</span><strong>¥{{ totalPayAmount.toFixed(2) }}</strong>
        </div>
      </div>
    </div>

    <div class="submit-bar" v-if="orderGroups.length > 0">
      <span class="submit-total"
        >实付金额：<strong>¥{{ totalPayAmount.toFixed(2) }}</strong></span
      >
      <el-button type="primary" size="large" :loading="submitting" @click="submitOrder"> 提交订单 </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/store/cart'
import request from '@/api/order'
import userRequest from '@/api/user'
import couponRequest from '@/api/coupon'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const router = useRouter()
const cartStore = useCartStore()
const orderGroups = ref([])
const submitting = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const availableCoupons = ref([])
const selectedCoupon = ref(null)
const usePoints = ref(false)
const userPoints = ref(0)
const maxPointsDeduct = ref(0)
const estimateResult = ref([])

// 明细金额
const totalGoodsAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.goodsAmount || 0), 0))
const totalFreightAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.freightAmount || 0), 0))
const totalPromotionDiscount = computed(() =>
  estimateResult.value.reduce((s, g) => s + (g.promotionDiscountAmount || 0), 0)
)
const totalCouponDiscount = computed(() => estimateResult.value.reduce((s, g) => s + (g.couponDiscountAmount || 0), 0))
const totalPointsDeduct = computed(() => estimateResult.value.reduce((s, g) => s + (g.pointsDeductAmount || 0), 0))
const totalPayAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.payAmount || 0), 0))

/** 商品图兜底。
 *  FRONT-05 修复：CartVO 返回字段为 mainImage（此前误用 productImage，图恒为占位图）。 */
function resolvedItemImage(item) {
  return resolveImg(item.mainImage || item.productImage || '', item.productId ?? item.productName ?? 'default', 120, 120)
}

function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 120, 120)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
}

onMounted(async () => {
  // FRONT-08 修复：刷新/直达本页时购物车可能尚未加载（父布局异步 fetchList），
  // 先确保购物车数据就绪再构建订单组，避免订单明细区空白无法下单
  if ((!cartStore.items || cartStore.items.length === 0) && typeof cartStore.fetchList === 'function') {
    await cartStore.fetchList()
  }

  // 加载收货地址
  try {
    const addrRes = await userRequest.getAddresses()
    addresses.value = addrRes.records || addrRes || []
    const defAddr = addresses.value.find((a) => a.isDefault === 1) || addresses.value[0]
    selectedAddressId.value = defAddr ? defAddr.id : null
  } catch {
    addresses.value = []
  }

  // 加载可用优惠券（仅未使用的）
  try {
    const coupons = await couponRequest.getUserCoupons({ status: 0 })
    availableCoupons.value = coupons || []
  } catch {
    availableCoupons.value = []
  }

  // 加载用户积分
  try {
    const pointsData = await couponRequest.getPoints()
    userPoints.value = pointsData.points || 0
  } catch {
    userPoints.value = 0
  }

  buildOrderGroups()
  await estimateOrder()
})

// FRONT-08 修复：购物车数据就绪/变化（如从购物车跳转）后兜底重算，防止空白页
watch(
  () => cartStore.items,
  () => {
    if (cartStore.items && cartStore.items.length > 0) {
      buildOrderGroups()
      estimateOrder()
    }
  }
)

function buildOrderGroups() {
  const items = cartStore.items.filter((i) => i.selected)
  const groups = {}
  items.forEach((item) => {
    // FRONT-06 修复：按 shopId 分组（后端 CartVO 已返回 shopId/shopName）；
    // 兼容数据缺失时按购物车项 id 兜底，避免全部商品合并进 undefined 组
    const key = item.shopId ?? `item-${item.id}`
    if (!groups[key]) {
      // B4-FR-01 修复：orderGroups 需含 freightAmount 兜底（模板 group-total 直接 toFixed，
      // 缺失会导致确认页渲染崩溃）；真实值由估价接口回填
      groups[key] = {
        shopId: item.shopId ?? null,
        shopName: item.shopName || '店铺',
        items: [],
        goodsAmount: 0,
        freightAmount: 0
      }
    }
    groups[key].items.push(item)
    groups[key].goodsAmount += item.price * item.quantity
  })
  orderGroups.value = Object.values(groups)
}

/** FRONT-06/07 修复：将后端估价结果（按店分组的运费/商品金额/实付）回填到本地分组，
 *  保证确认页组级小计与底部合计口径一致（此前组内运费恒为 0、金额为本地估算）。 */
function mergeEstimateToGroups(estimateGroups) {
  if (!Array.isArray(estimateGroups)) return
  const map = {}
  estimateGroups.forEach((g) => {
    if (g.shopId != null) map[g.shopId] = g
  })
  orderGroups.value.forEach((group) => {
    const est = group.shopId != null ? map[group.shopId] : null
    if (est) {
      if (typeof est.freightAmount === 'number') group.freightAmount = est.freightAmount
      if (typeof est.goodsAmount === 'number') group.goodsAmount = est.goodsAmount
    }
  })
}

async function estimateOrder() {
  if (!selectedAddressId.value || orderGroups.value.length === 0) {
    estimateResult.value = []
    return
  }
  try {
    const allItems = orderGroups.value.flatMap((g) =>
      g.items.map((item) => ({
        productId: item.productId,
        skuId: item.skuId || null,
        quantity: item.quantity
      }))
    )
    // F-01 修复：估价传 couponId = UserCouponVO.couponId（Coupon 模板 ID），
    // 后端按模板 ID 查券计算折扣
    const data = await request.estimateOrder({
      addressId: selectedAddressId.value,
      productItems: allItems,
      couponId: selectedCoupon.value?.couponId || null,
      usePoints: usePoints.value || null
    })
    estimateResult.value = data || []
    // FRONT-06/07 修复：估价结果回填分组（运费/金额以后端为准，显示金额=实付金额）
    mergeEstimateToGroups(estimateResult.value)
  } catch {
    estimateResult.value = []
  }
}

async function submitOrder() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const cartItemIds = cartStore.items.filter((i) => i.selected).map((i) => i.id)
    // F-01 修复：createOrder 传 userCouponId = UserCouponVO.id（UserCoupon 记录 ID），
    // 后端 CreateOrderDTO 字段为 userCouponId，此前误传 couponId 导致券不生效/不核销
    const orders = await request.createOrder({
      cartItemIds,
      addressId: selectedAddressId.value,
      userCouponId: selectedCoupon.value?.id || null,
      usePoints: usePoints.value || null,
      requestId: generateRequestId()
    })
    ElMessage.success('订单创建成功')
    if (orders.length === 1) {
      router.push(`/order/pay/${orders[0].orderId}`)
    } else {
      sessionStorage.setItem('pendingOrders', JSON.stringify(orders))
      router.push('/order/batch-pay')
    }
  } catch (e) {
    // error handled
  } finally {
    submitting.value = false
  }
}

function onAddressChange() {
  estimateOrder()
}

function getCouponThreshold(rule) {
  if (!rule) return 0
  try {
    const obj = JSON.parse(rule)
    return obj.threshold || 0
  } catch {
    return 0
  }
}

function getCouponDiscount(rule) {
  if (!rule) return 0
  try {
    const obj = JSON.parse(rule)
    return obj.discount || 0
  } catch {
    return 0
  }
}

function generateRequestId() {
  return 'req_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}
</script>

<style scoped>
.order-confirm {
  max-width: 860px;
  margin: 24px auto;
  padding: 0 24px;
}
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 22px;
  color: #0f172a;
}
.page-sub {
  font-size: 13px;
  color: #94a3b8;
}
.section-card {
  padding: 20px 24px;
  margin-bottom: 16px;
}
.section-label {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f6f8fb;
}
.discount-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.coupon-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.points-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.row-label {
  font-weight: 500;
  color: #334155;
}
.points-info {
  color: #64748b;
  font-size: 14px;
}
.order-group {
  background: #f8fafc;
  padding: 16px 18px;
  border-radius: 12px;
  margin-bottom: 16px;
}
.shop-name {
  font-weight: 700;
  margin-bottom: 10px;
  color: #0f172a;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #eef0f4;
}
.order-item:last-child {
  border-bottom: none;
}
.order-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #eef0f4;
  background: #fff;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-spec {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 4px;
}
.item-price {
  color: #64748b;
  font-size: 13px;
}
.group-total {
  text-align: right;
  padding-top: 10px;
  color: #64748b;
  font-size: 13px;
}
.price-breakdown {
  margin-top: 6px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
}
.breakdown-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  color: #64748b;
  font-size: 14px;
}
.breakdown-row .discount {
  color: #ef4444;
}
.breakdown-row.total {
  font-size: 16px;
  color: #0f172a;
  font-weight: 600;
  padding-top: 12px;
  border-top: 1px solid #eef0f4;
  margin-top: 8px;
}
.breakdown-row.total strong {
  color: #ef4444;
  font-size: 24px;
}
.submit-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  background: #fff;
  padding: 18px 24px;
  border-radius: 16px;
  border: 1px solid #eef0f4;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  position: sticky;
  bottom: 16px;
  z-index: 10;
}
.submit-total {
  font-size: 15px;
  color: #0f172a;
  font-weight: 600;
}
.submit-total strong {
  font-size: 24px;
  color: #ef4444;
}
</style>
