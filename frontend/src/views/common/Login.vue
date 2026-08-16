<template>
  <div class="login-page">
    <div class="login-card">
      <h2>用户登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { rules as formRules, validateForm } from '@/utils/useFormValidate'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = ref({ username: '', password: '' })
// F-3：统一校验规则
const rules = {
  username: [formRules.required('请输入用户名'), formRules.username()],
  password: [formRules.required('请输入密码')],
}

async function handleLogin() {
  if (!(await validateForm(formRef.value))) return
  loading.value = true
  try {
    const data = await request.post('/auth/login', form.value)
    userStore.setUser(data)
    ElMessage.success('登录成功')
    // 根据角色跳转到对应页面
    let redirect = route.query.redirect
    if (!redirect || redirect === '/') {
      if (data.role === 3) redirect = '/admin/dashboard'
      else if (data.role === 2) redirect = '/merchant/products'
      else redirect = '/'
    }
    router.push(redirect)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}
.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.login-footer {
  text-align: center;
  font-size: 14px;
  color: #666;
}
.login-footer a {
  color: #409eff;
  text-decoration: none;
  margin-left: 5px;
}
</style>
