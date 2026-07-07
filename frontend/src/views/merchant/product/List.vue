<template>
  <div class="merchant-products">
    <el-card>
      <div class="header">
        <h3>商品管理</h3>
        <div>
          <el-input v-model="keyword" placeholder="搜索商品" style="width: 200px; margin-right: 10px;" @keyup.enter="loadProducts" />
          <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-right: 10px;" @change="loadProducts">
            <el-option label="全部" value="" />
            <el-option :label="ProductStatusLabel[ProductStatus.ONLINE]" :value="ProductStatus.ONLINE" />
            <el-option :label="ProductStatusLabel[ProductStatus.OFFLINE]" :value="ProductStatus.OFFLINE" />
            <el-option :label="ProductStatusLabel[ProductStatus.PENDING]" :value="ProductStatus.PENDING" />
            <el-option :label="ProductStatusLabel[ProductStatus.REJECTED]" :value="ProductStatus.REJECTED" />
          </el-select>
          <el-button v-if="selectedIds.length > 0" type="success" size="small" @click="batchOperate('on')">批量上架</el-button>
          <el-button v-if="selectedIds.length > 0" type="warning" size="small" @click="batchOperate('off')">批量下架</el-button>
          <el-button v-if="selectedIds.length > 0" type="danger" size="small" @click="batchOperate('delete')">批量删除</el-button>
          <el-button type="primary" @click="$router.push('/merchant/products/edit')">发布商品</el-button>
        </div>
      </div>
      <el-table :data="products" style="width: 100%; margin-top: 15px;" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="price" label="价格" width="120" />
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="ProductStatusTagType[row.status] || 'info'">
              {{ ProductStatusLabel[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/merchant/products/edit?id=${row.id}`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'
import { ProductStatus, ProductStatusLabel } from '@/constants/product'

const products = ref([])
const selectedIds = ref([])
const keyword = ref('')
const statusFilter = ref('')

onMounted(async () => {
  loadProducts()
})

async function loadProducts() {
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value
    products.value = await request.getMerchantProducts(params)
  } catch {
    products.value = []
  }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(s => s.id)
}

async function batchOperate(action) {
  try {
    await request.batchOperateProducts({ productIds: selectedIds.value, action })
    ElMessage.success('操作成功')
    selectedIds.value = []
    loadProducts()
  } catch {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
</style>
