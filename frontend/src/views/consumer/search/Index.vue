<template>
  <div class="search-page">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索商品" @keyup.enter="doSearch" style="width: 400px;">
        <template #append>
          <el-button @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>
    <div class="search-filters" style="margin-top: 15px;">
      <span>排序：</span>
      <el-radio-group v-model="sort" @change="doSearch">
        <el-radio-button label="sales">销量</el-radio-button>
        <el-radio-button label="price_asc">价格升序</el-radio-button>
        <el-radio-button label="price_desc">价格降序</el-radio-button>
        <el-radio-button label="new">最新</el-radio-button>
      </el-radio-group>
    </div>
    <div class="search-results" style="margin-top: 20px;">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in results" :key="item.id" style="margin-bottom: 20px;">
          <el-card class="product-card" @click="$router.push(`/product/${item.id}`)">
            <div class="product-image">
              <img :src="item.mainImage" :alt="item.name" />
            </div>
            <div class="product-info">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-price">¥{{ item.price }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="results.length === 0" description="暂无搜索结果" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/product'

const route = useRoute()
const keyword = ref(route.query.keyword || '')
const sort = ref('')
const results = ref([])

async function doSearch() {
  try {
    results.value = await request.searchProducts({ keyword: keyword.value, sort: sort.value })
  } catch {
    results.value = []
  }
}

if (route.query.keyword) {
  doSearch()
}
</script>

<style scoped>
.search-bar {
  display: flex;
  justify-content: center;
  padding-top: 40px;
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
