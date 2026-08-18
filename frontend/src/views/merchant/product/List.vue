<template>
  <div class="merchant-products">
    <el-card>
      <div class="header">
        <h3>商品管理</h3>
        <div>
          <el-input
            v-model="keyword"
            placeholder="搜索商品"
            style="width: 200px; margin-right: 10px"
            @keyup.enter="loadProducts"
          />
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            style="width: 120px; margin-right: 10px"
            @change="loadProducts"
          >
            <el-option label="全部" value="" />
            <el-option :label="ProductStatusLabel[ProductStatus.ONLINE]" :value="ProductStatus.ONLINE" />
            <el-option :label="ProductStatusLabel[ProductStatus.OFFLINE]" :value="ProductStatus.OFFLINE" />
            <el-option :label="ProductStatusLabel[ProductStatus.PENDING]" :value="ProductStatus.PENDING" />
            <el-option :label="ProductStatusLabel[ProductStatus.REJECTED]" :value="ProductStatus.REJECTED" />
          </el-select>
          <el-button v-if="selectedIds.length > 0" type="success" size="small" @click="batchOperate('on')"
            >批量上架</el-button
          >
          <el-button v-if="selectedIds.length > 0" type="warning" size="small" @click="batchOperate('off')"
            >批量下架</el-button
          >
          <el-button v-if="selectedIds.length > 0" type="danger" size="small" @click="batchOperate('delete')"
            >批量删除</el-button
          >
          <el-button type="primary" @click="$router.push('/merchant/products/edit')">发布商品</el-button>
        </div>
      </div>
      <el-table :data="products" style="width: 100%; margin-top: 15px" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <!-- 缺陷 2 修复：API 创建商品 images 可能是 JSON 数组字符串 / 逗号分隔字符串 / 数组，
             任何形态都安全取首图；detail / skus 在列表页不渲染但已做容错避免上游错误传递 -->
        <el-table-column label="封面" width="72">
          <template #default="{ row }">
            <el-image
              v-if="getFirstImage(row)"
              :src="getFirstImage(row)"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px"
            />
            <span v-else class="thumb-placeholder">无图</span>
          </template>
        </el-table-column>
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
            <el-button type="primary" size="small" @click="$router.push(`/merchant/products/edit?id=${row.id}`)"
              >编辑</el-button
            >
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
import { ProductStatus, ProductStatusLabel, ProductStatusTagType } from '@/constants/product'

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
    const data = await request.getMerchantProducts(params)
    // 缺陷 2 修复：data 可能是分页 {records,total,...} 或裸数组，统一兜底为数组
    const list = data?.records || data?.list || data || []
    products.value = Array.isArray(list) ? list : []
  } catch {
    products.value = []
  }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map((s) => s.id)
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

/**
 * 缺陷 2 修复：安全取商品首图。
 * API 创建商品的 images 字段可能为：
 *   - JSON 数组字符串：'["https://a.jpg","https://b.jpg"]'（后端 DTO 约定）
 *   - 逗号分隔字符串：'https://a.jpg,https://b.jpg'（前端表单约定）
 *   - 真实数组：['https://a.jpg', ...]（异常形态）
 *   - null / undefined / 缺失
 * 任何形态都不能让 JSON.parse 抛异常导致整页 ErrorBoundary 渲染崩溃。
 */
function getFirstImage(row) {
  if (!row) return ''
  // 优先 images，回退 mainImage
  let raw = row.images
  if (!raw) raw = row.mainImage
  if (!raw) return ''
  if (Array.isArray(raw)) {
    return raw.length ? String(raw[0] || '').trim() : ''
  }
  if (typeof raw !== 'string') return ''
  const trimmed = raw.trim()
  if (!trimmed) return ''
  // 尝试 JSON 数组解析（包在 try/catch，失败不抛）
  if (trimmed.startsWith('[')) {
    try {
      const arr = JSON.parse(trimmed)
      if (Array.isArray(arr) && arr.length) return String(arr[0] || '').trim()
    } catch {
      // JSON 解析失败 → 回退到逗号分隔
    }
  }
  // 逗号分隔取第一张
  return trimmed.split(',')[0].trim() || ''
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.thumb-placeholder {
  display: inline-block;
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  border-radius: 4px;
}
</style>
