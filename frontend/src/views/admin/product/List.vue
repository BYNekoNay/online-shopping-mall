<template>
  <div class="admin-products">
    <el-card>
      <h3>商品审核</h3>
      <AppTable :columns="productColumns" :data="products">
        <template #status="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusMap[row.status] }}</el-tag>
        </template>
        <template #action="{ row }">
          <el-button v-if="row.status === 2" type="success" size="small" @click="auditProduct(row, true)"
            >通过</el-button
          >
          <el-button v-if="row.status === 2" type="danger" size="small" @click="auditProduct(row, false)"
            >拒绝</el-button
          >
          <el-button v-if="row.status === 1" type="warning" size="small" @click="offlineProduct(row)">下架</el-button>
        </template>
      </AppTable>
    </el-card>

    <!-- 审核对话框 -->
    <AppDialog
      v-model="auditDialogVisible"
      :title="auditForm.approved ? '审核通过' : '审核拒绝'"
      width="500px"
      confirm-text="确认"
      :loading="auditSubmitting"
      @confirm="confirmAudit"
    >
      <el-form :model="auditForm" label-width="100px">
        <el-form-item v-if="!auditForm.approved" label="拒绝原因" required>
          <el-input v-model="auditForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AppTable from '@/components/common/AppTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import request from '@/api/admin'
import { ProductStatusLabel, ProductStatusTagType } from '@/constants/product'

const products = ref([])
const statusMap = ProductStatusLabel
const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const currentProductId = ref(null)
const auditForm = ref({ approved: true, reason: '' })

const productColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '商品名称' },
  { prop: 'shopName', label: '店铺', width: 150 },
  { label: '状态', slot: 'status', width: 120 },
  { label: '操作', slot: 'action', width: 220, fixed: 'right' }
]

onMounted(async () => {
  try {
    products.value = (await request.getProducts()).records || []
  } catch {}
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
    await request.auditProduct(currentProductId.value, {
      approved: auditForm.value.approved,
      reason: auditForm.value.reason
    })
    ElMessage.success(auditForm.value.approved ? '审核通过' : '已拒绝')
    auditDialogVisible.value = false
    const p = products.value.find((p) => p.id === currentProductId.value)
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
