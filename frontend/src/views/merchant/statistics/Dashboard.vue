<template>
  <div class="merchant-statistics">
    <el-card>
      <h3>数据统计</h3>
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-label">销售总额</div>
            <div class="stat-value">{{ stats.totalAmount }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-label">订单数量</div>
            <div class="stat-value">{{ stats.totalOrders }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/merchant'

const stats = ref({ totalAmount: '0.00', totalOrders: 0 })
onMounted(async () => {
  try { stats.value = await request.getSalesStatistics() } catch {}
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 30px;
  background: #f5f5f5;
  border-radius: 8px;
}
.stat-label {
  color: #999;
  font-size: 14px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-top: 10px;
}
</style>
