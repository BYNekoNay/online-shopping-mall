<template>
  <div class="forbidden-page">
    <el-result icon="warning" title="403" sub-title="You don't have permission to access this page.">
      <template #extra>
        <el-button type="primary" @click="goHome">Back to Home</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goHome() { router.push('/') }
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
