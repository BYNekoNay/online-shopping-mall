<template>
  <div class="admin-users">
    <el-card>
      <div class="header">
        <h3>用户管理</h3>
      </div>
      <div class="filters">
        <el-select v-model="filters.role" placeholder="角色" clearable style="width: 120px;">
          <el-option label="消费者" :value="1" />
          <el-option label="商家" :value="2" />
          <el-option label="管理员" :value="3" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="用户名/昵称" style="width: 180px;" clearable />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="users" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ roleMap[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" style="width: 100px; margin-right: 6px;" @change="(val) => changeRole(row, val)">
              <el-option label="消费者" :value="1" />
              <el-option label="商家" :value="2" />
              <el-option label="管理员" :value="3" />
            </el-select>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" style="margin-top: 20px; justify-content: flex-end;" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/admin'

const users = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roleMap = { 1: '消费者', 2: '商家', 3: '管理员' }
const filters = ref({ role: null, status: null, keyword: '' })

async function load() {
  try {
    const data = await request.getUsers({ ...filters.value, pageNum: pageNum.value, pageSize: pageSize.value })
    users.value = data.records || []
    total.value = data.total || 0
  } catch {
    users.value = []
    total.value = 0
  }
}

async function toggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户 ${row.username}？`, '提示', { type: 'warning' })
    await request.updateUserStatus(row.id, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(`${action}成功`)
    load()
  } catch {}
}

async function changeRole(row, role) {
  const original = row.role
  if (original === role) return
  try {
    await ElMessageBox.confirm(`确认将用户 ${row.username} 的角色改为「${roleMap[role]}」？`, '提示', { type: 'warning' })
    await request.updateUserRole(row.id, { role })
    ElMessage.success('角色修改成功，该用户需重新登录后生效')
  } catch {
    row.role = original // 取消或失败时回滚下拉选择
  }
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; margin-top: 15px; }
</style>
