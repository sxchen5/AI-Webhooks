/**
 * 将后端 ISO 风格时间中的 `T` 显示为空格（如 2026-04-29 14:30:00）。
 * @param {string|null|undefined} v
 * @returns {string}
 */
export function formatBackendDateTime(v) {
  if (v == null || v === '') return '—'
  return String(v).replace('T', ' ')
}
