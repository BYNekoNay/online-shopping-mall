<template>
  <div class="merchant-statistics">
    <el-card>
      <div class="header">
        <h3>数据统计</h3>
        <el-select v-model="granularity" @change="load" style="width: 120px;">
          <el-option label="按日" value="day" />
          <el-option label="按周" value="week" />
          <el-option label="按月" value="month" />
        </el-select>
      </div>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <div class="stat-card">
            <div class="stat-label">销售总额</div>
            <div class="stat-value">¥ {{ stats.totalAmount }}</div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="stat-card">
            <div class="stat-label">订单数量</div>
            <div class="stat-value">{{ stats.totalOrders }} 单</div>
          </div>
        </el-col>
      </el-row>

      <div ref="trendChart" style="width: 100%; height: 350px; margin-top: 30px;"></div>

      <h3 style="margin-top: 40px;">热销商品 TOP10</h3>
      <el-table :data="topProducts" style="width: 100%; margin-top: 15px;">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column prop="amount" label="销售额" width="150">
          <template #default="{ row }">¥ {{ row.amount }}</template>
        </el-table-column>
        <!-- B-3 好评率（无评价显示 "-"） -->
        <el-table-column prop="positiveRate" label="好评率" width="110">
          <template #default="{ row }">{{ row.positiveRate || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="topProducts.length === 0" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/merchant'

const granularity = ref('day')
const stats = ref({ totalAmount: '0.00', totalOrders: 0, trend: [] })
const topProducts = ref([])
const trendChart = ref(null)
let chartInstance = null

function buildDateRange(g) {
  const end = new Date()
  const start = new Date()
  if (g === 'day') start.setDate(start.getDate() - 30)
  else if (g === 'week') start.setDate(start.getDate() - 90)
  else start.setMonth(start.getMonth() - 12)
  const fmt = d => d.toISOString().slice(0, 10)
  return { start: fmt(start), end: fmt(end) }
}

async function load() {
  try {
    const { start, end } = buildDateRange(granularity.value)
    const data = await request.getSalesStatistics({ startDate: start, endDate: end, granularity: granularity.value })
    stats.value = data || { totalAmount: '0.00', totalOrders: 0, trend: [] }
    renderTrend()
    const top = await request.getTopProducts()
    topProducts.value = top || []
  } catch {
    stats.value = { totalAmount: '0.00', totalOrders: 0, trend: [] }
    topProducts.value = []
  }
}

function renderTrend() {
  if (!trendChart.value) return
  if (!chartInstance) chartInstance = echarts.init(trendChart.value)
  const trend = stats.value.trend || []
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(t => t.date) },
    yAxis: { type: 'value' },
    series: [{
      name: '销售额',
      type: 'line',
      data: trend.map(t => t.amount),
      smooth: true,
      areaStyle: { opacity: 0.3 },
    }],
  })
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) chartInstance.dispose()
})
function handleResize() { if (chartInstance) chartInstance.resize() }
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.stat-card {
  text-align: center; padding: 30px; background: #f5f5f5; border-radius: 8px;
}
.stat-label { color: #999; font-size: 14px; }
.stat-value { font-size: 32px; font-weight: bold; color: #409eff; margin-top: 10px; }
</style>
