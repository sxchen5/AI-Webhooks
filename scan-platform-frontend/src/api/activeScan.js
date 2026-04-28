import http from './http'

export function fetchActiveRepos(page, size) {
  return http.get('/active-scan/repos', { params: { page, size } })
}

export function getActiveRepo(id) {
  return http.get(`/active-scan/repos/${id}`)
}

export function createActiveRepo(data) {
  return http.post('/active-scan/repos', data)
}

export function updateActiveRepo(id, data) {
  return http.put(`/active-scan/repos/${id}`, data)
}

export function deleteActiveRepo(id) {
  return http.delete(`/active-scan/repos/${id}`)
}

export function fetchActiveJobs(page, size) {
  return http.get('/active-scan/jobs', { params: { page, size } })
}

export function getActiveJob(id) {
  return http.get(`/active-scan/jobs/${id}`)
}

export function createActiveJob(data) {
  return http.post('/active-scan/jobs', data)
}

export function updateActiveJob(id, data) {
  return http.put(`/active-scan/jobs/${id}`, data)
}

export function deleteActiveJob(id) {
  return http.delete(`/active-scan/jobs/${id}`)
}

export function runActiveJob(id) {
  return http.post(`/active-scan/jobs/${id}/run`)
}

export function fetchActiveLogs(page, size) {
  return http.get('/active-scan/logs', { params: { page, size } })
}

export function getActiveLog(id) {
  return http.get(`/active-scan/logs/${id}`)
}
