<template>
  <div class="category-page">
    <div class="category-sidebar">
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="category-item"
        :class="{ active: selectedCategoryId === cat.id }"
        @click="selectCategory(cat.id)"
      >
        {{ cat.name }}
      </div>
    </div>
    <div class="product-area">
      <div v-if="selectedCategoryId" class="selected-header">
        <h3>{{ currentCategoryName }}</h3>
      </div>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in products" :key="item.id" style="margin-bottom: 20px;">
          <el-card class="product-card" @click="$router.push(`/product/${item.id}`)">
            <div class="product-image"><img :src="item.mainImage" :alt="item.name" /></div>
            <div class="product-info">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-price">¥{{ item.price }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="products.length === 0 && selectedCategoryId" description="该分类下暂无商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/api/product'

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
      list.forEach(c => {
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
    products.value = await request.getProducts(params)
  } catch {
    products.value = []
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  loadProducts(id)
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.category-page {
  display: flex;
  max-width: 1200px;
  margin: 20px auto;
  gap: 20px;
}
.category-sidebar {
  width: 200px;
  background: #fff;
  border-radius: 8px;
  padding: 10px;
  flex-shrink: 0;
}
.category-item {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.2s;
}
.category-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.category-item.active {
  background: #409eff;
  color: #fff;
  font-weight: bold;
}
.product-area {
  flex: 1;
}
.selected-header {
  margin-bottom: 15px;
}
.selected-header h3 {
  font-size: 18px;
  color: #333;
}
.product-card {
  cursor: pointer;
}
.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}
.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-info {
  padding: 10px;
}
.product-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-price {
  color: #f56c6c;
  font-weight: bold;
  margin-top: 5px;
}
</style>
