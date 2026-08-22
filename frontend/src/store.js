import { reactive, watch } from 'vue'

const KEY = 'amica-state'
const saved = JSON.parse(localStorage.getItem(KEY) || '{}')

// 全局状态：后端没有列表接口，所有已注册资源全部缓存在本地，便于重复测试
export const state = reactive({
  // 当前登录用户
  account: saved.account || '',
  userId: saved.userId || null,
  // 从 /auth/show 拉到的用户列表（userId 唯一来源）
  users: saved.users || [],
  // providers: [{ providerId, protocol, baseUrl, name, apiKeyMasked }]
  providers: saved.providers || [],
  // models: [{ id, providerId, name, modelId }]
  models: saved.models || [],
  // assistants: [{ assistantId, userId, modelId, name, prompt }]
  assistants: saved.assistants || [],
  // conversations: [{ id, userId, assistantId, title, status, systemPrompt, messages: [{role,content,seq,error?}] }]
  conversations: saved.conversations || [],
  // 调试日志（不持久化）
  logs: [],
})

watch(
  state,
  () => {
    localStorage.setItem(KEY, JSON.stringify(state))
  },
  { deep: true }
)

export function resetLocalData() {
  localStorage.removeItem(KEY)
  location.reload()
}

export function upsert(list, item, keyFn) {
  const idx = list.findIndex(keyFn)
  if (idx >= 0) list[idx] = { ...list[idx], ...item }
  else list.unshift(item)
}
