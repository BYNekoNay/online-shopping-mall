import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

service.interceptors.request.use((config) => {
  const userStore = JSON.parse(localStorage.getItem('user') || '{}')
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code !== 0) {
      ElMessage.error(message || 'Request failed')
      return Promise.reject(new Error(message))
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('user')
      router.push('/login')
    } else if (error.response?.status === 403) {
      router.push('/403')
    } else {
      ElMessage.error('Network error, please try again')
    }
    return Promise.reject(error)
  }
)

export default service
