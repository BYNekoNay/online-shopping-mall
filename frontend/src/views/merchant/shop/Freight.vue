<template>
  <div class="merchant-freight">
    <el-card>
      <h3>运费模板</h3>
      <el-form :model="form" label-width="120px" style="max-width: 600px; margin-top: 20px;">
        <el-form-item label="模板名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="默认运费">
          <el-input-number v-model="form.defaultFee" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="包邮门槛">
          <el-input-number v-model="form.freeShippingThreshold" :min="0" :step="10" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveTemplate">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const form = ref({ name: '', defaultFee: 10, freeShippingThreshold: 99, regionRules: [] })
async function saveTemplate() {
  try {
    await request.saveFreightTemplate(form.value)
    ElMessage.success('Saved')
  } catch {}
}
</script>
