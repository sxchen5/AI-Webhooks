import http from './http'

export function getSysConfig() {
  return http.get('/sys-config')
}

export function saveSysConfig(data) {
  return http.put('/sys-config', data)
}
