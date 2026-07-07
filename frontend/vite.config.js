import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { loadEnv } from 'vite'

const useMock = loadEnv('', process.cwd()).VITE_USE_MOCK === 'true'

export default defineConfig(async () => {
  const plugins = [vue()]

  if (useMock) {
    const { default: mockPlugin } = await import('vite-plugin-mock')
    plugins.push(mockPlugin({ mockPath: 'mock' }))
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
      },
    },
  }
})
