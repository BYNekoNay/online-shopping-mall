<template>
  <div class="search-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索商品"
          clearable
          size="large"
          @keyup.enter="doSearch"
          @clear="doSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- D-3 最近搜索（登录用户显示） -->
    <div v-if="searchHistory.length > 0" class="search-history">
      <span class="history-label">最近搜索：</span>
      <el-tag
        v-for="(kw, idx) in searchHistory"
        :key="idx"
        size="small"
        class="history-tag"
        @click="useHistoryKeyword(kw)"
        >{{ kw }}</el-tag
      >
      <el-button text size="small" @click="clearHistory">清空</el-button>
    </div>

    <!-- 排序与筛选 -->
    <div class="search-filters">
      <span class="filter-label">排序：</span>
      <el-radio-group v-model="sort" @change="doSearch">
        <el-radio-button label="sales">销量</el-radio-button>
        <el-radio-button label="price_asc">价格升序</el-radio-button>
        <el-radio-button label="price_desc">价格降序</el-radio-button>
        <el-radio-button label="new">最新</el-radio-button>
      </el-radio-group>
      <!-- D-2 价格区间筛选 -->
      <span class="filter-label" style="margin-left: 20px">价格：</span>
      <el-input-number
        v-model="minPrice"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最低价"
        class="price-input"
      />
      <span class="price-sep">—</span>
      <el-input-number
        v-model="maxPrice"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最高价"
        class="price-input"
      />
      <el-button type="primary" plain size="small" style="margin-left: 8px" @click="doSearch">筛选</el-button>
    </div>

    <!-- 结果列表 -->
    <div class="search-results">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in results" :key="item.id" style="margin-bottom: 20px">
          <ProductCard :item="item" />
        </el-col>
      </el-row>
      <el-empty v-if="!searching && results.length === 0" description="暂无搜索结果" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/api/product'
import { getSearchHistory, clearSearchHistory } from '@/api/product'
import { useUserStore } from '@/store/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const userStore = useUserStore()
const keyword = ref(route.query.keyword || '')
const sort = ref('')
// D-2 价格区间
const minPrice = ref(null)
const maxPrice = ref(null)
const results = ref([])
const searching = ref(false)
// D-3 搜索历史
const searchHistory = ref([])

async function doSearch() {
  searching.value = true
  try {
    const data = await request.searchProducts({
      keyword: keyword.value,
      sort: sort.value,
      minPrice: minPrice.value ?? undefined,
      maxPrice: maxPrice.value ?? undefined
    })
    results.value = data.records || data || []
    loadHistory()
  } catch {
    results.value = []
  } finally {
    searching.value = false
  }
}

// D-3：加载搜索历史（登录用户）
async function loadHistory() {
  if (!userStore.token) return
  try {
    searchHistory.value = (await getSearchHistory(10)) || []
  } catch {
    searchHistory.value = []
  }
}

// 点击历史关键词回填并搜索
function useHistoryKeyword(kw) {
  keyword.value = kw
  doSearch()
}

// 清空历史
async function clearHistory() {
  try {
    await clearSearchHistory()
    searchHistory.value = []
    ElMessage.success('已清空搜索历史')
  } catch {}
}

if (route.query.keyword) {
  doSearch()
} else {
  loadHistory()
}

onMounted(() => {
  if (!route.query.keyword) loadHistory()
})
</script>

<style scoped>
.search-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.search-bar {
  display: flex;
  justify-content: center;
  padding-top: 24px;
}
.search-box {
  width: 520px;
}
.search-history {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  flex-wrap: wrap;
}
.history-label {
  color: #94a3b8;
  font-size: 13px;
}
.history-tag {
  cursor: pointer;
}
.search-filters {
  display: flex;
  align-items: center;
  margin-top: 20px;
  padding: 14px 20px;
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  flex-wrap: wrap;
  gap: 4px;
}
.filter-label {
  font-size: 14px;
  color: #64748b;
}
.price-input {
  width: 110px;
}
.price-sep {
  margin: 0 6px;
  color: #b0b7c3;
}
.search-results {
  margin-top: 24px;
}
</style>
