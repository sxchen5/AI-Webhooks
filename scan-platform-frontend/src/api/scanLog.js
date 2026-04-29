import http from './http'

export function fetchScanLogs(page, size, projectId) {
  const params = { page, size }
  if (projectId != null && projectId !== '') params.projectId = projectId
  return http.get('/scan-logs', { params })
}

export function getScanLog(id) {
  return http.get(`/scan-logs/${id}`)
}
