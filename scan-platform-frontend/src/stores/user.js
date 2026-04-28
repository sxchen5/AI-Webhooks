import { defineStore } from 'pinia'
import { ref } from 'vue'

const TOKEN_KEY = 'scan_platform_token'
const USER_KEY = 'scan_platform_username'

/**
 * 简单登录态：内存 + localStorage 持久化 JWT。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref(localStorage.getItem(USER_KEY) || '')

  function setSession(t, u) {
    token.value = t
    username.value = u
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, u)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, username, setSession, logout }
})
