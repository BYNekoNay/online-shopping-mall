import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

service.interceptors.request.use((config) => {
  // M-24 修复：user 数据损坏时 JSON.parse 会抛异常导致所有请求失败，加容错
  let token = null
  try {
    const userStore = JSON.parse(localStorage.getItem('user') || '{}')
    token = userStore && userStore.token
  } catch {
    // 忽略损坏的本地数据，按未登录处理
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code !== 0) {
      // 权限不足 → 跳转 403 页面
      if (code === 10003) {
        router.push('/403')
        return Promise.reject(new Error(message))
      }
      // 未登录/登录过期 → 跳转登录页
      if (code === 10002) {
        const userStore = useUserStore()
        userStore.logout()
        // M-25 修复：原实现只清状态不跳转，用户停留在当前页持续报错
        router.push('/login')
        return Promise.reject(new Error(message))
      }
      // 其他业务错误 → toast 提示
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      // M-25 修复：登出后跳转登录页
      router.push('/login')
    } else if (error.response?.status === 403) {
      router.push('/403')
    } else if (error.code === 'ECONNABORTED' || !error.response) {
      ElMessage.error('网络连接失败，请检查网络后重试')
    } else {
      ElMessage.error('服务异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
