import { reactive, watch } from 'vue'

// ============ 全局状态：只保留「登录态」 ============
// 变更说明：provider / model / assistant / conversation 不再缓存在 localStorage ——
// 后端已补齐列表查询接口，各页面改为挂载时从服务端拉取（数据以服务端为准）。
// localStorage 现在只负责一件事：刷新页面后仍然记得「我是谁」。

const KEY = 'amica-auth'
const saved = JSON.parse(localStorage.getItem(KEY) || '{}')

// 兼容旧缓存：在修复雪花 ID 精度问题（issue 29）之前，落盘的 id 是「数字」且已经丢过精度。
// 这种缓存必须作废，否则会把错的 userId 继续传下去 —— 直接当作未登录，要求重新登录。
const savedOk = !!saved.token && typeof saved.id === 'string'

export const state = reactive({
  // 当前登录用户（字段来自后端 AuthVo）
  id: savedOk ? saved.id : null,
  account: savedOk ? saved.account || '' : '',
  nickname: savedOk ? saved.nickname || '' : '',
  // JWT：http.js 会把它放进 Authorization: Bearer 头
  // 后端 JwtAuthenticationFilter 对 /auth/** 之外的所有路径都要求带它
  token: savedOk ? saved.token : '',
})

// 是否已登录
export function isLoggedIn() {
  return !!state.token
}

// 登录 / 注册成功后写入（参数就是后端返回的 AuthVo）
export function setAuth(vo) {
  state.id = vo?.id ?? null
  state.account = vo?.account || ''
  state.nickname = vo?.nickname || ''
  state.token = vo?.token || ''
}

// 退出登录：只清本地登录态（JWT 是无状态的，服务端没有「登出」接口）
export function clearAuth() {
  state.id = null
  state.account = ''
  state.nickname = ''
  state.token = ''
  localStorage.removeItem(KEY)
}

// 深度监听：登录态一变就落盘，所以刷新页面不会掉登录
watch(
  state,
  () => {
    localStorage.setItem(KEY, JSON.stringify(state))
  },
  { deep: true }
)
