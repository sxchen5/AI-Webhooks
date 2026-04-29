import http from './http'

/** 启用中的平台技能（下拉） */
export function fetchPlatformSkillOptions() {
  return http.get('/platform-skills/options/enabled')
}

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
