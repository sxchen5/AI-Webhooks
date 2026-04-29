import http from './http'

export function fetchGitRepoOptions() {
  return http.get('/active-scan/repo-options')
}

export function fetchActiveRepos(page, size, projectName) {
  const params = { page, size }
  if (projectName) params.projectName = projectName
  return http.get('/active-scan/repos', { params })
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

export function fetchActiveJobs(page, size, jobName, repoId) {
  const params = { page, size }
  if (jobName) params.jobName = jobName
  if (repoId != null && repoId !== '') params.repoId = repoId
  return http.get('/active-scan/jobs', { params })
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

export function fetchActiveLogs(page, size, repoId, jobName, repoName, startTimeOrder) {
  const params = { page, size }
  if (repoId != null && repoId !== '') params.repoId = repoId
  if (jobName) params.jobName = jobName
  if (repoName) params.repoName = repoName
  if (startTimeOrder === 'asc' || startTimeOrder === 'desc') params.startTimeOrder = startTimeOrder
  return http.get('/active-scan/logs', { params })
}

export function getActiveLog(id) {
  return http.get(`/active-scan/logs/${id}`)
}
