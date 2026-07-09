<template>
  <div class="recommend-list">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="(item, index) in list" :key="item.productId" style="margin-bottom: 20px;">
        <el-card class="product-card" :body-style="{ padding: '0px' }" @click="handleClick(item, index)">
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendations, getSimilarProducts } from '@/api/recommend'
import { recommendExposure, recommendClick } from '@/api/behavior'

const props = defineProps({
  mode: { type: String, default: 'guess' },
  productId: { type: [String, Number], default: null },
})

const router = useRouter()
const list = ref([])
const exposed = ref(false)

/** 上报推荐位曝光（仅一次）。 */
function reportExposure() {
  if (exposed.value || !list.value || list.value.length === 0) return
  exposed.value = true
  const source = props.mode === 'similar' ? 'product-similar' : 'home-guess'
  const productIds = list.value.map(i => i.productId)
  recommendExposure({ source, productIds }).catch(() => {})
}

/** 上报推荐位点击（记为浏览行为）。 */
async function handleClick(item, index) {
  const source = props.mode === 'similar' ? 'product-similar' : 'home-guess'
  recommendClick({
    source,
    productId: item.productId,
    position: index + 1, // 1-based
  }).catch(() => {})

  // 跳转商品详情，携带推荐来源标记（供后端统计点击归因）
  router.push({ path: `/product/${item.productId}`, query: { from: 'recommend' } })
}

onMounted(async () => {
  try {
    if (props.mode === 'similar' && props.productId) {
      list.value = await getSimilarProducts(props.productId)
    } else {
      list.value = await getRecommendations()
    }
    // 数据加载完成后上报曝光（nextTick 确保 DOM 已渲染，用户真实可见）
    await nextTick()
    reportExposure()
  } catch {
    list.value = []
  }
})

onBeforeUnmount(() => {
  // 组件销毁时不再补曝（避免离开页面前重复上报）
})
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
