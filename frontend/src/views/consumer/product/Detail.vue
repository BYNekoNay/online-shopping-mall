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
                <el-image :src="img" fit="contain" style="width: 100%; height: 100%" />
              </el-carousel-item>
            </el-carousel>
            <el-image v-else :src="product.mainImage" fit="contain" style="width: 400px; height: 400px" />
          </div>
          <div class="info-section">
            <h1>{{ product.name }}</h1>
            <div class="price-row">
              <span class="price">¥{{ product.price }}</span>
              <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
            </div>
            <div class="stock-info">
              <span v-if="product.stock > 0">库存：{{ product.stock }} | 销量：{{ product.sales }}</span>
              <span v-else class="sold-out-text">已售罄</span>
            </div>
            <div class="promotion-tag" v-if="product.activePromotion">
              {{ formatPromotionTag(product.activePromotion) }}
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
            <div class="actions">
              <el-button type="primary" size="large" :disabled="!canAddToCart" @click="addToCart">
                {{ canAddToCartText }}
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
          <h3>商品评价 ({{ rating?.totalCount || 0 }})</h3>
          <!-- 评分概况 -->
          <div v-if="rating" class="rating-summary">
            <div class="average-rating">
              <span class="rating-score">{{ rating.averageRating }}</span>
              <el-rate :model-value="Math.round(rating.averageRating)" disabled />
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
import { useCartStore } from '@/store/cart'
import RecommendList from '@/components/RecommendList.vue'
// C-3：富文本详情 XSS 白名单过滤
import DOMPurify from 'dompurify'

const route = useRoute()
const cartStore = useCartStore()
const productId = computed(() => Number(route.params.id))
const loading = ref(false)
const product = ref({})
const selectedSkuId = ref(null)
const reviews = ref([])
const rating = ref(null)

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

// D-1 多图轮播：images 逗号分隔 → 数组；空则回退主图
const galleryImages = computed(() => {
  const raw = product.value.images
  if (!raw) return [product.value.mainImage].filter(Boolean)
  const list = raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return list.length > 0 ? list : [product.value.mainImage].filter(Boolean)
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
  margin: 20px auto;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
}
.detail-content {
  display: flex;
  gap: 30px;
}
.image-section {
  flex-shrink: 0;
}
.info-section {
  flex: 1;
}
.info-section h1 {
  font-size: 22px;
  margin-bottom: 15px;
}
.price-row {
  margin: 20px 0;
}
.price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
}
.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
  margin-left: 10px;
}
.stock-info {
  color: #666;
  margin-bottom: 15px;
}
.sold-out-text {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}
.sold-out-tip {
  color: #f56c6c;
  font-size: 14px;
  margin-top: 8px;
}
.promotion-tag {
  display: inline-block;
  padding: 5px 12px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  margin-bottom: 15px;
}
.sku-section {
  margin: 20px 0;
}
.sku-options {
  margin-top: 10px;
}
.actions {
  margin-top: 30px;
}
.similar-section {
  margin-top: 40px;
}
.similar-section h3 {
  margin-bottom: 15px;
}
/* C-3 商品详情展示区 */
.product-description {
  margin-top: 30px;
}
.product-description h3 {
  margin-bottom: 15px;
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
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e4e7ed;
}
.reviews-section h3 {
  font-size: 20px;
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
  color: #f56c6c;
}
.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-item {
  padding: 16px 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.2s;
}
.review-item:hover {
  border-color: #409eff;
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
  color: #999;
}
</style>
