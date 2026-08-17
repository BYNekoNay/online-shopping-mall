import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    // 接口测试与 E2E 测试从默认 vitest 扫描范围排除（分别由 vitest.api.config.js / playwright 执行）
    exclude: ['api-tests/**', 'e2e/**', 'node_modules/**', 'dist/**'],
    // 关闭结果缓存：避免 Windows 上 node_modules/.vite/vitest/results.json 被占用导致 EPERM 非零退出
    cache: false,
  },
})
