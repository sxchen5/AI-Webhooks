<template>
  <el-config-provider :locale="epLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import { usePreferencesStore } from '@/stores/preferences'
import { setI18nLocale } from '@/i18n'

const route = useRoute()
const preferences = usePreferencesStore()
const { t, locale } = useI18n()

const epLocale = computed(() => (preferences.locale === 'en' ? en : zhCn))

watch(
  () => preferences.locale,
  (v) => {
    setI18nLocale(v)
    locale.value = v === 'en' ? 'en' : 'zh'
  },
  { immediate: true },
)

watch(
  () => [route.fullPath, locale.value, route.meta],
  () => {
    const meta = route.meta || {}
    if (meta.public && route.name === 'Login') {
      document.title = t('app.loginTitle')
      return
    }
    const i18n = meta.i18n
    if (i18n?.child) {
      const part = t(`nav.${i18n.child}`)
      document.title = `${part} — ${t('app.title')}`
    } else {
      document.title = t('app.title')
    }
  },
  { immediate: true },
)
</script>
