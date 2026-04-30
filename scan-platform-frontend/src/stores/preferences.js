import { defineStore } from 'pinia'
import { ref } from 'vue'
import { DEFAULT_AVATAR_PRESET_ID, resolveAvatarPreset } from '@/constants/avatarPresets'

const LEGACY_AVATAR_DATA_URL_KEY = 'scan_platform_avatar_data_url'
const AVATAR_PRESET_KEY = 'scan_platform_avatar_preset'
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

function readInitialAvatarPreset() {
  if (typeof localStorage === 'undefined') return DEFAULT_AVATAR_PRESET_ID
  // 旧版上传头像不再使用，清除遗留大体积数据
  if (localStorage.getItem(LEGACY_AVATAR_DATA_URL_KEY)) {
    localStorage.removeItem(LEGACY_AVATAR_DATA_URL_KEY)
  }
  const raw = localStorage.getItem(AVATAR_PRESET_KEY)
  if (raw && resolveAvatarPreset(raw).id === raw) {
    return raw
  }
  return DEFAULT_AVATAR_PRESET_ID
}

/**
 * 用户偏好：内置头像样式、界面语言、浅色/深色主题（持久化 localStorage）。
 */
export const usePreferencesStore = defineStore('preferences', () => {
  const avatarPreset = ref(readInitialAvatarPreset())
  const locale = ref(localStorage.getItem(LOCALE_KEY) || 'zh')
  const theme = ref(localStorage.getItem(THEME_KEY) || 'light')

  applyThemeClass(theme.value)

  function setAvatarPreset(id) {
    const p = resolveAvatarPreset(id)
    avatarPreset.value = p.id
    localStorage.setItem(AVATAR_PRESET_KEY, p.id)
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
    avatarPreset,
    locale,
    theme,
    setAvatarPreset,
    setLocale,
    setTheme,
  }
})
