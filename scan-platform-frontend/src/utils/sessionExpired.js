import { ElMessageBox } from 'element-plus'
import { i18n } from '@/i18n'
import router from '@/router'

/** 后端 JWT 过期或未登录统一 code，用于与登录失败等其它 401 区分 */
export const SESSION_EXPIRED_CODE = 401000

let sessionExpiredPromise = null

/**
 * 显示「登录已过期」提示并跳转登录页（带 redirect）。
 * 同一时间只弹一次。
 */
export function runSessionExpiredFlow() {
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
