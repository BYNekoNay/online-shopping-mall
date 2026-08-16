<template>
  <div class="admin-logs">
    <el-card>
      <div class="header">
        <h3>操作日志</h3>
      </div>
      <AppTable :columns="logColumns" :data="logs" :pagination="pagination" @page-change="handlePageChange">
        <template #operatorRole="{ row }">
          <el-tag>{{ roleMap[row.operatorRole] }}</el-tag>
        </template>
      </AppTable>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppTable from '@/components/common/AppTable.vue'
import request from '@/api/admin'

const logs = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roleMap = { 1: '消费者', 2: '商家', 3: '管理员' }

const logColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'operatorId', label: '操作人ID', width: 120 },
  { label: '角色', slot: 'operatorRole', width: 100 },
  { prop: 'operation', label: '操作' },
  { prop: 'target', label: '目标' },
  { prop: 'createTime', label: '时间', width: 180 }
]

const pagination = computed(() => ({ currentPage: pageNum.value, pageSize: pageSize.value, total: total.value }))

async function load() {
  try {
    const data = await request.getLogs({ pageNum: pageNum.value, pageSize: pageSize.value })
    logs.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    logs.value = []
    total.value = 0
  }
}

function handlePageChange(page) {
  pageNum.value = page
  load()
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
