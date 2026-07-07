<template>
  <div class="merchant-refunds">
    <el-card>
      <h3>售后处理</h3>
      <el-table :data="refunds" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column prop="amount" label="退款金额" width="120">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'danger'">
              {{ statusMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="hasPending" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="openAuditDialog(row, true)">通过</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="openAuditDialog(row, false)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 审核对话框 -->
      <el-dialog v-model="auditDialogVisible" :title="auditForm.approved ? '通过退款' : '拒绝退款'" width="500px">
        <el-form :model="auditForm" label-width="100px">
          <el-form-item label="处理备注" required>
            <el-input v-model="auditForm.handleRemark" type="textarea" :rows="3" placeholder="请输入处理备注" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const refunds = ref([])
const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const currentRefundId = ref(null)
const auditForm = ref({ approved: true, handleRemark: '' })
const statusMap = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已退款' }

const hasPending = computed(() => refunds.value.some(r => r.status === 0))

onMounted(async () => {
  try { refunds.value = await request.getRefunds() } catch {}
})

function openAuditDialog(refund, approved) {
  currentRefundId.value = refund.id
  auditForm.value = { approved, handleRemark: '' }
  auditDialogVisible.value = true
}

async function confirmAudit() {
  if (!auditForm.value.handleRemark) {
    ElMessage.warning('请填写处理备注')
    return
  }
  auditSubmitting.value = true
  try {
    await request.auditRefund(currentRefundId.value, auditForm.value)
    ElMessage.success(auditForm.value.approved ? '已通过' : '已拒绝')
    auditDialogVisible.value = false
    const refund = refunds.value.find(r => r.id === currentRefundId.value)
    if (refund) {
      refund.status = auditForm.value.approved ? 1 : 2
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    auditSubmitting.value = false
  }
}
</script>
