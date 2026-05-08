import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { SESSION_EXPIRED_CODE, runSessionExpiredFlow } from '@/utils/sessionExpired'

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
    const hadAuth = !!(err.config?.headers?.Authorization)

    if (status === 401) {
      useUserStore().logout()
      if (hadAuth) {
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
