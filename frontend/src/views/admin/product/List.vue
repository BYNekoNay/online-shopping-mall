<template>
  <div class="admin-products">
    <el-card>
      <h3>商品审核</h3>
      <el-table :data="products" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="shopName" label="店铺" width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 2" type="success" size="small" @click="auditProduct(row, true)">通过</el-button>
            <el-button v-if="row.status === 2" type="danger" size="small" @click="auditProduct(row, false)">拒绝</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" @click="offlineProduct(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 审核对话框 -->
      <el-dialog v-model="auditDialogVisible" :title="auditForm.approved ? '审核通过' : '审核拒绝'" width="500px">
        <el-form :model="auditForm" label-width="100px">
          <el-form-item v-if="!auditForm.approved" label="拒绝原因" required>
            <el-input v-model="auditForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="auditDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="auditSubmitting" @click="confirmAudit">确认</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/admin'
import { ProductStatus, ProductStatusLabel, ProductStatusTagType } from '@/constants/product'

const products = ref([])
const statusMap = ProductStatusLabel
const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const currentProductId = ref(null)
const auditForm = ref({ approved: true, reason: '' })

onMounted(async () => {
  try { products.value = (await request.getProducts()).records || [] } catch {}
})

function statusType(status) {
  return ProductStatusTagType[status] || 'info'
}

function auditProduct(row, approved) {
  currentProductId.value = row.id
  auditForm.value = { approved, reason: '' }
  auditDialogVisible.value = true
}

async function confirmAudit() {
  if (!auditForm.value.approved && !auditForm.value.reason) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  auditSubmitting.value = true
  try {
    await request.auditProduct(currentProductId.value, { approved: auditForm.value.approved, reason: auditForm.value.reason })
    ElMessage.success(auditForm.value.approved ? '审核通过' : '已拒绝')
    auditDialogVisible.value = false
    const p = products.value.find(p => p.id === currentProductId.value)
    if (p) p.status = auditForm.value.approved ? 1 : 3
  } catch {
    ElMessage.error('操作失败')
  } finally {
    auditSubmitting.value = false
  }
}

async function offlineProduct(row) {
  try {
    await request.offlineProduct(row.id)
    ElMessage.success('已下架')
    row.status = 0
  } catch {
    ElMessage.error('操作失败')
  }
}
</script>
