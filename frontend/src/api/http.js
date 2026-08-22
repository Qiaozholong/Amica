import { reactive } from 'vue'

// 所有请求的调试日志（Postman 替代品的核心：每次调用可回看请求/响应原文）
export const debugLogs = reactive([])

let seq = 0

/**
 * 统一请求封装：
 * - 后端 Result 包装 { code, message, data }，HTTP 恒为 200，以 body.code 判断业务成败
 * - 失败时抛出 Error(message)，同时始终记录到 debugLogs
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
    const resp = await fetch('/api' + path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    entry.status = resp.status
    const text = await resp.text()
    let json = null
    try {
      json = text ? JSON.parse(text) : null
    } catch {
      json = { raw: text }
    }
    entry.respBody = json
    entry.ms = Date.now() - start

    const code = json && typeof json.code === 'number' ? json.code : resp.status
    entry.ok = code >= 200 && code < 300
    if (!entry.ok) {
      throw new Error((json && json.message) || `请求失败 (HTTP ${resp.status})`)
    }
    return json?.data
  } catch (e) {
    entry.ms = Date.now() - start
    if (e instanceof TypeError) {
      throw new Error('无法连接后端服务，请确认 Spring Boot 已在 http://localhost:9000 启动')
    }
    throw e
  }
}

export const get = (path) => request('GET', path)
export const post = (path, body) => request('POST', path, body)
