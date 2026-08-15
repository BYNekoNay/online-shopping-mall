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

    <!-- 限时活动 -->
    <section v-if="promotions.length > 0" class="promotions-section">
      <div class="section-header">
        <div class="header-accent promotion-accent"></div>
        <h3 class="promotion-title">🔥 限时活动</h3>
      </div>
      <div class="promotion-list">
        <div v-for="promo in promotions" :key="promo.id" class="promotion-card">
          <div class="promotion-tag">{{ typeMap[promo.type] }}</div>
          <div class="promotion-name">{{ promo.name }}</div>
          <div class="promotion-desc">{{ promo.description }}</div>
        </div>
      </div>
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

    <!-- A-1 浏览历史推荐（仅登录用户显示） -->
    <section v-if="isLogin" class="recommend-section">
      <div class="section-header">
        <div class="header-accent ai-accent"></div>
        <h3 class="ai-title">
          <span class="ai-badge" style="margin-right:10px;">AI 推荐</span>
          浏览历史推荐
        </h3>
      </div>
      <RecommendList mode="history" />
    </section>

    <!-- D-5 购买推荐（仅登录用户显示，无购买记录时后端返回空） -->
    <section v-if="isLogin" class="recommend-section">
      <div class="section-header">
        <div class="header-accent ai-accent"></div>
        <h3 class="ai-title">
          <span class="ai-badge" style="margin-right:10px;">AI 推荐</span>
          购买过同类
        </h3>
      </div>
      <RecommendList mode="purchase" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/product'
import { getActivePromotions } from '@/api/promotion'
import RecommendList from '@/components/RecommendList.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
// A-1：登录态判断（token 存在即视为已登录），控制"浏览历史推荐"区块显示
const isLogin = computed(() => !!userStore.token)
const categories = ref([])
const promotions = ref([])
const typeMap = { 1: '折扣', 2: '满减', 3: '满赠', 4: '套餐' }

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
  try {
    const res = await getActivePromotions({ scope: 'GLOBAL', scopeId: 0 })
    promotions.value = res || []
  } catch {
    promotions.value = []
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
/* 限时活动 */
.promotions-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.promotion-accent {
  background: linear-gradient(180deg, #f56c6c, #e6a23c);
}
.promotion-title {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}
.promotion-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.promotion-card {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
  cursor: default;
}
.promotion-card:hover {
  border-color: #f56c6c;
  box-shadow: 0 4px 12px rgba(245,108,108,0.1);
  transform: translateY(-2px);
}
.promotion-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  margin-bottom: 10px;
}
.promotion-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}
.promotion-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
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
