<template>
  <div class="recommend-list">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="product-grid">
      <div v-for="n in 8" :key="n" class="skeleton-card">
        <div class="skeleton-block skeleton-image"></div>
        <div class="skeleton-block skeleton-line w60"></div>
        <div class="skeleton-block skeleton-line w40"></div>
      </div>
    </div>

    <!-- 推荐商品卡片（现代电商风：评分/销量/促销角标/加购按钮） -->
    <el-row v-else :gutter="20">
      <el-col
        :xs="12"
        :sm="8"
        :md="6"
        :lg="4"
        v-for="(item, index) in list"
        :key="item.productId"
        style="margin-bottom: 20px"
      >
        <el-card class="product-card-modern" :body-style="{ padding: '0px' }" shadow="never" @click="handleClick(item, index)">
          <div class="card-image-wrap">
            <img
              :src="resolvedSrc(item)"
              :data-seed="seedOf(item)"
              :alt="item.name"
              class="card-image"
              loading="lazy"
              @error="handleImgError"
            />
            <span v-if="tagOf(item)" class="card-tag">{{ tagOf(item) }}</span>
            <button type="button" class="card-add-btn" title="加入购物车" @click.stop="handleAddToCart(item)">
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
              <span class="card-stars" aria-hidden="true">
                <span v-for="idx in 5" :key="idx" class="star" :class="{ filled: idx <= ratingOf(item) }">★</span>
              </span>
              <span v-if="ratingOf(item) > 0" class="card-rating-num">{{ ratingTextOf(item) }}</span>
              <span v-else class="card-rating-num muted">暂无评分</span>
              <span v-if="salesOf(item) > 0" class="card-sales">已售{{ formatSales(salesOf(item)) }}</span>
            </div>
            <div class="card-price-row">
              <span class="card-price">¥{{ formatPrice(item.price) }}</span>
              <span v-if="item.originalPrice" class="card-original-price">¥{{ formatPrice(item.originalPrice) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && (!list || list.length === 0)" description="暂无推荐" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  getRecommendations,
  getSimilarProducts,
  getHistoryRecommendations,
  getPurchaseRecommendations
} from '@/api/recommend'
import { recommendExposure, recommendClick } from '@/api/behavior'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const props = defineProps({
  mode: { type: String, default: 'guess' },
  productId: { type: [String, Number], default: null }
})

const router = useRouter()
const list = ref([])
const exposed = ref(false)
const loading = ref(true)

// A-1/D-5 扩展：source 语义映射（guess=首页猜你喜欢 / similar=商品详情相似 / history=浏览历史推荐 / purchase=购买推荐）
const SOURCE_MAP = {
  guess: 'home-guess',
  similar: 'product-similar',
  history: 'home-history',
  purchase: 'home-purchase'
}
const source = () => SOURCE_MAP[props.mode] || 'home-guess'

/** FRONT-01 修复：埋点仅登录用户生效——无 token 直接跳过，避免匿名用户触发
 *  未登录接口 10002 被请求拦截器强制跳转登录页（阻断匿名浏览）。
 *  直接读 localStorage（与 utils/request.js 同源），避免在纯展示组件引入 store 依赖。 */
function hasToken() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user && user.token) return true
  } catch {
    // 忽略损坏数据
  }
  return !!localStorage.getItem('token')
}

/** 上报推荐位曝光（仅一次，仅登录用户）。 */
function reportExposure() {
  if (exposed.value || !hasToken() || !list.value || list.value.length === 0) return
  exposed.value = true
  const productIds = list.value.map((i) => i.productId)
  recommendExposure({ source: source(), productIds }).catch(() => {})
}

/** 上报推荐位点击（记为浏览行为，仅登录用户）。 */
async function handleClick(item, index) {
  if (hasToken()) {
    recommendClick({
      source: source(),
      productId: item.productId,
      position: index + 1 // 1-based
    }).catch(() => {})
  }

  // 跳转商品详情，携带推荐来源标记（供后端统计点击归因）
  router.push({ path: `/product/${item.productId}`, query: { from: 'recommend' } })
}

// ============ 现代电商卡片展示辅助 ============

/** 商品主键（推荐接口统一返回 productId）。 */
function seedOf(item) {
  const seed = item?.productId ?? item?.id ?? 'default'
  return String(seed)
}

/** 占位图/空图 → picsum 确定性兜底图。 */
function resolvedSrc(item) {
  return resolveImg(item?.mainImage || '', seedOf(item), 400, 400)
}

/** 图片加载失败时的兜底处理（防止 picsum 本身不可达时显示破图）。 */
function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 400, 400)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
}

/** 评分（四舍五入到整数星，0 表示无评分）。
 *  FRONT-QA-02 修复：取真实商品评分 item.rating；score 为推荐算法分数，不再当作星级展示。 */
function ratingOf(item) {
  const raw = Number(item?.rating ?? 0)
  if (!Number.isFinite(raw) || raw <= 0) return 0
  return Math.min(5, Math.round(raw))
}

function ratingTextOf(item) {
  const raw = Number(item?.rating ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw.toFixed(1) : ''
}

function salesOf(item) {
  const raw = Number(item?.sales ?? item?.soldCount ?? 0)
  return Number.isFinite(raw) && raw > 0 ? raw : 0
}

/** 促销角标文案：优先取商品标签/促销名。 */
function tagOf(item) {
  const p = item || {}
  if (p.tag) return p.tag
  if (Array.isArray(p.tagList) && p.tagList.length) return p.tagList[0]
  if (typeof p.tags === 'string' && p.tags.trim()) return p.tags.split(',')[0].trim()
  if (Array.isArray(p.tags) && p.tags.length) return p.tags[0]
  if (p.activePromotion && p.activePromotion.name) return p.activePromotion.name
  if (p.promotionName) return p.promotionName
  return ''
}

function formatPrice(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function formatSales(value) {
  const num = Number(value)
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return String(num)
}

/** 加购：动态 import 避免在埋点测试环境引入额外模块依赖。 */
async function handleAddToCart(item) {
  if (!item?.productId) return
  const [{ addToCart: addToCartApi }, { useCartStore }, { ElMessage }] = await Promise.all([
    import('@/api/cart'),
    import('@/store/cart'),
    import('element-plus')
  ])
  try {
    await addToCartApi({ productId: item.productId, skuId: null, quantity: 1 })
    ElMessage.success('已加入购物车')
    useCartStore().fetchList()
  } catch {
    // 未登录/参数错误由请求拦截器统一提示
  }
}

onMounted(async () => {
  loading.value = true
  try {
    if (props.mode === 'similar' && props.productId) {
      list.value = await getSimilarProducts(props.productId)
    } else if (props.mode === 'history') {
      list.value = await getHistoryRecommendations()
    } else if (props.mode === 'purchase') {
      list.value = await getPurchaseRecommendations()
    } else {
      list.value = await getRecommendations()
    }
    // 数据加载完成后上报曝光（nextTick 确保 DOM 已渲染，用户真实可见）
    await nextTick()
    reportExposure()
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  // 组件销毁时不再补曝（避免离开页面前重复上报）
})
</script>

<style scoped>
.recommend-list {
  min-height: 120px;
}
/* 卡片视觉样式见全局 theme.css（.product-card-modern 及子类），此处仅保留布局兜底 */
</style>
