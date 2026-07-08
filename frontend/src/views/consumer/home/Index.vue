<template>
  <div class="home-page">
    <!-- Banner 轮播 -->
    <section class="banner-section">
      <el-carousel height="420px" :interval="5000" arrow="always" indicator-position="outside">
        <el-carousel-item v-for="(b, i) in banners" :key="i">
          <div class="banner-slide" :style="{ background: b.bg }">
            <div class="banner-content">
              <h2 class="banner-title">{{ b.title }}</h2>
              <p class="banner-desc">{{ b.desc }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 分类导航（卡片网格） -->
    <section class="category-section">
      <div class="section-header">
        <div class="header-accent"></div>
        <h3>全部分类</h3>
      </div>
      <div class="category-grid">
        <div v-for="cat in categories" :key="cat.id" class="category-card card-hover" @click="goCategory(cat.id)">
          <div class="category-icon">{{ cat.name.charAt(0) }}</div>
          <span class="category-name">{{ cat.name }}</span>
        </div>
      </div>
    </section>

    <!-- 猜你喜欢（AI 推荐） -->
    <section class="recommend-section">
      <div class="section-header">
        <div class="header-accent ai-accent"></div>
        <h3 class="ai-title">
          <span class="ai-badge" style="margin-right:10px;">AI 推荐</span>
          猜你喜欢
        </h3>
      </div>
      <RecommendList mode="guess" />
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/product'
import RecommendList from '@/components/RecommendList.vue'

const router = useRouter()
const categories = ref([])

const banners = [
  { title: 'AI 智能推荐', desc: '基于协同过滤算法，为你发现好物', bg: 'linear-gradient(135deg, #4F46E5, #7C3AED)' },
  { title: '品质好货', desc: '精选商品，正品保障', bg: 'linear-gradient(135deg, #0EA5E9, #6366F1)' },
  { title: '限时优惠', desc: '每日特价，不容错过', bg: 'linear-gradient(135deg, #7C3AED, #A855F7)' },
]

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
  padding: 24px 24px 0;
}
/* Banner */
.banner-section {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(79,70,229,0.08);
}
.banner-slide {
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-content {
  text-align: center;
  color: #fff;
}
.banner-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}
.banner-desc {
  font-size: 18px;
  opacity: 0.85;
}
/* Section header */
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.header-accent {
  width: 4px;
  height: 22px;
  border-radius: 2px;
  background: #4F46E5;
}
.ai-accent {
  background: linear-gradient(180deg, #7C3AED, #4F46E5);
}
.section-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}
.ai-title {
  display: flex;
  align-items: center;
}
/* 分类 */
.category-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 16px;
}
.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  border-radius: 12px;
  background: #F8FAFC;
  cursor: pointer;
  transition: all 0.2s;
}
.category-card:hover {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(79,70,229,0.15);
}
.category-card:hover .category-name { color: #fff; }
.category-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #E0E7FF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #4F46E5;
}
.category-card:hover .category-icon {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}
/* 推荐 */
.recommend-section {
  margin-bottom: 48px;
}
</style>
