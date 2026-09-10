<template>
  <div class="category-page">
    <div class="category-sidebar">
      <div class="sidebar-title">商品分类</div>
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="category-item"
        :class="{ active: selectedCategoryId === String(cat.id) }"
        @click="selectCategory(cat.id)"
      >
        <span class="item-emoji">{{ categoryEmoji(cat.name) }}</span>
        {{ cat.name }}
      </div>
    </div>
    <div class="product-area" ref="productAreaRef">
      <div class="selected-header">
        <h3>{{ currentCategoryName || '全部商品' }}</h3>
        <!-- A1：展示后端真实总数（total），而非当前页加载条数 products.length -->
        <span class="header-count">{{ total }} 件商品</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in products" :key="item.id" style="margin-bottom: 20px">
          <ProductCard :item="item" />
        </el-col>
      </el-row>
      <!-- A2：商品列表分页（任务书「商品浏览-商品列表（分页）」合规要求） -->
      <el-pagination
        v-if="total > 0"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 20px; justify-content: center"
        @current-change="handlePageChange"
      />
      <el-empty v-if="products.length === 0 && selectedCategoryId" description="该分类下暂无商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'
import { categoryEmoji } from '@/utils/category'

const route = useRoute()
const categories = ref([])
const products = ref([])
const selectedCategoryId = ref(null)
const categoryNameMap = ref({})
// A2：分页状态。pageSize 取 12（栅格 :xs="12" :sm="8" :md="6" :lg="6" 即 2/3/4 列，
// 12 能被 2、3、4 整除，尾行不会出现残缺；此处相对 admin 的 10 属有意偏离）。
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
// A2：商品区 DOM 引用，翻页后滚动回顶部使用
const productAreaRef = ref(null)

const currentCategoryName = computed(() => categoryNameMap.value[selectedCategoryId.value] || '')

async function loadCategories() {
  try {
    const data = await request.getCategories()
    categories.value = data || []
    // Build name map
    const buildMap = (list) => {
      list.forEach((c) => {
        categoryNameMap.value[c.id] = c.name
        if (c.children) buildMap(c.children)
      })
    }
    buildMap(categories.value)
  } catch {
    categories.value = []
  }
}

// A3：递归查找分类树（支持 children 嵌套），ID 一律用字符串比较，
// 避免 19 位雪花 ID 经 Number() 丢精度（docs/08 §3.7）。
function findCategoryById(tree, id) {
  if (!Array.isArray(tree)) return null
  for (const c of tree) {
    if (String(c.id) === String(id)) return c
    if (c.children && c.children.length) {
      const found = findCategoryById(c.children, id)
      if (found) return found
    }
  }
  return null
}

async function loadProducts(categoryId = selectedCategoryId.value) {
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    // categoryId 一律传字符串（docs/08 §3.7）
    if (categoryId !== null && categoryId !== undefined && categoryId !== '') {
      params.categoryId = String(categoryId)
    }
    const data = await request.getProducts(params)
    products.value = data.records || data || []
    // A1：取后端真实总数，缺失时回落当前页条数
    total.value = data.total ?? data.records?.length ?? 0
  } catch {
    products.value = []
    total.value = 0
  }
}

// A2：翻页处理——更新页码 → 重新加载 → 滚回商品区顶部
async function handlePageChange(page) {
  pageNum.value = page
  await loadProducts()
  await nextTick()
  productAreaRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 分类 ID 口径统一为字符串：19 位雪花 ID 经 Number() 会丢精度（docs/08 §3.7），
// 且模板用严格相等比较高亮，两侧必须同型，否则小整数分类高亮失效。
function selectCategory(id) {
  const sid = id === null || id === undefined || id === '' ? null : String(id)
  selectedCategoryId.value = sid
  // A2：切换分类必须重置页码，否则从第 3 页切到只有 1 页的分类会得到空列表
  pageNum.value = 1
  loadProducts(sid)
}

// FRONT-03 修复：首页分类卡片跳转 /category?id=xx 后，分类页需读取 query.id 并定位到该分类
function applyQueryCategory() {
  const raw = route.query.id
  // 保留字符串原值：19 位雪花 ID 经 Number() 会丢精度（docs/08 §3.7）
  const sid = raw === undefined || raw === null || raw === '' ? null : String(raw)
  // 正整数 ID 校验：容忍前导零，但排除 '0'/'00' 等全零输入——旧逻辑 Number('0') > 0 为假，
  // 应回落"全部商品"而非带 categoryId=0 请求。
  if (sid !== null && /^0*[1-9]\d*$/.test(sid)) {
    // 归一化前导零：'03'/'003' → '3'，保证与模板 String(cat.id) 的严格相等比较同型同值，
    // 否则会「带 categoryId 请求筛选了商品，但侧边栏不高亮、header 显示全部商品」自相矛盾。
    const normalized = sid.replace(/^0+/, '')
    // A3：校验分类 ID 是否真实存在于分类树中。不存在则视为无效筛选，回落「全部商品」，
    // 避免 header 回落到「全部商品」而列表为空 + el-empty 提示「该分类下暂无商品」的自相矛盾。
    if (findCategoryById(categories.value, normalized)) {
      selectCategory(normalized)
      return
    }
  }
  // 无有效 query 或分类不存在时展示全部商品
  selectCategory(null)
}

onMounted(async () => {
  await loadCategories()
  applyQueryCategory()
})

// FRONT-03 修复：分类页内/首页反复跳转不同分类时响应 query.id 变化
watch(
  () => route.query.id,
  () => {
    applyQueryCategory()
  }
)
</script>

<style scoped>
.category-page {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  gap: 20px;
  align-items: flex-start;
}
.category-sidebar {
  width: 200px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  padding: 12px;
  flex-shrink: 0;
  position: sticky;
  top: 84px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}
.sidebar-title {
  padding: 8px 14px 12px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 8px;
}
.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  cursor: pointer;
  border-radius: 10px;
  font-size: 14px;
  color: #334155;
  transition: all 0.2s;
}
.category-item:hover {
  background: #f5f3ff;
  color: #4f46e5;
}
.category-item.active {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.24);
}
.item-emoji {
  font-size: 16px;
  line-height: 1;
}
.product-area {
  flex: 1;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 16px;
  padding: 20px;
  min-height: 60vh;
}
.selected-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 18px;
}
.selected-header h3 {
  font-size: 18px;
  color: #0f172a;
}
.header-count {
  font-size: 13px;
  color: #94a3b8;
}
</style>
