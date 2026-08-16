<template>
  <div class="admin-dicts">
    <el-card>
      <div class="header">
        <h3>数据字典</h3>
        <el-button type="primary" @click="showCreateDialog">新建字典项</el-button>
      </div>
      <AppTable :columns="dictColumns" :data="dicts">
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
        <template #action="{ row }">
          <el-button size="small" @click="editRow(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteRow(row)">删除</el-button>
        </template>
      </AppTable>
    </el-card>

    <AppDialog v-model="dialogVisible" :title="editing ? '编辑字典项' : '新建字典项'" width="500px" @confirm="submit">
      <el-form :model="form" label-width="100px">
        <el-form-item label="类型"><el-input v-model="form.dictType" /></el-form-item>
        <el-form-item label="键"><el-input v-model="form.dictKey" /></el-form-item>
        <el-form-item label="值"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTable from '@/components/common/AppTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import request from '@/api/admin'

const dicts = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({ id: null, dictType: '', dictKey: '', dictValue: '', sort: 0, status: 1 })

const dictColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'dictType', label: '类型', width: 150 },
  { prop: 'dictKey', label: '键', width: 120 },
  { prop: 'dictValue', label: '值' },
  { prop: 'sort', label: '排序', width: 80 },
  { label: '状态', slot: 'status', width: 100 },
  { label: '操作', slot: 'action', width: 160, fixed: 'right' }
]

async function load() {
  try {
    dicts.value = (await request.getDicts()) || []
  } catch {
    dicts.value = []
  }
}

function showCreateDialog() {
  editing.value = false
  form.value = { id: null, dictType: '', dictKey: '', dictValue: '', sort: 0, status: 1 }
  dialogVisible.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  try {
    if (editing.value && form.value.id) {
      await request.updateDict(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.createDict(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } catch {}
}

async function deleteRow(row) {
  try {
    await ElMessageBox.confirm('确认删除该字典项？', '提示', { type: 'warning' })
    await request.deleteDict(row.id)
    ElMessage.success('已删除')
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
