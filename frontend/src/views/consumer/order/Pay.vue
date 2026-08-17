<template>
  <div class="order-pay">
    <div class="pay-panel">
      <div class="pay-header">
        <h2>订单支付</h2>
        <span class="pay-sub">选择支付方式完成付款</span>
      </div>
      <div class="order-info">
        <span class="info-label">订单金额</span>
        <span class="pay-amount">¥{{ orderDetail.payAmount }}</span>
      </div>
      <div class="pay-methods">
        <div class="method-title">支付方式</div>
        <el-radio-group v-model="payType" class="method-group">
          <el-radio :label="1" class="method-item" border>
            <span class="method-emoji">💰</span>
            <span>余额支付</span>
          </el-radio>
          <el-radio :label="2" class="method-item" border>
            <span class="method-emoji">🛰️</span>
            <span>模拟支付宝</span>
          </el-radio>
        </el-radio-group>
      </div>
      <el-button type="primary" size="large" :loading="paying" class="pay-btn" @click="handlePay">
        确认支付
      </el-button>
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
const orderDetail = ref({})
const payType = ref(1)
const paying = ref(false)
const payResult = ref(null)

onMounted(async () => {
  try {
    orderDetail.value = await request.getOrderDetail(route.params.id)
  } catch {
    ElMessage.error('加载订单失败')
  }
})

async function handlePay() {
  paying.value = true
  try {
    const res = await request.payOrder(route.params.id, { payType: payType.value })
    payResult.value = res
    ElMessage.success('支付成功')
    router.push('/orders')
  } catch {
    // error handled
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.order-pay {
  max-width: 640px;
  margin: 24px auto;
  padding: 0 24px;
}
.pay-panel {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}
.pay-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}
.pay-header h2 {
  font-size: 22px;
  color: #0f172a;
}
.pay-sub {
  font-size: 13px;
  color: #94a3b8;
}
.order-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f6f8fb;
}
.info-label {
  color: #64748b;
  font-size: 14px;
}
.pay-amount {
  font-size: 30px;
  font-weight: 800;
  color: #ef4444;
}
.pay-methods {
  margin-top: 20px;
}
.method-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}
.method-group {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}
.method-item {
  height: auto;
  padding: 14px 22px;
  margin-right: 0;
  border-radius: 12px;
  font-size: 14px;
}
.method-emoji {
  margin-right: 8px;
  font-size: 18px;
}
.pay-btn {
  width: 100%;
  margin-top: 28px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}
</style>
