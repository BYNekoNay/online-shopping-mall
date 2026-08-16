<template>
  <div class="merchant-shop-info">
    <el-card>
      <h3>店铺信息</h3>
      <el-form :model="form" label-width="100px" style="max-width: 500px; margin-top: 20px">
        <el-form-item label="店铺名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Logo"><el-input v-model="form.logo" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveInfo">保存</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const form = ref({ name: '', logo: '', description: '' })
onMounted(async () => {
  try {
    const data = await request.getShopInfo()
    form.value = { name: data.name || '', logo: data.logo || '', description: data.description || '' }
  } catch {}
})
async function saveInfo() {
  try {
    await request.updateShopInfo(form.value)
    ElMessage.success('保存成功')
  } catch {}
}
</script>
