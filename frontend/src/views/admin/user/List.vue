<template>
  <div class="admin-users">
    <el-card>
      <h3>用户管理</h3>
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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/admin'

const users = ref([])
const roleMap = { 1: '消费者', 2: '商家', 3: '管理员' }
onMounted(async () => {
  try { users.value = (await request.getUsers()).records || [] } catch {}
})
</script>
