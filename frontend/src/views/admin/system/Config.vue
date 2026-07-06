<template>
  <div class="admin-config">
    <el-card>
      <h3>系统配置</h3>
      <el-form label-width="150px" style="max-width: 500px; margin-top: 20px;">
        <el-form-item label="平台名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="订单超时时间（分钟）"><el-input-number v-model="form.orderTimeout" :min="10" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveConfig">保存</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const form = ref({ name: '智能推荐网络商城', orderTimeout: 30 })
function saveConfig() { ElMessage.success('Saved') }
</script>
