<template>
  <div class="admin-products">
    <el-card>
      <h3>商品审核</h3>
      <el-table :data="products" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" />
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

const products = ref([])
const statusMap = { 0: '下架', 1: '上架', 2: '待审核', 3: '审核拒绝' }
onMounted(async () => {
  try { products.value = (await request.getProducts()).records || [] } catch {}
})
</script>
