<template>
  <div class="user-history">
    <el-card>
      <h3>浏览历史</h3>
      <div v-if="records.length === 0" class="empty-state">
        <el-empty description="暂无浏览记录" />
      </div>
      <div v-else class="history-list">
        <div v-for="item in records" :key="item.id" class="history-item" @click="$router.push(`/product/${item.productId}`)">
          <div class="history-image"><img :src="item.mainImage" :alt="item.name" /></div>
          <div class="history-info">
            <div class="history-name">{{ item.name }}</div>
            <div class="history-price">¥{{ item.price }}</div>
          </div>
          <div class="history-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/user'

const records = ref([])

async function loadHistory() {
  try {
    const data = await request.getSearchHistory()
    records.value = (data || []).map(h => ({
      ...h,
      productId: h.relatedProductId || h.productId
    }))
  } catch {
    records.value = []
  }
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(loadHistory)
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 15px;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.history-item:hover {
  background: #f5f7fa;
}
.history-image {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
}
.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}
.history-info {
  flex: 1;
}
.history-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}
.history-price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 14px;
}
.history-time {
  color: #999;
  font-size: 12px;
  white-space: nowrap;
}
</style>
