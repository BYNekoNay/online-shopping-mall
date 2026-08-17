<template>
  <div class="product-card-modern" @click="handleClick">
    <div class="card-image-wrap">
      <img
        v-lazy-img
        :data-src="resolvedImage"
        :data-seed="seed"
        :alt="item.name"
        class="card-image"
      />
      <span v-if="tagText" class="card-tag">{{ tagText }}</span>
      <button v-if="showAdd" type="button" class="card-add-btn" title="加入购物车" @click.stop="handleAddToCart">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="9" cy="21" r="1.4" />
          <circle cx="19" cy="21" r="1.4" />
          <path d="M2 2h2.5l2.6 12.4a1.8 1.8 0 0 0 1.8 1.4h9.6a1.8 1.8 0 0 0 1.8-1.4L22 6H5.6" />
        </svg>
      </button>
    </div>
    <div class="card-info">
      <div class="card-name">{{ item.name }}</div>
      <div class="card-meta-row">
        <span class="card-stars" :class="{ 'is-empty': ratingValue === 0 }" aria-hidden="true">
          <span v-for="idx in 5" :key="idx" class="star" :class="{ filled: idx <= ratingValue }">★</span>
        </span>
        <span v-if="ratingValue > 0" class="card-rating-num">{{ ratingText }}</span>
        <span v-else class="card-rating-num muted">暂无评分</span>
        <span v-if="sales > 0" class="card-sales">已售{{ formatSales(sales) }}</span>
      </div>
      <div class="card-price-row">
        <span class="card-price">¥{{ formatPrice(item.price) }}</span>
        <span v-if="item.originalPrice" class="card-original-price">¥{{ formatPrice(item.originalPrice) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resolveImg } from '@/utils/image'
import { addToCart as addToCartApi } from '@/api/cart'
import { useCartStore } from '@/store/cart'

/**
 * 现代电商风商品卡片（消费者端商品网格通用）。
 * 白色卡片 + 商品图 + 评分销量 + 促销角标 + 加购悬浮按钮 + hover 光影。
 * 支持 id / productId 两种主键形态（搜索/分类返回 id，推荐接口返回 productId）。
 */
const props = defineProps({
  item: { type: Object, required: true },
  /** 是否显示“加入购物车”悬浮按钮，默认显示 */
  showAdd: { type: Boolean, default: true }
})

const router = useRouter()
const cartStore = useCartStore()

const productId = computed(() => props.item?.productId ?? props.item?.id ?? null)
const seed = computed(() => String(productId.value ?? props.item?.goodsId ?? 'default'))

const resolvedImage = computed(() =>
  resolveImg(props.item?.mainImage || props.item?.image || '', seed.value, 400, 400)
)

// FRONT-QA-02 修复：评分取真实商品评分 item.rating（后端 ProductVO/FavoriteVO 已填充 rating），
// 不再回退到 score（score 为推荐算法分，不应作为星级展示）
const ratingValue = computed(() => {
  const raw = props.item?.rating ?? 0
  const num = Number(raw)
  if (!Number.isFinite(num) || num <= 0) return 0
  return Math.min(5, Math.round(num))
})

const ratingText = computed(() => {
  const raw = Number(props.item?.rating ?? 0)
  return Number.isFinite(raw) && raw > 0 ? Number(raw).toFixed(1) : ''
})

const sales = computed(() => {
  const raw = props.item?.sales ?? props.item?.soldCount ?? 0
  const num = Number(raw)
  return Number.isFinite(num) && num > 0 ? num : 0
})

const tagText = computed(() => {
  const p = props.item || {}
  if (p.tag) return p.tag
  if (Array.isArray(p.tagList) && p.tagList.length) return p.tagList[0]
  if (typeof p.tags === 'string' && p.tags.trim()) return p.tags.split(',')[0].trim()
  if (Array.isArray(p.tags) && p.tags.length) return p.tags[0]
  if (p.activePromotion && p.activePromotion.name) return p.activePromotion.name
  if (p.promotionName) return p.promotionName
  return ''
})

function formatPrice(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function formatSales(value) {
  const num = Number(value)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return String(num)
}

function handleClick() {
  if (productId.value == null) return
  router.push(`/product/${productId.value}`)
}

async function handleAddToCart() {
  if (productId.value == null) return
  try {
    await addToCartApi({ productId: productId.value, skuId: null, quantity: 1 })
    ElMessage.success('已加入购物车')
    cartStore.fetchList()
  } catch {
    // 未登录/参数错误由请求拦截器统一提示
  }
}
</script>

<style scoped>
/* 卡片视觉样式见全局 theme.css（.product-card-modern 及子类），此处仅保留组件特有兜底 */
</style>
