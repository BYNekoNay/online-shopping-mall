<template>
  <div class="category-page">
    <div class="category-sidebar">
      <div class="sidebar-title">商品分类</div>
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="category-item"
        :class="{ active: selectedCategoryId === cat.id }"
        @click="selectCategory(cat.id)"
      >
        <span class="item-emoji">{{ categoryEmoji(cat.name) }}</span>
        {{ cat.name }}
      </div>
    </div>
    <div class="product-area">
      <div class="selected-header">
        <h3>{{ currentCategoryName || '全部商品' }}</h3>
        <span class="header-count">{{ products.length }} 件商品</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in products" :key="item.id" style="margin-bottom: 20px">
          <ProductCard :item="item" />
        </el-col>
      </el-row>
      <el-empty v-if="products.length === 0 && selectedCategoryId" description="该分类下暂无商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'
import { categoryEmoji } from '@/utils/category'

const route = useRoute()
const categories = ref([])
const products = ref([])
const selectedCategoryId = ref(null)
const categoryNameMap = ref({})

const currentCategoryName = computed(() => categoryNameMap.value[selectedCategoryId.value] || '')

async function loadCategories() {
  try {
    const data = await request.getCategories()
    categories.value = data || []
    // Build name map
    const buildMap = (list) => {
      list.forEach((c) => {
        categoryNameMap.value[c.id] = c.name
        if (c.children) buildMap(c.children)
      })
    }
    buildMap(categories.value)
  } catch {
    categories.value = []
  }
}

async function loadProducts(categoryId) {
  try {
    const params = {}
    if (categoryId) params.categoryId = categoryId
    const data = await request.getProducts(params)
    products.value = data.records || data || []
  } catch {
    products.value = []
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  loadProducts(id)
}

// FRONT-03 修复：首页分类卡片跳转 /category?id=xx 后，分类页需读取 query.id 并定位到该分类
function applyQueryCategory() {
  const id = route.query.id
  if (id !== undefined && id !== null && id !== '') {
    const num = Number(id)
    if (Number.isFinite(num) && num > 0) {
      selectCategory(num)
      return
    }
  }
  // 无有效 query 时展示全部商品
  selectedCategoryId.value = null
  loadProducts()
}

onMounted(async () => {
  await loadCategories()
  applyQueryCategory()
})

// FRONT-03 修复：分类页内/首页反复跳转不同分类时响应 query.id 变化
watch(
  () => route.query.id,
  () => {
    applyQueryCategory()
  }
)
</script>

<style scoped>
.category-page {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  gap: 20px;
  align-items: flex-start;
}
.category-sidebar {
  width: 200px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  padding: 12px;
  flex-shrink: 0;
  position: sticky;
  top: 84px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}
.sidebar-title {
  padding: 8px 14px 12px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 8px;
}
.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  cursor: pointer;
  border-radius: 10px;
  font-size: 14px;
  color: #334155;
  transition: all 0.2s;
}
.category-item:hover {
  background: #f5f3ff;
  color: #4f46e5;
}
.category-item.active {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.24);
}
.item-emoji {
  font-size: 16px;
  line-height: 1;
}
.product-area {
  flex: 1;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  padding: 20px;
  min-height: 60vh;
}
.selected-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 18px;
}
.selected-header h3 {
  font-size: 18px;
  color: #0f172a;
}
.header-count {
  font-size: 13px;
  color: #94a3b8;
}
</style>
