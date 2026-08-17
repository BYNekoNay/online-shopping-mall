<template>
  <div class="user-favorites">
    <div class="panel-card">
      <div class="panel-header">
        <h3>我的收藏</h3>
        <span class="panel-sub">{{ favorites.length }} 件商品</span>
      </div>
      <div v-if="favorites.length === 0" class="empty-state">
        <el-empty description="暂无收藏" />
      </div>
      <el-row v-else :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in favorites" :key="item.id" style="margin-bottom: 20px">
          <div class="favorite-card">
            <ProductCard :item="item" />
            <!-- FRONT-10 修复：取消收藏入口（此前收藏功能无任何 UI 入口，功能不可达） -->
            <el-button size="small" type="danger" plain style="width: 100%; margin-top: 8px" @click="unfavorite(item)">
              取消收藏
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/user'
import ProductCard from '@/components/ProductCard.vue'

const favorites = ref([])

async function loadFavorites() {
  try {
    favorites.value = await request.getFavorites()
  } catch {
    favorites.value = []
  }
}

// FRONT-10 修复：取消收藏（FavoriteVO 同时含 id/productId，兼容两种主键）
async function unfavorite(item) {
  const productId = item.productId ?? item.id
  if (productId == null) return
  try {
    await request.unfavoriteProduct(productId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch {
    // error handled by interceptor
  }
}

onMounted(loadFavorites)
</script>

<style scoped>
.user-favorites {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}
.panel-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}
.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f6f8fb;
}
.panel-header h3 {
  font-size: 18px;
  color: #0f172a;
}
.panel-sub {
  font-size: 13px;
  color: #94a3b8;
}
.empty-state {
  padding: 60px 0;
}
</style>
