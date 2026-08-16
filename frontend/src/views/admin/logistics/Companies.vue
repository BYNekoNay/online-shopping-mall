<template>
  <div class="admin-logistics">
    <el-card>
      <div class="header">
        <h3>物流公司管理</h3>
        <el-button type="primary" @click="openCreate">新增公司</el-button>
      </div>
      <el-table :data="companies" style="width: 100%; margin-top: 15px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="公司名称" />
        <el-table-column prop="code" label="公司编码" width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物流公司' : '新增物流公司'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="公司名称" required>
          <el-input v-model="form.name" maxlength="50" placeholder="如：顺丰速运" />
        </el-form-item>
        <el-form-item label="公司编码" required>
          <el-input v-model="form.code" maxlength="20" placeholder="如：SF" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLogisticsCompanies,
  createLogisticsCompany,
  updateLogisticsCompany,
  deleteLogisticsCompany
} from '@/api/admin'

const companies = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', code: '', sort: 0, status: 1 })

async function load() {
  try {
    companies.value = (await getLogisticsCompanies()) || []
  } catch {
    companies.value = []
  }
}

function openCreate() {
  isEdit.value = false
  form.value = { id: null, name: '', code: '', sort: 0, status: 1 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, code: row.code, sort: row.sort, status: row.status }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name || !form.value.code) {
    ElMessage.warning('请填写公司名称与编码')
    return
  }
  try {
    if (isEdit.value) {
      await updateLogisticsCompany(form.value.id, form.value)
    } else {
      await createLogisticsCompany(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除物流公司「${row.name}」？`, '提示', { type: 'warning' })
    await deleteLogisticsCompany(row.id)
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
