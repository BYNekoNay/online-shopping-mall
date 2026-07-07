<template>
  <div class="user-favorites">
    <el-card>
      <h3>我的收藏</h3>
      <div v-if="favorites.length === 0" class="empty-state">
        <el-empty description="暂无收藏" />
      </div>
      <div v-else class="favorite-list">
        <el-row :gutter="20">
          <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in favorites" :key="item.id" style="margin-bottom: 20px;">
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/user'

const favorites = ref([])

async function loadFavorites() {
  try {
    favorites.value = await request.getFavorites()
  } catch {
    favorites.value = []
  }
}

onMounted(loadFavorites)
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
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
