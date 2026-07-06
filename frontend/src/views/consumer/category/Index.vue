<template>
  <div class="category-page">
    <div class="category-sidebar">
      <div v-for="cat in categories" :key="cat.id" class="category-item" @click="selectCategory(cat.id)">
        {{ cat.name }}
      </div>
    </div>
    <div class="product-area">
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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/product'

const categories = ref([])
const products = ref([])
const selectedCategoryId = ref(null)

async function loadCategories() {
  categories.value = await request.getCategories()
}

async function loadProducts() {
  try {
    products.value = await request.getProducts({ categoryId: selectedCategoryId.value })
  } catch {
    products.value = []
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  loadProducts()
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
}
.category-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.product-area {
  flex: 1;
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
