<template>
  <div class="order-confirm">
    <h2>确认订单</h2>

    <!-- 收货地址选择 -->
    <el-card style="margin-bottom: 15px;">
      <div class="address-section">
        <span class="address-label">收货地址：</span>
        <el-select v-model="selectedAddressId" placeholder="请选择收货地址" style="width: 100%;" @change="onAddressChange">
          <el-option
            v-for="addr in addresses"
            :key="addr.id"
            :label="`${addr.receiver} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`"
            :value="addr.id"
          />
        </el-select>
      </div>
    </el-card>

    <!-- 优惠券/积分选择 + 价格明细 -->
    <el-card style="margin-bottom: 15px;" v-if="orderGroups.length > 0">
      <h3>优惠与抵扣</h3>
      <div class="discount-section">
        <div class="coupon-row">
          <span>优惠券：</span>
          <el-select v-model="selectedCouponId" placeholder="选择优惠券" clearable style="width: 260px;">
            <el-option
              v-for="c in availableCoupons"
              :key="c.id"
              :label="`${c.name}（满${getCouponThreshold(c.discountRule)}减${getCouponDiscount(c.discountRule)}）`"
              :value="c.id"
            />
          </el-select>
        </div>
        <div class="points-row">
          <el-checkbox v-model="usePoints">使用积分抵扣</el-checkbox>
          <span v-if="usePoints" class="points-info">
            可用积分：{{ userPoints }}，可抵扣 ¥{{ maxPointsDeduct.toFixed(2) }}
          </span>
        </div>
      </div>
    </el-card>

    <!-- 价格明细 -->
    <el-card style="margin-bottom: 15px;" v-if="orderGroups.length > 0">
      <h3>订单明细</h3>
      <div v-for="group in orderGroups" :key="group.shopId" class="order-group">
        <div class="shop-name">{{ group.shopName }}</div>
        <div v-for="item in group.items" :key="item.id" class="order-item">
          <img :src="item.productImage" />
          <div class="item-info">
            <div>{{ item.productName }}</div>
            <div v-if="item.specDesc">{{ item.specDesc }}</div>
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
    </el-card>

    <div class="submit-bar" v-if="orderGroups.length > 0">
      <span>实付金额：<strong>¥{{ totalPayAmount.toFixed(2) }}</strong></span>
      <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/store/cart'
import request from '@/api/order'
import userRequest from '@/api/user'
import couponRequest from '@/api/coupon'

const router = useRouter()
const cartStore = useCartStore()
const orderGroups = ref([])
const submitting = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const availableCoupons = ref([])
const selectedCouponId = ref(null)
const usePoints = ref(false)
const userPoints = ref(0)
const maxPointsDeduct = ref(0)
const estimateResult = ref([])

// 明细金额
const totalGoodsAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.goodsAmount || 0), 0))
const totalFreightAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.freightAmount || 0), 0))
const totalPromotionDiscount = computed(() => estimateResult.value.reduce((s, g) => s + (g.promotionDiscountAmount || 0), 0))
const totalCouponDiscount = computed(() => estimateResult.value.reduce((s, g) => s + (g.couponDiscountAmount || 0), 0))
const totalPointsDeduct = computed(() => estimateResult.value.reduce((s, g) => s + (g.pointsDeductAmount || 0), 0))
const totalPayAmount = computed(() => estimateResult.value.reduce((s, g) => s + (g.payAmount || 0), 0))

onMounted(async () => {
  // 加载收货地址
  try {
    const addrRes = await userRequest.getAddresses()
    addresses.value = addrRes.records || addrRes || []
    const defAddr = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
    selectedAddressId.value = defAddr ? defAddr.id : null
  } catch {
    addresses.value = []
  }

  // 加载可用优惠券
  try {
    const coupons = await couponRequest.getUserCoupons()
    availableCoupons.value = coupons || []
  } catch {
    availableCoupons.value = []
  }

  // 加载用户积分
  try {
    const pointsData = await userRequest.getPoints()
    userPoints.value = pointsData.points || 0
  } catch {
    userPoints.value = 0
  }

  buildOrderGroups()
  await estimateOrder()
})

function buildOrderGroups() {
  const items = cartStore.items.filter(i => i.selected)
  const groups = {}
  items.forEach(item => {
    if (!groups[item.shopId]) {
      groups[item.shopId] = { shopId: item.shopId, shopName: item.shopName || '店铺', items: [], goodsAmount: 0 }
    }
    groups[item.shopId].items.push(item)
    groups[item.shopId].goodsAmount += item.price * item.quantity
  })
  orderGroups.value = Object.values(groups)
}

async function estimateOrder() {
  if (!selectedAddressId.value || orderGroups.value.length === 0) {
    estimateResult.value = []
    return
  }
  try {
    const allItems = orderGroups.value.flatMap(g =>
      g.items.map(item => ({
        productId: item.productId,
        skuId: item.skuId || null,
        quantity: item.quantity
      }))
    )
    const data = await request.estimateOrder({
      addressId: selectedAddressId.value,
      productItems: allItems,
      couponId: selectedCouponId.value || null,
      usePoints: usePoints.value || null
    })
    estimateResult.value = data || []
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
    const cartItemIds = cartStore.items.filter(i => i.selected).map(i => i.id)
    const orders = await request.createOrder({
      cartItemIds,
      addressId: selectedAddressId.value,
      couponId: selectedCouponId.value || null,
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
  } catch { return 0 }
}

function getCouponDiscount(rule) {
  if (!rule) return 0
  try {
    const obj = JSON.parse(rule)
    return obj.discount || 0
  } catch { return 0 }
}

function generateRequestId() {
  return 'req_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}
</script>

<style scoped>
.address-section {
  display: flex;
  align-items: center;
  gap: 10px;
}
.address-label {
  font-weight: bold;
  white-space: nowrap;
}
.order-confirm {
  max-width: 800px;
  margin: 20px auto;
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
.points-info {
  color: #666;
  font-size: 14px;
}
.order-group {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 15px;
}
.shop-name {
  font-weight: bold;
  margin-bottom: 10px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.order-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}
.item-info {
  flex: 1;
}
.item-price {
  color: #666;
}
.group-total {
  text-align: right;
  padding: 10px 0;
  color: #666;
}
.price-breakdown {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid #f5f5f5;
}
.breakdown-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  color: #666;
}
.breakdown-row .discount {
  color: #f56c6c;
}
.breakdown-row.total {
  font-size: 18px;
  color: #333;
  padding-top: 10px;
  border-top: 1px solid #eee;
  margin-top: 8px;
}
.breakdown-row.total strong {
  color: #f56c6c;
  font-size: 24px;
}
.submit-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}
.submit-bar strong {
  font-size: 24px;
  color: #f56c6c;
}
</style>
