<template>
  <div class="home-page">
    <!-- Banner 轮播：渐变背景 + 真实商品构图 -->
    <section class="banner-section">
      <el-carousel height="400px" :interval="5000" arrow="always" indicator-position="outside">
        <el-carousel-item v-for="(b, i) in banners" :key="i">
          <div class="banner-slide" :style="{ background: b.bg }">
            <div class="banner-content">
              <span class="banner-chip">{{ b.chip }}</span>
              <h2 class="banner-title">{{ b.title }}</h2>
              <p class="banner-desc">{{ b.desc }}</p>
              <div class="banner-actions">
                <el-button round type="primary" class="banner-btn" @click="$router.push('/search')">立即选购</el-button>
                <el-button round plain class="banner-btn ghost" @click="$router.push('/category')">浏览分类</el-button>
              </div>
            </div>
            <div class="banner-art" aria-hidden="true">
              <div class="art-card art-card-1">
                <img v-lazy-img :data-src="b.img1" :data-seed="b.seed1" alt="" />
              </div>
              <div class="art-card art-card-2">
                <img v-lazy-img :data-src="b.img2" :data-seed="b.seed2" alt="" />
              </div>
              <div class="art-card art-card-3">
                <img v-lazy-img :data-src="b.img3" :data-seed="b.seed3" alt="" />
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 限时活动 -->
    <section v-if="promotions.length > 0" class="promotions-section">
      <div class="section-title">
        限时活动
        <span class="section-sub">🔥 热门促销进行中</span>
      </div>
      <div class="promotion-list">
        <div v-for="promo in promotions" :key="promo.id" class="promotion-card">
          <div class="promotion-tag">{{ typeMap[promo.type] }}</div>
          <div class="promotion-name">{{ promo.name }}</div>
          <div class="promotion-desc">{{ promo.description }}</div>
        </div>
      </div>
    </section>

    <!-- 分类导航（卡片网格 + emoji 图标） -->
    <section class="category-section">
      <div class="section-title">
        全部分类
        <span class="section-sub">按需浏览商品</span>
      </div>
      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-card card-hover"
          @click="goCategory(cat.id)"
        >
          <div class="category-icon">{{ categoryEmoji(cat.name) }}</div>
          <span class="category-name">{{ cat.name }}</span>
        </div>
      </div>
    </section>

    <!-- 猜你喜欢（AI 推荐） -->
    <section class="recommend-section">
      <div class="section-title">
        <span class="ai-badge" style="margin-right: 10px">AI 推荐</span>
        猜你喜欢
        <span class="section-sub">基于协同过滤算法，为你发现好物</span>
      </div>
      <RecommendList mode="guess" />
    </section>

    <!-- A-1 浏览历史推荐（仅登录用户显示） -->
    <section v-if="isLogin" class="recommend-section">
      <div class="section-title">
        <span class="ai-badge" style="margin-right: 10px">AI 推荐</span>
        浏览历史推荐
        <span class="section-sub">根据你的足迹精选</span>
      </div>
      <RecommendList mode="history" />
    </section>

    <!-- D-5 购买推荐（仅登录用户显示，无购买记录时后端返回空） -->
    <section v-if="isLogin" class="recommend-section">
      <div class="section-title">
        <span class="ai-badge" style="margin-right: 10px">AI 推荐</span>
        购买过同类
        <span class="section-sub">买了又买，回购好物</span>
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
import { categoryEmoji } from '@/utils/category'

const router = useRouter()
const userStore = useUserStore()
// A-1：登录态判断（token 存在即视为已登录），控制"浏览历史推荐"区块显示
const isLogin = computed(() => !!userStore.token)
const categories = ref([])
const promotions = ref([])
const typeMap = { 1: '折扣', 2: '满减', 3: '满赠', 4: '套餐' }

// Banner：渐变背景 + 右侧真实商品构图（picsum 确定性图源，同 seed 恒同图）
const banners = [
  {
    chip: 'AI 智能推荐',
    title: '为你发现好物',
    desc: '基于协同过滤算法，越逛越懂你',
    bg: 'linear-gradient(120deg, #4F46E5 0%, #6D28D9 55%, #8B5CF6 100%)',
    img1: 'https://picsum.photos/seed/banner-phone-1/360/360',
    seed1: 'banner-phone-1',
    img2: 'https://picsum.photos/seed/banner-watch-1/300/300',
    seed2: 'banner-watch-1',
    img3: 'https://picsum.photos/seed/banner-shoe-1/260/260',
    seed3: 'banner-shoe-1'
  },
  {
    chip: '品质好货',
    title: '精选好物 正品保障',
    desc: '严选商家，正品承诺，放心购',
    bg: 'linear-gradient(120deg, #0EA5E9 0%, #2563EB 55%, #6366F1 100%)',
    img1: 'https://picsum.photos/seed/banner-cam-1/340/340',
    seed1: 'banner-cam-1',
    img2: 'https://picsum.photos/seed/banner-head-1/280/280',
    seed2: 'banner-head-1',
    img3: 'https://picsum.photos/seed/banner-bag-1/240/240',
    seed3: 'banner-bag-1'
  },
  {
    chip: '限时优惠',
    title: '每日特价 不容错过',
    desc: '爆款直降，限时抢购',
    bg: 'linear-gradient(120deg, #7C3AED 0%, #A855F7 55%, #D946EF 100%)',
    img1: 'https://picsum.photos/seed/banner-perf-1/340/340',
    seed1: 'banner-perf-1',
    img2: 'https://picsum.photos/seed/banner-lamp-1/280/280',
    seed2: 'banner-lamp-1',
    img3: 'https://picsum.photos/seed/banner-cup-1/240/240',
    seed3: 'banner-cup-1'
  }
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
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(79, 70, 229, 0.14);
}
.banner-slide {
  height: 400px;
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 64px;
  overflow: hidden;
}
.banner-content {
  position: relative;
  z-index: 3;
  color: #fff;
  max-width: 520px;
}
.banner-chip {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.28);
  margin-bottom: 16px;
}
.banner-title {
  font-size: 40px;
  font-weight: 800;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
  line-height: 1.2;
}
.banner-desc {
  font-size: 17px;
  opacity: 0.88;
  margin-bottom: 28px;
}
.banner-actions {
  display: flex;
  gap: 12px;
}
.banner-btn {
  border: none;
  font-weight: 600;
  padding: 10px 26px;
}
.banner-btn.ghost {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.35);
}
.banner-btn.ghost:hover {
  background: rgba(255, 255, 255, 0.26);
  color: #fff;
}
.banner-art {
  position: absolute;
  right: 60px;
  top: 0;
  bottom: 0;
  width: 420px;
  pointer-events: none;
}
.art-card {
  position: absolute;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.28);
  border: 3px solid rgba(255, 255, 255, 0.55);
  animation: art-float 6s ease-in-out infinite;
}
.art-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.art-card-1 {
  width: 200px;
  height: 200px;
  top: 42px;
  right: 120px;
  transform: rotate(4deg);
}
.art-card-2 {
  width: 150px;
  height: 150px;
  top: 210px;
  right: 12px;
  animation-delay: 1.2s;
  transform: rotate(-6deg);
}
.art-card-3 {
  width: 130px;
  height: 130px;
  top: 230px;
  right: 190px;
  animation-delay: 2.4s;
  transform: rotate(10deg);
}
@keyframes art-float {
  0%,
  100% {
    translate: 0 0;
  }
  50% {
    translate: 0 -10px;
  }
}
/* Section header */
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}
/* 限时活动 */
.promotions-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin: 28px 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.promotion-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.promotion-card {
  border: 1px solid #f1f0f5;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
  cursor: default;
}
.promotion-card:hover {
  border-color: #fca5a5;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.1);
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
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
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
}
.category-card:hover {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.15);
}
.category-card:hover .category-name {
  color: #fff;
}
.category-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #e0e7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  line-height: 1;
}
.category-card:hover .category-icon {
  background: rgba(255, 255, 255, 0.2);
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
@media (max-width: 900px) {
  .banner-art {
    display: none;
  }
  .banner-slide {
    padding: 0 32px;
  }
}
</style>
