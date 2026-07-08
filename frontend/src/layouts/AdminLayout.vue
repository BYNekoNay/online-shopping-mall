<template>
  <div class="admin-layout">
    <aside class="sidebar sidebar-dark">
      <div class="sidebar-header">
        <div class="sidebar-logo-dot"></div>
        <span class="sidebar-logo-text">管理后台</span>
      </div>
      <el-menu :default-active="currentRoute" router class="sidebar-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon><span>商品审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon><span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/shops">
          <el-icon><Shop /></el-icon><span>商家管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/coupons">
          <el-icon><Ticket /></el-icon><span>优惠券管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/promotions">
          <el-icon><Promotion /></el-icon><span>促销管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon><span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/dicts">
          <el-icon><List /></el-icon><span>数据字典</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Setting /></el-icon><span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="content-area">
      <header class="top-bar">
        <div class="top-bar-left">
          <el-breadcrumb>
            <el-breadcrumb-item>管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRoute.split('/').pop() || 'dashboard' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-bar-right">
          <el-avatar :size="32" class="admin-avatar">{{ nickname?.charAt(0) || 'A' }}</el-avatar>
          <span class="admin-name">{{ nickname }}</span>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { DataAnalysis, User, Goods, Menu, Shop, Ticket, Promotion, Document, List, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentRoute = computed(() => route.path)
const nickname = computed(() => userStore.nickname || '管理员')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}
.sidebar-dark {
  width: 220px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1E293B, #0F172A);
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.sidebar-logo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7C3AED;
  box-shadow: 0 0 8px rgba(124,58,237,0.6);
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
  color: rgba(255,255,255,0.65);
  font-size: 14px;
}
.sidebar-menu .el-menu-item:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}
.sidebar-menu .el-menu-item.is-active {
  background: rgba(255,255,255,0.12);
  color: #fff;
  font-weight: 600;
}
.sidebar-menu .el-menu-item .el-icon {
  color: inherit;
}
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}
.top-bar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #E2E8F0;
}
.top-bar-left .el-breadcrumb {
  font-size: 14px;
}
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.admin-avatar {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  font-weight: 600;
}
.admin-name {
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
