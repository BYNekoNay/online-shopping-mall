<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6"><el-card><div class="stat"><div class="label">GMV</div><div class="value">{{ dashboard.gmv }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">订单数</div><div class="value">{{ dashboard.orderCount }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">新增用户</div><div class="value">{{ dashboard.newUserCount }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">推荐点击率</div><div class="value">{{ dashboard.recommendCtr }}</div></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/admin'

const dashboard = ref({ gmv: '0', orderCount: 0, newUserCount: 0, recommendCtr: '0%' })
onMounted(async () => {
  try { dashboard.value = await request.getDashboard() } catch {}
})
</script>

<style scoped>
.stat { text-align: center; padding: 20px; }
.stat .label { color: #999; font-size: 14px; }
.stat .value { font-size: 28px; font-weight: bold; color: #409eff; margin-top: 10px; }
</style>
