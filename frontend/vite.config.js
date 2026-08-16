import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(async ({ mode }) => {
  // F-04 修复：从 configEnv.mode 取 mode（而非硬编码 ''），使 `vite --mode mock` 能读到 .env.mock
  const useMock = loadEnv(mode, process.cwd()).VITE_USE_MOCK === 'true'

  const plugins = [vue()]

  if (useMock) {
    // M-27 修复：Mock 文件位于 src/mock/（原配置指向不存在的根级 mock/，Mock 模式恒不生效）
    // F-04 修复：vite-plugin-mock v3 为命名导出 viteMockServe（原 `default` 解构为 undefined，mock 恒未挂载）
    const { viteMockServe } = await import('vite-plugin-mock')
    plugins.push(viteMockServe({ mockPath: 'src/mock' }))
  }

  return {
    plugins,
    resolve: {
      alias: {
        '@': '/src',
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // M-40 修复：开发环境补充 /uploads 代理，否则上传的图片资源 404
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    // G 阶段：生产构建本地托管（vite preview）同样代理后端
    preview: {
      port: 4173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
