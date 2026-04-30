import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { setI18nLocale } from './i18n'
import { usePreferencesStore } from './stores/preferences'
import './styles/global.scss'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
const preferences = usePreferencesStore()
setI18nLocale(preferences.locale)
app.use(i18n)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
