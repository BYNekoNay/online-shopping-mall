<template>
  <div class="user-history">
    <div class="panel-card">
      <div class="panel-header">
        <h3>浏览历史</h3>
        <span class="panel-sub">最近浏览过的商品</span>
      </div>
      <!-- R-3：展示浏览过的商品（非搜索历史） -->
      <div v-if="records.length === 0" class="empty-state">
        <el-empty description="暂无浏览记录，快去逛逛吧">
          <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
        </el-empty>
      </div>
      <el-row v-else :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="item in records" :key="item.id" style="margin-bottom: 20px">
          <ProductCard :item="item" />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBrowseHistory } from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'

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
.user-history {
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
