<template>
  <div class="consumer-layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">智能商城</router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/search">搜索</router-link>
        </nav>
        <div class="user-actions">
          <template v-if="!token">
            <router-link to="/login">登录</router-link>
            <router-link to="/login">注册</router-link>
          </template>
          <template v-else>
            <router-link to="/cart">购物车</router-link>
            <router-link to="/orders">我的订单</router-link>
            <el-dropdown>
              <span class="el-dropdown-link">
                {{ nickname }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <router-link to="/user/profile"><el-dropdown-item>个人中心</el-dropdown-item></router-link>
                  <router-link to="/user/addresses"><el-dropdown-item>收货地址</el-dropdown-item></router-link>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <p>基于协同过滤算法的智能推荐网络商城</p>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const token = computed(() => userStore.token)
const nickname = computed(() => userStore.nickname)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
}
.nav a {
  margin: 0 15px;
  text-decoration: none;
  color: #333;
}
.nav a.router-link-active {
  color: #409eff;
}
.user-actions a {
  margin-left: 15px;
  text-decoration: none;
  color: #333;
}
.main-content {
  min-height: calc(100vh - 120px);
}
.footer {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}
</style>
