<template>
  <div class="order-batch-pay">
    <div class="page-header">
      <h2>订单支付</h2>
      <span class="page-sub">多店铺订单，请依次完成支付</span>
    </div>
    <div class="batch-tip">
      <el-alert title="多店铺订单提示" type="info" :closable="false">
        <template #default>
          本次共创建 {{ orders.length }} 个子订单，请依次完成支付。您也可以稍后在"我的订单"中分别支付。
        </template>
      </el-alert>
    </div>
    <div v-for="order in orders" :key="order.orderId" class="order-card">
      <div class="order-header">
        <span class="order-no">订单号：{{ order.orderNo }}</span>
        <span class="order-amount">¥{{ order.payAmount }}</span>
      </div>
      <div class="order-items">
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <img
            :src="resolvedItemImage(item)"
            :data-seed="item.productId"
            :alt="item.productName"
            loading="lazy"
            @error="handleImgError"
          />
          <div class="item-info">
            <div class="item-name">{{ item.productName }}</div>
            <div v-if="item.isGift" class="gift-tag">赠品</div>
          </div>
          <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
        </div>
      </div>
      <div class="order-footer">
        <span class="pay-amount"
          >实付：<strong>¥{{ order.payAmount }}</strong></span
        >
        <el-button type="primary" :loading="payingId === order.orderId" @click="handlePay(order)"> 去支付 </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/order'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const router = useRouter()
const orders = ref([])
const payingId = ref(null)

/** 商品图兜底。 */
function resolvedItemImage(item) {
  return resolveImg(item.productImage || '', item.productId ?? item.productName ?? 'default', 120, 120)
}

function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 120, 120)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
}

onMounted(() => {
  // 从路由 state 或 sessionStorage 恢复订单列表
  // H-20 修复：本路由无 :id 参数，原兜底 getOrderDetail(route.params.id) 的 id 恒为 undefined；
  // 订单列表仅从 sessionStorage 恢复，缺失时跳回订单列表
  try {
    const stored = sessionStorage.getItem('pendingOrders')
    if (stored) {
      orders.value = JSON.parse(stored)
      return
    }
  } catch {
    // 存储数据损坏，按无数据处理
  }
  ElMessage.warning('未找到待支付订单，请从订单列表进入')
  router.replace('/orders')
})

async function handlePay(order) {
  payingId.value = order.orderId
  try {
    await request.payOrder(order.orderId, { payType: 1 })
    ElMessage.success('支付成功')
    // 移除已支付订单
    orders.value = orders.value.filter((o) => o.orderId !== order.orderId)
    if (orders.value.length === 0) {
      router.push('/orders')
    }
  } catch {
    ElMessage.error('支付失败')
  } finally {
    payingId.value = null
  }
}
</script>

<style scoped>
.order-batch-pay {
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
.batch-tip {
  margin-bottom: 16px;
}
.order-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  padding: 18px 22px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.2s, border-color 0.2s;
}
.order-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.08);
}
.order-header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f6f8fb;
}
.order-no {
  font-size: 13px;
  color: #334155;
}
.order-amount {
  color: #ef4444;
  font-weight: 700;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f6f8fb;
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
  background: #f8fafc;
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
.item-price {
  color: #64748b;
  font-size: 13px;
}
.gift-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 5px;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f6f8fb;
}
.pay-amount {
  font-size: 13px;
  color: #334155;
}
.pay-amount strong {
  font-size: 18px;
  color: #ef4444;
}
</style>
