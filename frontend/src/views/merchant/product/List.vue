<template>
  <div class="merchant-products">
    <el-card>
      <div class="header">
        <h3>商品管理</h3>
        <el-button type="primary" @click="$router.push('/merchant/products/edit')">发布商品</el-button>
      </div>
      <el-table :data="products" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="price" label="价格" width="120" />
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'">
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

const products = ref([])
const statusMap = { 0: '已下架', 1: '已上架', 2: '待审核', 3: '审核拒绝' }

onMounted(async () => {
  try { products.value = await request.getMerchantProducts() } catch {}
})
</script>

<style scoped>
.header { display: flex; justify-content: space-between; }
</style>
