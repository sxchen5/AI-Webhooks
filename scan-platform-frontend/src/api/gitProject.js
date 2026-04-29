import http from './http'

export function fetchGitProjects(page, size, projectName) {
  const params = { page, size }
  if (projectName) params.projectName = projectName
  return http.get('/active-scan/git-projects', { params })
}

export function fetchGitProjectOptions() {
  return http.get('/active-scan/git-projects/options')
}

export function getGitProject(id) {
  return http.get(`/active-scan/git-projects/${id}`)
}

export function createGitProject(data) {
  return http.post('/active-scan/git-projects', data)
}

export function updateGitProject(id, data) {
  return http.put(`/active-scan/git-projects/${id}`, data)
}

export function deleteGitProject(id) {
  return http.delete(`/active-scan/git-projects/${id}`)
}
