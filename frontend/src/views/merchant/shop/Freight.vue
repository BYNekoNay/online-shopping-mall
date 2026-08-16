<template>
  <div class="merchant-freight">
    <el-card>
      <h3>运费模板</h3>
      <el-form :model="form" label-width="120px" style="max-width: 600px; margin-top: 20px">
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

    <!-- 运费试算 -->
    <el-card style="margin-top: 20px">
      <h3>运费试算</h3>
      <el-form :model="calcForm" label-width="120px" style="max-width: 600px; margin-top: 20px">
        <el-form-item label="省份">
          <el-input v-model="calcForm.province" placeholder="请输入省份名称，如：广东省" />
        </el-form-item>
        <el-form-item label="商品金额">
          <el-input-number v-model="calcForm.goodsAmount" :min="0" :step="10" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="calcLoading" @click="doCalculateFreight">试算运费</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="calcResult !== null"
        :title="'预估运费：¥' + calcResult"
        type="success"
        :closable="false"
        style="max-width: 600px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const form = ref({ name: '', defaultFee: 10, freeShippingThreshold: 99, regionRules: [] })
async function saveTemplate() {
  try {
    await request.saveFreightTemplate(form.value)
    ElMessage.success('保存成功')
  } catch {}
}

// 运费试算
const calcForm = ref({ province: '', goodsAmount: 0 })
const calcLoading = ref(false)
const calcResult = ref(null)
const shopInfo = ref({})

onMounted(async () => {
  try {
    shopInfo.value = await request.getShopInfo()
  } catch {
    shopInfo.value = { id: 0 }
  }
})

async function doCalculateFreight() {
  if (!calcForm.value.province) {
    ElMessage.warning('请输入省份')
    return
  }
  if (calcForm.value.goodsAmount <= 0) {
    ElMessage.warning('请输入商品金额')
    return
  }
  calcLoading.value = true
  calcResult.value = null
  try {
    const res = await request.calculateFreight({
      shopId: shopInfo.value.id || 0,
      province: calcForm.value.province,
      goodsAmount: calcForm.value.goodsAmount
    })
    calcResult.value = res.freight ?? res
  } catch {
    ElMessage.error('试算运费失败')
  } finally {
    calcLoading.value = false
  }
}
</script>
