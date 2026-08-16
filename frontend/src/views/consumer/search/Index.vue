<template>
  <div class="search-page">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索商品" @keyup.enter="doSearch" style="width: 400px">
        <template #append>
          <el-button @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- D-3 最近搜索（登录用户显示） -->
    <div v-if="searchHistory.length > 0" class="search-history" style="margin-top: 15px; text-align: center">
      <span style="color: #999; font-size: 13px; margin-right: 8px">最近搜索：</span>
      <el-tag
        v-for="(kw, idx) in searchHistory"
        :key="idx"
        size="small"
        style="margin-right: 8px; cursor: pointer"
        @click="useHistoryKeyword(kw)"
        >{{ kw }}</el-tag
      >
      <el-button type="text" size="small" @click="clearHistory">清空</el-button>
    </div>

    <div class="search-filters" style="margin-top: 15px">
      <span>排序：</span>
      <el-radio-group v-model="sort" @change="doSearch">
        <el-radio-button label="sales">销量</el-radio-button>
        <el-radio-button label="price_asc">价格升序</el-radio-button>
        <el-radio-button label="price_desc">价格降序</el-radio-button>
        <el-radio-button label="new">最新</el-radio-button>
      </el-radio-group>
      <!-- D-2 价格区间筛选 -->
      <span style="margin-left: 20px">价格：</span>
      <el-input-number
        v-model="minPrice"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最低价"
        style="width: 110px"
      />
      <span style="margin: 0 6px; color: #999">—</span>
      <el-input-number
        v-model="maxPrice"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="最高价"
        style="width: 110px"
      />
      <el-button type="primary" plain size="small" style="margin-left: 8px" @click="doSearch">筛选</el-button>
    </div>
    <div class="search-results" style="margin-top: 20px">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in results" :key="item.id" style="margin-bottom: 20px">
          <el-card class="product-card" @click="$router.push(`/product/${item.id}`)">
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
      <el-empty v-if="results.length === 0" description="暂无搜索结果" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/product'
import { getSearchHistory, clearSearchHistory } from '@/api/product'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const keyword = ref(route.query.keyword || '')
const sort = ref('')
// D-2 价格区间
const minPrice = ref(null)
const maxPrice = ref(null)
const results = ref([])
// D-3 搜索历史
const searchHistory = ref([])

async function doSearch() {
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
.search-bar {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}
.product-card {
  cursor: pointer;
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
