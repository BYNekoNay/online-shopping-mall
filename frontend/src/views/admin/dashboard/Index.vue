<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6"><el-card><div class="stat"><div class="label">GMV</div><div class="value">{{ dashboard.gmv }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">订单数</div><div class="value">{{ dashboard.orderCount }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">新增用户</div><div class="value">{{ dashboard.newUserCount }}</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="label">推荐点击率</div><div class="value">{{ dashboard.recommendCtr }}</div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <h4>转化漏斗</h4>
          <div ref="funnelChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h4>流量指标</h4>
          <div style="padding: 20px 0;">
            <div class="metric-row"><span class="metric-label">PV（页面浏览量）</span><span class="metric-value">{{ detail.pv }}</span></div>
            <div class="metric-row"><span class="metric-label">UV（独立访客）</span><span class="metric-value">{{ detail.uv }}</span></div>
            <div class="metric-row"><span class="metric-label">跳出率</span><span class="metric-value">{{ detail.bounceRate }}%</span></div>
            <div class="metric-row"><span class="metric-label">平均停留时长</span><span class="metric-value">{{ detail.avgStayDuration }} 秒</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/admin'

const dashboard = ref({ gmv: '0', orderCount: 0, newUserCount: 0, recommendCtr: '0%' })
const detail = ref({ pv: 0, uv: 0, bounceRate: 0, avgStayDuration: 0, funnel: {} })
const funnelChart = ref(null)
let chartInstance = null

async function loadDashboard() {
  try {
    dashboard.value = await request.getDashboard()
  } catch {
    dashboard.value = { gmv: '0', orderCount: 0, newUserCount: 0, recommendCtr: '0%' }
  }
}

async function loadDetail() {
  try {
    const data = await request.getStatisticsDetail()
    detail.value = data || { pv: 0, uv: 0, bounceRate: 0, avgStayDuration: 0, funnel: {} }
    renderFunnel()
  } catch {
    detail.value = { pv: 0, uv: 0, bounceRate: 0, avgStayDuration: 0, funnel: {} }
  }
}

function renderFunnel() {
  if (!funnelChart.value) return
  if (!chartInstance) chartInstance = echarts.init(funnelChart.value)
  const f = detail.value.funnel || {}
  chartInstance.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      left: '10%', top: 20, bottom: 20, width: '80%',
      data: [
        { value: f.view || 0, name: '浏览' },
        { value: f.cart || 0, name: '加购' },
        { value: f.order || 0, name: '下单' },
        { value: f.pay || 0, name: '支付' },
      ],
    }],
  })
}

onMounted(() => {
  loadDashboard()
  loadDetail()
  window.addEventListener('resize', () => chartInstance && chartInstance.resize())
})
</script>

<style scoped>
.stat { text-align: center; padding: 20px; }
.stat .label { color: #999; font-size: 14px; }
.stat .value { font-size: 28px; font-weight: bold; color: #409eff; margin-top: 10px; }
.metric-row { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.metric-row:last-child { border-bottom: none; }
.metric-label { color: #666; }
.metric-value { font-weight: bold; color: #333; font-size: 18px; }
</style>
