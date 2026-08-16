<template>
  <!-- F-2 全局错误边界：页面级 JS 错误不白屏 -->
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
</template>

<script setup>
import ErrorBoundary from '@/components/ErrorBoundary.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  background-color: #f5f5f5;
}
#app {
  min-height: 100vh;
}
</style>
