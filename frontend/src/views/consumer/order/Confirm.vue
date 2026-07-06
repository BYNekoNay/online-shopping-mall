<template>
  <div class="order-confirm">
    <h2>确认订单</h2>
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
    <div class="submit-bar">
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

const router = useRouter()
const cartStore = useCartStore()
const orderGroups = ref([])
const submitting = ref(false)
const totalPayAmount = computed(() => orderGroups.value.reduce((sum, g) => sum + g.payAmount, 0))

onMounted(() => {
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
})

async function submitOrder() {
  submitting.value = true
  try {
    const cartItemIds = cartStore.items.filter(i => i.selected).map(i => i.id)
    const orders = await request.createOrder({ cartItemIds, requestId: generateRequestId() })
    ElMessage.success('Order created')
    if (orders.length === 1) {
      router.push(`/order/pay/${orders[0].orderId}`)
    } else {
      // multi-shop: show batch payment page (simplified here)
      router.push(`/order/pay/${orders[0].orderId}`)
    }
  } catch {
    // error handled
  } finally {
    submitting.value = false
  }
}

function generateRequestId() {
  return 'req_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}
</script>

<style scoped>
.order-confirm {
  max-width: 800px;
  margin: 20px auto;
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
