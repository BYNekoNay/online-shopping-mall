<template>
  <div class="admin-shops">
    <el-card>
      <div class="header">
        <h3>商家管理</h3>
      </div>
      <el-table :data="shops" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="店铺名称" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="80">
          <template #default="{ row }">
            <el-rate v-model="row.level" disabled :max="5" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" @click="audit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
            <el-button size="small" type="warning" @click="showLevelDialog(row)">调级</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" style="margin-top: 20px; justify-content: flex-end;" @current-change="load" />
    </el-card>

    <el-dialog v-model="levelDialogVisible" title="调整商家等级" width="400px">
      <el-form :model="levelForm" label-width="100px">
        <el-form-item label="店铺">{{ levelForm.name }}</el-form-item>
        <el-form-item label="等级">
          <el-rate v-model="levelForm.level" :max="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLevel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/admin'

const shops = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusMap = { 0: '待审核', 1: '正常', 2: '已拒绝', 3: '已禁用' }
const levelDialogVisible = ref(false)
const levelForm = ref({ id: null, name: '', level: 1 })

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
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
