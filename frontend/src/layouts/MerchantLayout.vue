<template>
  <div class="merchant-layout">
    <aside class="sidebar">
      <div class="logo">商家后台</div>
      <el-menu :default-active="currentRoute" router>
        <el-menu-item index="/merchant/shop/info">
          <el-icon><Shop /></el-icon><span>店铺管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/products">
          <el-icon><Goods /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><Document /></el-icon><span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/refunds">
          <el-icon><RefreshRight /></el-icon><span>售后处理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/statistics">
          <el-icon><TrendCharts /></el-icon><span>数据统计</span>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Shop, Goods, Document, RefreshRight, TrendCharts } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentRoute = computed(() => route.path)
const nickname = computed(() => userStore.nickname)

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
