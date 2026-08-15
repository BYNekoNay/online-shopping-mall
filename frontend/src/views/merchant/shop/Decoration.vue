<template>
  <div class="merchant-decoration">
    <el-card>
      <h3>店铺装修</h3>
      <p style="color: #999; margin-top: 10px;">配置店铺首页的 banner、主题色与店铺信息</p>
      <el-form :model="form" label-width="120px" style="margin-top: 20px;">
        <el-form-item label="Banner 图片">
          <el-input v-model="form.bannerImage" placeholder="Banner 图片 URL（最长500字符）" />
        </el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="form.themeColor" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">用于店铺页面主色调</span>
        </el-form-item>
        <el-form-item label="店铺公告">
          <el-input v-model="form.announcement" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="如：本店新品上架，全场满 199 包邮" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="如：专注数码配件 8 年，正品保障" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveDecoration">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

// B-5：4 字段装修（兼容旧版 floors 字段——读取时保留、保存时不覆盖）
const form = ref({ bannerImage: '', themeColor: '#409EFF', announcement: '', intro: '', floors: [] })

async function loadDecoration() {
  try {
    const shop = await request.getShopInfo()
    if (shop && shop.decorationConfig) {
      try {
        const cfg = JSON.parse(shop.decorationConfig)
        form.value.bannerImage = cfg.bannerImage || ''
        form.value.themeColor = cfg.themeColor || '#409EFF'
        form.value.announcement = cfg.announcement || ''
        form.value.intro = cfg.intro || ''
        form.value.floors = cfg.floors || []
      } catch {
        // 旧格式容错：保留默认值
      }
    }
  } catch {}
}

async function saveDecoration() {
  try {
    // 仅提交 4 字段（floors 保留旧值不覆盖，避免误清楼层配置）
    const payload = {
      bannerImage: form.value.bannerImage,
      themeColor: form.value.themeColor,
      announcement: form.value.announcement,
      intro: form.value.intro,
      floors: form.value.floors,
    }
    await request.updateShopInfo({ decorationConfig: JSON.stringify(payload) })
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(loadDecoration)
</script>
