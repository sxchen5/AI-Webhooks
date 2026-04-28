import http from './http'

export function fetchScanLogs(page, size) {
  return http.get('/scan-logs', { params: { page, size } })
}

export function getScanLog(id) {
  return http.get(`/scan-logs/${id}`)
}
