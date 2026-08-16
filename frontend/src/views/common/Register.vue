<template>
  <div class="login-page">
    <div class="login-card">
      <h2>用户注册</h2>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称（可选）" prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleRegister">注册</el-button>
        </el-form-item>
        <div class="login-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { rules as formRules, validateForm } from '@/utils/useFormValidate'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = ref({ username: '', nickname: '', password: '', confirmPassword: '' })
// F-3：统一校验规则
const rules = {
  username: [formRules.required('请输入用户名'), formRules.username()],
  password: [formRules.required('请输入密码'), formRules.length(8, 20, '密码需为 8~20 个字符')],
  confirmPassword: [formRules.required('请确认密码'), formRules.confirmPassword(() => form.value.password)]
}

async function handleRegister() {
  if (!(await validateForm(formRef.value))) return
  loading.value = true
  try {
    await request.post('/auth/register', {
      username: form.value.username,
      nickname: form.value.nickname,
      password: form.value.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}
.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
.login-footer {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}
.login-footer a {
  color: #409eff;
  text-decoration: none;
  margin-left: 5px;
}
</style>
