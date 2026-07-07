<!-- Consumer homepage -->
<template>
  <div class="home-page">
    <div class="banner-section">
      <el-carousel height="400px">
        <el-carousel-item v-for="i in 3" :key="i">
          <div class="banner-item" :style="{ background: bannerColors[i-1] }">
            <h3>Banner {{ i }}</h3>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="category-nav">
      <div class="category-inner">
        <div v-for="cat in categories" :key="cat.id" class="category-item" @click="goCategory(cat.id)">
          {{ cat.name }}
        </div>
      </div>
    </div>

    <div class="recommend-section">
      <h3>猜你喜欢</h3>
      <RecommendList mode="guess" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/product'
import RecommendList from '@/components/RecommendList.vue'

const router = useRouter()
const categories = ref([])
const bannerColors = ['#409eff', '#67c23a', '#e6a23c']

onMounted(async () => {
  try {
    categories.value = await request.getCategories()
  } catch {
    categories.value = []
  }
})

function goCategory(id) {
  router.push(`/category?id=${id}`)
}
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}
.banner-section {
  margin-top: 20px;
  border-radius: 8px;
  overflow: hidden;
}
.banner-item {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}
.category-nav {
  background: #fff;
  margin: 20px 0;
  padding: 15px 20px;
  border-radius: 8px;
}
.category-inner {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.category-item {
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.category-item:hover {
  background: #409eff;
  color: #fff;
}
.recommend-section {
  margin-bottom: 40px;
}
.recommend-section h3 {
  margin-bottom: 15px;
  font-size: 18px;
}
</style>
