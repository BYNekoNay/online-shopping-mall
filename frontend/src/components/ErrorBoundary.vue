<template>
  <!-- F-2 错误边界：捕获子组件渲染期错误，避免整页白屏 -->
  <div v-if="hasError" class="error-boundary">
    <el-empty description="页面渲染出错，请刷新重试">
      <el-button type="primary" @click="reload">刷新页面</el-button>
    </el-empty>
  </div>
  <slot v-else />
</template>

<script setup>
// Vue3 无类组件 ErrorBoundary，通过 onErrorCaptured 在父组件捕获子组件错误
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)

onErrorCaptured((err, instance, info) => {
  console.error('[ErrorBoundary] 捕获渲染错误:', err, info)
  hasError.value = true
  // 吞掉错误，阻止向上传播导致整页崩溃
  return false
})

function reload() {
  window.location.reload()
}
</script>

<style scoped>
.error-boundary {
  padding: 80px 0;
}
</style>
