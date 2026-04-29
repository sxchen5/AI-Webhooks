import http from './http'

export function fetchPlatformSkills(page, size) {
  return http.get('/platform-skills', { params: { page, size } })
}

export function getPlatformSkill(id) {
  return http.get(`/platform-skills/${id}`)
}

export function createPlatformSkill(data) {
  return http.post('/platform-skills', data)
}

export function updatePlatformSkill(id, data) {
  return http.put(`/platform-skills/${id}`, data)
}

export function deletePlatformSkill(id) {
  return http.delete(`/platform-skills/${id}`)
}
