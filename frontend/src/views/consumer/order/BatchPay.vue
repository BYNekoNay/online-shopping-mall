<template>
  <div class="order-batch-pay">
    <h2>订单支付</h2>
    <el-card style="margin-bottom: 15px;">
      <div class="batch-tip">
        <el-alert title="多店铺订单提示" type="info" :closable="false">
          <template #default>
            本次共创建 {{ orders.length }} 个子订单，请依次完成支付。您也可以稍后在"我的订单"中分别支付。
          </template>
        </el-alert>
      </div>
    </el-card>
    <div v-for="order in orders" :key="order.orderId" class="order-card">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <span class="order-amount">¥{{ order.payAmount }}</span>
      </div>
      <div class="order-items">
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <img :src="item.productImage" />
          <div class="item-info">
            <div>{{ item.productName }}</div>
            <div v-if="item.isGift" class="gift-tag">赠品</div>
          </div>
          <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
        </div>
      </div>
      <div class="order-footer">
        <span>实付：<strong>¥{{ order.payAmount }}</strong></span>
        <el-button type="primary" :loading="payingId === order.orderId" @click="handlePay(order)">
          去支付
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/order'

const route = useRoute()
const router = useRouter()
const orders = ref([])
const payingId = ref(null)

onMounted(async () => {
  // 从路由 state 或 localStorage 恢复订单列表
  try {
    const stored = sessionStorage.getItem('pendingOrders')
    if (stored) {
      orders.value = JSON.parse(stored)
    } else {
      // fallback: 通过 orderId 获取详情
      const detail = await request.getOrderDetail(route.params.id)
      orders.value = [detail]
    }
  } catch {
    ElMessage.error('加载订单失败')
  }
})

async function handlePay(order) {
  payingId.value = order.orderId
  try {
    await request.payOrder(order.orderId, { payType: 1 })
    ElMessage.success('支付成功')
    // 移除已支付订单
    orders.value = orders.value.filter(o => o.orderId !== order.orderId)
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
  max-width: 800px;
  margin: 20px auto;
}
.batch-tip {
  margin-bottom: 15px;
}
.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
}
.order-header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
}
.order-amount {
  color: #f56c6c;
  font-weight: bold;
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
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
</style>
