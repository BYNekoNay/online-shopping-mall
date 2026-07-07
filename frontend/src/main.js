import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import request from './utils/request'
import pageViewPlugin from './plugins/page-view'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)

// 激活页面访问埋点
pageViewPlugin(router, (data) => request.post('/behavior/page-view', data))

app.config.globalProperties.$request = request
app.mount('#app')
