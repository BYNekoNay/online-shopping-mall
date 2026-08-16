<template>
  <div class="admin-shops">
    <el-card>
      <div class="header">
        <h3>商家管理</h3>
      </div>
      <AppTable :columns="shopColumns" :data="shops" :pagination="pagination" @page-change="handlePageChange">
        <template #status="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusMap[row.status] }}</el-tag>
        </template>
        <template #level="{ row }">
          <el-rate v-model="row.level" disabled :max="5" />
        </template>
        <template #action="{ row }">
          <el-button v-if="row.status === 0" size="small" type="success" @click="audit(row, true)">通过</el-button>
          <el-button v-if="row.status === 0" size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
          <el-button size="small" type="warning" @click="showLevelDialog(row)">调级</el-button>
        </template>
      </AppTable>
    </el-card>

    <AppDialog v-model="levelDialogVisible" title="调整商家等级" width="400px" @confirm="submitLevel">
      <el-form :model="levelForm" label-width="100px">
        <el-form-item label="店铺">{{ levelForm.name }}</el-form-item>
        <el-form-item label="等级">
          <el-rate v-model="levelForm.level" :max="5" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTable from '@/components/common/AppTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import request from '@/api/admin'

const shops = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusMap = { 0: '待审核', 1: '正常', 2: '已拒绝', 3: '已禁用' }
const levelDialogVisible = ref(false)
const levelForm = ref({ id: null, name: '', level: 1 })

const shopColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '店铺名称' },
  { prop: 'contactName', label: '联系人', width: 120 },
  { prop: 'contactPhone', label: '联系电话', width: 140 },
  { label: '状态', slot: 'status', width: 120 },
  { label: '等级', slot: 'level', width: 80 },
  { prop: 'createTime', label: '申请时间', width: 180 },
  { label: '操作', slot: 'action', width: 220, fixed: 'right' }
]

const pagination = computed(() => ({ currentPage: pageNum.value, pageSize: pageSize.value, total: total.value }))

function statusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info'
}

async function load() {
  try {
    const data = await request.getShops({ pageNum: pageNum.value, pageSize: pageSize.value })
    shops.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    shops.value = []
    total.value = 0
  }
}

function handlePageChange(page) {
  pageNum.value = page
  load()
}

async function audit(row, approved) {
  try {
    const action = approved ? '通过' : '拒绝'
    await ElMessageBox.confirm(`确认${action}店铺 ${row.name}？`, '提示', { type: 'warning' })
    const payload = approved ? { approved } : { approved, reason: '不符合入驻要求' }
    await request.auditShop(row.id, payload)
    ElMessage.success('操作成功')
    load()
  } catch {}
}

function showLevelDialog(row) {
  levelForm.value = { id: row.id, name: row.name, level: row.level || 1 }
  levelDialogVisible.value = true
}

async function submitLevel() {
  try {
    await request.updateShopLevel(levelForm.value.id, { level: levelForm.value.level })
    ElMessage.success('等级已更新')
    levelDialogVisible.value = false
    load()
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
