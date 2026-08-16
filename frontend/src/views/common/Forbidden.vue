<template>
  <div class="forbidden-page">
    <el-result icon="warning" title="403" sub-title="您没有权限访问此页面">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="reLogin">重新登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
const router = useRouter()
const userStore = useUserStore()
function goHome() {
  router.push('/')
}
function reLogin() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
</style>
