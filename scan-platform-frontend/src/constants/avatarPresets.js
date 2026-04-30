import { Avatar, Female, Male, Monitor, UserFilled } from '@element-plus/icons-vue'

/**
 * 内置管理员头像样式（仅图标 + 背景色，不上传文件）。
 * id 持久化到 localStorage。
 */
export const AVATAR_PRESETS = [
  {
    id: 'user',
    icon: UserFilled,
    labelKey: 'settings.avatarPresetUser',
    swatch: 'linear-gradient(135deg, #409eff, #6366f1)',
  },
  {
    id: 'monitor',
    icon: Monitor,
    labelKey: 'settings.avatarPresetMonitor',
    swatch: 'linear-gradient(135deg, #13c2c2, #1677ff)',
  },
  {
    id: 'avatar',
    icon: Avatar,
    labelKey: 'settings.avatarPresetAvatar',
    swatch: 'linear-gradient(135deg, #722ed1, #eb2f96)',
  },
  {
    id: 'male',
    icon: Male,
    labelKey: 'settings.avatarPresetMale',
    swatch: 'linear-gradient(135deg, #2f54eb, #597ef7)',
  },
  {
    id: 'female',
    icon: Female,
    labelKey: 'settings.avatarPresetFemale',
    swatch: 'linear-gradient(135deg, #eb2f96, #f759ab)',
  },
]

const byId = Object.fromEntries(AVATAR_PRESETS.map((p) => [p.id, p]))

export function resolveAvatarPreset(id) {
  return byId[id] || byId.user
}

export const DEFAULT_AVATAR_PRESET_ID = 'user'
