import { reactive, watch } from 'vue'

// 全局状态管理：相当于后端的“内存缓存” + “数据库”二合一
// reactive()：把普通对象变成响应式对象 —— 页面模板引用它，这里改了值，页面自动更新（无需手动刷新）
// watch()：监听 state 的变化，每次变化把整个状态写进 localStorage（浏览器自带的小型持久化存储）
const KEY = 'amica-state'
const saved = JSON.parse(localStorage.getItem(KEY) || '{}')

export const state = reactive({
  // 当前登录用户
  account: saved.account || '',
  // 当前用户的 id（后端 login 不返回 id，见 AuthPanel）
  userId: saved.userId || null,
  // 从 /auth/get 拉到的用户列表（userId 的唯一来源）
  // saved 里的旧数据同样读出来，相当于“打开页面恢复上次的数据”
  users: saved.users || [],
  // providers: [{ providerId, protocol, baseUrl, name, apiKeyMasked }]
  providers: saved.providers || [],
  // models: [{ id, providerId, name, modelId }]
  models: saved.models || [],
  // assistants: [{ assistantId, userId, modelId, name, prompt }]
  assistants: saved.assistants || [],
  // conversations: [{ id, userId, assistantId, title, status, systemPrompt, messages: [{role,content,seq,error?}] }]
  conversations: saved.conversations || [],
})

// watch(对象, 回调, {deep:true})：深度监听，state 里任何一层属性变了都会触发
// 效果等同于后端每次修改数据后自动 commit —— 这里是自动存 localStorage
watch(
  state,
  () => {
    localStorage.setItem(KEY, JSON.stringify(state))
  },
  { deep: true }
)

// 清空本地缓存并刷新页面（相当于“重置开发环境”）
export function resetLocalData() {
  localStorage.removeItem(KEY)
  location.reload()
}

// 向列表里插入或更新一条数据：先按 keyFn 找，找不到就插到头部（像 findAll 后手动 de-dup）
export function upsert(list, item, keyFn) {
  const idx = list.findIndex(keyFn)
  if (idx >= 0) list[idx] = { ...list[idx], ...item }
  else list.unshift(item)
}
