<template>
  <div class="order-pay">
    <h2>订单支付</h2>
    <div class="order-info">
      <p>订单金额：<strong>¥{{ orderDetail.payAmount }}</strong></p>
    </div>
    <div class="pay-methods">
      <el-radio-group v-model="payType">
        <el-radio :label="1">余额支付</el-radio>
        <el-radio :label="2">模拟支付宝</el-radio>
      </el-radio-group>
    </div>
    <el-button type="primary" size="large" :loading="paying" @click="handlePay" style="margin-top: 20px;">
      确认支付
    </el-button>
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
    ElMessage.error('Failed to load order')
  }
})

async function handlePay() {
  paying.value = true
  try {
    const res = await request.payOrder(route.params.id, { payType: payType.value })
    payResult.value = res
    ElMessage.success('Payment successful')
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
  max-width: 600px;
  margin: 20px auto;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
}
.order-info {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}
.pay-methods {
  margin-top: 20px;
}
</style>
