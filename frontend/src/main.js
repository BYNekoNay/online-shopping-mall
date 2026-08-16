import { createApp } from 'vue'
import { createPinia } from 'pinia'
// F-05 Element Plus 按需引入：移除全量注册与全量 CSS，模板组件由 unplugin-vue-components 自动解析
// 命令式组件（JS API 调用，模板解析器不会注入样式）显式引入并补 style
// eslint-disable-next-line no-unused-vars -- 显式引入确保命令式 API 可用，样式由下方 style import 兜底
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
// F-05 主题兜底：显式引入 EP base css 于 theme.css 之前，保证 --el-color-* 品牌色覆盖生效
import 'element-plus/theme-chalk/base.css'
import './assets/styles/theme.css' // 自定义设计主题
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import request from './utils/request'
import pageViewPlugin from './plugins/page-view'

const app = createApp(App)
app.use(createPinia())
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
