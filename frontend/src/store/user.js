import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId') || 0))
  const role = ref(Number(localStorage.getItem('role') || 0))
  const nickname = ref(localStorage.getItem('nickname') || '')

  function setUser(data) {
    token.value = data.token || ''
    userId.value = data.userId || 0
    role.value = data.role || 0
    nickname.value = data.nickname || ''
    localStorage.setItem('token', token.value)
    localStorage.setItem('userId', String(userId.value))
    localStorage.setItem('role', String(role.value))
    localStorage.setItem('nickname', nickname.value)
    localStorage.setItem(
      'user',
      JSON.stringify({ token: token.value, userId: userId.value, role: role.value, nickname: nickname.value })
    )
  }

  function logout() {
    token.value = ''
    userId.value = 0
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
