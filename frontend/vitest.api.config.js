import { defineConfig } from 'vitest/config'

/**
 * 接口测试独立配置（T02）。
 * 说明：
 *  - 运行环境为 node（不加载 jsdom/vue 插件），仅依赖 Node 内置 fetch 与既有 vitest；
 *  - 测试目标为线上/本地后端真实接口，默认 baseUrl 见 api-tests/helpers/client.js（可用 API_BASE_URL 切换）；
 *  - 从默认 vitest 扫描中排除（见 vitest.config.js exclude），与前端单测互不干扰。
 */
export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    include: ['api-tests/**/*.spec.js'],
    exclude: ['node_modules/**', 'dist/**', 'e2e/**', 'src/**'],
    testTimeout: 30_000,
    hookTimeout: 30_000,
    // 关闭结果缓存：避免 Windows 上 node_modules/.vite/vitest/results.json 被占用导致 EPERM
    cache: false,
  },
})
