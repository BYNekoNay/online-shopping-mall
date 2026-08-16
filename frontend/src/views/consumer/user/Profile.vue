<template>
  <div class="user-profile">
    <el-card>
      <h3>个人信息</h3>
      <el-form :model="form" label-width="100px" style="max-width: 500px; margin-top: 20px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/user'

const form = ref({ username: '', nickname: '', phone: '', email: '' })

onMounted(async () => {
  try {
    const data = await request.getProfile()
    form.value = {
      username: data.username,
      nickname: data.nickname || '',
      phone: data.phone || '',
      email: data.email || ''
    }
  } catch {
    ElMessage.error('加载个人信息失败')
  }
})

async function saveProfile() {
  try {
    await request.updateProfile({ nickname: form.value.nickname, phone: form.value.phone, email: form.value.email })
    ElMessage.success('保存成功')
  } catch {
    // error handled
  }
}
</script>
