<template>
  <div class="admin-logs">
    <el-card>
      <div class="header">
        <h3>操作日志</h3>
      </div>
      <el-table :data="logs" style="width: 100%; margin-top: 15px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorId" label="操作人ID" width="120" />
        <el-table-column prop="operatorRole" label="角色" width="100">
          <template #default="{ row }">
            <el-tag>{{ roleMap[row.operatorRole] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" />
        <el-table-column prop="target" label="目标" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/admin'

const logs = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const roleMap = { 1: '消费者', 2: '商家', 3: '管理员' }

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

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
