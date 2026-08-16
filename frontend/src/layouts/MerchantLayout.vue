<template>
  <div class="merchant-layout">
    <aside class="sidebar sidebar-dark">
      <div class="sidebar-header">
        <div class="sidebar-logo-dot"></div>
        <span class="sidebar-logo-text">商家后台</span>
      </div>
      <el-menu :default-active="currentRoute" router class="sidebar-menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="content-area">
      <header class="top-bar">
        <div class="top-bar-left">
          <span style="font-size: 15px; font-weight: 600; color: #0f172a">{{ currentTitle }}</span>
        </div>
        <div class="top-bar-right">
          <el-avatar :size="32" class="merchant-avatar">{{ nickname?.charAt(0) || 'M' }}</el-avatar>
          <span class="merchant-name">{{ nickname }}</span>
          <el-button text size="small" @click="handleLogout">退出</el-button>
        </div>
      </header>
      <main class="content-body">
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

const titleMap = {
  '/merchant/shop/info': '店铺管理',
  '/merchant/products': '商品管理',
  '/merchant/orders': '订单管理',
  '/merchant/refunds': '售后处理',
  '/merchant/statistics': '数据统计',
  '/merchant/apply': '商家入驻',
  '/merchant/apply-pending': '审核进度'
}
const currentTitle = computed(() => titleMap[currentRoute.value] || '商家后台')

const menuItems = computed(() => {
  if (shopStatus.value === 0) return [{ index: '/merchant/apply-pending', icon: EditPen, title: '审核中' }]
  if (shopStatus.value === 2) return [{ index: '/merchant/apply', icon: EditPen, title: '重新入驻' }]
  return [
    { index: '/merchant/shop/info', icon: Shop, title: '店铺管理' },
    { index: '/merchant/products', icon: Goods, title: '商品管理' },
    { index: '/merchant/orders', icon: Document, title: '订单管理' },
    { index: '/merchant/refunds', icon: RefreshRight, title: '售后处理' },
    { index: '/merchant/statistics', icon: TrendCharts, title: '数据统计' }
  ]
})

const redirectMap = {
  0: '/merchant/apply-pending',
  2: '/merchant/apply',
  3: '/merchant/apply-pending'
}

onMounted(async () => {
  if (shopStatus.value === -1) {
    await merchantStore.fetchApplyStatus()
  }
  if (shopStatus.value !== 1 && shopStatus.value !== -1) {
    router.replace(redirectMap[shopStatus.value] || '/merchant/apply')
  }
})

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
.sidebar-dark {
  width: 220px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1e293b, #0f172a);
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.sidebar-logo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7c3aed;
  box-shadow: 0 0 8px rgba(124, 58, 237, 0.6);
}
.sidebar-logo-text {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}
.sidebar-menu {
  flex: 1;
  border-right: none !important;
  background: transparent !important;
  padding: 8px 0;
}
.sidebar-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
}
.sidebar-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.sidebar-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-weight: 600;
}
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}
.top-bar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e2e8f0;
}
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.merchant-avatar {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-weight: 600;
}
.merchant-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.content-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
