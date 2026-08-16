<template>
  <div class="admin-config">
    <el-card>
      <div class="header">
        <h3>系统配置</h3>
        <el-button type="primary" @click="saveConfig">保存配置</el-button>
      </div>
      <el-form :model="form" label-width="200px" style="max-width: 600px; margin-top: 20px">
        <el-form-item label="平台名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="订单超时时间（分钟）">
          <el-input-number v-model="form.orderTimeout" :min="10" :max="1440" />
        </el-form-item>
        <el-form-item label="推荐结果刷新周期（小时）">
          <el-input-number v-model="form.recommendRefreshHours" :min="1" :max="168" />
        </el-form-item>
        <el-form-item label="物流查询超时（秒）">
          <el-input-number v-model="form.logisticsTimeout" :min="1" :max="30" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/admin'

const form = ref({
  name: '智能推荐网络商城',
  orderTimeout: 30,
  recommendRefreshHours: 24,
  logisticsTimeout: 3
})

onMounted(async () => {
  try {
    const data = await request.listConfig()
    if (data && typeof data === 'object') {
      form.value = {
        name: data['mall.name'] || form.value.name,
        orderTimeout: data['order.timeout'] !== undefined ? Number(data['order.timeout']) : form.value.orderTimeout,
        recommendRefreshHours:
          data['recommend.refresh.hours'] !== undefined
            ? Number(data['recommend.refresh.hours'])
            : form.value.recommendRefreshHours,
        logisticsTimeout:
          data['logistics.timeout'] !== undefined ? Number(data['logistics.timeout']) : form.value.logisticsTimeout
      }
    }
  } catch {
    // use defaults
  }
})

async function saveConfig() {
  try {
    await request.updateConfig('mall.name', { value: form.value.name, description: '平台名称' })
    await request.updateConfig('order.timeout', {
      value: String(form.value.orderTimeout),
      description: '订单超时时间（分钟）'
    })
    await request.updateConfig('recommend.refresh.hours', {
      value: String(form.value.recommendRefreshHours),
      description: '推荐结果刷新周期（小时）'
    })
    await request.updateConfig('logistics.timeout', {
      value: String(form.value.logisticsTimeout),
      description: '物流查询超时（秒）'
    })
    ElMessage.success('配置已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
