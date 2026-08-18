import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  // 雪花 ID 修复：userId 为后端 19 位 Long，必须以字符串存取，
  // 避免 Number(localStorage.getItem('userId')) 二次丢精度。
  const userId = ref(localStorage.getItem('userId') || '')
  // role 为枚举（0 匿名 / 1 用户 / 2 商家 / 3 管理员），不是雪花 ID，保持 Number
  const role = ref(Number(localStorage.getItem('role') || 0))
  const nickname = ref(localStorage.getItem('nickname') || '')

  function setUser(data) {
    token.value = data.token || ''
    userId.value = data.userId != null ? String(data.userId) : ''
    role.value = data.role || 0
    nickname.value = data.nickname || ''
    localStorage.setItem('token', token.value)
    localStorage.setItem('userId', userId.value)
    localStorage.setItem('role', String(role.value))
    localStorage.setItem('nickname', nickname.value)
    localStorage.setItem(
      'user',
      JSON.stringify({ token: token.value, userId: userId.value, role: role.value, nickname: nickname.value })
    )
  }

  function logout() {
    token.value = ''
    userId.value = ''
    role.value = 0
    nickname.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    localStorage.removeItem('nickname')
    localStorage.removeItem('user')
  }

  return { token, userId, role, nickname, setUser, logout }
})
