import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { loadEnv } from 'vite'

const useMock = loadEnv('', process.cwd()).VITE_USE_MOCK === 'true'

export default defineConfig(async () => {
  const plugins = [vue()]

  if (useMock) {
    const { default: mockPlugin } = await import('vite-plugin-mock')
    // M-27 修复：Mock 文件位于 src/mock/（原配置指向不存在的根级 mock/，Mock 模式恒不生效）
    plugins.push(mockPlugin({ mockPath: 'src/mock' }))
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
  }
})
