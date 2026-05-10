import http from './http'

/** 下拉：启用的模型键（按 CLI 类型） */
export function fetchAgentModelOptions(cli) {
  return http.get('/agent-models/options', { params: { cli: cli || 'CURSOR' } })
}

export function fetchAgentModelList(page, size, cli, modelKey) {
  const params = { page, size }
  if (cli) params.cli = cli
  if (modelKey && String(modelKey).trim()) params.modelKey = String(modelKey).trim()
  return http.get('/agent-models', { params })
}

export function createAgentModel(data) {
  return http.post('/agent-models', data)
}

export function updateAgentModel(id, data) {
  return http.put(`/agent-models/${id}`, data)
}

export function deleteAgentModel(id) {
  return http.delete(`/agent-models/${id}`)
}
