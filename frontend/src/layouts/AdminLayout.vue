<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="logo">管理后台</div>
      <el-menu :default-active="currentRoute" router>
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
    <div class="content">
      <header class="content-header">
        <span>管理员</span>
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
import { DataAnalysis, User, Goods, Menu, Shop, Ticket, Promotion, Document, List, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentRoute = computed(() => route.path)

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
