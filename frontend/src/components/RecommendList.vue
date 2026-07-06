<template>
  <div class="recommend-list">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="item in list" :key="item.productId" style="margin-bottom: 20px;">
        <el-card class="product-card" :body-style="{ padding: '0px' }" @click="goToDetail(item.productId)">
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
    <el-empty v-if="!list || list.length === 0" description="暂无推荐" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendations, getSimilarProducts } from '@/api/recommend'

const props = defineProps({
  mode: { type: String, default: 'guess' },
  productId: { type: [String, Number], default: null },
})

const router = useRouter()
const list = ref([])

onMounted(async () => {
  try {
    if (props.mode === 'similar' && props.productId) {
      list.value = await getSimilarProducts(props.productId)
    } else {
      list.value = await getRecommendations()
    }
  } catch {
    list.value = []
  }
})

function goToDetail(productId) {
  router.push(`/product/${productId}`)
}
</script>

<style scoped>
.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.product-card:hover {
  transform: translateY(-4px);
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
  color: #333;
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
