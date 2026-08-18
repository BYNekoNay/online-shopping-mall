<template>
  <div class="product-detail">
    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="image" style="width: 400px; height: 400px" />
        <div style="flex: 1; padding-left: 30px">
          <el-skeleton :rows="5" />
        </div>
      </template>
      <template #default>
        <div class="detail-content">
          <div class="image-section">
            <!-- D-1 多图轮播：images 逗号分隔多图 → el-carousel；单图/无多图用主图 -->
            <el-carousel v-if="galleryImages.length > 1" height="400px" indicator-position="outside" arrow="always">
              <el-carousel-item v-for="(img, idx) in galleryImages" :key="idx">
                <el-image :src="img" fit="contain" style="width: 100%; height: 100%" :preview-src-list="galleryImages" />
              </el-carousel-item>
            </el-carousel>
            <el-image
              v-else
              :src="galleryImages[0] || ''"
              fit="contain"
              style="width: 400px; height: 400px"
              :preview-src-list="galleryImages"
            />
          </div>
          <div class="info-section">
            <div class="title-row">
              <h1>{{ product.name }}</h1>
              <span v-if="product.activePromotion" class="detail-promo-ribbon">
                {{ formatPromotionTag(product.activePromotion) }}
              </span>
            </div>
            <div class="price-panel">
              <div class="price-row">
                <span class="price">¥{{ product.price }}</span>
                <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
                <span class="price-off" v-if="discountRate">约 {{ discountRate }} 折</span>
              </div>
              <div class="rating-sales-row">
                <span class="rating-stars" aria-hidden="true">
                  <span v-for="idx in 5" :key="idx" class="star" :class="{ filled: idx <= starRating }">★</span>
                </span>
                <span v-if="averageRating > 0" class="rating-num">{{ averageRating }} 分</span>
                <span v-else class="rating-num muted">暂无评分</span>
                <span class="dot-sep"></span>
                <span class="sales-num">销量 {{ product.sales || 0 }}</span>
                <span class="dot-sep"></span>
                <span class="stock-num" :class="{ 'sold-out-text': product.stock === 0 }">
                  {{ product.stock > 0 ? `库存 ${product.stock}` : '已售罄' }}
                </span>
              </div>
            </div>
            <div class="sku-section" v-if="product.skuList && product.skuList.length > 0">
              <h4>选择规格</h4>
              <div class="sku-options">
                <el-radio-group v-model="selectedSkuId" @change="onSkuChange">
                  <el-radio v-for="sku in product.skuList" :key="sku.id" :label="sku.id" border>
                    {{ sku.specJson }} ¥{{ sku.price }}
                  </el-radio>
                </el-radio-group>
              </div>
              <div v-if="selectedSku && selectedSku.stock === 0" class="sold-out-tip">该规格已售罄</div>
            </div>
            <div class="service-row">
              <span class="service-item">✓ 正品保障</span>
              <span class="service-item">✓ 七天无理由退换</span>
              <span class="service-item">✓ 极速发货</span>
            </div>
            <div class="actions">
              <el-button type="primary" size="large" :disabled="!canAddToCart" @click="addToCart">
                {{ canAddToCartText }}
              </el-button>
              <!-- FRONT-10 修复：收藏/取消收藏入口（此前收藏功能无任何 UI 入口，功能不可达） -->
              <el-button v-if="isLogin" size="large" :type="isFavorited ? 'warning' : 'default'" plain @click="toggleFavorite">
                {{ isFavorited ? '已收藏 ♥' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>
        <!-- C-3 商品详情（富文本，DOMPurify 白名单过滤） -->
        <div v-if="safeDetail" class="product-description">
          <h3>商品详情</h3>
          <!-- eslint-disable-next-line vue/no-v-html -- C-3 已用 DOMPurify 白名单过滤（safeDetail），非直接渲染用户输入 -->
          <div class="description-body" v-html="safeDetail"></div>
        </div>

        <div class="similar-section">
          <h3>相似商品推荐</h3>
          <RecommendList mode="similar" :product-id="productId" />
        </div>

        <!-- 商品评价 -->
        <div class="reviews-section">
          <h3>商品评价 ({{ rating?.reviewCount || 0 }})</h3>
          <!-- 评分概况 -->
          <div v-if="rating" class="rating-summary">
            <div class="average-rating">
              <span class="rating-score">{{ rating.avgRating }}</span>
              <el-rate :model-value="Math.round(Number(rating.avgRating) || 0)" disabled />
              <span class="rating-total">共 {{ rating.reviewCount || 0 }} 条评价</span>
            </div>
          </div>
          <!-- 评价列表 -->
          <div v-if="reviews.length > 0" class="reviews-list">
            <div v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-user">{{ review.userNickname || '匿名用户' }}</div>
              <el-rate :model-value="review.rating" disabled />
              <p class="review-content">{{ review.content }}</p>
              <span class="review-time">{{ review.createTime?.replace('T', ' ') }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无评价" />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/product'
import { addToCart as addToCartApi } from '@/api/cart'
import { getFavorites, favoriteProduct, unfavoriteProduct } from '@/api/user'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import RecommendList from '@/components/RecommendList.vue'
// C-3：富文本详情 XSS 白名单过滤
import DOMPurify from 'dompurify'
import { resolveImg } from '@/utils/image'

const route = useRoute()
const cartStore = useCartStore()
const userStore = useUserStore()
// 雪花 ID（19 位 Long）超 JS Number 安全整数，转字符串避免 JSON.parse 后丢精度；
// 后端 Long 可直接接受字符串参数，路由 params 本身就是字符串。
const productId = computed(() => String(route.params.id ?? ''))
const loading = ref(false)
const product = ref({})
const selectedSkuId = ref(null)
const reviews = ref([])
const rating = ref(null)
// FRONT-10 修复：收藏状态（登录用户进入详情时从收藏列表判断）
const isLogin = computed(() => !!userStore.token)
const isFavorited = ref(false)

// C-3：商品详情经 DOMPurify 白名单过滤后再 v-html 渲染（防 XSS）
const safeDetail = computed(() => {
  const detail = product.value.detail
  if (!detail) return ''
  return DOMPurify.sanitize(detail, {
    ALLOWED_TAGS: [
      'p',
      'br',
      'img',
      'strong',
      'em',
      'ul',
      'ol',
      'li',
      'h1',
      'h2',
      'h3',
      'blockquote',
      'a',
      'span',
      'div',
      'table',
      'tr',
      'td',
      'th'
    ],
    ALLOWED_ATTR: ['src', 'alt', 'href', 'title', 'style', 'width', 'height'],
    ALLOWED_STYLE_PROPERTIES: ['color', 'font-size', 'font-weight', 'text-align', 'background-color']
  })
})

// D-1 多图轮播：images 逗号分隔 → 数组；空则回退主图；全部经占位图兜底
const galleryImages = computed(() => {
  const raw = product.value.images
  const seed = productId.value || product.value.id || 'default'
  let list = []
  if (raw) {
    list = raw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  }
  if (list.length === 0 && product.value.mainImage) list = [product.value.mainImage]
  if (list.length === 0) list = ['']
  return list.map((src) => resolveImg(src, seed, 600, 600))
})

const selectedSku = computed(() => {
  if (!product.value.skuList || !selectedSkuId.value) return null
  return product.value.skuList.find((s) => s.id === selectedSkuId.value) || null
})

const canAddToCart = computed(() => {
  if (product.value.stock === 0) return false
  if (product.value.skuList && product.value.skuList.length > 0) {
    if (selectedSkuId.value === null) return false
    return (selectedSku.value?.stock || 0) > 0
  }
  return product.value.stock > 0
})

const canAddToCartText = computed(() => {
  if (product.value.stock === 0) return '已售罄'
  if (product.value.skuList && product.value.skuList.length > 0) {
    if (selectedSkuId.value === null) return '请选择规格'
    if ((selectedSku.value?.stock || 0) === 0) return '该规格已售罄'
  }
  return '加入购物车'
})

// 评分星（四舍五入到整星）
// FRONT-04 修复：后端 ProductRatingVO 字段为 avgRating/reviewCount（此前误用 averageRating/totalCount）
const starRating = computed(() => {
  const avg = Number(product.value.rating ?? rating.value?.avgRating ?? 0)
  if (!Number.isFinite(avg) || avg <= 0) return 0
  return Math.min(5, Math.round(avg))
})

const averageRating = computed(() => {
  const avg = Number(product.value.rating ?? rating.value?.avgRating ?? 0)
  return Number.isFinite(avg) && avg > 0 ? avg.toFixed(1) : 0
})

// 折扣率（原价 > 现价时展示）
const discountRate = computed(() => {
  const price = Number(product.value.price)
  const original = Number(product.value.originalPrice)
  if (!Number.isFinite(price) || !Number.isFinite(original) || original <= 0 || price >= original) return ''
  return ((price / original) * 10).toFixed(1)
})

function formatPromotionTag(promotion) {
  const typeMap = { 1: '折扣', 2: '满减', 3: '满赠', 4: '套餐' }
  return `${typeMap[promotion.type] || '促销'}: ${promotion.name}`
}

function onSkuChange(skuId) {
  selectedSkuId.value = skuId
}

async function addToCart() {
  try {
    const skuId = selectedSkuId.value || (product.value.skuList && product.value.skuList[0]?.id) || null
    await addToCartApi({ productId: product.value.id, skuId, quantity: 1 })
    ElMessage.success('已加入购物车')
    cartStore.fetchList()
  } catch {
    // error handled by interceptor
  }
}

// FRONT-10 修复：加载收藏状态（登录用户从收藏列表判断当前商品是否已收藏）
async function loadFavoriteState() {
  if (!isLogin.value || !productId.value) return
  try {
    const favorites = (await getFavorites()) || []
    isFavorited.value = favorites.some((f) => String(f.productId ?? f.id ?? '') === productId.value)
  } catch {
    isFavorited.value = false
  }
}

// FRONT-10 修复：收藏/取消收藏切换
async function toggleFavorite() {
  try {
    if (isFavorited.value) {
      await unfavoriteProduct(productId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteProduct(productId.value)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // error handled by interceptor（未登录会跳转登录页）
  }
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await request.getProduct(productId.value)
    // 加载评价和评分
    const [reviewsRes, ratingRes] = await Promise.all([
      request.getProductReviews(productId.value).catch(() => []),
      request.getProductRating(productId.value).catch(() => null)
    ])
    reviews.value = reviewsRes?.records || []
    rating.value = ratingRes || null
    // FRONT-10 修复：加载收藏状态（登录用户）
    loadFavoriteState()
  } catch {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}
.detail-content {
  display: flex;
  gap: 40px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}
.image-section {
  flex-shrink: 0;
  width: 400px;
}
.image-section :deep(.el-carousel__container) {
  border-radius: 14px;
  background: #f8fafc;
}
.image-section :deep(.el-image) {
  background: #f8fafc;
  border-radius: 14px;
}
.info-section {
  flex: 1;
  min-width: 0;
}
.title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.info-section h1 {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
  flex: 1;
}
.detail-promo-ribbon {
  flex-shrink: 0;
  margin-top: 4px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #f97316);
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}
.price-panel {
  margin: 18px 0;
  background: linear-gradient(135deg, #fff7f7, #fff);
  border: 1px solid #fee2e2;
  border-radius: 14px;
  padding: 18px 20px;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.price {
  font-size: 32px;
  color: #ef4444;
  font-weight: 800;
  line-height: 1;
}
.original-price {
  font-size: 15px;
  color: #b0b7c3;
  text-decoration: line-through;
}
.price-off {
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #ef4444;
  background: #fff1f1;
  border-radius: 6px;
}
.rating-sales-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.rating-stars .star.filled {
  color: #f59e0b;
}
.rating-num {
  font-size: 13px;
  color: #f59e0b;
  font-weight: 600;
}
.rating-num.muted {
  color: #94a3b8;
  font-weight: 400;
}
.dot-sep {
  width: 1px;
  height: 14px;
  background: #e2e8f0;
}
.sales-num,
.stock-num {
  font-size: 13px;
  color: #64748b;
}
.sold-out-text {
  color: #ef4444;
  font-weight: 600;
}
.sku-section {
  margin: 20px 0;
}
.sku-section h4 {
  font-size: 14px;
  color: #334155;
  margin-bottom: 10px;
}
.sku-options {
  margin-top: 10px;
}
.sold-out-tip {
  color: #ef4444;
  font-size: 14px;
  margin-top: 8px;
}
.service-row {
  display: flex;
  gap: 20px;
  padding: 14px 0;
  border-top: 1px dashed #eef0f4;
  border-bottom: 1px dashed #eef0f4;
  flex-wrap: wrap;
}
.service-item {
  font-size: 13px;
  color: #059669;
}
.actions {
  margin-top: 24px;
  display: flex;
  gap: 14px;
}
.actions .el-button {
  min-width: 140px;
}
.similar-section {
  margin-top: 40px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 24px;
}
.similar-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
  color: #0f172a;
}
/* C-3 商品详情展示区 */
.product-description {
  margin-top: 24px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 24px;
}
.product-description h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #0f172a;
}
.description-body {
  line-height: 1.8;
  font-size: 14px;
  color: #333;
  word-break: break-word;
}
.description-body img {
  max-width: 100%;
  height: auto;
}
/* 评价区域 */
.reviews-section {
  margin-top: 24px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 24px;
}
.reviews-section h3 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #0f172a;
}
.rating-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 24px;
}
.average-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}
.rating-score {
  font-size: 36px;
  font-weight: 700;
  color: #f59e0b;
}
.rating-total {
  font-size: 13px;
  color: #94a3b8;
}
.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-item {
  padding: 16px 20px;
  border: 1px solid #eef0f4;
  border-radius: 12px;
  transition: all 0.2s;
}
.review-item:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.06);
}
.review-user {
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}
.review-content {
  color: #555;
  line-height: 1.6;
  margin: 8px 0;
}
.review-time {
  font-size: 12px;
  color: #94a3b8;
}
@media (max-width: 900px) {
  .detail-content {
    flex-direction: column;
    padding: 20px;
  }
  .image-section {
    width: 100%;
  }
}
</style>
