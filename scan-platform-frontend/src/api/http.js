import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { i18n } from '@/i18n'

/** 后端 JWT 过期或未登录统一 code，用于与登录失败等其它 401 区分（若有） */
const SESSION_EXPIRED_CODE = 401000

let sessionExpiredPromise = null

function runSessionExpiredFlow() {
  if (!sessionExpiredPromise) {
    sessionExpiredPromise = (async () => {
      try {
        await ElMessageBox.alert(
          i18n.global.t('session.expiredBody'),
          i18n.global.t('session.expiredTitle'),
          {
            confirmButtonText: i18n.global.t('session.confirm'),
            type: 'warning',
            closeOnClickModal: false,
            closeOnPressEscape: false,
          },
        )
      } catch {
        /* 关闭弹窗仍跳转登录 */
      }
      const fullPath = router.currentRoute.value?.fullPath || '/'
      await router.replace({
        name: 'Login',
        query: { redirect: fullPath === '/login' ? undefined : fullPath },
      })
    })().finally(() => {
      sessionExpiredPromise = null
    })
  }
  return sessionExpiredPromise
}

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

http.interceptors.request.use((config) => {
  const user = useUserStore()
  if (user.token) {
    config.headers.Authorization = `Bearer ${user.token}`
  }
  return config
})

http.interceptors.response.use(
  async (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      if (body.code === SESSION_EXPIRED_CODE && res.status === 401) {
        useUserStore().logout()
        await runSessionExpiredFlow()
        return Promise.reject(new Error(body.message || '登录已过期'))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data !== undefined ? body.data : body
  },
  async (err) => {
    const status = err.response?.status
    const body = err.response?.data
    const msg = body?.message || err.message || '网络错误'

    if (status === 401) {
      const user = useUserStore()
      const sessionExpired = body?.code === SESSION_EXPIRED_CODE
      user.logout()
      if (sessionExpired) {
        await runSessionExpiredFlow()
      } else {
        ElMessage.error(msg)
      }
      return Promise.reject(err)
    }

    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

export default http
