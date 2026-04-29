import http from './http'

export function fetchProjects(page, size, projectName) {
  const params = { page, size }
  if (projectName) params.projectName = projectName
  return http.get('/projects', { params })
}

export function getProject(id) {
  return http.get(`/projects/${id}`)
}

export function createProject(data) {
  return http.post('/projects', data)
}

export function updateProject(id, data) {
  return http.put(`/projects/${id}`, data)
}

export function deleteProject(id) {
  return http.delete(`/projects/${id}`)
}
