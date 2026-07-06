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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/merchant'

const refunds = ref([])
const statusMap = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已退款' }

onMounted(async () => {
  try { refunds.value = await request.getRefunds() } catch {}
})
</script>
