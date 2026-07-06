<template>
  <div class="merchant-decoration">
    <el-card>
      <h3>店铺装修</h3>
      <p style="color: #999; margin-top: 10px;">配置店铺首页的 banner 图和楼层布局</p>
      <el-form :model="form" label-width="120px" style="margin-top: 20px;">
        <el-form-item label="Banner图片">
          <el-input v-model="form.bannerImage" placeholder="Banner图片URL" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="saveDecoration">保存</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const form = ref({ bannerImage: '', floors: [] })
async function saveDecoration() {
  try {
    await request.updateShopInfo({ decorationConfig: form.value })
    ElMessage.success('Saved')
  } catch {}
}
</script>
