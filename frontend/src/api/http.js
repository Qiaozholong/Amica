import { reactive } from 'vue'

// ============ 请求封装（本文件是前端“最像你会的 Java”的部分，建议第一个看）============
// 对应后端 GlobalExceptionHandler 那套约定：HTTP 恒为 200，业务成败看响应体里的 code
// 全部接口只用 fetch（浏览器的发请求方法，等价于 HttpClient）+ async/await（等价于同步阻塞调用）
// 另外顺带做了“调试日志”：每一次请求的原文都记录到 debugLogs，调试页直接展示

// 所有请求的调试日志（Postman 替代品的核心：每次调用可回看请求/响应原文）
export const debugLogs = reactive([])

let seq = 0

/**
 * 统一请求入口
 * @param method GET/POST
 * @param path   后端路径，如 /auth/login（带 /api 前缀，由 vite 代理转发到 :9000）
 * @param body   请求体对象，会自动 JSON.stringify（等价于后端 @RequestBody 接收的 JSON）
 * @returns 后端 Result 里的 data 字段
 */
export async function request(method, path, body) {
  // 日志条目：先用“草稿”结构占位，请求结束后把结果填回去（unshift 插到列表最前，最新的在上面）
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
    // await 挂起当前“线程”（其实是协程），等 fetch 返回后再继续，写法上像同步代码
    const resp = await fetch('/api' + path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    entry.status = resp.status
    const text = await resp.text()
    // 响应体尝试解析成 JSON；解析失败（如代理返回 HTML 错误页）就用 {raw: 原文} 兜底，不抛异常
    let json = null
    try {
      json = text ? JSON.parse(text) : null
    } catch {
      json = { raw: text }
    }
    entry.respBody = json
    entry.ms = Date.now() - start

    // 判断业务成败：后端 HTTP 恒 200，所以优先看响应体里的 code，取不到才看 HTTP 状态码
    const code = json && typeof json.code === 'number' ? json.code : resp.status
    entry.ok = code >= 200 && code < 300
    if (!entry.ok) {
      // 失败抛 Error，message 用后端返回的 message，页面 catch 后直接展示
      throw new Error((json && json.message) || `请求失败 (HTTP ${resp.status})`)
    }
    return json?.data
  } catch (e) {
    entry.ms = Date.now() - start
    // TypeError 一般是连不上后端（网络错误），给一个更友好的提示
    if (e instanceof TypeError) {
      throw new Error('无法连接后端服务，请确认 Spring Boot 已在 http://localhost:9000 启动')
    }
    throw e
  }
}

// 快捷方法：GET 不传 body
export const get = (path) => request('GET', path)
export const post = (path, body) => request('POST', path, body)
