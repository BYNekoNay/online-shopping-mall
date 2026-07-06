<template>
  <div class="merchant-orders">
    <el-card>
      <h3>订单管理</h3>
      <el-table :data="orders" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ row.payAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag>{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/merchant'

const orders = ref([])
const statusMap = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已收货', 4: '已完成', 5: '已取消' }

onMounted(async () => {
  try { orders.value = await request.getMerchantOrders() } catch {}
})
</script>
