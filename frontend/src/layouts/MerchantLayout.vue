<template>
  <div class="merchant-layout">
    <aside class="sidebar">
      <div class="logo">商家后台</div>
      <el-menu :default-active="currentRoute" router>
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="content">
      <header class="content-header">
        <span>{{ nickname }}</span>
        <el-button text @click="handleLogout">退出</el-button>
      </header>
      <main class="content-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useMerchantStore } from '@/store/merchant'
import { Shop, Goods, Document, RefreshRight, TrendCharts, EditPen } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const merchantStore = useMerchantStore()
const currentRoute = computed(() => route.path)
const nickname = computed(() => userStore.nickname)
const shopStatus = computed(() => merchantStore.shopStatus)
const menuItems = computed(() => {
  if (shopStatus.value === 0) return [{ index: '/merchant/apply-pending', icon: EditPen, title: '审核中' }]
  if (shopStatus.value === 2) return [{ index: '/merchant/apply', icon: EditPen, title: '重新入驻' }]
  return [
    { index: '/merchant/shop/info', icon: Shop, title: '店铺管理' },
    { index: '/merchant/products', icon: Goods, title: '商品管理' },
    { index: '/merchant/orders', icon: Document, title: '订单管理' },
    { index: '/merchant/refunds', icon: RefreshRight, title: '售后处理' },
    { index: '/merchant/statistics', icon: TrendCharts, title: '数据统计' },
  ]
})

const redirectMap = {
  0: '/merchant/apply-pending',
  2: '/merchant/apply',
  3: '/merchant/apply-pending', // disabled reuses pending page with different message
}

onMounted(async () => {
  if (shopStatus.value === -1) {
    await merchantStore.fetchApplyStatus()
  }
  // After loading status, redirect if not approved
  if (shopStatus.value !== 1 && shopStatus.value !== -1) {
    router.replace(redirectMap[shopStatus.value] || '/merchant/apply')
  }
})

// Watch for status changes (e.g. after re-submitting application)
watch(shopStatus, (newStatus) => {
  if (newStatus !== 1 && newStatus !== -1 && route.path !== redirectMap[newStatus]) {
    router.replace(redirectMap[newStatus] || '/merchant/apply')
  }
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.merchant-layout {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 200px;
  background: #304156;
  flex-shrink: 0;
}
.sidebar .logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.sidebar .el-menu {
  border-right: none;
  background: #304156;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.content-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.content-main {
  flex: 1;
  padding: 20px;
  background: #f0f2f5;
}
</style>
