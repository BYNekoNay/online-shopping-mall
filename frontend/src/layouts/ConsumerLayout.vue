<template>
  <div class="consumer-layout">
    <header class="consumer-header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-text brand-gradient">AI 智能商城</span>
        </router-link>
        <nav class="nav-links">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/search" class="nav-link">搜索</router-link>
          <router-link to="/category" class="nav-link">分类</router-link>
        </nav>
        <div class="header-actions">
          <template v-if="!token">
            <router-link to="/login" class="action-link">登录</router-link>
            <router-link to="/register" class="action-link action-register">注册</router-link>
          </template>
          <template v-else>
            <router-link to="/cart" class="action-link">
              <el-badge :value="0" :hidden="true">
                <el-icon :size="20"><ShoppingCart /></el-icon>
              </el-badge>
            </router-link>
            <router-link to="/orders" class="action-link">我的订单</router-link>
            <router-link v-if="role === 3" to="/admin/dashboard" class="action-link action-admin">管理后台</router-link>
            <router-link v-if="role === 2" to="/merchant/products" class="action-link action-admin">商家中心</router-link>
            <el-dropdown trigger="click">
              <span class="user-dropdown">
                <el-avatar :size="32" class="user-avatar">{{ nickname?.charAt(0) || 'U' }}</el-avatar>
                <span class="user-name">{{ nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <router-link to="/user/profile"><el-dropdown-item>个人中心</el-dropdown-item></router-link>
                  <router-link to="/user/addresses"><el-dropdown-item>收货地址</el-dropdown-item></router-link>
                  <router-link to="/user/favorites"><el-dropdown-item>我的收藏</el-dropdown-item></router-link>
                  <router-link to="/user/coupons"><el-dropdown-item>我的优惠券</el-dropdown-item></router-link>
                  <router-link to="/user/points"><el-dropdown-item>我的积分</el-dropdown-item></router-link>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
      <div class="header-divider"></div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="consumer-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="brand-gradient" style="font-size:18px;font-weight:700;">AI 智能商城</span>
          <p>基于协同过滤算法的智能推荐网络商城</p>
        </div>
        <div class="footer-links">
          <span>攀枝花学院 · 毕业设计</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ArrowDown, ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const token = computed(() => userStore.token)
const role = computed(() => userStore.role)
const nickname = computed(() => userStore.nickname)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.consumer-header {
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-divider {
  height: 2px;
  background: linear-gradient(90deg, #4F46E5, #7C3AED, transparent);
}
.logo-text {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.5px;
  text-decoration: none;
}
.nav-links {
  display: flex;
  gap: 32px;
  align-items: center;
}
.nav-link {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  text-decoration: none;
  position: relative;
  padding: 4px 0;
  transition: color 0.2s;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #4F46E5, #7C3AED);
  transition: width 0.3s;
  border-radius: 1px;
}
.nav-link:hover { color: #4F46E5; }
.nav-link:hover::after,
.nav-link.router-link-exact-active::after { width: 100%; }
.nav-link.router-link-exact-active {
  color: #4F46E5;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.action-link {
  font-size: 14px;
  color: var(--el-text-color-regular);
  text-decoration: none;
  transition: color 0.2s;
}
.action-link:hover { color: #4F46E5; }
.action-register {
  padding: 6px 18px;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  border-radius: 20px;
  font-weight: 500;
}
.action-register:hover { color: #fff; opacity: 0.9; }
.action-admin {
  padding: 6px 14px;
  background: linear-gradient(135deg, #DC2626, #991B1B);
  color: #fff;
  border-radius: 20px;
  font-weight: 500;
}
.action-admin:hover { color: #fff; opacity: 0.9; }
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-dropdown:hover { background: #F1F5F9; }
.user-avatar {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.main-content {
  min-height: calc(100vh - 180px);
  background: var(--el-bg-color);
}
.consumer-footer {
  background: #1E293B;
  color: rgba(255,255,255,0.6);
  padding: 32px 24px;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.footer-brand p {
  margin-top: 6px;
  font-size: 13px;
}
.footer-links {
  font-size: 13px;
}
</style>
