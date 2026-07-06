<template>
  <div class="user-points">
    <el-card>
      <div class="points-header">
        <div class="points-balance">
          <span class="label">当前积分</span>
          <span class="value">{{ points }}</span>
        </div>
      </div>
      <h3 style="margin-top: 30px;">积分流水</h3>
      <el-table :data="records" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="changeAmount" label="变动" width="120">
          <template #default="{ row }">
            <span :class="row.changeAmount > 0 ? 'positive' : 'negative'">
              {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            {{ typeMap[row.type] || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" />
      </el-table>
      <el-empty v-if="records.length === 0" description="暂无积分记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/order'

const points = ref(0)
const records = ref([])
const typeMap = { 1: '下单获取', 2: '订单抵扣', 3: '兑换' }

onMounted(async () => {
  try {
    const data = await request.getPoints()
    points.value = data.points
    records.value = data.records || []
  } catch {
    // error handled
  }
})
</script>

<style scoped>
.points-header {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}
.points-balance {
  text-align: center;
}
.points-balance .label {
  display: block;
  color: #999;
  font-size: 14px;
}
.points-balance .value {
  display: block;
  font-size: 48px;
  font-weight: bold;
  color: #f56c6c;
  margin-top: 10px;
}
.positive { color: #67c23a; }
.negative { color: #f56c6c; }
</style>
