import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/styles/theme.css'         // 自定义设计主题
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import request from './utils/request'
import pageViewPlugin from './plugins/page-view'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus, { size: 'default' })
app.use(router)

// 激活页面访问埋点
pageViewPlugin(router, (data) => request.post('/behavior/page-view', data))

// F-2 全局兜底：路由懒加载失败/未捕获 Promise 异常不白屏
router.onError((error) => {
  console.error('[Router] 路由加载失败:', error)
})
window.addEventListener('unhandledrejection', (event) => {
  console.error('[Global] 未捕获 Promise 异常:', event.reason)
})

app.config.globalProperties.$request = request
app.mount('#app')
