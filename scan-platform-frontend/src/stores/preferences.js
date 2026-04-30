import { defineStore } from 'pinia'
import { ref } from 'vue'

const AVATAR_KEY = 'scan_platform_avatar_data_url'
const LOCALE_KEY = 'scan_platform_locale'
const THEME_KEY = 'scan_platform_theme'

function applyThemeClass(theme) {
  const root = document.documentElement
  if (theme === 'dark') {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
}

/**
 * 用户偏好：头像（Data URL）、界面语言、浅色/深色主题（持久化 localStorage）。
 */
export const usePreferencesStore = defineStore('preferences', () => {
  const avatarDataUrl = ref(localStorage.getItem(AVATAR_KEY) || '')
  const locale = ref(localStorage.getItem(LOCALE_KEY) || 'zh')
  const theme = ref(localStorage.getItem(THEME_KEY) || 'light')

  applyThemeClass(theme.value)

  function setAvatarDataUrl(url) {
    const v = url || ''
    avatarDataUrl.value = v
    if (v) {
      localStorage.setItem(AVATAR_KEY, v)
    } else {
      localStorage.removeItem(AVATAR_KEY)
    }
  }

  function setLocale(code) {
    const v = code === 'en' ? 'en' : 'zh'
    locale.value = v
    localStorage.setItem(LOCALE_KEY, v)
  }

  function setTheme(mode) {
    const v = mode === 'dark' ? 'dark' : 'light'
    theme.value = v
    localStorage.setItem(THEME_KEY, v)
    applyThemeClass(v)
  }

  return {
    avatarDataUrl,
    locale,
    theme,
    setAvatarDataUrl,
    setLocale,
    setTheme,
  }
})
