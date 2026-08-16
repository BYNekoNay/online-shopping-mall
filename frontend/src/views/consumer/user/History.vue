<template>
  <div class="user-history">
    <el-card>
      <h3>浏览历史</h3>
      <!-- R-3：展示浏览过的商品（非搜索历史） -->
      <div v-if="records.length === 0" class="empty-state">
        <el-empty description="暂无浏览记录，快去逛逛吧">
          <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
        </el-empty>
      </div>
      <el-row v-else :gutter="20" style="margin-top: 15px">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in records" :key="item.id" style="margin-bottom: 20px">
          <el-card class="history-item" :body-style="{ padding: '0px' }" @click="$router.push(`/product/${item.id}`)">
            <div class="history-image">
              <img :src="item.mainImage" :alt="item.name" />
            </div>
            <div class="history-info">
              <div class="history-name">{{ item.name }}</div>
              <div class="history-price">¥{{ item.price }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBrowseHistory } from '@/api/product'

const records = ref([])

async function loadHistory() {
  try {
    records.value = (await getBrowseHistory(20)) || []
  } catch {
    records.value = []
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.history-item {
  cursor: pointer;
}
.history-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
}
.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.history-info {
  padding: 10px;
}
.history-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-price {
  color: #f56c6c;
  font-weight: bold;
  margin-top: 5px;
}
</style>
