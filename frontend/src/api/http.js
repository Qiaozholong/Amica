import { reactive } from 'vue'
import { state, clearAuth } from '../store'
// 长整数保护：雪花 ID 超出 JS 安全整数，必须在 JSON.parse 之前把长整数转成字符串（见 issue 29）
import { quoteBigInts } from './bigint'

// ============ 请求封装 ============
// 约定：后端业务错误走「HTTP 200 + body.code」，但 JWT 过滤器拦截时是「HTTP 401 + body.code=401」。
// 两种都要处理，且每次请求/响应原文都记进 debugLogs（调试页可回看）。

// 所有请求的调试日志（Postman 替代品的核心）
export const debugLogs = reactive([])

let seq = 0

// ---------- 长整数保护 ----------
// 实现已抽到 ./bigint.js（quoteBigInts），原因见 issue.md 问题 29：
// 雪花 ID 是 19 位，超出 Number.MAX_SAFE_INTEGER（16 位），JSON.parse 会四舍五入丢精度。

/**
 * 统一请求入口
 * @param method GET/POST
 * @param path   后端路径，如 /auth/login（带 /api 前缀，由 vite 代理转发到 :9000）
 * @param body   请求体对象，自动 JSON.stringify
 * @returns 后端 Result 里的 data 字段
 */
export async function request(method, path, body) {
  const entry = {
    id: ++seq,
    time: new Date().toLocaleTimeString(),
    method,
    path,
    body,
    status: null,
    respBody: null,
    ok: false,
    ms: 0,
    expanded: false,
  }
  debugLogs.unshift(entry)
  const start = Date.now()
  try {
    const headers = { 'Content-Type': 'application/json' }
    // 带上 JWT —— 少了这个头，除 /auth/** 之外的接口一律 401
    if (state.token) headers.Authorization = `Bearer ${state.token}`

    const resp = await fetch('/api' + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    entry.status = resp.status
    const text = await resp.text()
    // 响应体尝试解析成 JSON；解析失败（如代理返回 HTML 错误页）就用 {raw: 原文} 兜底
    let json = null
    try {
      json = text ? JSON.parse(quoteBigInts(text)) : null
    } catch {
      json = { raw: text }
    }
    entry.respBody = json
    entry.ms = Date.now() - start

    // 业务成败：优先看 body.code，取不到才看 HTTP 状态码
    const code = json && typeof json.code === 'number' ? json.code : resp.status
    entry.ok = code >= 200 && code < 300
    if (!entry.ok) {
      // 401 = token 缺失/过期/无效 → 清掉本地登录态，让界面回到未登录
      if (code === 401 || resp.status === 401) {
        clearAuth()
        throw new Error((json && json.message) || '登录已失效，请重新登录')
      }
      throw new Error((json && json.message) || `请求失败 (HTTP ${resp.status})`)
    }
    return json?.data
  } catch (e) {
    entry.ms = Date.now() - start
    // TypeError 一般是连不上后端（网络错误）
    if (e instanceof TypeError) {
      throw new Error('无法连接后端服务，请确认 Spring Boot 已在 http://localhost:9000 启动')
    }
    throw e
  }
}

// 快捷方法：GET 不传 body
export const get = (path) => request('GET', path)
export const post = (path, body) => request('POST', path, body)
