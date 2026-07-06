<template>
  <div class="admin-shops">
    <el-card>
      <h3>商家管理</h3>
      <el-table :data="shops" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="店铺名称" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag>{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/admin'

const shops = ref([])
const statusMap = { 0: '待审核', 1: '正常', 2: '已拒绝', 3: '已禁用' }
onMounted(async () => {
  try { shops.value = (await request.getShops()).records || [] } catch {}
})
</script>
