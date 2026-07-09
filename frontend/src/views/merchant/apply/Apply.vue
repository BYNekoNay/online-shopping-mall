<template>
  <div class="merchant-apply">
    <el-card>
      <h2>商家入驻申请</h2>
      <p v-if="rejectReason" class="reject-reason">审核拒绝原因：{{ rejectReason }}</p>
      <el-form :model="form" label-width="120px" style="max-width: 600px; margin-top: 20px;">
        <el-form-item label="店铺名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人姓名" required>
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="营业执照编号" required>
          <el-input v-model="form.licenseNo" />
        </el-form-item>
        <el-form-item label="营业执照图片" required>
          <el-upload action="/api/upload/image" :show-file-list="false" :on-success="onLicenseUpload">
            <img v-if="form.licenseImage" :src="form.licenseImage" class="license-preview" />
            <el-button v-else>上传营业执照</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyReason" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const form = ref({ name: '', contactName: '', contactPhone: '', licenseNo: '', licenseImage: '', applyReason: '' })
const submitting = ref(false)
const rejectReason = ref('')

async function submitApply() {
  submitting.value = true
  try {
    await request.applyShop(form.value)
    ElMessage.success('申请已提交')
  } catch (e) {
    // handled
  } finally {
    submitting.value = false
  }
}

function onLicenseUpload(response) {
  form.value.licenseImage = response?.url || (typeof response === 'string' ? response : response?.data?.url) || ''
}
</script>

<style scoped>
.reject-reason {
  color: #f56c6c;
  padding: 10px;
  background: #fef0f0;
  border-radius: 4px;
}
.license-preview {
  width: 200px;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}
</style>
