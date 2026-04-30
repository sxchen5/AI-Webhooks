import http from './http'

export function login(payload) {
  return http.post('/auth/login', payload)
}

/** 获取登录图形验证码（不走带 Token 的业务逻辑分支，与登录页共用 baseURL） */
export function fetchCaptcha() {
  return http.get('/auth/captcha')
}
