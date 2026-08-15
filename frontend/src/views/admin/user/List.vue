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
            <el-button size="small" type="primary" plain @click="showDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" style="margin-top: 20px; justify-content: flex-end;" @current-change="load" />
    </el-card>

    <!-- B-2 用户详情 Dialog -->
    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ roleMap[detail.role] }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
              {{ detail.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ detail.points }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="订单数">{{ detail.orderCount }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">¥{{ detail.totalSpend }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px;">
          <p style="font-weight: 500; margin: 0 0 8px;">最近行为</p>
          <el-table :data="detail.recentBehaviors || []" size="small" empty-text="暂无行为记录">
            <el-table-column label="行为类型" width="100">
              <template #default="{ row }">
                {{ behaviorMap[row.behaviorType] || '未知' }}
              </template>
            </el-table-column>
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column prop="createTime" label="时间" />
          </el-table>
        </div>
      </template>
    </el-dialog>
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

// B-2 用户详情
const detailVisible = ref(false)
const detail = ref(null)
const behaviorMap = { 1: '浏览', 2: '收藏', 3: '购买', 4: '评价' }

async function showDetail(id) {
  try {
    detail.value = await request.getUserDetail(id)
    detailVisible.value = true
  } catch {
    ElMessage.error('加载用户详情失败')
  }
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; margin-top: 15px; }
</style>
